package com.pedrohmsouza.sliced.domain

import com.pedrohmsouza.sliced.SampleData
import com.pedrohmsouza.sliced.SampleData.ex
import com.pedrohmsouza.sliced.SampleData.session
import com.pedrohmsouza.sliced.SampleData.wr
import com.pedrohmsouza.sliced.domain.WorkoutRules.ToggleResult
import com.pedrohmsouza.sliced.domain.WorkoutRules.updateExercise
import com.pedrohmsouza.sliced.domain.WorkoutRules.updateSet
import com.pedrohmsouza.sliced.model.DraftSet
import com.pedrohmsouza.sliced.model.FieldKey
import com.pedrohmsouza.sliced.model.LoggedSet
import com.pedrohmsouza.sliced.model.StaticProgram
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

/** Uma regra por grupo de testes, na mesma numeração da especificação. */
class WorkoutRulesTest {
    private val P = StaticProgram
    private val monday = LocalDate.of(2026, 11, 9)
    private val supino = P.exercise("supino-maquina")!!

    // ---------- Regra 1: próximo treino ----------

    @Test fun next_followsSequenceAfterLastSession() {
        assertEquals("B", WorkoutRules.nextTreino(listOf(session("A", monday, emptyList())), monday, P))
        assertEquals("A", WorkoutRules.nextTreino(listOf(session("E", monday, emptyList())), monday, P))
    }

    @Test fun next_withoutHistoryUsesWeekday() {
        val expected = listOf("A", "B", "C", "D", "E", "A", "A") // seg..dom
        (0..6).forEach { i -> assertEquals(expected[i], WorkoutRules.nextTreino(emptyList(), monday.plusDays(i.toLong()), P)) }
    }

    // ---------- Regra 2: "Anterior" ----------

    @Test fun previous_isFromMostRecentSessionContainingTheExercise() {
        assertEquals(wr(40.0, 12, 12, 12), WorkoutRules.previousSets(SampleData.history, "supino-maquina"))
    }

    @Test fun previous_skipsSessionsWhereExerciseHadOnlyANote() {
        val onlyNote = session("A", monday, listOf(ex("supino-maquina", "Supino", emptyList(), note = "máquina quebrada")))
        assertEquals(wr(40.0, 12, 12, 12), WorkoutRules.previousSets(listOf(onlyNote) + SampleData.history, "supino-maquina"))
    }

    @Test fun previous_isNullWhenNeverDone() {
        assertNull(WorkoutRules.previousSets(SampleData.history, "face-pull"))
    }

    // ---------- Regra 3: marcar série ----------

    private fun draftA() = WorkoutRules.newDraft(P.treino("A")!!, SampleData.history, 0L)

    @Test fun toggle_fillsEmptyFieldsFromSameSetLastTime() {
        val d = draftA().updateSet(1, 1) { it.copy(values = mapOf(FieldKey.KG to "47,5")) }
        val r = WorkoutRules.toggleSet(d, 1, 1, SampleData.history, P) as ToggleResult.Updated
        val s = r.draft.exercises[1].sets[1]
        assertTrue(s.done)
        assertEquals("47,5", s.value(FieldKey.KG)) // o que foi digitado é mantido
        assertEquals("11", s.value(FieldKey.REPS)) // série 2 da última vez: 45×11
        assertEquals(90, r.restSec)
    }

    @Test fun toggle_newSetCopiesLastPreviousSet() {
        val d = draftA().updateExercise(1) { it.copy(sets = it.sets + DraftSet()) } // 4ª série nova
        val r = WorkoutRules.toggleSet(d, 1, 3, SampleData.history, P) as ToggleResult.Updated
        assertEquals("45", r.draft.exercises[1].sets[3].value(FieldKey.KG))
        assertEquals("10", r.draft.exercises[1].sets[3].value(FieldKey.REPS))
    }

    @Test fun toggle_decimalIsCopiedWithComma() {
        val hist = listOf(session("A", monday, listOf(ex("supino-maquina", "Supino", wr(42.5, 10)))))
        val r = WorkoutRules.toggleSet(draftA(), 0, 0, hist, P) as ToggleResult.Updated
        assertEquals("42,5", r.draft.exercises[0].sets[0].value(FieldKey.KG))
    }

    @Test fun toggle_withNothingToCopyAsksToFill() {
        val r = WorkoutRules.toggleSet(draftA(), 3, 0, emptyList(), P) // tríceps, sem histórico
        assertEquals(ToggleResult.Missing(listOf(FieldKey.KG, FieldKey.REPS)), r)
    }

    @Test fun toggle_partialInputIsEnough() {
        val d = draftA().updateSet(3, 0) { it.copy(values = mapOf(FieldKey.REPS to "12")) }
        val r = WorkoutRules.toggleSet(d, 3, 0, emptyList(), P)
        assertTrue(r is ToggleResult.Updated)
    }

    @Test fun toggle_againUnchecksWithoutRest() {
        val once = (WorkoutRules.toggleSet(draftA(), 0, 0, SampleData.history, P) as ToggleResult.Updated).draft
        val r = WorkoutRules.toggleSet(once, 0, 0, SampleData.history, P) as ToggleResult.Updated
        assertFalse(r.draft.exercises[0].sets[0].done)
        assertEquals(0, r.restSec)
    }

    @Test fun toggle_cardioHasNoRest() {
        val d = WorkoutRules.newDraft(P.treino("C")!!, emptyList(), 0L).updateSet(0, 0) { it.copy(values = mapOf(FieldKey.MINUTES to "30")) }
        val r = WorkoutRules.toggleSet(d, 0, 0, emptyList(), P) as ToggleResult.Updated
        assertEquals(0, r.restSec)
    }

    // ---------- Regra 4: "Subir carga" ----------

    @Test fun increase_whenAllSetsHitTopOfRange() {
        assertTrue(WorkoutRules.canIncrease(supino, SampleData.history)) // 40×12 ×3, faixa 8–12
    }

    @Test fun increase_notWhenOneSetMissedTop() {
        val h = listOf(session("A", monday, listOf(ex("supino-maquina", "Supino", wr(40.0, 12, 12, 11)))))
        assertFalse(WorkoutRules.canIncrease(supino, h))
    }

    @Test fun increase_notWhenFewerSetsThanPlanned() {
        val h = listOf(session("A", monday, listOf(ex("supino-maquina", "Supino", wr(40.0, 12, 12)))))
        assertFalse(WorkoutRules.canIncrease(supino, h))
    }

    @Test fun increase_extraSetsAllMustHitTop() {
        val h = listOf(session("A", monday, listOf(ex("supino-maquina", "Supino", wr(40.0, 12, 12, 12, 13)))))
        assertTrue(WorkoutRules.canIncrease(supino, h))
    }

    @Test fun increase_onlyForWeightAndReps() {
        assertFalse(WorkoutRules.canIncrease(P.exercise("dead-bug")!!, emptyList()))
        assertFalse(WorkoutRules.canIncrease(P.exercise("prancha-frontal")!!, emptyList()))
    }

    // ---------- Regra 5: anotações ----------

    @Test fun newDraft_bringsLastNoteAndPlannedSets() {
        val d = draftA()
        assertEquals("banco no furo 4", d.exercises[0].note)
        assertEquals(listOf(3, 3, 2, 2, 2), d.exercises.map { it.sets.size })
        assertTrue(d.id.matches(Regex("""\d{4}-\d{2}-\d{2}_A_[a-z0-9]{6}""")))
    }

    @Test fun newDraft_idIsRandomPerStart() {
        val a = WorkoutRules.newDraft(P.treino("A")!!, emptyList(), 0L, random = Random(1))
        val b = WorkoutRules.newDraft(P.treino("A")!!, emptyList(), 0L, random = Random(2))
        assertTrue(a.id != b.id)
    }

    // ---------- Regra 6: salvar ----------

    @Test fun build_keepsOnlyCheckedSetsAndDropsEmptyExercises() {
        var d = draftA()
        d = d.updateSet(0, 0) { DraftSet(mapOf(FieldKey.KG to "42,5", FieldKey.REPS to "12"), done = true) }
        d = d.updateSet(0, 1) { DraftSet(mapOf(FieldKey.KG to "42,5", FieldKey.REPS to "10"), done = false) }
        d = d.updateExercise(2) { it.copy(note = "  máquina 2  ") } // só nota, sem séries
        val s = WorkoutRules.buildSession(d, now = 52 * 60_000L, program = P, zone = SampleData.zone)
        assertEquals(listOf("supino-maquina", "desenvolvimento-maquina"), s.exercises.map { it.exerciseId })
        assertEquals(listOf(LoggedSet(kg = 42.5, reps = 12)), s.exercises[0].sets)
        assertEquals("máquina 2", s.exercises[1].note)
        assertEquals(52, s.durationMin)
        assertEquals("Superior 1", s.title)
    }

    @Test fun duration_isAtLeastOneMinute() {
        assertEquals(1, WorkoutRules.durationMin(0, 10_000))
        assertEquals(60, WorkoutRules.durationMin(0, 3_599_000))
    }

    @Test fun progress_countsVolumeOnlyForCheckedWeightSets() {
        var d = draftA()
        d = d.updateSet(0, 0) { DraftSet(mapOf(FieldKey.KG to "40", FieldKey.REPS to "10"), done = true) }
        d = d.updateSet(0, 1) { DraftSet(mapOf(FieldKey.KG to "40", FieldKey.REPS to "10"), done = false) }
        val p = WorkoutRules.progress(d)
        assertEquals(1, p.done)
        assertEquals(12, p.total)
        assertEquals(400.0, p.volumeKg, 0.0)
    }

    // ---------- Regra 9: semana começa na segunda ----------

    @Test fun week_startsOnMondayAndCountsSessions() {
        val sunday = LocalDate.of(2026, 11, 15)
        val h = listOf(
            session("B", LocalDate.of(2026, 11, 10), emptyList()),
            session("A", monday, emptyList()),
            session("E", LocalDate.of(2026, 11, 8), emptyList()), // domingo anterior: fora
        )
        val w = WorkoutRules.week(h, sunday)
        assertEquals(monday, w.days.first().date)
        assertEquals(listOf("A", "B", null, null, null, null, null), w.days.map { it.treino })
        assertEquals(2, w.count)
        assertTrue(w.days.last().isToday)
    }

    // ---------- Histórico e gráfico ----------

    @Test fun stats_monthTotalAndAverageOfLast10() {
        val s = WorkoutRules.stats(SampleData.history, SampleData.today)
        assertEquals(5, s.month) // 02/11 a 06/11
        assertEquals(7, s.total)
        assertEquals(53, s.avgMin)
    }

    @Test fun chart_pointsAreHeaviestSetPerSessionOldestFirst() {
        val pts = WorkoutRules.chartPoints(SampleData.history, "supino-maquina")
        assertEquals(listOf(35.0, 37.5, 40.0), pts.map { it.kg })
        assertEquals(12, WorkoutRules.bestPoint(pts)!!.reps)
    }

    @Test fun chart_limitsToLast12() {
        val many = (0 until 20).map { i ->
            session("A", monday.minusDays(i.toLong() * 7), listOf(ex("supino-maquina", "Supino", wr(60.0 - i, 10))))
                .copy(id = "s$i")
        }
        val pts = WorkoutRules.chartPoints(many, "supino-maquina")
        assertEquals(12, pts.size)
        assertEquals(60.0, pts.last().kg, 0.0)
    }

    @Test fun chartOptions_onlyWeightExercisesWithData_inProgramOrder() {
        val ids = WorkoutRules.chartOptions(SampleData.history, P).map { it.exercise.id }
        assertEquals(
            listOf("supino-maquina", "puxada-frontal", "leg-press-45", "remada-maquina", "supino-inclinado-halteres", "agachamento-smith", "stiff-halteres"),
            ids,
        )
    }

    @Test fun volume_sumsKgTimesRepsOfWeightExercises() {
        assertEquals(40.0 * 36 + 45 * 33, WorkoutRules.volume(SampleData.history[4]), 0.0)
    }
}
