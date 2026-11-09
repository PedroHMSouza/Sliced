package com.pedrohmsouza.sliced.ui.workout

import com.pedrohmsouza.sliced.SampleData
import com.pedrohmsouza.sliced.data.InMemoryDraftRepository
import com.pedrohmsouza.sliced.data.InMemorySessionRepository
import com.pedrohmsouza.sliced.domain.WorkoutRules
import com.pedrohmsouza.sliced.model.FieldKey
import com.pedrohmsouza.sliced.model.StaticProgram
import com.pedrohmsouza.sliced.ui.UserMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private class FakeRest : RestControl {
        val started = mutableListOf<Pair<Int, String>>()
        var stopped = false
        override fun start(seconds: Int, label: String) { started += seconds to label }
        override fun stop() { stopped = true }
    }

    private class Env(scope: TestScope) {
        val sessions = InMemorySessionRepository(SampleData.history)
        val drafts = InMemoryDraftRepository(
            WorkoutRules.newDraft(StaticProgram.treino("A")!!, SampleData.history, 1_000_000L),
        )
        val rest = FakeRest()
        var clock = 1_000_000L
        val vm = WorkoutViewModel(StaticProgram, sessions, drafts, UserMessages(), rest, { clock }, scope.backgroundScope)
    }

    @Test fun checkEmptySet_copiesPreviousValuesAndStartsRest() = runTest(dispatcher) {
        val env = Env(this)
        backgroundScope.launch { env.vm.uiState.collect {} }
        env.vm.onToggleSet(0, 0)
        val set = env.vm.uiState.value.draft!!.exercises[0].sets[0]
        assertTrue(set.done)
        assertEquals("40", set.value(FieldKey.KG))
        assertEquals("12", set.value(FieldKey.REPS))
        assertEquals(listOf(90 to "Supino reto na máquina"), env.rest.started)
        assertTrue(env.vm.uiState.value.exercises[0].canIncrease)
    }

    @Test fun editsArePersistedToTheDraftRepository() = runTest(dispatcher) {
        val env = Env(this)
        backgroundScope.launch { env.vm.uiState.collect {} }
        env.vm.onFieldChange(1, 0, FieldKey.KG, "47,5kg")
        advanceUntilIdle()
        assertEquals("47,5", env.drafts.draft.first()!!.exercises[1].sets[0].value(FieldKey.KG))
    }

    @Test fun saveStoresOnlyCheckedSetsAndClearsDraft() = runTest(dispatcher) {
        val env = Env(this)
        val draftId = env.drafts.draft.first()!!.id
        backgroundScope.launch { env.vm.uiState.collect {} }
        val events = mutableListOf<WorkoutEvent>()
        backgroundScope.launch { env.vm.eventFlow.collect { events += it } }
        env.vm.onToggleSet(0, 0)
        env.vm.onFieldChange(0, 1, FieldKey.KG, "42,5")
        env.clock += 50 * 60_000L
        env.vm.onSave()
        advanceUntilIdle()

        val saved = env.sessions.sessions.first().first { it.id == draftId }
        assertEquals("A", saved.treino)
        assertEquals(50, saved.durationMin)
        // Só o supino tem série marcada; a puxada tinha nada (sem nota) e saiu.
        assertEquals(listOf("supino-maquina"), saved.exercises.filter { it.sets.isNotEmpty() }.map { it.exerciseId })
        assertEquals(1, saved.exercises.first { it.exerciseId == "supino-maquina" }.sets.size)
        assertNull(env.drafts.draft.first())
        assertTrue(env.rest.stopped)
        assertTrue(events.any { it is WorkoutEvent.Saved })
    }
}
