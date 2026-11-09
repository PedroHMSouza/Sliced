package com.pedrohmsouza.sliced.ui.history

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedrohmsouza.sliced.R
import com.pedrohmsouza.sliced.domain.Fmt
import com.pedrohmsouza.sliced.domain.WorkoutRules
import com.pedrohmsouza.sliced.ui.components.Card
import com.pedrohmsouza.sliced.ui.components.Line
import com.pedrohmsouza.sliced.ui.components.PeriodNav
import com.pedrohmsouza.sliced.ui.components.Plate
import com.pedrohmsouza.sliced.ui.components.PlateSize
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme

/**
 * Calendário do mês: cada dia treinado vira uma "anilha" na cor do treino. Embaixo, os
 * dias da semana em que cada treino foi feito.
 */
@Composable
fun MonthCard(month: WorkoutRules.Month, canNext: Boolean, onShift: (Int) -> Unit) {
    val c = SlicedTheme.colors
    Card {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PeriodNav(
                title = Fmt.monthTitle(month.month),
                subtitle = pluralStringResource(R.plurals.days_trained, month.trainedDays, month.trainedDays) + " · " +
                    pluralStringResource(R.plurals.workouts_count, month.sessions, month.sessions),
                onPrev = { onShift(-1) },
                onNext = { onShift(1) },
                canNext = canNext,
            )
            Column(Modifier.padding(horizontal = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
                    Fmt.WEEK_LETTERS.forEach {
                        Text(it.uppercase(), style = SlicedTheme.type.tiny, color = c.muted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    }
                }
                month.cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { day -> DayCell(day, Modifier.weight(1f)) }
                    }
                }
            }
            if (month.byTreino.isNotEmpty()) {
                Line(Modifier.padding(horizontal = 6.dp))
                Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.month_by_treino), style = SlicedTheme.type.small.copy(fontSize = 13.sp), color = c.muted)
                    month.byTreino.forEach { t ->
                        Row(
                            Modifier.semantics(mergeDescendants = true) {},
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Plate(t.treino, size = PlateSize.Small)
                            Text(
                                t.dates.joinToString(" · ") { Fmt.weekdayDay(it) },
                                style = SlicedTheme.type.small, color = c.fg,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: WorkoutRules.MonthDay?, modifier: Modifier) {
    val c = SlicedTheme.colors
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        if (day == null) return@Box
        val treino = day.treinos.lastOrNull()
        val desc = buildString {
            append(Fmt.weekdayDay(day.date))
            if (day.treinos.isNotEmpty()) append(": Treino ").append(day.treinos.joinToString(" e "))
        }
        Box(
            Modifier
                .size(36.dp)
                .then(
                    when {
                        treino != null -> Modifier.border(4.dp, c.plate(treino), CircleShape)
                        day.isToday -> Modifier.border(1.5.dp, c.fg, CircleShape)
                        else -> Modifier
                    },
                )
                .semantics { contentDescription = desc },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${day.date.dayOfMonth}",
                style = SlicedTheme.type.display.copy(fontSize = 15.sp, lineHeight = 15.sp),
                color = if (treino != null || day.isToday) c.fg else c.muted,
            )
        }
        // Mais de um treino no mesmo dia: um ponto embaixo avisa.
        if (day.treinos.size > 1) {
            Box(Modifier.align(Alignment.BottomCenter).size(5.dp).border(3.dp, c.plate(day.treinos.first()), CircleShape))
        }
    }
}
