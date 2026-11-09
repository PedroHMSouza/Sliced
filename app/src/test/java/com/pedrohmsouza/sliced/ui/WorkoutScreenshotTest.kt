package com.pedrohmsouza.sliced.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.pedrohmsouza.sliced.SampleData
import com.pedrohmsouza.sliced.domain.WorkoutRules
import com.pedrohmsouza.sliced.domain.WorkoutRules.updateSet
import com.pedrohmsouza.sliced.model.Draft
import com.pedrohmsouza.sliced.model.DraftSet
import com.pedrohmsouza.sliced.model.FieldKey
import com.pedrohmsouza.sliced.model.StaticProgram
import com.pedrohmsouza.sliced.ui.components.CenteredColumn
import com.pedrohmsouza.sliced.ui.components.ScreenPadding
import com.pedrohmsouza.sliced.ui.theme.SlicedTheme
import com.pedrohmsouza.sliced.ui.workout.ExerciseUi
import com.pedrohmsouza.sliced.ui.workout.WorkoutActions
import com.pedrohmsouza.sliced.ui.workout.WorkoutScreen
import com.pedrohmsouza.sliced.ui.workout.WorkoutTopBar
import com.pedrohmsouza.sliced.ui.workout.WorkoutUiState
import com.pedrohmsouza.sliced.model.FieldKey as K
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = Screens.PHONE)
class WorkoutScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val noop = object : WorkoutActions {
        override fun field(ei: Int, si: Int, key: FieldKey, value: String) {}
        override fun note(ei: Int, text: String) {}
        override fun toggle(ei: Int, si: Int) {}
        override fun addSet(ei: Int) {}
        override fun removeSet(ei: Int) {}
        override fun rest(ei: Int) {}
        override fun finish() {}
    }

    fun state(treino: String): WorkoutUiState {
        val s = SampleData.history
        val start = SampleData.at(SampleData.today, 7, 0)
        var d: Draft = WorkoutRules.newDraft(StaticProgram.treino(treino)!!, s, start)
        d = d.updateSet(0, 0) { DraftSet(mapOf(K.KG to "42,5", K.REPS to "12"), done = true) }
        d = d.updateSet(0, 1) { DraftSet(mapOf(K.KG to "42,5", K.REPS to "11"), done = true) }
        d = d.updateSet(0, 2) { DraftSet(mapOf(K.KG to "42,5")) }
        return WorkoutUiState(
            loading = false, draft = d, treino = StaticProgram.treino(treino),
            exercises = d.exercises.map { e ->
                val p = StaticProgram.exercise(e.exerciseId)!!
                ExerciseUi(p, WorkoutRules.previousSets(s, e.exerciseId).orEmpty(), WorkoutRules.canIncrease(p, s))
            },
            progress = WorkoutRules.progress(d),
        )
    }

    private fun shoot(name: String, dark: Boolean, st: WorkoutUiState) {
        compose.setContent {
            SlicedTheme(darkTheme = dark) {
                Column(Modifier.background(SlicedTheme.colors.bg)) {
                    WorkoutTopBar(st, {}, {}, now = { st.draft!!.startedAt + 23 * 60_000L + 7_000 })
                    CenteredColumn { WorkoutScreen(st, noop, null, ScreenPadding) }
                }
            }
        }
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }

    @Test fun workoutA_light() = shoot("workout_a_light", false, state("A"))
    @Test fun workoutA_dark() = shoot("workout_a_dark", true, state("A"))
    @Test fun workoutC_light() = shoot("workout_c_light", false, state("C"))
}
