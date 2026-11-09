package com.pedrohmsouza.sliced.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pedrohmsouza.sliced.ui.components.MainTab
import com.pedrohmsouza.sliced.ui.components.MainTopBar
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme

/**
 * Raiz da UI: um Scaffold global (onde depois entram a barra do descanso e os avisos)
 * e o NavHost com as três telas.
 */
@Composable
fun SlicedAppUi() {
    val nav = rememberNavController()
    val goTab: (MainTab) -> Unit = { tab ->
        val route: Any = if (tab == MainTab.Treinos) HomeRoute else HistoryRoute()
        nav.navigate(route) {
            popUpTo(HomeRoute) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    Scaffold(containerColor = SlicedTheme.colors.bg) { inner ->
        NavHost(
            navController = nav,
            startDestination = HomeRoute,
            modifier = Modifier.padding(inner).consumeWindowInsets(inner).imePadding(),
        ) {
            composable<HomeRoute> {
                Column(Modifier.fillMaxSize()) {
                    MainTopBar(MainTab.Treinos, goTab)
                    Box(Modifier.padding(16.dp)) { Text("Treinos") }
                }
            }
            composable<HistoryRoute> {
                Column(Modifier.fillMaxSize()) {
                    MainTopBar(MainTab.Historico, goTab)
                    Box(Modifier.padding(16.dp)) { Text("Histórico") }
                }
            }
        }
    }
}
