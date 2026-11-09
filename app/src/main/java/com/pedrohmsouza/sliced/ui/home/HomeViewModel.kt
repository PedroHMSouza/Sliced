package com.pedrohmsouza.sliced.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pedrohmsouza.sliced.data.DraftRepository
import com.pedrohmsouza.sliced.data.SessionRepository
import com.pedrohmsouza.sliced.domain.Fmt
import com.pedrohmsouza.sliced.domain.WorkoutRules
import com.pedrohmsouza.sliced.model.ProgramSource
import com.pedrohmsouza.sliced.model.Session
import com.pedrohmsouza.sliced.model.Treino
import com.pedrohmsouza.sliced.ui.container
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TreinoItem(val treino: Treino, val lastLabel: String?)
data class DraftBanner(val treinoId: String, val startedAt: String)

/** Tudo que a tela Treinos precisa para se desenhar, já calculado. */
data class HomeUiState(
    val loading: Boolean = true,
    val week: WorkoutRules.Week = WorkoutRules.Week(emptyList(), 0),
    val next: String = "A",
    val treinos: List<TreinoItem> = emptyList(),
    val draft: DraftBanner? = null,
)

/**
 * ViewModel: sobrevive a rotações de tela e guarda o estado da tela. Ele combina três
 * fluxos (ficha + histórico + rascunho) num único [HomeUiState]. A tela só observa `uiState`
 * e chama funções; ela não sabe de onde vêm os dados ("fluxo de dados unidirecional").
 */
class HomeViewModel(
    private val programs: Flow<ProgramSource>,
    private val sessionRepo: SessionRepository,
    private val draftRepo: DraftRepository,
    private val now: () -> Long,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(programs, sessionRepo.sessions, draftRepo.draft) { program, sessions, draft ->
        buildState(program, sessions, draft?.let { DraftBanner(it.treino, Fmt.hhmm(it.startedAt)) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun buildState(program: ProgramSource, sessions: List<Session>, draft: DraftBanner?): HomeUiState {
        val today = LocalDate.now()
        return HomeUiState(
            loading = false,
            week = WorkoutRules.week(sessions, today),
            next = WorkoutRules.nextTreino(sessions, today, program),
            treinos = program.treinos.map { t ->
                TreinoItem(t, WorkoutRules.lastOf(sessions, t.id)?.let { Fmt.dateLabel(it.startedAt) })
            },
            draft = draft,
        )
    }

    /** Cria o rascunho e só então avisa a tela para navegar. */
    fun start(treinoId: String, onStarted: () -> Unit) {
        viewModelScope.launch {
            if (draftRepo.draft.first() == null) {
                val treino = programs.first().treino(treinoId) ?: return@launch
                draftRepo.save(WorkoutRules.newDraft(treino, sessionRepo.sessions.first(), now()))
            }
            onStarted()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this.container
                HomeViewModel(c.programs.program, c.sessions, c.drafts, c.now)
            }
        }
    }
}
