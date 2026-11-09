package com.pedrohmsouza.sliced.ui.edit

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pedrohmsouza.sliced.R
import com.pedrohmsouza.sliced.data.MediaFiles
import com.pedrohmsouza.sliced.data.ProgramRepository
import com.pedrohmsouza.sliced.domain.ProgramRules
import com.pedrohmsouza.sliced.model.LibraryExercise
import com.pedrohmsouza.sliced.model.Muscle
import com.pedrohmsouza.sliced.model.PlannedExercise
import com.pedrohmsouza.sliced.model.RecordType
import com.pedrohmsouza.sliced.model.toProgram
import com.pedrohmsouza.sliced.ui.UserMessages
import com.pedrohmsouza.sliced.ui.container
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * O que está sendo digitado no formulário. Números de reps ficam como texto para a tela
 * não "corrigir" a digitação (mesma ideia do rascunho do treino).
 */
data class ExerciseForm(
    val name: String = "",
    val type: RecordType = RecordType.WR,
    val sets: Int = 3,
    val repMin: String = "8",
    val repMax: String = "12",
    val restSec: Int = 60,
    val target: String = "",
    val tip: String = "",
    val libraryId: String? = null,
    val media: String? = null,
    val primary: List<Muscle> = emptyList(),
    val secondary: List<Muscle> = emptyList(),
) {
    val usesReps: Boolean get() = type == RecordType.WR || type == RecordType.R

    /** O exercício como vai para a ficha. */
    fun toExercise(id: String): PlannedExercise {
        val lo = repMin.toIntOrNull(); val hi = repMax.toIntOrNull()
        val range = if (usesReps && lo != null && hi != null && lo > 0 && hi > 0) minOf(lo, hi)..maxOf(lo, hi) else null
        return PlannedExercise(
            id = id, name = name.trim(), sets = sets, repRange = range, restSec = restSec, type = type,
            tip = tip.trim(), target = target.trim().ifEmpty { null }, libraryId = libraryId, media = media,
            primary = primary, secondary = secondary - primary.toSet(),
        )
    }

    companion object {
        fun of(e: PlannedExercise) = ExerciseForm(
            name = e.name, type = e.type, sets = e.sets,
            repMin = e.repRange?.first?.toString().orEmpty(), repMax = e.repRange?.last?.toString().orEmpty(),
            restSec = e.restSec, target = e.target.orEmpty(), tip = e.tip, libraryId = e.libraryId, media = e.media,
            primary = e.primary, secondary = e.secondary,
        )
    }
}

data class ExerciseEditUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val treinoId: String = "",
    val form: ExerciseForm = ExerciseForm(),
)

sealed interface ExerciseEditEvent { data object Done : ExerciseEditEvent }

class ExerciseEditViewModel(
    private val treinoId: String,
    private val exerciseId: String?,
    private val repo: ProgramRepository,
    private val mediaFiles: MediaFiles,
    private val messages: UserMessages,
    private val appScope: CoroutineScope,
) : ViewModel() {

    private val state = MutableStateFlow(ExerciseEditUiState(treinoId = treinoId))
    val uiState: StateFlow<ExerciseEditUiState> = state

    private val events = Channel<ExerciseEditEvent>(Channel.BUFFERED)
    val eventFlow: Flow<ExerciseEditEvent> = events.receiveAsFlow()

    /** Foto que o exercício tinha ao abrir a tela e cópias feitas aqui (para apagar as que sobrarem). */
    private var originalMedia: String? = null
    private val imported = mutableListOf<String>()
    private var finished = false

    init {
        viewModelScope.launch {
            val existing = exerciseId?.let { id -> repo.program.first().treino(treinoId)?.exercises?.firstOrNull { it.id == id } }
            originalMedia = existing?.media
            state.value = ExerciseEditUiState(
                loading = false, isNew = existing == null, treinoId = treinoId,
                form = existing?.let(ExerciseForm::of) ?: ExerciseForm(),
            )
        }
    }

    private fun form(f: (ExerciseForm) -> ExerciseForm) = state.update { it.copy(form = f(it.form)) }

    fun setName(v: String) = form { it.copy(name = v.take(60)) }
    fun setType(v: RecordType) = form { it.copy(type = v) }
    fun changeSets(delta: Int) = form { it.copy(sets = (it.sets + delta).coerceIn(1, 10)) }
    fun setRepMin(v: String) = form { it.copy(repMin = v.filter(Char::isDigit).take(3)) }
    fun setRepMax(v: String) = form { it.copy(repMax = v.filter(Char::isDigit).take(3)) }
    fun changeRest(delta: Int) = form { it.copy(restSec = (it.restSec + delta).coerceIn(0, 600)) }
    fun setTarget(v: String) = form { it.copy(target = v.take(60)) }
    fun setTip(v: String) = form { it.copy(tip = v.take(300)) }

    /** Toca num músculo: principal ↔ secundário são exclusivos. */
    fun toggleMuscle(m: Muscle, primary: Boolean) = form { f ->
        if (primary) f.copy(primary = f.primary.toggle(m), secondary = f.secondary - m)
        else f.copy(secondary = f.secondary.toggle(m), primary = f.primary - m)
    }

    /** Usa as fotos e os músculos do catálogo; o nome e o tipo só se ainda não houver nome. */
    fun pickLibrary(lib: LibraryExercise) = form { f ->
        val fresh = f.name.isBlank()
        f.copy(
            libraryId = lib.id, media = null, primary = lib.primary, secondary = lib.secondary,
            name = if (fresh) lib.name else f.name,
            type = if (fresh) lib.type else f.type,
        )
    }

    fun pickMedia(uri: Uri) {
        viewModelScope.launch {
            val path = mediaFiles.import(uri)
            if (path == null) { messages.show(R.string.msg_media_failed); return@launch }
            imported += path
            form { it.copy(media = path) }
        }
    }

    fun clearMedia() = form { it.copy(media = null) }

    fun save() {
        val s = state.value
        if (s.loading || finished) return
        if (s.form.name.isBlank()) { messages.show(R.string.msg_name_required); return }
        finished = true
        appScope.launch {
            val ok = runCatching {
                repo.update { p ->
                    val id = exerciseId ?: ProgramRules.newExerciseId(s.form.name, p.treinos.flatMap { t -> t.exercises.map { it.id } }.toSet())
                    ProgramRules.saveExercise(p.toProgram(), treinoId, s.form.toExercise(id))
                }
            }.isSuccess
            if (!ok) { finished = false; messages.show(R.string.msg_program_save_failed); return@launch }
            cleanUp(keep = s.form.media)
            messages.show(R.string.msg_exercise_saved)
            events.trySend(ExerciseEditEvent.Done)
        }
    }

    fun delete() {
        val id = exerciseId ?: return
        if (finished) return
        finished = true
        appScope.launch {
            val ok = runCatching { repo.update { ProgramRules.removeExercise(it, treinoId, id) } }.isSuccess
            if (!ok) { finished = false; messages.show(R.string.msg_program_save_failed); return@launch }
            cleanUp(keep = null)
            messages.show(R.string.msg_exercise_removed)
            events.trySend(ExerciseEditEvent.Done)
        }
    }

    /** Apaga as cópias de fotos que não ficaram na ficha. */
    private suspend fun cleanUp(keep: String?) {
        (imported + listOfNotNull(originalMedia)).filter { it != keep }.forEach { mediaFiles.delete(it) }
        imported.clear()
    }

    override fun onCleared() {
        // Saiu sem salvar: as fotos escolhidas aqui não são usadas por ninguém.
        if (!finished && imported.isNotEmpty()) {
            val leftovers = imported.toList()
            appScope.launch { leftovers.forEach { mediaFiles.delete(it) } }
        }
    }

    companion object {
        fun factory(treinoId: String, exerciseId: String?) = viewModelFactory {
            initializer {
                val c = this.container
                ExerciseEditViewModel(treinoId, exerciseId, c.programs, c.media, c.messages, c.appScope)
            }
        }
    }
}

private fun List<Muscle>.toggle(m: Muscle) = if (m in this) this - m else this + m
