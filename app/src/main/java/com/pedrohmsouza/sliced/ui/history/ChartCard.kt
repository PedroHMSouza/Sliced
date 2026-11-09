package com.pedrohmsouza.sliced.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedrohmsouza.sliced.R
import com.pedrohmsouza.sliced.domain.Fmt
import com.pedrohmsouza.sliced.domain.WorkoutRules.ChartPoint
import com.pedrohmsouza.sliced.ui.components.Card
import com.pedrohmsouza.sliced.ui.components.Eyebrow
import com.pedrohmsouza.sliced.ui.icons.SlicedIcons
import com.pedrohmsouza.sliced.ui.theme.BarlowCondensed
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme
import kotlin.math.abs

/**
 * "Evolução da carga": seletor de exercício + gráfico de linha com a maior carga de cada
 * treino (últimos 12), na cor do treino. Tocar num ponto mostra data e carga × reps.
 */
@Composable
fun ChartCard(chart: ChartUi, onSelect: (String) -> Unit) {
    val c = SlicedTheme.colors
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Eyebrow(stringResource(R.string.chart_title), Modifier.weight(1f))
                Eyebrow("kg")
            }
            ExercisePicker(chart, onSelect)
            // O índice tocado é zerado quando muda o exercício.
            var selected by rememberSaveable(chart.selected.exercise.id) { mutableStateOf<Int?>(null) }
            val sel = selected?.let { chart.points.getOrNull(it) }
            Text(
                sel?.let { "${Fmt.dateLabel(it.startedAt)} · ${Fmt.number(it.kg)} kg × ${it.reps}" }
                    ?: stringResource(R.string.chart_tap_hint),
                style = SlicedTheme.type.small,
                color = if (sel != null) c.fg else c.muted,
            )
            LineChart(chart.points, chart.selected.treinoId, selected) { selected = it }
            Caption(chart)
        }
    }
}

@Composable
private fun ExercisePicker(chart: ChartUi, onSelect: (String) -> Unit) {
    val c = SlicedTheme.colors
    var open by remember { mutableStateOf(false) }
    val label = "${chart.selected.treinoId} · ${chart.selected.exercise.name}"
    val pickerDesc = stringResource(R.string.chart_picker, label)
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(c.bg)
                .border(1.dp, c.line, RoundedCornerShape(10.dp))
                .clickable { open = true }
                .semantics { role = Role.DropdownList; contentDescription = pickerDesc }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = SlicedTheme.type.body.copy(fontSize = 15.sp), color = c.fg, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(SlicedIcons.ChevronDown, null, tint = c.muted, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(open, onDismissRequest = { open = false }, containerColor = c.surface) {
            chart.options.forEach { o ->
                DropdownMenuItem(
                    text = { Text("${o.treinoId} · ${o.exercise.name}", style = SlicedTheme.type.body.copy(fontSize = 15.sp)) },
                    onClick = { open = false; onSelect(o.exercise.id) },
                )
            }
        }
    }
}

@Composable
private fun LineChart(points: List<ChartPoint>, treinoId: String, selected: Int?, onSelect: (Int?) -> Unit) {
    if (points.isEmpty()) return
    val c = SlicedTheme.colors
    val color = c.plate(treinoId)
    val measurer = rememberTextMeasurer()
    val axisStyle = TextStyle(fontFamily = BarlowCondensed, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = c.muted)
    val dateStyle = SlicedTheme.type.small.copy(fontSize = 11.sp, color = c.muted)
    val mn = points.minOf { it.kg }
    val mx = points.maxOf { it.kg }
    val desc = stringResource(R.string.chart_a11y, Fmt.number(mn), Fmt.number(mx), points.size)

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(160.dp)
            .semantics { contentDescription = desc }
            .pointerInput(points) {
                detectTapGestures { tap ->
                    // Alvo de toque generoso: o ponto mais próximo na horizontal.
                    val left = 40.dp.toPx(); val right = size.width - 16.dp.toPx()
                    val xs = points.indices.map { i -> if (points.size == 1) (left + right) / 2 else left + i * (right - left) / (points.size - 1) }
                    val i = xs.indices.minBy { abs(xs[it] - tap.x) }
                    onSelect(if (i == selected) null else i)
                }
            },
    ) {
        val pl = 40.dp.toPx(); val pr = 16.dp.toPx(); val pt = 12.dp.toPx(); val pb = 24.dp.toPx()
        val w = size.width; val h = size.height
        val span = (mx - mn).takeIf { it > 0 } ?: maxOf(5.0, mx * 0.1)
        val lo = mn - span * 0.18; val hi = mx + span * 0.18
        fun x(i: Int) = if (points.size == 1) pl + (w - pl - pr) / 2 else pl + i * (w - pl - pr) / (points.size - 1)
        fun y(v: Double) = (pt + (hi - v) / (hi - lo) * (h - pt - pb)).toFloat()

        // Grade recessiva: só mínimo e máximo, tracejados.
        val ticks = if (mn == mx) listOf(mx) else listOf(mn, mx)
        ticks.forEach { v ->
            drawLine(c.line, Offset(pl, y(v)), Offset(w - pr, y(v)), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx())))
            val t = measurer.measure(Fmt.number(v), axisStyle)
            drawText(t, topLeft = Offset(pl - 8.dp.toPx() - t.size.width, y(v) - t.size.height / 2))
        }
        drawLine(c.line, Offset(pl, h - pb), Offset(w - pr, h - pb), 1.dp.toPx())

        if (points.size > 1) {
            val line = Path().apply { points.forEachIndexed { i, p -> if (i == 0) moveTo(x(i), y(p.kg)) else lineTo(x(i), y(p.kg)) } }
            val area = Path().apply {
                addPath(line); lineTo(x(points.lastIndex), h - pb); lineTo(x(0), h - pb); close()
            }
            drawPath(area, color.copy(alpha = 0.13f))
            drawPath(line, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        selected?.let { i -> drawLine(c.muted.copy(alpha = 0.5f), Offset(x(i), pt), Offset(x(i), h - pb), 1.dp.toPx()) }
        points.forEachIndexed { i, p ->
            val emphasized = i == points.lastIndex || i == selected
            val r = if (emphasized) 5.dp.toPx() else 4.dp.toPx()
            // Anel da cor da superfície separa o ponto da linha.
            drawCircle(c.surface, r + 2.dp.toPx(), Offset(x(i), y(p.kg)))
            drawCircle(if (emphasized) color else c.surface, r, Offset(x(i), y(p.kg)))
            drawCircle(color, r, Offset(x(i), y(p.kg)), style = Stroke(2.dp.toPx()))
        }

        val first = measurer.measure(Fmt.dayMonth(points.first().startedAt), dateStyle)
        val firstX = if (points.size == 1) x(0) - first.size.width / 2 else x(0)
        drawText(first, topLeft = Offset(firstX, h - first.size.height))
        if (points.size > 1) {
            val last = measurer.measure(Fmt.dayMonth(points.last().startedAt), dateStyle)
            drawText(last, topLeft = Offset(x(points.lastIndex) - last.size.width, h - last.size.height))
        }
    }
}

@Composable
private fun Caption(chart: ChartUi) {
    val c = SlicedTheme.colors
    val best = chart.best
    val delta = chart.deltaKg
    val text = if (best == null || delta == null) {
        buildAnnotatedString { append(stringResource(R.string.chart_need_more)) }
    } else {
        val bold = SpanStyle(color = c.fg, fontWeight = FontWeight.SemiBold)
        buildAnnotatedString {
            append(stringResource(R.string.chart_best) + " ")
            withStyle(bold) { append("${Fmt.number(best.kg)} kg × ${best.reps}") }
            append(". " + stringResource(R.string.chart_delta) + " ")
            withStyle(bold) { append((if (delta >= 0) "+" else "") + Fmt.number(delta) + " kg") }
            append(".")
        }
    }
    Text(text, style = SlicedTheme.type.small, color = c.muted)
}
