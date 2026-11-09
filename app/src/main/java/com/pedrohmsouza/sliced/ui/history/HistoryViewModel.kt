package com.pedrohmsouza.sliced.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pedrohmsouza.sliced.R
import com.pedrohmsouza.sliced.data.SessionRepository
import com.pedrohmsouza.sliced.domain.Fmt
import com.pedrohmsouza.sliced.domain.WorkoutRules
import com.pedrohmsouza.sliced.model.ProgramSource
import com.pedrohmsouza.sliced.model.Session
import com.pedrohmsouza.sliced.ui.UserMessages
import com.pedrohmsouza.sliced.ui.container
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class SessionItem(
    val session: Session,
    val name: String,
    val dateLabel: String,
    val sets: Int,
    val volumeKg: Double,
)

data class ChartUi(
    val options: List<WorkoutRules.ChartOption>,
    val selected: WorkoutRules.ChartOption,
    val points: List<WorkoutRules.ChartPoint>,
    val best: WorkoutRules.ChartPoint?,
    /** Variação de carga entre o primeiro e o último ponto (null com menos de 2 pontos). */
    val deltaKg: Double?,
)

data class HistoryUiState(
    val loading: Boolean = true,
    val week: WorkoutRules.Week = WorkoutRules.Week(emptyList(), 0),
    val stats: WorkoutRules.Stats = WorkoutRules.Stats(0, 0, null),
    val month: WorkoutRules.Month? = null,
    /** Dá para avançar o mês (não passa do mês atual). */
    val canNextMonth: Boolean = false,
    val items: List<SessionItem> = emptyList(),
    val chart: ChartUi? = null,
)

class HistoryViewModel(
    programs: Flow<ProgramSource>,
    private val sessionRepo: SessionRepository,
    private val messages: UserMessages,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val chartExercise = MutableStateFlow<String?>(null)

    /** Meses antes do atual que o calendário está mostrando (0 = este mês). */
    private val monthsBack = MutableStateFlow(0)

    val uiState: StateFlow<HistoryUiState> = combine(programs, sessionRepo.sessions, chartExercise, monthsBack) { program, sessions, ex, back ->
        val today = today()
        HistoryUiState(
            loading = false,
            week = WorkoutRules.week(sessions, today),
            stats = WorkoutRules.stats(sessions, today),
            month = WorkoutRules.month(sessions, YearMonth.from(today).minusMonths(back.toLong()), today, program),
            canNextMonth = back > 0,
            items = sessions.map { s ->
                SessionItem(
                    session = s,
                    name = program.treino(s.treino)?.name ?: s.title,
                    dateLabel = Fmt.dateLabel(s.startedAt),
                    sets = s.exercises.sumOf { it.sets.size },
                    volumeKg = WorkoutRules.volume(s),
                )
            },
            chart = chart(program, sessions, ex),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    private fun chart(program: ProgramSource, sessions: List<Session>, exerciseId: String?): ChartUi? {
        val options = WorkoutRules.chartOptions(sessions, program)
        if (options.isEmpty()) return null
        val selected = options.firstOrNull { it.exercise.id == exerciseId } ?: options.first()
        val points = WorkoutRules.chartPoints(sessions, selected.exercise.id)
        return ChartUi(
            options = options,
            selected = selected,
            points = points,
            best = WorkoutRules.bestPoint(points),
            deltaKg = if (points.size > 1) points.last().kg - points.first().kg else null,
        )
    }

    fun selectChartExercise(id: String) { chartExercise.value = id }

    /** −1 = mês anterior, +1 = próximo (até o mês atual). */
    fun shiftMonth(delta: Int) = monthsBack.update { (it - delta).coerceAtLeast(0) }

    fun delete(id: String) {
        viewModelScope.launch {
            val ok = runCatching { sessionRepo.delete(id) }.isSuccess
            messages.show(if (ok) R.string.msg_deleted else R.string.msg_delete_failed)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this.container
                HistoryViewModel(c.programs.program, c.sessions, c.messages)
            }
        }
    }
}
