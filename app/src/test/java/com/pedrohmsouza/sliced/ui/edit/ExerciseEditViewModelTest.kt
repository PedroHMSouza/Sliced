package com.pedrohmsouza.sliced.ui.edit

import android.net.Uri
import com.pedrohmsouza.sliced.data.InMemoryProgramRepository
import com.pedrohmsouza.sliced.data.MediaFiles
import com.pedrohmsouza.sliced.domain.ProgramRules
import com.pedrohmsouza.sliced.model.ExerciseLibrary
import com.pedrohmsouza.sliced.model.Muscle
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseEditViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private class FakeMedia : MediaFiles {
        val deleted = mutableListOf<String?>()
        override suspend fun import(uri: Uri): String? = null
        override suspend fun delete(path: String?) { deleted += path }
    }

    private class Env(scope: TestScope, exerciseId: String?) {
        val repo = InMemoryProgramRepository()
        val media = FakeMedia()
        val vm = ExerciseEditViewModel("A", exerciseId, repo, media, UserMessages(), scope.backgroundScope)
    }

    @Test fun newExerciseFromLibraryIsAddedAtTheEnd() = runTest(dispatcher) {
        val env = Env(this, null)
        val events = mutableListOf<ExerciseEditEvent>()
        backgroundScope.launch { env.vm.eventFlow.collect { events += it } }
        assertTrue(env.vm.uiState.value.isNew)

        env.vm.pickLibrary(ExerciseLibrary.get("Preacher_Curl")!!)
        env.vm.changeSets(1)
        env.vm.save()
        advanceUntilIdle()

        val added = env.repo.program.first().treino("A")!!.exercises.last()
        assertTrue(added.id.startsWith("rosca-scott-"))
        assertEquals("Rosca Scott", added.name)
        assertEquals("Preacher_Curl", added.libraryId)
        assertEquals(4, added.sets)
        assertEquals(8..12, added.repRange)
        assertEquals(listOf(Muscle.BICEPS), added.primary)
        assertEquals(listOf(ExerciseEditEvent.Done), events)
    }

    @Test fun editingKeepsIdSoHistoryStaysLinked() = runTest(dispatcher) {
        val env = Env(this, "supino-maquina")
        assertFalse(env.vm.uiState.value.isNew)
        assertEquals("Supino reto na máquina", env.vm.uiState.value.form.name)

        env.vm.setName("Supino máquina articulada")
        env.vm.setRepMin("12"); env.vm.setRepMax("6") // invertido: vira 6..12
        env.vm.toggleMuscle(Muscle.TRICEPS, primary = true) // sai dos secundários
        env.vm.save()
        advanceUntilIdle()

        val e = env.repo.program.first().exercise("supino-maquina")!!
        assertEquals("Supino máquina articulada", e.name)
        assertEquals(6..12, e.repRange)
        assertEquals(listOf(Muscle.CHEST, Muscle.TRICEPS), e.primary)
        assertFalse(Muscle.TRICEPS in e.secondary)
        assertEquals(StaticProgram.treino("A")!!.exercises.map { it.id }, env.repo.program.first().treino("A")!!.exercises.map { it.id })
    }

    @Test fun blankNameIsNotSaved() = runTest(dispatcher) {
        val env = Env(this, null)
        env.vm.save()
        advanceUntilIdle()
        assertEquals(StaticProgram.treino("A")!!.exercises.size, env.repo.program.first().treino("A")!!.exercises.size)
    }

    @Test fun deleteRemovesFromProgramAndDropsCustomPhoto() = runTest(dispatcher) {
        val env = Env(this, "rosca-direta")
        env.repo.update { p ->
            ProgramRules.saveExercise(p, "A", p.exercise("rosca-direta")!!.copy(media = "/files/minha.gif"))
        }
        val vm = ExerciseEditViewModel("A", "rosca-direta", env.repo, env.media, UserMessages(), backgroundScope)
        vm.delete()
        advanceUntilIdle()
        assertNull(env.repo.program.first().exercise("rosca-direta"))
        assertEquals(listOf<String?>("/files/minha.gif"), env.media.deleted)
    }
}
