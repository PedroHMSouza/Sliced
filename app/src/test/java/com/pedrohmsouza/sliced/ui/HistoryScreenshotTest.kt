package com.pedrohmsouza.sliced.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.pedrohmsouza.sliced.SampleData
import com.pedrohmsouza.sliced.data.InMemorySessionRepository
import com.pedrohmsouza.sliced.model.Session
import com.pedrohmsouza.sliced.model.StaticProgram
import com.pedrohmsouza.sliced.ui.components.CenteredColumn
import com.pedrohmsouza.sliced.ui.components.MainTab
import com.pedrohmsouza.sliced.ui.components.MainTopBar
import com.pedrohmsouza.sliced.ui.components.ScreenPadding
import com.pedrohmsouza.sliced.ui.history.HistoryScreen
import com.pedrohmsouza.sliced.ui.history.HistoryUiState
import com.pedrohmsouza.sliced.ui.history.HistoryViewModel
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = Screens.PHONE)
class HistoryScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun state(sessions: List<Session>): HistoryUiState = runBlocking {
        val vm = HistoryViewModel(flowOf(StaticProgram), InMemorySessionRepository(sessions), UserMessages()) { SampleData.today }
        vm.uiState.first { !it.loading }
    }

    private fun shoot(name: String, dark: Boolean, st: HistoryUiState, open: String? = null) {
        compose.setContent {
            SlicedTheme(darkTheme = dark) {
                Column(Modifier.background(SlicedTheme.colors.bg)) {
                    MainTopBar(MainTab.Historico) {}
                    CenteredColumn { HistoryScreen(st, open, {}, {}, ScreenPadding) }
                }
            }
        }
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }

    @Test fun history_light() = shoot("history_light", false, state(SampleData.history), SampleData.history[4].id)
    @Test fun history_dark() = shoot("history_dark", true, state(SampleData.history))
    @Test fun history_empty() = shoot("history_empty", false, state(emptyList()))
}
