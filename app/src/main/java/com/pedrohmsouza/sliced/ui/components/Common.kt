package com.pedrohmsouza.sliced.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme

enum class PlateSize(val size: Dp, val border: Dp, val font: TextUnit) {
    Small(26.dp, 4.dp, 13.sp), Medium(34.dp, 5.dp, 16.sp), Large(46.dp, 7.dp, 20.sp)
}

/** A "anilha": letra do treino num círculo com borda grossa na cor do treino. */
@Composable
fun Plate(treinoId: String, modifier: Modifier = Modifier, size: PlateSize = PlateSize.Medium) {
    val c = SlicedTheme.colors
    Box(
        modifier
            .size(size.size)
            .clip(CircleShape)
            .background(c.surface)
            .border(size.border, c.plate(treinoId), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(treinoId, style = SlicedTheme.type.display.copy(fontSize = size.font, lineHeight = size.font), color = c.fg)
    }
}

/** Cartão com borda fina, padrão de todos os blocos do app. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    borderColor: Color = SlicedTheme.colors.line,
    radius: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = SlicedTheme.colors
    Column(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(c.surface)
            .border(1.dp, borderColor, RoundedCornerShape(radius)),
        content = content,
    )
}

/** Botão principal (preto no tema claro, claro no escuro). Altura mínima de 48 dp. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    small: Boolean = false,
) {
    val c = SlicedTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = if (small) 40.dp else 48.dp),
        shape = RoundedCornerShape(if (small) 10.dp else 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = c.fg, contentColor = c.bg,
            disabledContainerColor = c.fg.copy(alpha = 0.45f), disabledContentColor = c.bg,
        ),
        contentPadding = PaddingValues(horizontal = if (small) 14.dp else 18.dp),
    ) { Text(text, style = SlicedTheme.type.button.copy(fontSize = if (small) 15.sp else 16.sp)) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = SlicedTheme.colors
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, c.line),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = c.surface, contentColor = c.fg),
    ) { Text(text, style = SlicedTheme.type.button) }
}

/** Botão de texto discreto ("+ Série", "Descanso 90 s"). */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = SlicedTheme.colors.muted,
    leading: (@Composable RowScope.() -> Unit)? = null,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = color),
    ) {
        leading?.invoke(this)
        Text(text, style = SlicedTheme.type.button.copy(fontSize = 14.sp))
    }
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = SlicedTheme.type.eyebrow, color = SlicedTheme.colors.muted, modifier = modifier)
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = SlicedTheme.type.section, color = SlicedTheme.colors.fg, modifier = modifier)
}

/** Etiqueta em forma de pílula ("Próximo", "Subir carga"). */
@Composable
fun Tag(text: String, borderColor: Color? = null, background: Color? = null, color: Color = SlicedTheme.colors.fg) {
    val shape = RoundedCornerShape(999.dp)
    Text(
        text.uppercase(),
        style = SlicedTheme.type.tiny,
        color = color,
        maxLines = 1,
        modifier = Modifier
            .clip(shape)
            .then(if (background != null) Modifier.background(background) else Modifier)
            .then(if (borderColor != null) Modifier.border(1.5.dp, borderColor, shape) else Modifier)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

/** Caixa tracejada usada nos estados vazios. */
@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    val c = SlicedTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .dashedBorder(c.line, 14.dp)
            .padding(horizontal = 18.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = SlicedTheme.type.treinoName, color = c.fg)
        Text(body, style = SlicedTheme.type.body, color = c.muted, textAlign = TextAlign.Start)
    }
}

@Composable
fun StoreNote(text: String, warn: Boolean = false) {
    Text(
        text,
        style = SlicedTheme.type.small.copy(fontSize = 13.sp),
        color = if (warn) SlicedTheme.colors.warn else SlicedTheme.colors.muted,
        modifier = Modifier.padding(top = 2.dp),
    )
}

/** Linha horizontal simples. */
@Composable
fun Line(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().heightIn(min = 1.dp, max = 1.dp).background(SlicedTheme.colors.line))
}

@Composable
fun SpacedRow(modifier: Modifier = Modifier, gap: Dp = 12.dp, content: @Composable RowScope.() -> Unit) =
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically, content = content)
