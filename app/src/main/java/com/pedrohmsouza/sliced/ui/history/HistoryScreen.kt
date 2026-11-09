package com.pedrohmsouza.sliced.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedrohmsouza.sliced.R
import com.pedrohmsouza.sliced.domain.Fmt
import com.pedrohmsouza.sliced.ui.components.Card
import com.pedrohmsouza.sliced.ui.components.EmptyState
import com.pedrohmsouza.sliced.ui.components.GhostButton
import com.pedrohmsouza.sliced.ui.components.Plate
import com.pedrohmsouza.sliced.ui.components.SectionTitle
import com.pedrohmsouza.sliced.ui.components.StoreNote
import com.pedrohmsouza.sliced.ui.components.WeekStrip
import com.pedrohmsouza.sliced.ui.components.dashedTop
import com.pedrohmsouza.sliced.ui.icons.SlicedIcons
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    initiallyOpen: String?,
    onDelete: (String) -> Unit,
    onSelectChartExercise: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    var openId by rememberSaveable { mutableStateOf(initiallyOpen) }
    var confirmDelete by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        // 8 dp entre os cartões de treino; as seções ganham 14 dp extras no topo (22 no total).
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "week") { WeekStrip(state.week) }
        item(key = "stats") { Box(Modifier.padding(top = 14.dp)) { StatsRow(state) } }
        if (state.items.isEmpty() && !state.loading) {
            item(key = "empty") {
                EmptyState(stringResource(R.string.history_empty_title), stringResource(R.string.history_empty_body), Modifier.padding(top = 14.dp))
            }
        }
        state.chart?.let { chart -> item(key = "chart") { Box(Modifier.padding(top = 14.dp)) { ChartCard(chart, onSelectChartExercise) } } }
        if (state.items.isNotEmpty()) {
            item(key = "title") { SectionTitle(stringResource(R.string.history_list_title), Modifier.padding(top = 14.dp, bottom = 4.dp)) }
            items(state.items, key = { it.session.id }) { item ->
                SessionCard(
                    item = item,
                    open = openId == item.session.id,
                    onToggle = { openId = if (openId == item.session.id) null else item.session.id },
                    onDelete = { confirmDelete = item.session.id },
                )
            }
        }
        item(key = "note") { Box(Modifier.padding(top = 14.dp)) { StoreNote(stringResource(R.string.store_local)) } }
    }

    confirmDelete?.let { id ->
        val item = state.items.firstOrNull { it.session.id == id }
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(item?.let { stringResource(R.string.delete_text, it.dateLabel, it.session.treino) }.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = null; openId = null; onDelete(id) }) {
                    Text(stringResource(R.string.delete_confirm), color = SlicedTheme.colors.danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.cancel)) } },
            containerColor = SlicedTheme.colors.surface,
        )
    }
}

@Composable
private fun StatsRow(state: HistoryUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Stat(stringResource(R.string.stat_month), state.stats.month.toString(), Modifier.weight(1f))
        Stat(stringResource(R.string.stat_total), state.stats.total.toString(), Modifier.weight(1f))
        Stat(stringResource(R.string.stat_avg), state.stats.avgMin?.let { "$it min" } ?: "—", Modifier.weight(1f))
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    val c = SlicedTheme.colors
    Card(modifier, radius = 12.dp) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label.uppercase(), style = SlicedTheme.type.tiny, color = c.muted, maxLines = 1)
            Text(value, style = SlicedTheme.type.stat, color = c.fg, maxLines = 1)
        }
    }
}

@Composable
private fun SessionCard(item: SessionItem, open: Boolean, onToggle: () -> Unit, onDelete: () -> Unit) {
    val c = SlicedTheme.colors
    val s = item.session
    val rotation by animateFloatAsState(if (open) 180f else 0f, label = "chev")
    val stateText = stringResource(if (open) R.string.a11y_expanded else R.string.a11y_collapsed)
    Card {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { stateDescription = stateText }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Plate(s.treino)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("${item.dateLabel} · ${item.name}", style = SlicedTheme.type.display.copy(fontSize = 18.sp), color = c.fg)
                val meta = buildString {
                    append("${s.durationMin} min · ${item.sets} séries")
                    if (item.volumeKg > 0) append(" · ${Fmt.number(Math.round(item.volumeKg))} kg")
                }
                Text(meta, style = SlicedTheme.type.small, color = c.muted)
            }
            Icon(SlicedIcons.ChevronDown, null, tint = c.muted, modifier = Modifier.size(18.dp).rotate(rotation))
        }
        AnimatedVisibility(open) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                s.exercises.forEach { e ->
                    Column(
                        Modifier.fillMaxWidth().dashedTop(c.line).padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        Text(e.name, style = SlicedTheme.type.button.copy(fontSize = 15.sp), color = c.fg)
                        val sets = e.sets.joinToString(" · ") { Fmt.setShort(e.type, it) }.ifEmpty { stringResource(R.string.history_no_sets) }
                        Text(sets, style = SlicedTheme.type.small, color = c.muted)
                        if (e.note.isNotBlank()) Text(e.note, style = SlicedTheme.type.small, color = c.muted)
                    }
                }
                GhostButton(stringResource(R.string.history_delete), onDelete, color = c.danger, modifier = Modifier.padding(start = 0.dp))
            }
        }
    }
}
