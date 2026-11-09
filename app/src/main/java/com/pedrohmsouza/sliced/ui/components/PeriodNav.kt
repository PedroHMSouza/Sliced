package com.pedrohmsouza.sliced.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pedrohmsouza.sliced.R
import com.pedrohmsouza.sliced.ui.icons.SlicedIcons
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme

/** Cabeçalho "‹ Novembro de 2026 ›" para andar entre meses ou semanas. */
@Composable
fun PeriodNav(
    title: String,
    subtitle: String?,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    canNext: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = SlicedTheme.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onPrev) { Icon(SlicedIcons.Back, stringResource(R.string.a11y_previous), tint = c.fg, modifier = Modifier.size(20.dp)) }
        Column(
            Modifier.weight(1f).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(title, style = SlicedTheme.type.treinoName, color = c.fg, textAlign = TextAlign.Center)
            subtitle?.let { Text(it, style = SlicedTheme.type.small, color = c.muted, textAlign = TextAlign.Center) }
        }
        IconButton(onNext, enabled = canNext) {
            Icon(SlicedIcons.Forward, stringResource(R.string.a11y_next), tint = if (canNext) c.fg else c.line, modifier = Modifier.size(20.dp))
        }
    }
}
