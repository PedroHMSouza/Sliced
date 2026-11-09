package com.pedrohmsouza.sliced.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedrohmsouza.sliced.R
import com.pedrohmsouza.sliced.domain.Fmt
import com.pedrohmsouza.sliced.timer.RestState
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme
import kotlinx.coroutines.delay

/** Barra do descanso, fixa embaixo: contagem grande, exercício, progresso, −15 / +15 / Pular. */
@Composable
fun RestBar(
    state: RestState,
    onAdjust: (Int) -> Unit,
    onSkip: () -> Unit,
    now: () -> Long = System::currentTimeMillis,
) {
    val c = SlicedTheme.colors
    val left by produceState(state.secondsLeft(now()), state) {
        while (true) { value = state.secondsLeft(now()); delay(250) }
    }
    val over = state.finished || left == 0
    val fg = c.bg // a barra é "invertida": fundo escuro no tema claro e vice-versa
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(16.dp)
            .background(c.fg)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.widthIn(max = 640.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
                Text(
                    if (over) stringResource(R.string.rest_over) else stringResource(R.string.rest_label, state.label),
                    style = SlicedTheme.type.small.copy(fontSize = 13.sp), color = fg.copy(alpha = 0.75f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(Fmt.seconds(left), style = SlicedTheme.type.bigTimer, color = if (over) c.ok else fg)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RestButton("−15", stringResource(R.string.a11y_minus15), fg, Color.Transparent, fg) { onAdjust(-15) }
                RestButton("+15", stringResource(R.string.a11y_plus15), fg, Color.Transparent, fg) { onAdjust(15) }
                RestButton(stringResource(R.string.rest_skip), null, c.fg, c.bg, c.bg, onSkip)
            }
        }
        Box(
            Modifier
                .padding(top = 10.dp)
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0x597F7F7F)),
        ) {
            val frac = if (state.totalSec > 0) (left.toFloat() / state.totalSec).coerceIn(0f, 1f) else 0f
            Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(fg))
        }
    }
}

@Composable
private fun RestButton(text: String, a11y: String?, color: Color, bg: Color, border: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .size(width = 56.dp, height = 48.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, border.copy(alpha = 0.9f), shape)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button; if (a11y != null) contentDescription = a11y },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = SlicedTheme.type.display.copy(fontSize = 17.sp), color = color)
    }
}
