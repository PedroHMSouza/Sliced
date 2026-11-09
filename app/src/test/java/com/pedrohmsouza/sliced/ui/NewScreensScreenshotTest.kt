package com.pedrohmsouza.sliced.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.pedrohmsouza.sliced.SampleData
import com.pedrohmsouza.sliced.data.InMemorySessionRepository
import com.pedrohmsouza.sliced.model.LibraryExercise
import com.pedrohmsouza.sliced.model.Muscle
import com.pedrohmsouza.sliced.model.RecordType
import com.pedrohmsouza.sliced.model.StaticProgram
import com.pedrohmsouza.sliced.ui.body.BodyScreen
import com.pedrohmsouza.sliced.ui.body.BodyViewModel
import com.pedrohmsouza.sliced.ui.components.BackTopBar
import com.pedrohmsouza.sliced.ui.components.CenteredColumn
import com.pedrohmsouza.sliced.ui.components.MainTab
import com.pedrohmsouza.sliced.ui.components.MainTopBar
import com.pedrohmsouza.sliced.ui.components.ScreenPadding
import com.pedrohmsouza.sliced.ui.edit.ExerciseEditActions
import com.pedrohmsouza.sliced.ui.edit.ExerciseEditScreen
import com.pedrohmsouza.sliced.ui.edit.ExerciseEditTopBar
import com.pedrohmsouza.sliced.ui.edit.ExerciseEditUiState
import com.pedrohmsouza.sliced.ui.edit.ExerciseForm
import com.pedrohmsouza.sliced.ui.edit.ProgramEditActions
import com.pedrohmsouza.sliced.ui.edit.ProgramEditScreen
import com.pedrohmsouza.sliced.ui.edit.ProgramEditUiState
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Telas novas: mapa do corpo, editar ficha e editar exercício. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = Screens.PHONE)
class NewScreensScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun shoot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent {
            SlicedTheme(darkTheme = dark) {
                Column(Modifier.background(SlicedTheme.colors.bg)) { content() }
            }
        }
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }

    private fun bodyState(offset: Int) = runBlocking {
        val vm = BodyViewModel(flowOf(StaticProgram), InMemorySessionRepository(SampleData.history)) { SampleData.today }
        vm.shiftWeek(offset)
        vm.uiState.first { !it.loading && it.offset == offset }
    }

    @Test fun body_lastWeek() {
        val st = bodyState(-1)
        shoot("body_light") {
            MainTopBar(MainTab.Corpo) {}
            CenteredColumn { BodyScreen(st, {}, ScreenPadding) }
        }
    }

    @Test fun body_dark() {
        val st = bodyState(-1)
        shoot("body_dark", dark = true) {
            MainTopBar(MainTab.Corpo) {}
            CenteredColumn { BodyScreen(st, {}, ScreenPadding) }
        }
    }

    @Test fun body_emptyWeek() {
        val st = bodyState(0)
        shoot("body_empty") {
            MainTopBar(MainTab.Corpo) {}
            CenteredColumn { BodyScreen(st, {}, ScreenPadding) }
        }
    }

    @Test fun editProgram() {
        val st = ProgramEditUiState(loading = false, treinos = StaticProgram.treinos, selected = "B")
        val actions = object : ProgramEditActions {
            override fun select(treinoId: String) {}
            override fun rename(name: String) {}
            override fun setDay(day: String) {}
            override fun move(exerciseId: String, delta: Int) {}
            override fun openExercise(exerciseId: String?) {}
        }
        shoot("edit_program") {
            BackTopBar("Treinos", "Editar ficha", {})
            CenteredColumn { ProgramEditScreen(st, actions, ScreenPadding) }
        }
    }

    @Test fun editExercise() {
        val st = ExerciseEditUiState(
            loading = false, isNew = false, treinoId = "A",
            form = ExerciseForm.of(StaticProgram.exercise("supino-maquina")!!),
        )
        val actions = object : ExerciseEditActions {
            override fun name(v: String) {}
            override fun type(v: RecordType) {}
            override fun sets(delta: Int) {}
            override fun repMin(v: String) {}
            override fun repMax(v: String) {}
            override fun rest(delta: Int) {}
            override fun target(v: String) {}
            override fun tip(v: String) {}
            override fun muscle(m: Muscle, primary: Boolean) {}
            override fun library(lib: LibraryExercise) {}
            override fun pickPhoto() {}
            override fun clearPhoto() {}
            override fun delete() {}
        }
        shoot("edit_exercise") {
            ExerciseEditTopBar(st, {}, {})
            CenteredColumn { ExerciseEditScreen(st, actions, ScreenPadding) }
        }
    }
}
