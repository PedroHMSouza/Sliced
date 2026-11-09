package com.pedrohmsouza.sliced.ui.edit

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedrohmsouza.sliced.R
import com.pedrohmsouza.sliced.domain.ProgramRules
import com.pedrohmsouza.sliced.ui.components.Card
import com.pedrohmsouza.sliced.ui.components.Eyebrow
import com.pedrohmsouza.sliced.ui.components.ExerciseImage
import com.pedrohmsouza.sliced.ui.components.Line
import com.pedrohmsouza.sliced.ui.components.Plate
import com.pedrohmsouza.sliced.ui.components.PlateSize
import com.pedrohmsouza.sliced.ui.components.SecondaryButton
import com.pedrohmsouza.sliced.ui.components.SectionTitle
import com.pedrohmsouza.sliced.ui.components.StoreNote
import com.pedrohmsouza.sliced.ui.icons.SlicedIcons
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme

/** Ações da tela "Editar ficha". */
interface ProgramEditActions {
    fun select(treinoId: String)
    fun rename(name: String)
    fun setDay(day: String)
    fun move(exerciseId: String, delta: Int)
    fun openExercise(exerciseId: String?)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgramEditScreen(state: ProgramEditUiState, actions: ProgramEditActions, contentPadding: PaddingValues) {
    val c = SlicedTheme.colors
    val t = state.treino ?: return
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item(key = "plates") {
            Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.SpaceBetween) {
                state.treinos.forEach { tr ->
                    val on = tr.id == state.selected
                    Box(
                        Modifier
                            .size(60.dp)
                            .border(2.dp, if (on) c.fg else Color.Transparent, CircleShape)
                            .selectable(on, role = Role.Tab, onClick = { actions.select(tr.id) })
                            .semantics { contentDescription = "Treino ${tr.id}, ${tr.name}" },
                        contentAlignment = Alignment.Center,
                    ) { Plate(tr.id, size = PlateSize.Large) }
                }
            }
        }
        item(key = "info-${t.id}") {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Texto local: digitar não espera a gravação no banco (mesmo padrão dos campos do treino).
                    var name by remember(t.id) { mutableStateOf(t.name) }
                    LaunchedEffect(t.name) { if (t.name != name) name = t.name }
                    TextInput(name, { name = it; actions.rename(it) }, stringResource(R.string.edit_treino_name))
                    Eyebrow(stringResource(R.string.edit_treino_day))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ProgramRules.WEEKDAYS.forEach { d -> Chip(d, d == t.day) { actions.setDay(d) } }
                    }
                }
            }
        }
        item(key = "ex-title") { SectionTitle(stringResource(R.string.edit_exercises)) }
        item(key = "ex-list") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (t.exercises.isEmpty()) {
                    Text(stringResource(R.string.edit_no_exercises), style = SlicedTheme.type.body, color = c.muted)
                } else {
                    Card {
                        t.exercises.forEachIndexed { i, e ->
                            if (i > 0) Line()
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Button) { actions.openExercise(e.id) }
                                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                ExerciseImage(e.media, e.libraryId, Modifier.size(52.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(e.name, style = SlicedTheme.type.body.copy(fontSize = 15.sp), color = c.fg)
                                    Text(e.targetText, style = SlicedTheme.type.small, color = c.muted)
                                }
                                IconButton({ actions.move(e.id, -1) }, enabled = i > 0) {
                                    Icon(SlicedIcons.Up, stringResource(R.string.edit_move_up, e.name), tint = if (i > 0) c.fg else c.line, modifier = Modifier.size(20.dp))
                                }
                                IconButton({ actions.move(e.id, 1) }, enabled = i < t.exercises.lastIndex) {
                                    Icon(
                                        SlicedIcons.Down, stringResource(R.string.edit_move_down, e.name),
                                        tint = if (i < t.exercises.lastIndex) c.fg else c.line, modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                SecondaryButton(stringResource(R.string.edit_add_exercise), { actions.openExercise(null) }, Modifier.fillMaxWidth())
            }
        }
        item(key = "note") { StoreNote(stringResource(R.string.edit_note)) }
    }
}
