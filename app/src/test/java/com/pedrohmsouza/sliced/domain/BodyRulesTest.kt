package com.pedrohmsouza.sliced.domain

import com.pedrohmsouza.sliced.SampleData
import com.pedrohmsouza.sliced.SampleData.ex
import com.pedrohmsouza.sliced.SampleData.session
import com.pedrohmsouza.sliced.SampleData.wr
import com.pedrohmsouza.sliced.model.Muscle
import com.pedrohmsouza.sliced.model.Program
import com.pedrohmsouza.sliced.model.StaticProgram
import com.pedrohmsouza.sliced.model.toProgram
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BodyRulesTest {
    private val P = StaticProgram
    private val monday = LocalDate.of(2026, 11, 9)

    @Test fun primaryCountsOneAndSecondaryHalfPerSet() {
        // Supino na máquina: peito (principal); ombros e tríceps (secundários).
        val s = listOf(session("A", monday, listOf(ex("supino-maquina", "Supino", wr(40.0, 12, 12, 12)))))
        val sets = BodyRules.muscleSets(s, P)
        assertEquals(3.0, sets[Muscle.CHEST]!!, 0.0)
        assertEquals(1.5, sets[Muscle.TRICEPS]!!, 0.0)
        assertEquals(null, sets[Muscle.QUADS])
    }

    @Test fun weekGoesFromMondayToSundayAndSkipsOtherWeeks() {
        // SampleData: hoje = segunda 09/11; os treinos são de 02/11 a 06/11 e antes.
        val thisWeek = BodyRules.week(SampleData.history, P, SampleData.today)
        assertEquals(SampleData.today, thisWeek.start)
        assertEquals(0, thisWeek.sessions)
        assertTrue(thisWeek.sets.isEmpty())

        val last = BodyRules.week(SampleData.history, P, SampleData.today, offset = -1)
        assertEquals(LocalDate.of(2026, 11, 2), last.start)
        assertEquals(LocalDate.of(2026, 11, 8), last.end)
        assertEquals(5, last.sessions)
        // Leg press (3 séries) + agachamento no Smith (3) + esteira (1) = 7 séries de quadríceps.
        assertEquals(7.0, last.sets[Muscle.QUADS]!!, 0.0)
    }

    @Test fun exerciseWithoutMusclesFallsBackToLibrary() {
        val p = ProgramRules.saveExercise(
            P.toProgram(), "A", P.exercise("supino-maquina")!!.copy(primary = emptyList(), secondary = emptyList()),
        )
        assertEquals(listOf(Muscle.CHEST), BodyRules.muscles("supino-maquina", p).first)
    }

    @Test fun exerciseRemovedFromProgramCountsNothing() {
        val p = Program(emptyList())
        val s = listOf(session("A", monday, listOf(ex("supino-maquina", "Supino", wr(40.0, 12)))))
        assertTrue(BodyRules.muscleSets(s, p).isEmpty())
    }

    @Test fun levelThresholds() {
        assertEquals(listOf(0, 1, 1, 2, 2, 3, 3, 4), listOf(0.0, 0.5, 2.5, 3.0, 5.5, 6.0, 9.5, 10.0).map(BodyRules::level))
    }
}
