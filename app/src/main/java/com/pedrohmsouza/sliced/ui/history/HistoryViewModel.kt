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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

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
    val items: List<SessionItem> = emptyList(),
    val chart: ChartUi? = null,
)

class HistoryViewModel(
    private val program: ProgramSource,
    private val sessionRepo: SessionRepository,
    private val messages: UserMessages,
) : ViewModel() {

    private val chartExercise = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(sessionRepo.sessions, chartExercise) { sessions, ex ->
        val today = LocalDate.now()
        HistoryUiState(
            loading = false,
            week = WorkoutRules.week(sessions, today),
            stats = WorkoutRules.stats(sessions, today),
            items = sessions.map { s ->
                SessionItem(
                    session = s,
                    name = program.treino(s.treino)?.name ?: s.title,
                    dateLabel = Fmt.dateLabel(s.startedAt),
                    sets = s.exercises.sumOf { it.sets.size },
                    volumeKg = WorkoutRules.volume(s),
                )
            },
            chart = chart(sessions, ex),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    private fun chart(sessions: List<Session>, exerciseId: String?): ChartUi? {
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
                HistoryViewModel(c.program, c.sessions, c.messages)
            }
        }
    }
}
