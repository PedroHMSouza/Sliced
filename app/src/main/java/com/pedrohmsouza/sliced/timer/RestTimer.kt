package com.pedrohmsouza.sliced.timer

import android.content.SharedPreferences
import androidx.core.content.edit
import com.pedrohmsouza.sliced.ui.workout.RestControl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max

/** Estado do descanso. A contagem é sempre calculada a partir de [endAt] (timestamp de término). */
data class RestState(
    val endAt: Long,
    val totalSec: Int,
    val label: String,
    val finished: Boolean = false,
) {
    fun secondsLeft(now: Long): Int = max(0, ceil((endAt - now) / 1000.0).toInt())
}

/** Efeitos colaterais do timer, separados para os testes trocarem por versões falsas. */
interface RestEffects {
    fun scheduleAlarm(endAt: Long, label: String)
    fun cancelAlarm()
    fun showCountdown(state: RestState)
    fun showDone(label: String)
    fun cancelNotifications()
    /** Som + vibração com o app aberto. */
    fun alertInApp()
}

/**
 * Timer de descanso.
 *
 * Por que timestamp e não "contar segundos"? Um contador que subtrai 1 a cada segundo
 * atrasa ou para quando o Android suspende o app (tela apagada). Guardando o horário de
 * término, a conta "quanto falta" está sempre certa, não importa quanto tempo o app ficou
 * parado.
 *
 * Como o aviso chega com a tela apagada:
 * - um alarme exato do sistema ([RestEffects.scheduleAlarm]) acorda o app no horário de
 *   término, mesmo em modo Doze, e o [RestAlarmReceiver] chama [onAlarm];
 * - com o app em segundo plano, uma notificação mostra a contagem (o próprio sistema
 *   desenha o cronômetro regressivo, sem o app precisar atualizar nada);
 * - com o app aberto, toca o bipe e vibra direto.
 *
 * Vive no AppContainer (um por processo) e roda na thread principal.
 */
class RestTimer(
    private val scope: CoroutineScope,
    private val effects: RestEffects,
    private val prefs: SharedPreferences?,
    private val now: () -> Long,
) : RestControl {

    private val _state = MutableStateFlow(restore())
    val state: StateFlow<RestState?> = _state

    @Volatile var isForeground: Boolean = false
        private set

    private var endJob: Job? = null
    private var hideJob: Job? = null
    private var alertedFor = 0L

    init {
        _state.value?.let { armEnd(it) }
    }

    override fun start(seconds: Int, label: String) {
        if (seconds <= 0) return
        set(RestState(now() + seconds * 1000L, seconds, label))
    }

    /** −15 / +15. Como na versão web, o total da barra cresce se o tempo restante passar dele. */
    fun adjust(deltaSec: Int) {
        val s = _state.value ?: return
        val base = if (s.finished) now() else s.endAt
        val end = base + deltaSec * 1000L
        val left = ceil((end - now()) / 1000.0).toInt()
        set(RestState(end, max(s.totalSec, left), s.label))
    }

    override fun stop() {
        endJob?.cancel(); hideJob?.cancel()
        _state.value = null
        persist(null)
        effects.cancelAlarm()
        effects.cancelNotifications()
    }

    /** Chamado pelo alarme do sistema. Ignora alarmes antigos (de um descanso já alterado). */
    fun onAlarm(endAt: Long) {
        val s = _state.value ?: return
        if (s.endAt == endAt) finish(s)
    }

    fun onAppForeground() {
        isForeground = true
        effects.cancelNotifications()
    }

    fun onAppBackground() {
        isForeground = false
        val s = _state.value ?: return
        if (!s.finished) effects.showCountdown(s)
    }

    private fun set(s: RestState) {
        hideJob?.cancel()
        _state.value = s
        persist(s)
        effects.scheduleAlarm(s.endAt, s.label)
        if (!isForeground) effects.showCountdown(s)
        armEnd(s)
    }

    /** Com o processo vivo, também acompanhamos o fim por corrotina (não depende do alarme). */
    private fun armEnd(s: RestState) {
        endJob?.cancel()
        endJob = scope.launch {
            delay(max(0L, s.endAt - now()))
            finish(s)
        }
    }

    private fun finish(s: RestState) {
        if (alertedFor == s.endAt) return // alarme e corrotina podem chegar juntos: avisa uma vez só
        alertedFor = s.endAt
        endJob?.cancel()
        val done = s.copy(finished = true)
        _state.value = done
        effects.cancelAlarm()
        if (isForeground) {
            effects.cancelNotifications()
            effects.alertInApp()
        } else {
            effects.showDone(s.label)
        }
        // A barra some sozinha ~6 s depois (a notificação de "acabou" continua lá).
        hideJob = scope.launch {
            delay(HIDE_AFTER_MS)
            if (_state.value == done) {
                _state.value = null
                persist(null)
            }
        }
    }

    private fun persist(s: RestState?) = prefs?.edit {
        if (s == null) clear() else putLong(K_END, s.endAt).putInt(K_TOTAL, s.totalSec).putString(K_LABEL, s.label)
    }

    /** Se o processo morreu durante o descanso, recupera o timer (se ainda faz sentido). */
    private fun restore(): RestState? {
        val p = prefs ?: return null
        val end = p.getLong(K_END, 0L)
        if (end == 0L || end < now() - HIDE_AFTER_MS) return null
        return RestState(end, p.getInt(K_TOTAL, 0), p.getString(K_LABEL, "").orEmpty())
    }

    companion object {
        const val HIDE_AFTER_MS = 6_000L
        private const val K_END = "endAt"
        private const val K_TOTAL = "total"
        private const val K_LABEL = "label"
    }
}
