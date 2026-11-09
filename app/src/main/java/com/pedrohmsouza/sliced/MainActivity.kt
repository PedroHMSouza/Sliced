package com.pedrohmsouza.sliced

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pedrohmsouza.sliced.ui.SlicedAppUi
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme

/**
 * Única Activity do app. Com Compose + Navigation, as "telas" são composables e a
 * Activity só hospeda a árvore de UI (padrão "single activity").
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Desenha atrás das barras do sistema (obrigatório no Android 15+); cada tela
        // trata os "insets" para o conteúdo não ficar embaixo da barra de status.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SlicedTheme {
                SlicedAppUi()
            }
        }
    }
}
