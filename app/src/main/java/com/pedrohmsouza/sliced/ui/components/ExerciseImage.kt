package com.pedrohmsouza.sliced.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.pedrohmsouza.sliced.R
import com.pedrohmsouza.sliced.model.ExerciseLibrary
import com.pedrohmsouza.sliced.model.Muscle
import com.pedrohmsouza.sliced.ui.icons.SlicedIcons
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme
import kotlinx.coroutines.delay
import java.io.File

/**
 * Foto do movimento. Ordem de prioridade:
 * 1. [media]: foto ou GIF escolhido no celular (o GIF anima sozinho);
 * 2. [libraryId]: as duas fotos do catálogo (início e fim). Com [animate], elas se alternam
 *    como um GIF; sem, mostra só a primeira (miniaturas em listas, que gastam menos bateria);
 * 3. nada: um quadro com o ícone de foto.
 */
@Composable
fun ExerciseImage(
    media: String?,
    libraryId: String?,
    modifier: Modifier = Modifier,
    animate: Boolean = false,
    radius: Int = 10,
) {
    val c = SlicedTheme.colors
    val frames = ExerciseLibrary.get(libraryId)?.frames
    Box(
        modifier
            .clip(RoundedCornerShape(radius.dp))
            .background(c.soft),
        contentAlignment = Alignment.Center,
    ) {
        when {
            media != null -> AsyncImage(File(media), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            frames != null -> {
                var frame by remember(libraryId) { mutableIntStateOf(0) }
                if (animate) {
                    LaunchedEffect(libraryId) {
                        while (true) { delay(1_100); frame = (frame + 1) % frames.size }
                    }
                }
                Crossfade(frame, animationSpec = tween(350), label = "frame") { i ->
                    AsyncImage("file:///android_asset/${frames[i]}", null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
            else -> Icon(SlicedIcons.Photo, null, tint = c.muted, modifier = Modifier.size(22.dp))
        }
    }
}

/** Janela com a foto grande do movimento (animada), os músculos e a dica. */
@Composable
fun ExerciseInfoDialog(
    name: String,
    media: String?,
    libraryId: String?,
    primary: List<Muscle>,
    secondary: List<Muscle>,
    tip: String,
    onDismiss: () -> Unit,
) {
    val c = SlicedTheme.colors
    Dialog(onDismiss, DialogProperties(usePlatformDefaultWidth = false)) {
        Card(Modifier.padding(16.dp).widthIn(max = 560.dp).fillMaxWidth()) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ExerciseImage(
                    media, libraryId,
                    Modifier.fillMaxWidth().aspectRatio(3f / 2f).semantics { contentDescription = "Foto do movimento: $name" },
                    animate = true, radius = 12,
                )
                Text(name, style = SlicedTheme.type.exerciseName, color = c.fg)
                if (primary.isNotEmpty()) MuscleLine(stringResource(R.string.muscles_primary), primary)
                if (secondary.isNotEmpty()) MuscleLine(stringResource(R.string.muscles_secondary), secondary)
                if (tip.isNotBlank()) Text(tip, style = SlicedTheme.type.body, color = c.fg)
                if (media == null && libraryId != null) {
                    Text(stringResource(R.string.media_credit), style = SlicedTheme.type.small.copy(fontSize = SlicedTheme.type.tiny.fontSize), color = c.muted)
                }
                PrimaryButton(stringResource(R.string.action_close), onDismiss, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun MuscleLine(label: String, muscles: List<Muscle>) {
    Column {
        Eyebrow(label)
        Text(muscles.joinToString(" · ") { it.label }, style = SlicedTheme.type.small, color = SlicedTheme.colors.fg)
    }
}
