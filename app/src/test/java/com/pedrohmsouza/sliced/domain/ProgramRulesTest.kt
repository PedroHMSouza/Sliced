package com.pedrohmsouza.sliced.domain

import com.pedrohmsouza.sliced.model.ExerciseLibrary
import com.pedrohmsouza.sliced.model.StaticProgram
import com.pedrohmsouza.sliced.model.toProgram
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ProgramRulesTest {
    private val P = StaticProgram.toProgram()
    private val ids get() = P.treino("A")!!.exercises.map { it.id }

    @Test fun saveExistingReplacesInPlace() {
        val e = P.exercise("puxada-frontal")!!.copy(name = "Puxada aberta", sets = 4)
        val p = ProgramRules.saveExercise(P, "A", e)
        assertEquals(ids, p.treino("A")!!.exercises.map { it.id })
        assertEquals("Puxada aberta", p.exercise("puxada-frontal")!!.name)
        assertEquals(4, p.exercise("puxada-frontal")!!.sets)
    }

    @Test fun saveNewGoesToTheEnd() {
        val e = P.exercise("supino-maquina")!!.copy(id = "crucifixo-ab12", name = "Crucifixo")
        val p = ProgramRules.saveExercise(P, "A", e)
        assertEquals(ids + "crucifixo-ab12", p.treino("A")!!.exercises.map { it.id })
        assertEquals(P.treino("B"), p.treino("B"))
    }

    @Test fun removeAndMove() {
        val removed = ProgramRules.removeExercise(P, "A", "triceps-polia")
        assertEquals(ids - "triceps-polia", removed.treino("A")!!.exercises.map { it.id })

        val down = ProgramRules.moveExercise(P, "A", "supino-maquina", 1)
        assertEquals(listOf("puxada-frontal", "supino-maquina"), down.treino("A")!!.exercises.take(2).map { it.id })
        // Primeiro não sobe; último não desce.
        assertEquals(P, ProgramRules.moveExercise(P, "A", "supino-maquina", -1))
        assertEquals(P, ProgramRules.moveExercise(P, "A", "rosca-direta", 1))
    }

    @Test fun newIdIsSlugAndNeverTaken() {
        assertEquals("rosca-scott", ProgramRules.slug("Rosca Scott"))
        assertEquals("triceps-na-polia-corda", ProgramRules.slug("Tríceps na polia (corda)"))
        val id = ProgramRules.newExerciseId("Rosca Scott", emptySet(), Random(1))
        assertTrue(id.startsWith("rosca-scott-"))
        // Com a mesma semente o primeiro sorteio colide; tem que tentar de novo.
        assertNotEquals(id, ProgramRules.newExerciseId("Rosca Scott", setOf(id), Random(1)))
    }

    @Test fun everyStartingExerciseHasPhotosAndMuscles() {
        StaticProgram.treinos.flatMap { it.exercises }.forEach { e ->
            assertNotNull(e.id, ExerciseLibrary.get(e.libraryId))
            assertTrue(e.id, e.primary.isNotEmpty())
        }
    }
}
