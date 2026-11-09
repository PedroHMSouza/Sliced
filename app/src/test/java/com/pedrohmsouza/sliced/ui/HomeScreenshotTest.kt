package com.pedrohmsouza.sliced.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.pedrohmsouza.sliced.SampleData
import com.pedrohmsouza.sliced.domain.Fmt
import com.pedrohmsouza.sliced.domain.WorkoutRules
import com.pedrohmsouza.sliced.model.StaticProgram
import com.pedrohmsouza.sliced.ui.components.CenteredColumn
import com.pedrohmsouza.sliced.ui.components.MainTab
import com.pedrohmsouza.sliced.ui.components.MainTopBar
import com.pedrohmsouza.sliced.ui.components.ScreenPadding
import com.pedrohmsouza.sliced.ui.home.DraftBanner
import com.pedrohmsouza.sliced.ui.home.HomeScreen
import com.pedrohmsouza.sliced.ui.home.HomeUiState
import com.pedrohmsouza.sliced.ui.home.TreinoItem
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = Screens.PHONE)
class HomeScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun state(draft: DraftBanner? = null): HomeUiState {
        val s = SampleData.history
        return HomeUiState(
            loading = false,
            week = WorkoutRules.week(s, SampleData.today),
            next = WorkoutRules.nextTreino(s, SampleData.today, StaticProgram),
            treinos = StaticProgram.treinos.map { t -> TreinoItem(t, WorkoutRules.lastOf(s, t.id)?.let { Fmt.dateLabel(it.startedAt, SampleData.zone) }) },
            draft = draft,
        )
    }

    private fun shoot(name: String, dark: Boolean, st: HomeUiState) {
        compose.setContent {
            SlicedTheme(darkTheme = dark) {
                Column(Modifier.background(SlicedTheme.colors.bg)) {
                    MainTopBar(MainTab.Treinos) {}
                    CenteredColumn { HomeScreen(st, {}, {}, ScreenPadding) }
                }
            }
        }
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }

    @Test fun home_light() = shoot("home_light", false, state())
    @Test fun home_dark() = shoot("home_dark", true, state())
    @Test fun home_inProgress() = shoot("home_in_progress", false, state(DraftBanner("A", "07:12")))
}

object Screens {
    /** Galaxy S22 Ultra: ~412 × 915 dp. Densidade baixa para imagens leves. */
    const val PHONE = "w412dp-h915dp-xhdpi"
}
