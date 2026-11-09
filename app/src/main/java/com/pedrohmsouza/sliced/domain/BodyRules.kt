package com.pedrohmsouza.sliced.domain

import com.pedrohmsouza.sliced.model.ExerciseLibrary
import com.pedrohmsouza.sliced.model.Muscle
import com.pedrohmsouza.sliced.model.ProgramSource
import com.pedrohmsouza.sliced.model.Session
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Mapa do corpo: quanto cada músculo foi trabalhado numa semana. Funções puras, como em
 * [WorkoutRules].
 *
 * A conta é em séries: cada série registrada vale 1 para os músculos principais do
 * exercício e ½ para os secundários. É a medida usual de volume semanal (algo entre 10 e
 * 20 séries por músculo é o alvo comum para hipertrofia).
 */
object BodyRules {

    data class Week(
        val start: LocalDate,
        val end: LocalDate,
        val sets: Map<Muscle, Double>,
        val sessions: Int,
    )

    /** Semana de segunda a domingo [offset] semanas antes (negativo) ou depois da semana de [today]. */
    fun week(sessions: List<Session>, program: ProgramSource, today: LocalDate, offset: Int = 0): Week {
        val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(offset.toLong())
        val end = start.plusDays(6)
        val inWeek = sessions.filter { LocalDate.parse(it.date) in start..end }
        return Week(start, end, muscleSets(inWeek, program), inWeek.size)
    }

    fun muscleSets(sessions: List<Session>, program: ProgramSource): Map<Muscle, Double> {
        val total = mutableMapOf<Muscle, Double>()
        sessions.forEach { s ->
            s.exercises.forEach { e ->
                val n = e.sets.size
                if (n == 0) return@forEach
                val (primary, secondary) = muscles(e.exerciseId, program)
                primary.forEach { total[it] = (total[it] ?: 0.0) + n }
                secondary.forEach { total[it] = (total[it] ?: 0.0) + n * 0.5 }
            }
        }
        return total
    }

    /** Músculos do exercício na ficha; se ele não tiver nenhum marcado, os do catálogo. */
    fun muscles(exerciseId: String, program: ProgramSource): Pair<List<Muscle>, List<Muscle>> {
        val p = program.exercise(exerciseId) ?: return emptyList<Muscle>() to emptyList()
        if (p.primary.isNotEmpty() || p.secondary.isNotEmpty()) return p.primary to (p.secondary - p.primary.toSet())
        val lib = ExerciseLibrary.get(p.libraryId) ?: return emptyList<Muscle>() to emptyList()
        return lib.primary to lib.secondary
    }

    /** Intensidade da cor no boneco: 0 = não treinado, 1 = 1–2 séries, 2 = 3–5, 3 = 6–9, 4 = 10+. */
    fun level(sets: Double): Int = when {
        sets <= 0.0 -> 0
        sets < 3 -> 1
        sets < 6 -> 2
        sets < 10 -> 3
        else -> 4
    }
}
