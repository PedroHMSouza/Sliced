package com.pedrohmsouza.sliced.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.pedrohmsouza.sliced.ui.history.HistoryScreen
import com.pedrohmsouza.sliced.ui.history.HistoryViewModel
import com.pedrohmsouza.sliced.R
import com.pedrohmsouza.sliced.SlicedApp
import com.pedrohmsouza.sliced.ui.body.BodyScreen
import com.pedrohmsouza.sliced.ui.body.BodyViewModel
import com.pedrohmsouza.sliced.ui.components.BackTopBar
import com.pedrohmsouza.sliced.ui.edit.ExerciseEditEvent
import com.pedrohmsouza.sliced.ui.edit.ExerciseEditScreen
import com.pedrohmsouza.sliced.ui.edit.ExerciseEditTopBar
import com.pedrohmsouza.sliced.ui.edit.ExerciseEditViewModel
import com.pedrohmsouza.sliced.ui.edit.ProgramEditActions
import com.pedrohmsouza.sliced.ui.edit.ProgramEditScreen
import com.pedrohmsouza.sliced.ui.edit.ProgramEditViewModel
import com.pedrohmsouza.sliced.ui.edit.asActions
import com.pedrohmsouza.sliced.ui.edit.rememberPhotoPicker
import com.pedrohmsouza.sliced.ui.components.CenteredColumn
import com.pedrohmsouza.sliced.ui.components.MainTab
import com.pedrohmsouza.sliced.ui.components.MainTopBar
import com.pedrohmsouza.sliced.ui.components.ScreenPadding
import com.pedrohmsouza.sliced.ui.home.HomeScreen
import com.pedrohmsouza.sliced.ui.home.HomeViewModel
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme
import com.pedrohmsouza.sliced.ui.workout.FinishSheet
import com.pedrohmsouza.sliced.ui.workout.KeepScreenOn
import com.pedrohmsouza.sliced.ui.components.RestBar
import com.pedrohmsouza.sliced.ui.workout.rememberNotificationAccess
import com.pedrohmsouza.sliced.ui.workout.WorkoutEvent
import com.pedrohmsouza.sliced.ui.workout.WorkoutScreen
import com.pedrohmsouza.sliced.ui.workout.WorkoutTopBar
import com.pedrohmsouza.sliced.ui.workout.WorkoutViewModel
import com.pedrohmsouza.sliced.ui.workout.asActions
import kotlinx.coroutines.flow.first

/**
 * Raiz da UI: um Scaffold global (barra do descanso embaixo e avisos/snackbar) e o
 * NavHost com as telas. Cada tela desenha a própria barra superior.
 */
@Composable
fun SlicedAppUi() {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as SlicedApp).container }
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current

    // Avisos vindos dos ViewModels (ex.: "Treino A salvo · 52 min").
    LaunchedEffect(Unit) {
        container.messages.messages.collect { m ->
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(resources.getString(m.res, *m.args.toTypedArray()))
        }
    }

    // Se o app foi fechado no meio de um treino, abre direto nele (só na primeira composição).
    var resumed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!resumed) {
            resumed = true
            if (container.drafts.draft.first() != null) nav.navigate(WorkoutRoute)
        }
    }

    val rest = container.restTimer
    val restState by rest.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = SlicedTheme.colors.bg,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            AnimatedVisibility(restState != null, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                // Guarda o último estado para a animação de saída ter o que desenhar.
                val last = remember { mutableStateOf(restState) }
                restState?.let { last.value = it }
                last.value?.let { RestBar(it, onAdjust = rest::adjust, onSkip = rest::stop, now = container.now) }
            }
        },
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = HomeRoute,
            modifier = Modifier.padding(inner).consumeWindowInsets(inner).imePadding(),
        ) {
            composable<HomeRoute> {
                val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
                val state by vm.uiState.collectAsStateWithLifecycle()
                Column(Modifier.fillMaxSize()) {
                    MainTopBar(MainTab.Treinos) { nav.goTab(it) }
                    CenteredColumn {
                        HomeScreen(
                            state = state,
                            onStart = { id -> vm.start(id) { nav.navigate(WorkoutRoute) { launchSingleTop = true } } },
                            onContinue = { nav.navigate(WorkoutRoute) { launchSingleTop = true } },
                            contentPadding = ScreenPadding,
                            onEdit = { id -> nav.navigate(EditProgramRoute(id)) { launchSingleTop = true } },
                        )
                    }
                }
            }
            composable<WorkoutRoute> {
                val vm: WorkoutViewModel = viewModel(factory = WorkoutViewModel.factory(rest))
                val state by vm.uiState.collectAsStateWithLifecycle()
                val notifications = rememberNotificationAccess()
                KeepScreenOn()
                LaunchedEffect(state.loading, state.draft, state.closed) {
                    // Não havia treino em andamento (ex.: voltou por um atalho velho): volta para Treinos.
                    // Depois de salvar/descartar, quem navega é o evento Saved/Closed.
                    if (!state.loading && state.draft == null && !state.closed) nav.leaveWorkout(null)
                }
                // Um único coletor de eventos (um Channel entrega cada evento a um só coletor).
                var focusRequest by remember { mutableStateOf<WorkoutEvent.FocusField?>(null) }
                LaunchedEffect(vm) {
                    vm.eventFlow.collect { e ->
                        when (e) {
                            is WorkoutEvent.Saved -> nav.leaveWorkout(e.sessionId)
                            WorkoutEvent.Closed -> nav.leaveWorkout(null)
                            is WorkoutEvent.FocusField -> focusRequest = e
                        }
                    }
                }
                Column(Modifier.fillMaxSize()) {
                    if (state.draft == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    } else {
                        WorkoutTopBar(state, onBack = { nav.leaveWorkout(null) }, onFinish = vm::onOpenFinish, now = container.now)
                        CenteredColumn {
                            if (state.showFinish) {
                                FinishSheet(state, container.now, vm::onSave, vm::onCloseFinish, vm::onDiscard)
                            }
                            WorkoutScreen(
                                state, remember(vm) { vm.asActions() }, focusRequest, ScreenPadding,
                                onEnableNotifications = notifications.request.takeIf { !notifications.enabled },
                            )
                        }
                    }
                }
            }
            composable<HistoryRoute> { entry ->
                val route = entry.toRoute<HistoryRoute>()
                val vm: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory)
                val state by vm.uiState.collectAsStateWithLifecycle()
                Column(Modifier.fillMaxSize()) {
                    MainTopBar(MainTab.Historico) { nav.goTab(it) }
                    CenteredColumn {
                        HistoryScreen(state, route.openSessionId, vm::delete, vm::selectChartExercise, ScreenPadding, vm::shiftMonth)
                    }
                }
            }
            composable<BodyRoute> {
                val vm: BodyViewModel = viewModel(factory = BodyViewModel.Factory)
                val state by vm.uiState.collectAsStateWithLifecycle()
                Column(Modifier.fillMaxSize()) {
                    MainTopBar(MainTab.Corpo) { nav.goTab(it) }
                    CenteredColumn { BodyScreen(state, vm::shiftWeek, ScreenPadding) }
                }
            }
            composable<EditProgramRoute> { entry ->
                val route = entry.toRoute<EditProgramRoute>()
                val vm: ProgramEditViewModel = viewModel(factory = ProgramEditViewModel.factory(route.treinoId))
                val state by vm.uiState.collectAsStateWithLifecycle()
                val actions = remember(vm) {
                    object : ProgramEditActions {
                        override fun select(treinoId: String) = vm.select(treinoId)
                        override fun rename(name: String) = vm.rename(name)
                        override fun setDay(day: String) = vm.setDay(day)
                        override fun move(exerciseId: String, delta: Int) = vm.move(exerciseId, delta)
                        override fun openExercise(exerciseId: String?) {
                            nav.navigate(EditExerciseRoute(vm.uiState.value.selected, exerciseId)) { launchSingleTop = true }
                        }
                    }
                }
                Column(Modifier.fillMaxSize()) {
                    BackTopBar(stringResource(R.string.workout_back), stringResource(R.string.edit_program_title), { nav.popBackStack() })
                    CenteredColumn { ProgramEditScreen(state, actions, ScreenPadding) }
                }
            }
            composable<EditExerciseRoute> { entry ->
                val route = entry.toRoute<EditExerciseRoute>()
                val vm: ExerciseEditViewModel = viewModel(factory = ExerciseEditViewModel.factory(route.treinoId, route.exerciseId))
                val state by vm.uiState.collectAsStateWithLifecycle()
                val pickPhoto = rememberPhotoPicker(vm::pickMedia)
                val actions = remember(vm) { vm.asActions { pickPhoto() } }
                LaunchedEffect(vm) {
                    vm.eventFlow.collect { e ->
                        when (e) {
                            ExerciseEditEvent.Done -> if (entry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) nav.popBackStack()
                        }
                    }
                }
                Column(Modifier.fillMaxSize()) {
                    ExerciseEditTopBar(state, { nav.popBackStack() }, vm::save)
                    CenteredColumn { ExerciseEditScreen(state, actions, ScreenPadding) }
                }
            }
        }
    }
}

private fun NavHostController.goTab(tab: MainTab) {
    val route: Any = when (tab) {
        MainTab.Treinos -> HomeRoute
        MainTab.Historico -> HistoryRoute()
        MainTab.Corpo -> BodyRoute
    }
    navigate(route) {
        popUpTo(HomeRoute) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Sai do treino: para o Histórico (com a sessão salva aberta) ou de volta para Treinos. */
private fun NavHostController.leaveWorkout(savedSessionId: String?) {
    // Durante a animação de saída a tela de treino ainda está na composição e pode pedir
    // para sair de novo; só age se o treino for mesmo a tela atual.
    if (currentDestination?.hasRoute(WorkoutRoute::class) != true) return
    if (savedSessionId != null) {
        navigate(HistoryRoute(savedSessionId)) { popUpTo(HomeRoute) }
    } else if (!popBackStack(HomeRoute, inclusive = false)) {
        navigate(HomeRoute) { popUpTo(0) }
    }
}
