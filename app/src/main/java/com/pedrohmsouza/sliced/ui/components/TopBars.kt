package com.pedrohmsouza.sliced.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pedrohmsouza.sliced.R
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme

enum class MainTab { Treinos, Historico }

/** Barra superior das telas principais: marca + seletor Treinos/Histórico. */
@Composable
fun MainTopBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    val c = SlicedTheme.colors
    Column(Modifier.background(c.bg)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                PlateMark()
                Text(
                    stringResource(R.string.app_name),
                    style = SlicedTheme.type.brand,
                    color = c.fg,
                    modifier = Modifier.padding(start = 8.dp),
                    maxLines = 2,
                )
            }
            Segmented(selected, onSelect)
        }
        HorizontalDivider(color = c.line)
    }
}

/** Seletor compacto (o SegmentedButton do Material ocupa espaço demais ao lado do título). */
@Composable
private fun Segmented(selected: MainTab, onSelect: (MainTab) -> Unit) {
    val c = SlicedTheme.colors
    val tabs = listOf(MainTab.Treinos to R.string.tab_treinos, MainTab.Historico to R.string.tab_historico)
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(c.soft)
            .padding(3.dp)
            .selectableGroup(),
    ) {
        tabs.forEach { (tab, label) ->
            val on = tab == selected
            Box(
                Modifier
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .then(if (on) Modifier.shadow(1.dp, RoundedCornerShape(8.dp)).background(c.surface) else Modifier)
                    .selectable(selected = on, role = Role.Tab, onClick = { onSelect(tab) })
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(label),
                    style = SlicedTheme.type.button.copy(fontSize = 15.sp),
                    color = if (on) c.fg else c.muted,
                )
            }
        }
    }
}

/** Ícone da marca: uma anilha vermelha. */
@Composable
private fun PlateMark() {
    val c = SlicedTheme.colors
    val red = c.plate("A")
    Box(Modifier.size(22.dp)) {
        Canvas(Modifier.size(22.dp)) {
            val s = size.minDimension
            drawCircle(red, radius = s / 2 - s * 0.09f, style = Stroke(width = s * 0.18f))
            drawCircle(c.fg, radius = s * 0.1f, center = Offset(s / 2, s / 2))
        }
    }
}
