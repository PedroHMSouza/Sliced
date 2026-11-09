package com.pedrohmsouza.sliced.ui.edit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pedrohmsouza.sliced.R
import com.pedrohmsouza.sliced.domain.ProgramRules
import com.pedrohmsouza.sliced.model.ExerciseLibrary
import com.pedrohmsouza.sliced.model.LibraryExercise
import com.pedrohmsouza.sliced.model.Muscle
import com.pedrohmsouza.sliced.model.RecordType
import com.pedrohmsouza.sliced.ui.components.BackTopBar
import com.pedrohmsouza.sliced.ui.components.Card
import com.pedrohmsouza.sliced.ui.components.Eyebrow
import com.pedrohmsouza.sliced.ui.components.ExerciseImage
import com.pedrohmsouza.sliced.ui.components.GhostButton
import com.pedrohmsouza.sliced.ui.components.Line
import com.pedrohmsouza.sliced.ui.components.PrimaryButton
import com.pedrohmsouza.sliced.ui.components.StoreNote
import com.pedrohmsouza.sliced.ui.icons.SlicedIcons
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme

/** Ações do formulário de exercício (as funções do ViewModel, agrupadas). */
interface ExerciseEditActions {
    fun name(v: String)
    fun type(v: RecordType)
    fun sets(delta: Int)
    fun repMin(v: String)
    fun repMax(v: String)
    fun rest(delta: Int)
    fun target(v: String)
    fun tip(v: String)
    fun muscle(m: Muscle, primary: Boolean)
    fun library(lib: LibraryExercise)
    fun pickPhoto()
    fun clearPhoto()
    fun delete()
}

fun ExerciseEditViewModel.asActions(onPickPhoto: () -> Unit) = object : ExerciseEditActions {
    override fun name(v: String) = setName(v)
    override fun type(v: RecordType) = setType(v)
    override fun sets(delta: Int) = changeSets(delta)
    override fun repMin(v: String) = setRepMin(v)
    override fun repMax(v: String) = setRepMax(v)
    override fun rest(delta: Int) = changeRest(delta)
    override fun target(v: String) = setTarget(v)
    override fun tip(v: String) = setTip(v)
    override fun muscle(m: Muscle, primary: Boolean) = toggleMuscle(m, primary)
    override fun library(lib: LibraryExercise) = pickLibrary(lib)
    override fun pickPhoto() = onPickPhoto()
    override fun clearPhoto() = clearMedia()
    override fun delete() = this@asActions.delete()
}

private val TYPE_LABELS = listOf(
    RecordType.WR to R.string.type_wr, RecordType.R to R.string.type_r, RecordType.S to R.string.type_s,
    RecordType.WM to R.string.type_wm, RecordType.CARDIO to R.string.type_cardio,
)

@Composable
fun ExerciseEditTopBar(state: ExerciseEditUiState, onBack: () -> Unit, onSave: () -> Unit) {
    BackTopBar(
        stringResource(R.string.edit_back),
        stringResource(if (state.isNew) R.string.new_exercise_title else R.string.edit_exercise_title),
        onBack,
    ) {
        PrimaryButton(
            stringResource(R.string.edit_save), onSave, small = true, enabled = !state.loading,
            modifier = Modifier.padding(end = 8.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseEditScreen(state: ExerciseEditUiState, actions: ExerciseEditActions, contentPadding: PaddingValues) {
    if (state.loading) return
    val c = SlicedTheme.colors
    val f = state.form
    var showLibrary by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(key = "media") {
            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExerciseImage(f.media, f.libraryId, Modifier.fillMaxWidth().aspectRatio(3f / 2f), animate = true, radius = 12)
                    if (f.media == null && f.libraryId == null) {
                        Text(stringResource(R.string.edit_media_hint), style = SlicedTheme.type.small, color = c.muted)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        GhostButton(stringResource(R.string.edit_media_library), { showLibrary = true }, color = c.fg, leading = {
                            Icon(SlicedIcons.Search, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                        })
                        GhostButton(stringResource(R.string.edit_media_phone), actions::pickPhoto, color = c.fg, leading = {
                            Icon(SlicedIcons.Photo, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                        })
                        if (f.media != null) GhostButton(stringResource(R.string.edit_media_remove), actions::clearPhoto, color = c.danger)
                    }
                }
            }
        }
        item(key = "basics") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextInput(f.name, actions::name, stringResource(R.string.edit_name))
                Eyebrow(stringResource(R.string.edit_type))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TYPE_LABELS.forEach { (t, label) -> Chip(stringResource(label), f.type == t) { actions.type(t) } }
                }
            }
        }
        item(key = "numbers") {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Stepper(stringResource(R.string.edit_sets), "${f.sets}", { actions.sets(-1) }, { actions.sets(1) })
                    Line()
                    Stepper(stringResource(R.string.edit_rest), "${f.restSec} s", { actions.rest(-15) }, { actions.rest(15) })
                    if (f.usesReps) {
                        Line()
                        Text(stringResource(R.string.edit_reps), style = SlicedTheme.type.body, color = c.fg)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TextInput(f.repMin, actions::repMin, stringResource(R.string.edit_reps_from), Modifier.weight(1f), number = true)
                            TextInput(f.repMax, actions::repMax, stringResource(R.string.edit_reps_to), Modifier.weight(1f), number = true)
                        }
                    }
                }
            }
        }
        item(key = "texts") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextInput(f.target, actions::target, stringResource(R.string.edit_target), placeholder = stringResource(R.string.edit_target_hint))
                TextInput(f.tip, actions::tip, stringResource(R.string.edit_tip), singleLine = false)
            }
        }
        item(key = "muscles") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MuscleChips(stringResource(R.string.muscles_primary), f.primary) { actions.muscle(it, true) }
                MuscleChips(stringResource(R.string.muscles_secondary), f.secondary) { actions.muscle(it, false) }
            }
        }
        if (!state.isNew) {
            item(key = "delete") {
                GhostButton(stringResource(R.string.edit_delete), { confirmDelete = true }, color = c.danger)
            }
        }
        item(key = "note") { StoreNote(stringResource(R.string.edit_note)) }
    }

    if (showLibrary) LibraryDialog(onPick = { showLibrary = false; actions.library(it) }, onDismiss = { showLibrary = false })
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_exercise_title)) },
            text = { Text(stringResource(R.string.delete_exercise_text, f.name, state.treinoId)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; actions.delete() }) {
                    Text(stringResource(R.string.delete_exercise_confirm), color = c.danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
            containerColor = c.surface,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MuscleChips(label: String, selected: List<Muscle>, onToggle: (Muscle) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Eyebrow(label)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Muscle.entries.forEach { m -> Chip(m.label, m in selected) { onToggle(m) } }
        }
    }
}

/** Catálogo com busca: toque num exercício para usar as fotos e os músculos dele. */
@Composable
private fun LibraryDialog(onPick: (LibraryExercise) -> Unit, onDismiss: () -> Unit) {
    val c = SlicedTheme.colors
    var query by remember { mutableStateOf("") }
    val q = ProgramRules.slug(query)
    val list = remember(q) {
        ExerciseLibrary.all.filter { q.isEmpty() || q in ProgramRules.slug(it.name) || q in ProgramRules.slug(it.primary.joinToString { m -> m.label }) }
    }
    Dialog(onDismiss, DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.padding(12.dp).widthIn(max = 600.dp).fillMaxWidth().imePadding()) {
            Column(Modifier.padding(top = 14.dp)) {
                Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.library_title), style = SlicedTheme.type.treinoName, color = c.fg, modifier = Modifier.weight(1f))
                    GhostButton(stringResource(R.string.action_close), onDismiss)
                }
                TextInput(query, { query = it }, stringResource(R.string.library_search), Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                LazyColumn(Modifier.fillMaxWidth().aspectRatio(0.62f, matchHeightConstraintsFirst = false)) {
                    if (list.isEmpty()) item { Text(stringResource(R.string.library_empty), style = SlicedTheme.type.body, color = c.muted, modifier = Modifier.padding(14.dp)) }
                    items(list, key = { it.id }) { lib ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) { onPick(lib) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ExerciseImage(null, lib.id, Modifier.size(56.dp))
                            Column(Modifier.weight(1f)) {
                                Text(lib.name, style = SlicedTheme.type.body.copy(fontSize = 15.sp), color = c.fg)
                                Text(lib.primary.joinToString(" · ") { it.label }, style = SlicedTheme.type.small, color = c.muted)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Abre o seletor de fotos do Android (só imagens; GIF incluso). */
@Composable
fun rememberPhotoPicker(onPicked: (Uri) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(onPicked) }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}
