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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
                    maxLines = 1,
                )
            }
            SingleChoiceSegmentedButtonRow {
                val tabs = listOf(MainTab.Treinos to R.string.tab_treinos, MainTab.Historico to R.string.tab_historico)
                tabs.forEachIndexed { i, (tab, label) ->
                    SegmentedButton(
                        selected = selected == tab,
                        onClick = { onSelect(tab) },
                        shape = SegmentedButtonDefaults.itemShape(i, tabs.size),
                        icon = {},
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = c.surface,
                            activeContentColor = c.fg,
                            inactiveContainerColor = c.soft,
                            inactiveContentColor = c.muted,
                            activeBorderColor = c.line,
                            inactiveBorderColor = c.line,
                        ),
                    ) { Text(stringResource(label), style = SlicedTheme.type.button.copy(fontSize = SlicedTheme.type.small.fontSize)) }
                }
            }
        }
        HorizontalDivider(color = c.line)
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
