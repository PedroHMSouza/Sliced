package com.pedrohmsouza.sliced.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedrohmsouza.sliced.R
import com.pedrohmsouza.sliced.domain.Fmt
import com.pedrohmsouza.sliced.domain.WorkoutRules
import com.pedrohmsouza.sliced.ui.components.GhostButton
import com.pedrohmsouza.sliced.ui.components.PrimaryButton
import com.pedrohmsouza.sliced.ui.components.SecondaryButton
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme

/** Bottom sheet "Finalizar treino": resumo + Salvar / Continuar treinando / Descartar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishSheet(
    state: WorkoutUiState,
    now: () -> Long,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
) {
    val d = state.draft ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SlicedTheme.colors.surface,
    ) {
        FinishSheetContent(
            treinoId = d.treino,
            minutes = WorkoutRules.durationMin(d.startedAt, now()),
            progress = state.progress,
            saving = state.saving,
            onSave = onSave,
            onContinue = onDismiss,
            onDiscard = { confirmDiscard = true },
        )
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.discard_title, d.treino)) },
            text = { Text(stringResource(R.string.discard_text)) },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onDiscard() }) {
                    Text(stringResource(R.string.discard_confirm), color = SlicedTheme.colors.danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.cancel)) } },
            containerColor = SlicedTheme.colors.surface,
        )
    }
}

/** Conteúdo separado do ModalBottomSheet para dar para testar/visualizar sozinho. */
@Composable
fun FinishSheetContent(
    treinoId: String,
    minutes: Int,
    progress: WorkoutRules.Progress,
    saving: Boolean,
    onSave: () -> Unit,
    onContinue: () -> Unit,
    onDiscard: () -> Unit,
) {
    val c = SlicedTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.finish_title, treinoId), style = SlicedTheme.type.display.copy(fontSize = 26.sp), color = c.fg)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryItem(stringResource(R.string.finish_duration), "$minutes min", Modifier.weight(1f))
            SummaryItem(stringResource(R.string.finish_sets), "${progress.done}/${progress.total}", Modifier.weight(1f))
            SummaryItem(stringResource(R.string.finish_volume), "${Fmt.number(Math.round(progress.volumeKg))} kg", Modifier.weight(1f))
        }
        Text(
            stringResource(if (progress.done > 0) R.string.finish_only_checked else R.string.finish_none_checked),
            style = SlicedTheme.type.small, color = c.muted,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(
                stringResource(if (saving) R.string.finish_saving else R.string.finish_save),
                onSave, Modifier.fillMaxWidth(), enabled = progress.done > 0 && !saving,
            )
            SecondaryButton(stringResource(R.string.finish_continue), onContinue, Modifier.fillMaxWidth())
        }
        GhostButton(stringResource(R.string.finish_discard), onDiscard, color = c.danger)
    }
}

@Composable
private fun SummaryItem(label: String, value: String, modifier: Modifier) {
    val c = SlicedTheme.colors
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(c.bg).padding(10.dp)) {
        Text(label.uppercase(), style = SlicedTheme.type.tiny, color = c.muted)
        Text(value, style = SlicedTheme.type.display.copy(fontSize = 22.sp), color = c.fg, modifier = Modifier.padding(top = 2.dp))
    }
}
