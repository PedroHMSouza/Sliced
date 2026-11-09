package com.pedrohmsouza.sliced.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedrohmsouza.sliced.R
import com.pedrohmsouza.sliced.ui.components.Card
import com.pedrohmsouza.sliced.ui.components.Line
import com.pedrohmsouza.sliced.ui.components.Plate
import com.pedrohmsouza.sliced.ui.components.PlateSize
import com.pedrohmsouza.sliced.ui.components.PrimaryButton
import com.pedrohmsouza.sliced.ui.components.SectionTitle
import com.pedrohmsouza.sliced.ui.components.StoreNote
import com.pedrohmsouza.sliced.ui.components.Tag
import com.pedrohmsouza.sliced.ui.components.WeekStrip
import com.pedrohmsouza.sliced.ui.components.dashedBottom
import com.pedrohmsouza.sliced.ui.icons.SlicedIcons
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme

private const val DEFAULT_OPEN = "*"

@Composable
fun HomeScreen(
    state: HomeUiState,
    onStart: (String) -> Unit,
    onContinue: () -> Unit,
    contentPadding: PaddingValues,
) {
    val c = SlicedTheme.colors
    // Qual treino está expandido. "*" = padrão (o próximo); "" = todos fechados.
    // rememberSaveable mantém a escolha ao girar a tela ou voltar de outra aba.
    var expanded by rememberSaveable { mutableStateOf(DEFAULT_OPEN) }
    val openId = if (expanded == DEFAULT_OPEN) state.next else expanded

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        state.draft?.let { d ->
            item(key = "banner") {
                Card(radius = 12.dp) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Plate(d.treinoId)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(stringResource(R.string.home_in_progress, d.treinoId), style = SlicedTheme.type.button, color = c.fg)
                            Text(stringResource(R.string.home_started_at, d.startedAt), style = SlicedTheme.type.small, color = c.muted)
                        }
                        PrimaryButton(stringResource(R.string.action_continue), onContinue, small = true)
                    }
                }
            }
        }
        item(key = "week") { WeekStrip(state.week) }
        item(key = "title") { SectionTitle(stringResource(R.string.home_program)) }
        item(key = "list") {
            Card {
                state.treinos.forEachIndexed { i, item ->
                    if (i > 0) Line()
                    TreinoRow(
                        item = item,
                        isNext = item.treino.id == state.next,
                        open = openId == item.treino.id,
                        draft = state.draft,
                        onToggle = { expanded = if (openId == item.treino.id) "" else item.treino.id },
                        onStart = { onStart(item.treino.id) },
                        onContinue = onContinue,
                    )
                }
            }
        }
        item(key = "note") { StoreNote(stringResource(R.string.store_local)) }
    }
}

@Composable
private fun TreinoRow(
    item: TreinoItem,
    isNext: Boolean,
    open: Boolean,
    draft: DraftBanner?,
    onToggle: () -> Unit,
    onStart: () -> Unit,
    onContinue: () -> Unit,
) {
    val c = SlicedTheme.colors
    val t = item.treino
    val rotation by animateFloatAsState(if (open) 180f else 0f, label = "chev")
    val stateText = stringResource(if (open) R.string.a11y_expanded else R.string.a11y_collapsed)
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { stateDescription = stateText }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Plate(t.id, PlateSize.Large)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(t.name, style = SlicedTheme.type.treinoName, color = c.fg)
                val meta = buildString {
                    append(t.day).append(" · ")
                    append(pluralStringResourceText(t.exercises.size))
                    item.lastLabel?.let { append(" · último ").append(it) }
                }
                Text(meta, style = SlicedTheme.type.small, color = c.muted)
            }
            if (isNext) Tag(stringResource(R.string.tag_next), borderColor = c.plate(t.id))
            Icon(SlicedIcons.ChevronDown, null, tint = c.muted, modifier = Modifier.size(18.dp).rotate(rotation))
        }
        AnimatedVisibility(open) {
            Column(
                Modifier.padding(start = 60.dp, end = 14.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column {
                    t.exercises.forEachIndexed { i, e ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .then(if (i < t.exercises.lastIndex) Modifier.dashedBottom(c.line) else Modifier)
                                .padding(vertical = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(e.name, style = SlicedTheme.type.body.copy(fontSize = 15.sp), color = c.fg, modifier = Modifier.weight(1f))
                            Text(e.targetText, style = SlicedTheme.type.body.copy(fontSize = 15.sp), color = c.muted)
                        }
                    }
                }
                when {
                    draft == null -> PrimaryButton(stringResource(R.string.home_start, t.id), onStart, Modifier.fillMaxWidth())
                    draft.treinoId == t.id -> PrimaryButton(stringResource(R.string.home_continue_x, t.id), onContinue, Modifier.fillMaxWidth())
                    else -> Text(stringResource(R.string.home_blocked, draft.treinoId), style = SlicedTheme.type.small, color = c.muted)
                }
            }
        }
    }
}

@Composable
private fun pluralStringResourceText(n: Int) = pluralStringResource(R.plurals.exercises_count, n, n)
