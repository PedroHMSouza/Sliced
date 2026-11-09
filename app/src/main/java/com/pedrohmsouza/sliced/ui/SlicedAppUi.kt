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
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pedrohmsouza.sliced.ui.components.CenteredColumn
import com.pedrohmsouza.sliced.ui.components.ScreenPadding
import com.pedrohmsouza.sliced.ui.home.HomeScreen
import com.pedrohmsouza.sliced.ui.home.HomeViewModel
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
                val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
                val state by vm.uiState.collectAsStateWithLifecycle()
                Column(Modifier.fillMaxSize()) {
                    MainTopBar(MainTab.Treinos, goTab)
                    CenteredColumn {
                        HomeScreen(
                            state = state,
                            onStart = { id -> vm.start(id) { nav.navigate(WorkoutRoute) } },
                            onContinue = { nav.navigate(WorkoutRoute) },
                            contentPadding = ScreenPadding,
                        )
                    }
                }
            }
            composable<WorkoutRoute> {
                Box(Modifier.padding(16.dp)) { Text("Treino em andamento") }
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
