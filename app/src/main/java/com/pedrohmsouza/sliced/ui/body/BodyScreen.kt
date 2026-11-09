package com.pedrohmsouza.sliced.ui.body

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedrohmsouza.sliced.R
import com.pedrohmsouza.sliced.domain.BodyRules
import com.pedrohmsouza.sliced.domain.Fmt
import com.pedrohmsouza.sliced.ui.components.Card
import com.pedrohmsouza.sliced.ui.components.Eyebrow
import com.pedrohmsouza.sliced.ui.components.Line
import com.pedrohmsouza.sliced.ui.components.PeriodNav
import com.pedrohmsouza.sliced.ui.components.SectionTitle
import com.pedrohmsouza.sliced.ui.components.StoreNote
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme

/**
 * Mapa do corpo: o boneco de frente e de costas vai sendo pintado conforme os músculos
 * são treinados na semana (segunda a domingo). Quanto mais séries, mais forte a cor.
 */
@Composable
fun BodyScreen(state: BodyUiState, onShiftWeek: (Int) -> Unit, contentPadding: PaddingValues) {
    val c = SlicedTheme.colors
    val week = state.week ?: return
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item(key = "nav") {
            PeriodNav(
                title = stringResource(if (state.offset == 0) R.string.body_this_week else R.string.body_week),
                subtitle = Fmt.range(week.start, week.end) + " · " +
                    pluralStringResource(R.plurals.workouts_count, week.sessions, week.sessions),
                onPrev = { onShiftWeek(-1) },
                onNext = { onShiftWeek(1) },
                canNext = state.offset < 0,
            )
        }
        item(key = "figure") {
            val trainedNames = state.trained.joinToString(", ") { "${it.first.label} ${Fmt.number(it.second)}" }
            val desc = if (state.trained.isEmpty()) stringResource(R.string.body_a11y_none)
            else stringResource(R.string.body_a11y, trainedNames)
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = desc },
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        listOf(true to R.string.body_front, false to R.string.body_back).forEach { (front, label) ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Eyebrow(stringResource(label))
                                BodyFigure(front, state::level, Modifier.padding(top = 8.dp).fillMaxWidth())
                            }
                        }
                    }
                    Legend()
                }
            }
        }
        item(key = "list") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(stringResource(R.string.body_sets_title))
                if (state.trained.isEmpty()) {
                    Text(stringResource(R.string.body_empty), style = SlicedTheme.type.body, color = c.muted)
                } else {
                    val max = state.trained.maxOf { it.second }.coerceAtLeast(10.0)
                    Card {
                        state.trained.forEachIndexed { i, (m, sets) ->
                            if (i > 0) Line()
                            MuscleRow(m.label, sets, max, BodyRules.level(sets))
                        }
                    }
                }
                if (state.untrained.isNotEmpty()) {
                    Text(
                        stringResource(R.string.body_untrained, state.untrained.joinToString(", ") { it.label }),
                        style = SlicedTheme.type.small, color = c.muted,
                    )
                }
            }
        }
        item(key = "note") { StoreNote(stringResource(R.string.body_note)) }
    }
}

@Composable
private fun MuscleRow(label: String, sets: Double, max: Double, level: Int) {
    val c = SlicedTheme.colors
    val shades = heatShades()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = SlicedTheme.type.body.copy(fontSize = 15.sp), color = c.fg, modifier = Modifier.width(132.dp), maxLines = 1)
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.soft)) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth((sets / max).toFloat().coerceIn(0.04f, 1f))
                    .clip(RoundedCornerShape(4.dp)).background(shades[level.coerceIn(1, 4)]),
            )
        }
        Text(
            pluralStringResource(R.plurals.sets_short, Math.ceil(sets).toInt(), Fmt.number(sets)),
            style = SlicedTheme.type.small, color = c.muted, modifier = Modifier.width(64.dp),
        )
    }
}

@Composable
private fun Legend() {
    val c = SlicedTheme.colors
    val shades = heatShades()
    val labels = listOf("0", "1–2", "3–5", "6–9", "10+")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.body_legend), style = SlicedTheme.type.small.copy(fontSize = 13.sp), color = c.muted)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            shades.zip(labels).forEach { (color, text) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(color))
                    Text(text, style = SlicedTheme.type.small.copy(fontSize = 13.sp), color = c.fg)
                }
            }
        }
    }
}
