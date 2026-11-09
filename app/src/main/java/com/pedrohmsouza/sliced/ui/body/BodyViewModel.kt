package com.pedrohmsouza.sliced.ui.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pedrohmsouza.sliced.data.SessionRepository
import com.pedrohmsouza.sliced.domain.BodyRules
import com.pedrohmsouza.sliced.model.Muscle
import com.pedrohmsouza.sliced.model.ProgramSource
import com.pedrohmsouza.sliced.ui.container
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

data class BodyUiState(
    val loading: Boolean = true,
    val week: BodyRules.Week? = null,
    /** 0 = esta semana, −1 = a anterior... */
    val offset: Int = 0,
    /** Músculos treinados, do mais para o menos trabalhado. */
    val trained: List<Pair<Muscle, Double>> = emptyList(),
    val untrained: List<Muscle> = emptyList(),
) {
    fun level(m: Muscle): Int = BodyRules.level(week?.sets?.get(m) ?: 0.0)
}

class BodyViewModel(
    programs: Flow<ProgramSource>,
    sessionRepo: SessionRepository,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val offset = MutableStateFlow(0)

    val uiState: StateFlow<BodyUiState> = combine(programs, sessionRepo.sessions, offset) { program, sessions, off ->
        val week = BodyRules.week(sessions, program, today(), off)
        val trained = week.sets.entries.filter { it.value > 0 }.sortedByDescending { it.value }.map { it.key to it.value }
        BodyUiState(
            loading = false,
            week = week,
            offset = off,
            trained = trained,
            untrained = Muscle.entries.filter { m -> trained.none { it.first == m } && m != Muscle.NECK },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BodyUiState())

    /** −1 = semana anterior, +1 = próxima (até a semana atual). */
    fun shiftWeek(delta: Int) = offset.update { (it + delta).coerceAtMost(0) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = this.container
                BodyViewModel(c.programs.program, c.sessions)
            }
        }
    }
}
