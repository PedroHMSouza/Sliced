package com.pedrohmsouza.sliced.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Espaçamento padrão do conteúdo das telas (16 dp nas laterais, como na versão web). */
val ScreenPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 32.dp)

/** Limita a largura a 640 dp e centraliza (celular deitado, tablet, tela dobrável). */
@Composable
fun CenteredColumn(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = 640.dp)) { content() }
    }
}
