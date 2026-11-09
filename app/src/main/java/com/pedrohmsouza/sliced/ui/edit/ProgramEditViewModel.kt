package com.pedrohmsouza.sliced.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pedrohmsouza.sliced.R
import com.pedrohmsouza.sliced.data.ProgramRepository
import com.pedrohmsouza.sliced.domain.ProgramRules
import com.pedrohmsouza.sliced.model.Program
import com.pedrohmsouza.sliced.model.Treino
import com.pedrohmsouza.sliced.ui.UserMessages
import com.pedrohmsouza.sliced.ui.container
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProgramEditUiState(
    val loading: Boolean = true,
    val treinos: List<Treino> = emptyList(),
    val selected: String = "A",
) {
    val treino: Treino? get() = treinos.firstOrNull { it.id == selected }
}

/** Tela "Editar ficha": nome e dia de cada treino, ordem dos exercícios. Cada mudança é gravada na hora. */
class ProgramEditViewModel(
    initialTreino: String,
    private val repo: ProgramRepository,
    private val messages: UserMessages,
    /** Escopo do app: uma gravação não é cancelada se a pessoa sair da tela logo depois. */
    private val appScope: CoroutineScope,
) : ViewModel() {

    private val selected = MutableStateFlow(initialTreino)

    val uiState: StateFlow<ProgramEditUiState> = combine(repo.program, selected) { p, sel ->
        ProgramEditUiState(loading = false, treinos = p.treinos, selected = sel.takeIf { id -> p.treinos.any { it.id == id } } ?: p.order.first())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgramEditUiState())

    fun select(treinoId: String) { selected.value = treinoId }

    fun rename(name: String) = edit { ProgramRules.updateTreino(it, selected.value) { t -> t.copy(name = name.take(40)) } }

    fun setDay(day: String) = edit { ProgramRules.updateTreino(it, selected.value) { t -> t.copy(day = day) } }

    fun move(exerciseId: String, delta: Int) = edit { ProgramRules.moveExercise(it, selected.value, exerciseId, delta) }

    private fun edit(transform: (Program) -> Program) {
        appScope.launch {
            if (runCatching { repo.update(transform) }.isFailure) messages.show(R.string.msg_program_save_failed)
        }
    }

    companion object {
        fun factory(treinoId: String) = viewModelFactory {
            initializer {
                val c = this.container
                ProgramEditViewModel(treinoId, c.programs, c.messages, c.appScope)
            }
        }
    }
}
