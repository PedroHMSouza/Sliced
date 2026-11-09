package com.pedrohmsouza.sliced.ui.workout

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedrohmsouza.sliced.R
import com.pedrohmsouza.sliced.domain.Fmt
import com.pedrohmsouza.sliced.model.DraftExercise
import com.pedrohmsouza.sliced.model.FieldKey
import com.pedrohmsouza.sliced.ui.components.Card
import com.pedrohmsouza.sliced.ui.components.GhostButton
import com.pedrohmsouza.sliced.ui.components.Line
import com.pedrohmsouza.sliced.ui.components.Plate
import com.pedrohmsouza.sliced.ui.components.PlateSize
import com.pedrohmsouza.sliced.ui.components.PrimaryButton
import com.pedrohmsouza.sliced.ui.components.Tag
import com.pedrohmsouza.sliced.ui.components.dashedTop
import com.pedrohmsouza.sliced.ui.icons.SlicedIcons
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme
import kotlinx.coroutines.delay

/** Ações da tela, agrupadas para não passar uma dúzia de lambdas soltas. */
interface WorkoutActions {
    fun field(ei: Int, si: Int, key: FieldKey, value: String)
    fun note(ei: Int, text: String)
    fun toggle(ei: Int, si: Int)
    fun addSet(ei: Int)
    fun removeSet(ei: Int)
    fun rest(ei: Int)
    fun finish()
}

fun WorkoutViewModel.asActions() = object : WorkoutActions {
    override fun field(ei: Int, si: Int, key: FieldKey, value: String) = onFieldChange(ei, si, key, value)
    override fun note(ei: Int, text: String) = onNoteChange(ei, text)
    override fun toggle(ei: Int, si: Int) = onToggleSet(ei, si)
    override fun addSet(ei: Int) = onAddSet(ei)
    override fun removeSet(ei: Int) = onRemoveSet(ei)
    override fun rest(ei: Int) = onStartRest(ei)
    override fun finish() = onOpenFinish()
}

/** Mantém a tela acesa enquanto este composable estiver visível (FLAG_KEEP_SCREEN_ON). */
@Composable
fun KeepScreenOn() {
    val activity = LocalActivity.current
    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

@Composable
fun WorkoutTopBar(state: WorkoutUiState, onBack: () -> Unit, onFinish: () -> Unit, now: () -> Long = System::currentTimeMillis) {
    val c = SlicedTheme.colors
    val d = state.draft ?: return
    val t = state.treino
    // Recalcula o cronômetro a cada meio segundo. A conta usa o horário de início salvo,
    // então o tempo continua certo mesmo se o app ficar em segundo plano.
    val elapsed by produceState(now() - d.startedAt, d.startedAt) {
        while (true) { value = now() - d.startedAt; delay(500) }
    }
    Column(Modifier.background(c.bg)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GhostButton(stringResource(R.string.workout_back), onBack, color = c.muted, leading = {
                Icon(SlicedIcons.Back, null, Modifier.size(18.dp))
            })
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Plate(d.treino, PlateSize.Small)
                Text(t?.name.orEmpty(), style = SlicedTheme.type.treinoName, color = c.fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                Fmt.clock(elapsed),
                style = SlicedTheme.type.clock,
                color = if (elapsed >= 55 * 60_000L) c.warn else c.muted,
                modifier = Modifier.semantics { contentDescription = "Tempo de treino ${Fmt.clock(elapsed)}" },
            )
            PrimaryButton(stringResource(R.string.workout_finish), onFinish, small = true, modifier = Modifier.padding(end = 8.dp))
        }
        Line()
    }
}

@Composable
fun WorkoutScreen(
    state: WorkoutUiState,
    actions: WorkoutActions,
    focusRequest: WorkoutEvent.FocusField?,
    contentPadding: PaddingValues,
) {
    val c = SlicedTheme.colors
    val d = state.draft ?: return
    val focus = remember { mutableMapOf<String, FocusRequester>() }
    LaunchedEffect(focusRequest) {
        focusRequest?.let { e -> focus["${e.exerciseIndex}-${e.setIndex}-${e.key}"]?.requestFocus() }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        itemsIndexed(d.exercises, key = { i, e -> "$i-${e.exerciseId}" }) { ei, e ->
            ExerciseCard(ei, e, state.exercises.getOrNull(ei), actions, focus)
        }
        item(key = "end") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.workout_progress, state.progress.done, state.progress.total),
                    style = SlicedTheme.type.small, color = c.muted,
                )
                PrimaryButton(stringResource(R.string.workout_finish_x, d.treino), actions::finish, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    ei: Int,
    e: DraftExercise,
    ui: ExerciseUi?,
    actions: WorkoutActions,
    focus: MutableMap<String, FocusRequester>,
) {
    val c = SlicedTheme.colors
    val plan = ui?.planned
    val allDone = e.sets.isNotEmpty() && e.sets.all { it.done }
    val fields = e.type.fields
    Card(borderColor = if (allDone) c.ok else c.line) {
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.padding(horizontal = 2.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(e.name, style = SlicedTheme.type.exerciseName, color = c.fg)
                    if (plan != null) {
                        val meta = plan.targetText + if (plan.restSec > 0) " · descanso ${plan.restSec} s" else ""
                        Text(meta, style = SlicedTheme.type.small, color = c.muted, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                if (ui?.canIncrease == true) Tag(stringResource(R.string.tag_increase), background = c.okSoft, color = c.ok)
            }
            plan?.tip?.let { Text(it, style = SlicedTheme.type.small, color = c.muted, modifier = Modifier.padding(horizontal = 2.dp)) }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Cabeçalho da tabela de séries (oculto para leitores de tela: cada campo tem rótulo próprio).
                SetRowLayout(
                    Modifier.padding(horizontal = 4.dp).clearAndSetSemantics { },
                    number = { HeaderText(stringResource(R.string.col_set), TextAlign.Center) },
                    previous = { HeaderText(stringResource(R.string.col_previous), TextAlign.Start) },
                    fields = fields.map { f -> { HeaderText(f.label, TextAlign.Center) } },
                    check = { HeaderText(stringResource(R.string.col_done), TextAlign.Center) },
                )
                e.sets.forEachIndexed { si, s ->
                    val prev = ui?.previous?.getOrNull(si)
                    SetRowLayout(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (s.done) c.okSoft else c.surface)
                            .padding(horizontal = 4.dp, vertical = 5.dp),
                        number = {
                            Text(
                                "${si + 1}", style = SlicedTheme.type.display.copy(fontSize = 18.sp),
                                color = if (s.done) c.ok else c.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                            )
                        },
                        previous = {
                            Text(
                                Fmt.set(e.type, prev), style = SlicedTheme.type.small.copy(fontSize = 13.sp), color = c.muted,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        },
                        fields = fields.mapIndexed { fi, f ->
                            {
                                val req = focus.getOrPut("$ei-$si-$f") { FocusRequester() }
                                NumberField(
                                    value = s.value(f),
                                    placeholder = prev?.get(f)?.let(Fmt::number).orEmpty(),
                                    decimal = f.decimal,
                                    done = s.done,
                                    label = "${e.name}, série ${si + 1}, ${f.label}",
                                    last = fi == fields.lastIndex,
                                    modifier = Modifier.focusRequester(req),
                                    onChange = { actions.field(ei, si, f, it) },
                                )
                            }
                        },
                        check = { CheckButton(s.done, si) { actions.toggle(ei, si) } },
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                GhostButton(stringResource(R.string.workout_add_set), { actions.addSet(ei) })
                if (e.sets.size > 1) GhostButton(stringResource(R.string.workout_remove_set), { actions.removeSet(ei) })
                Spacer(Modifier.weight(1f))
                if (plan != null && plan.restSec > 0) {
                    GhostButton(stringResource(R.string.workout_rest, plan.restSec), { actions.rest(ei) }, leading = {
                        Icon(SlicedIcons.Timer, null, Modifier.size(16.dp).padding(end = 0.dp))
                        Spacer(Modifier.width(6.dp))
                    })
                }
            }
            NoteField(e.note, e.name) { actions.note(ei, it) }
        }
    }
}

/** Grade da linha de série: nº | anterior | campos | ✓ (mesmas larguras no cabeçalho e nas linhas). */
@Composable
private fun SetRowLayout(
    modifier: Modifier,
    number: @Composable () -> Unit,
    previous: @Composable () -> Unit,
    fields: List<@Composable () -> Unit>,
    check: @Composable () -> Unit,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.width(26.dp), contentAlignment = Alignment.Center) { number() }
        Box(Modifier.weight(1f)) { previous() }
        fields.forEach { Box(Modifier.width(72.dp), contentAlignment = Alignment.Center) { it() } }
        Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) { check() }
    }
}

@Composable
private fun HeaderText(text: String, align: TextAlign) {
    Text(
        text.uppercase(), style = SlicedTheme.type.tiny, color = SlicedTheme.colors.muted,
        textAlign = align, maxLines = 1, modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Campo numérico. O texto fica num estado local para a digitação ser instantânea; cada
 * mudança vai para o ViewModel, e se o ViewModel mudar o valor por conta própria (ex.:
 * preencher com o valor da última vez ao tocar ✓), o campo acompanha.
 */
@Composable
private fun NumberField(
    value: String,
    placeholder: String,
    decimal: Boolean,
    done: Boolean,
    label: String,
    last: Boolean,
    modifier: Modifier,
    onChange: (String) -> Unit,
) {
    val c = SlicedTheme.colors
    var text by remember { mutableStateOf(value) }
    LaunchedEffect(value) { if (value != text) text = value }
    val shape = RoundedCornerShape(9.dp)
    BasicTextField(
        value = text,
        onValueChange = { raw -> text = raw.filter { it.isDigit() || (decimal && (it == ',' || it == '.')) }; onChange(text) },
        singleLine = true,
        textStyle = SlicedTheme.type.input.copy(color = c.fg, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(c.fg),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = if (last) ImeAction.Done else ImeAction.Next,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics { contentDescription = label },
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(if (done) c.okSoft else c.bg)
                    .then(if (done) Modifier else Modifier.border(1.dp, c.line, shape))
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (text.isEmpty()) Text(placeholder, style = SlicedTheme.type.input, color = c.muted.copy(alpha = 0.55f), textAlign = TextAlign.Center)
                inner()
            }
        },
    )
}

@Composable
private fun CheckButton(done: Boolean, setIndex: Int, onClick: () -> Unit) {
    val c = SlicedTheme.colors
    val shape = RoundedCornerShape(10.dp)
    val label = stringResource(R.string.a11y_set_done, setIndex + 1)
    val st = stringResource(if (done) R.string.a11y_done else R.string.a11y_not_done)
    Box(
        Modifier
            .size(48.dp)
            .clip(shape)
            .background(if (done) c.ok else c.surface)
            .border(1.5.dp, if (done) c.ok else c.line, shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label; stateDescription = st; role = Role.Checkbox },
        contentAlignment = Alignment.Center,
    ) {
        Icon(SlicedIcons.Check, null, tint = if (done) c.onOk else c.muted, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun NoteField(value: String, exerciseName: String, onChange: (String) -> Unit) {
    val c = SlicedTheme.colors
    var text by remember { mutableStateOf(value) }
    LaunchedEffect(value) { if (value != text) text = value }
    BasicTextField(
        value = text,
        onValueChange = { text = it.take(200); onChange(text) },
        singleLine = true,
        textStyle = SlicedTheme.type.small.copy(color = c.fg),
        cursorBrush = SolidColor(c.fg),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        modifier = Modifier
            .fillMaxWidth()
            .dashedTop(c.line)
            .padding(start = 2.dp, end = 2.dp, top = 12.dp, bottom = 8.dp)
            .semantics { contentDescription = "Anotação de $exerciseName" },
        decorationBox = { inner ->
            Box {
                if (text.isEmpty()) Text(stringResource(R.string.workout_note_hint), style = SlicedTheme.type.small, color = c.muted)
                inner()
            }
        },
    )
}
