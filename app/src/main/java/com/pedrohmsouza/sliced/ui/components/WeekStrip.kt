package com.pedrohmsouza.sliced.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedrohmsouza.sliced.R
import com.pedrohmsouza.sliced.domain.WorkoutRules
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme

/** Faixa "Esta semana": Seg a Dom com a anilha do treino feito em cada dia. */
@Composable
fun WeekStrip(week: WorkoutRules.Week, modifier: Modifier = Modifier) {
    val c = SlicedTheme.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Eyebrow(stringResource(R.string.week_title), Modifier.weight(1f))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = c.fg, fontSize = 24.sp)) { append(week.count.toString()) }
                    append(" " + stringResource(R.string.week_of_5))
                },
                style = SlicedTheme.type.display.copy(fontSize = 18.sp, lineHeight = 24.sp),
                color = c.muted,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            week.days.forEach { d ->
                val desc = d.treino?.let { stringResource(R.string.week_day_done, d.label, it) } ?: d.label
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.surface)
                        .border(1.dp, if (d.isToday) c.fg else c.line, RoundedCornerShape(10.dp))
                        .padding(top = 8.dp, bottom = 10.dp)
                        .semantics(mergeDescendants = true) { contentDescription = desc },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(d.label.uppercase(), style = SlicedTheme.type.tiny, color = c.muted)
                    if (d.treino != null) Plate(d.treino, PlateSize.Small)
                    else Box(Modifier.size(26.dp).dashedBorder(c.line, 13.dp, 2.dp, circle = true))
                }
            }
        }
    }
}
