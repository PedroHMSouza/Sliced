package com.pedrohmsouza.sliced.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedrohmsouza.sliced.R
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme

/** Campo de texto com as cores do app. */
@Composable
fun TextInput(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    number: Boolean = false,
) {
    val c = SlicedTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = c.muted.copy(alpha = 0.6f)) } },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        textStyle = SlicedTheme.type.body.copy(color = c.fg),
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
        else KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = if (singleLine) ImeAction.Next else ImeAction.Default),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.fg, unfocusedBorderColor = c.line, focusedLabelColor = c.fg, unfocusedLabelColor = c.muted,
            cursorColor = c.fg, focusedContainerColor = c.bg, unfocusedContainerColor = c.bg,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Pílula selecionável (dia da semana, tipo, músculo). */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = SlicedTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Box(
        Modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(if (selected) c.fg else c.surface)
            .border(1.dp, if (selected) c.fg else c.line, shape)
            .clickable(onClick = onClick)
            .semantics { role = Role.Checkbox; this.selected = selected }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = SlicedTheme.type.button.copy(fontSize = 14.sp), color = if (selected) c.bg else c.fg)
    }
}

/** "−  3  +" para valores pequenos (séries, descanso). */
@Composable
fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier = Modifier) {
    val c = SlicedTheme.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = SlicedTheme.type.body, color = c.fg, modifier = Modifier.weight(1f))
        StepButton("−", stringResource(R.string.a11y_decrease) + " " + label, onMinus)
        Text(
            value, style = SlicedTheme.type.display.copy(fontSize = 20.sp), color = c.fg, textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 64.dp).padding(horizontal = 4.dp),
        )
        StepButton("+", stringResource(R.string.a11y_increase) + " " + label, onPlus)
    }
}

@Composable
private fun StepButton(text: String, a11y: String, onClick: () -> Unit) {
    val c = SlicedTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .size(48.dp)
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.line, shape)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button; contentDescription = a11y },
        contentAlignment = Alignment.Center,
    ) { Text(text, style = SlicedTheme.type.display.copy(fontSize = 22.sp), color = c.fg) }
}
