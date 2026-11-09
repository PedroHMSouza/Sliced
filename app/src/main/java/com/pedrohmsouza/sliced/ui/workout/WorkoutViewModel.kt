package com.pedrohmsouza.sliced.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pedrohmsouza.sliced.R
import com.pedrohmsouza.sliced.data.DraftRepository
import com.pedrohmsouza.sliced.data.SessionRepository
import com.pedrohmsouza.sliced.domain.Fmt
import com.pedrohmsouza.sliced.domain.WorkoutRules
import com.pedrohmsouza.sliced.domain.WorkoutRules.updateExercise
import com.pedrohmsouza.sliced.domain.WorkoutRules.updateSet
import com.pedrohmsouza.sliced.model.Draft
import com.pedrohmsouza.sliced.model.DraftSet
import com.pedrohmsouza.sliced.model.FieldKey
import com.pedrohmsouza.sliced.model.LoggedSet
import com.pedrohmsouza.sliced.model.PlannedExercise
import com.pedrohmsouza.sliced.model.ProgramSource
import com.pedrohmsouza.sliced.model.Session
import com.pedrohmsouza.sliced.model.Treino
import com.pedrohmsouza.sliced.ui.UserMessages
import com.pedrohmsouza.sliced.ui.container
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Dados de um exercício para a tela: o plano da ficha + o que foi feito da última vez. */
data class ExerciseUi(
    val planned: PlannedExercise?,
    val previous: List<LoggedSet>,
    val canIncrease: Boolean,
)

data class WorkoutUiState(
    val loading: Boolean = true,
    val draft: Draft? = null,
    val treino: Treino? = null,
    val exercises: List<ExerciseUi> = emptyList(),
    val progress: WorkoutRules.Progress = WorkoutRules.Progress(0, 0, 0.0),
    val showFinish: Boolean = false,
    val saving: Boolean = false,
    /** O treino acabou de ser salvo/descartado (a navegação vem pelo evento, não pelo "sem rascunho"). */
    val closed: Boolean = false,
)

sealed interface WorkoutEvent {
    /** Série sem nenhum valor: a tela foca o primeiro campo vazio. */
    data class FocusField(val exerciseIndex: Int, val setIndex: Int, val key: FieldKey) : WorkoutEvent
    data class Saved(val sessionId: String) : WorkoutEvent
    data object Closed : WorkoutEvent
}

/** Controle do timer de descanso (implementado por RestTimer). Interface = fácil de testar. */
interface RestControl {
    fun start(seconds: Int, label: String)
    fun stop()
}

class WorkoutViewModel(
    private val program: ProgramSource,
    private val sessionRepo: SessionRepository,
    private val draftRepo: DraftRepository,
    private val messages: UserMessages,
    private val rest: RestControl,
    private val now: () -> Long,
    /** Escopo do app: as gravações terminam mesmo que a tela feche no meio. */
    private val appScope: CoroutineScope,
) : ViewModel() {

    /** Fonte da verdade do treino em andamento enquanto a tela está aberta. */
    private val draft = MutableStateFlow<Draft?>(null)
    private val loaded = MutableStateFlow(false)
    private data class Flags(val sheet: Boolean = false, val saving: Boolean = false, val closed: Boolean = false)
    private val flags = MutableStateFlow(Flags())

    private val sessions: StateFlow<List<Session>> =
        sessionRepo.sessions.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val events = Channel<WorkoutEvent>(Channel.BUFFERED)
    val eventFlow: Flow<WorkoutEvent> = events.receiveAsFlow()

    /**
     * Fila de gravação do rascunho. CONFLATED guarda só o pedido mais recente: se a pessoa
     * digitar rápido, gravamos o estado final sem enfileirar dezenas de escritas. Um único
     * consumidor garante a ordem (um "salvar" antigo nunca sobrescreve um mais novo).
     */
    private sealed interface Write { data class Save(val d: Draft) : Write; data object Clear : Write }
    private val writes = Channel<Write>(Channel.CONFLATED)

    init {
        appScope.launch {
            for (w in writes) when (w) {
                is Write.Save -> draftRepo.save(w.d)
                Write.Clear -> draftRepo.clear()
            }
        }
        viewModelScope.launch {
            draft.value = draftRepo.draft.first()
            loaded.value = true
        }
    }

    val uiState: StateFlow<WorkoutUiState> = combine(draft, sessions, loaded, flags) { d, s, isLoaded, f ->
        if (d == null) return@combine WorkoutUiState(loading = !isLoaded, closed = f.closed)
        WorkoutUiState(
            loading = false,
            draft = d,
            treino = program.treino(d.treino),
            exercises = d.exercises.map { e ->
                val planned = program.exercise(e.exerciseId)
                ExerciseUi(
                    planned = planned,
                    previous = WorkoutRules.previousSets(s, e.exerciseId).orEmpty(),
                    canIncrease = planned != null && WorkoutRules.canIncrease(planned, s),
                )
            },
            progress = WorkoutRules.progress(d),
            showFinish = f.sheet,
            saving = f.saving,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState())

    private fun edit(transform: (Draft) -> Draft) {
        val current = draft.value ?: return
        val next = transform(current).copy(updatedAt = now())
        draft.value = next
        writes.trySend(Write.Save(next))
    }

    fun onFieldChange(exerciseIndex: Int, setIndex: Int, key: FieldKey, raw: String) {
        val clean = Fmt.sanitize(raw, key)
        edit { it.updateSet(exerciseIndex, setIndex) { s -> s.copy(values = s.values + (key to clean)) } }
    }

    fun onNoteChange(exerciseIndex: Int, text: String) =
        edit { it.updateExercise(exerciseIndex) { e -> e.copy(note = text.take(200)) } }

    fun onToggleSet(exerciseIndex: Int, setIndex: Int) {
        val d = draft.value ?: return
        when (val r = WorkoutRules.toggleSet(d, exerciseIndex, setIndex, sessions.value, program)) {
            is WorkoutRules.ToggleResult.Missing -> {
                messages.show(R.string.msg_fill_fields, r.fields.joinToString(" e ") { it.label })
                events.trySend(WorkoutEvent.FocusField(exerciseIndex, setIndex, r.fields.first()))
            }
            is WorkoutRules.ToggleResult.Updated -> {
                edit { r.draft }
                if (r.restSec > 0) rest.start(r.restSec, r.exerciseName)
            }
        }
    }

    fun onAddSet(exerciseIndex: Int) = edit { it.updateExercise(exerciseIndex) { e -> e.copy(sets = e.sets + DraftSet()) } }

    fun onRemoveSet(exerciseIndex: Int) = edit {
        it.updateExercise(exerciseIndex) { e -> if (e.sets.size > 1) e.copy(sets = e.sets.dropLast(1)) else e }
    }

    fun onStartRest(exerciseIndex: Int) {
        val e = draft.value?.exercises?.getOrNull(exerciseIndex) ?: return
        val sec = program.exercise(e.exerciseId)?.restSec ?: 0
        if (sec > 0) rest.start(sec, e.name)
    }

    fun onOpenFinish() = flags.update { it.copy(sheet = true) }
    fun onCloseFinish() = flags.update { it.copy(sheet = false) }

    /** Salva no histórico; só depois apaga o rascunho (nada se perde se der erro). */
    fun onSave() {
        val d = draft.value ?: return
        if (flags.value.saving || WorkoutRules.progress(d).done == 0) return
        flags.update { it.copy(saving = true) }
        viewModelScope.launch {
            val session = WorkoutRules.buildSession(d, now(), program)
            val ok = runCatching { sessionRepo.save(session) }.isSuccess
            flags.update { it.copy(saving = false) }
            if (!ok) {
                messages.show(R.string.msg_save_failed)
                return@launch
            }
            closeDraft()
            messages.show(R.string.msg_saved, session.treino, session.durationMin)
            events.trySend(WorkoutEvent.Saved(session.id))
        }
    }

    fun onDiscard() {
        closeDraft()
        messages.show(R.string.msg_discarded)
        events.trySend(WorkoutEvent.Closed)
    }

    private fun closeDraft() {
        flags.value = Flags(closed = true)
        draft.value = null
        writes.trySend(Write.Clear)
        rest.stop()
    }

    override fun onCleared() {
        // Fecha a fila: o consumidor (no escopo do app) ainda grava o último pedido e termina.
        writes.close()
    }

    companion object {
        fun factory(rest: RestControl) = viewModelFactory {
            initializer {
                val c = this.container
                WorkoutViewModel(c.program, c.sessions, c.drafts, c.messages, rest, c.now, c.appScope)
            }
        }
    }
}
