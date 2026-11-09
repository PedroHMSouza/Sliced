package com.pedrohmsouza.sliced.domain

import com.pedrohmsouza.sliced.model.Draft
import com.pedrohmsouza.sliced.model.DraftExercise
import com.pedrohmsouza.sliced.model.DraftSet
import com.pedrohmsouza.sliced.model.FieldKey
import com.pedrohmsouza.sliced.model.LoggedExercise
import com.pedrohmsouza.sliced.model.LoggedSet
import com.pedrohmsouza.sliced.model.PlannedExercise
import com.pedrohmsouza.sliced.model.ProgramSource
import com.pedrohmsouza.sliced.model.RecordType
import com.pedrohmsouza.sliced.model.Session
import com.pedrohmsouza.sliced.model.Treino
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Regras de negócio do diário, todas como funções puras: recebem dados e devolvem
 * dados, sem banco, sem Android e sem relógio implícito (a data/hora entra por parâmetro).
 * Isso permite testar cada regra com JUnit simples (ver WorkoutRulesTest).
 *
 * Convenção: `sessions` sempre vem ordenada do treino mais recente para o mais antigo.
 */
object WorkoutRules {

    // ---------- Próximo treino (regra 1) ----------

    /** O treino seguinte ao último registrado (A→B→C→D→E→A); sem registros, o marcado para o dia da semana. */
    fun nextTreino(sessions: List<Session>, today: LocalDate, program: ProgramSource): String {
        val order = program.order
        val last = sessions.firstOrNull()
        if (last != null) {
            val i = order.indexOf(last.treino)
            return order[(i + 1).mod(order.size)]
        }
        val weekday = ProgramRules.WEEKDAYS[today.dayOfWeek.value - 1]
        return program.treinos.firstOrNull { it.day == weekday }?.id ?: order.first()
    }

    fun lastOf(sessions: List<Session>, treinoId: String): Session? = sessions.firstOrNull { it.treino == treinoId }

    // ---------- "Anterior" (regra 2) ----------

    /** Séries da sessão mais recente que tem esse exercício com pelo menos uma série. */
    fun previousSets(sessions: List<Session>, exerciseId: String): List<LoggedSet>? {
        for (s in sessions) {
            val e = s.exercises.firstOrNull { it.exerciseId == exerciseId }
            if (e != null && e.sets.isNotEmpty()) return e.sets
        }
        return null
    }

    // ---------- "Subir carga" (regra 4: dupla progressão) ----------

    fun canIncrease(planned: PlannedExercise, sessions: List<Session>): Boolean {
        if (planned.type != RecordType.WR) return false
        val range = planned.repRange ?: return false
        val prev = previousSets(sessions, planned.id) ?: return false
        if (prev.size < planned.sets) return false
        return prev.all { (it.reps ?: 0) >= range.last }
    }

    // ---------- Começar treino (regra 5) ----------

    /** Última anotação registrada de cada exercício. */
    fun lastNotes(sessions: List<Session>): Map<String, String> {
        val notes = mutableMapOf<String, String>()
        sessions.forEach { s -> s.exercises.forEach { e -> if (e.note.isNotBlank() && e.exerciseId !in notes) notes[e.exerciseId] = e.note } }
        return notes
    }

    fun newDraft(
        treino: Treino,
        sessions: List<Session>,
        now: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        random: Random = Random.Default,
    ): Draft {
        val notes = lastNotes(sessions)
        val suffix = (1..6).map { "abcdefghijklmnopqrstuvwxyz0123456789"[random.nextInt(36)] }.joinToString("")
        return Draft(
            id = "${localDate(now, zone)}_${treino.id}_$suffix",
            treino = treino.id,
            startedAt = now,
            updatedAt = now,
            exercises = treino.exercises.map { e ->
                DraftExercise(
                    exerciseId = e.id,
                    name = e.name,
                    type = e.type,
                    note = notes[e.id].orEmpty(),
                    sets = List(e.sets) { DraftSet() },
                )
            },
        )
    }

    // ---------- Marcar série (regra 3) ----------

    sealed interface ToggleResult {
        /** [restSec] > 0 quando o timer de descanso deve começar. */
        data class Updated(val draft: Draft, val restSec: Int, val exerciseName: String) : ToggleResult

        /** Nenhum campo preenchido e nada para copiar da última vez. */
        data class Missing(val fields: List<FieldKey>) : ToggleResult
    }

    fun toggleSet(
        draft: Draft,
        exerciseIndex: Int,
        setIndex: Int,
        sessions: List<Session>,
        program: ProgramSource,
    ): ToggleResult {
        val ex = draft.exercises[exerciseIndex]
        val set = ex.sets[setIndex]
        if (set.done) {
            return ToggleResult.Updated(draft.updateSet(exerciseIndex, setIndex) { it.copy(done = false) }, 0, ex.name)
        }
        val prev = previousSets(sessions, ex.exerciseId).orEmpty()
        // A série N copia a série N da última vez; se ela não existia, copia a última série.
        val source = prev.getOrNull(setIndex) ?: prev.lastOrNull()
        val fields = ex.type.fields
        val filled = fields.associateWith { key ->
            set.value(key).ifEmpty { source?.get(key)?.let(Fmt::plain).orEmpty() }
        }
        if (fields.all { filled[it].isNullOrEmpty() }) return ToggleResult.Missing(fields)
        val rest = program.exercise(ex.exerciseId)?.restSec ?: 0
        val updated = draft.updateSet(exerciseIndex, setIndex) { DraftSet(values = filled, done = true) }
        return ToggleResult.Updated(updated, rest, ex.name)
    }

    fun Draft.updateSet(exerciseIndex: Int, setIndex: Int, f: (DraftSet) -> DraftSet): Draft =
        updateExercise(exerciseIndex) { e -> e.copy(sets = e.sets.mapIndexed { i, s -> if (i == setIndex) f(s) else s }) }

    fun Draft.updateExercise(exerciseIndex: Int, f: (DraftExercise) -> DraftExercise): Draft =
        copy(exercises = exercises.mapIndexed { i, e -> if (i == exerciseIndex) f(e) else e })

    data class Progress(val done: Int, val total: Int, val volumeKg: Double)

    /** Séries feitas / total e volume (kg × reps das séries `wr` marcadas). */
    fun progress(draft: Draft): Progress {
        var done = 0; var total = 0; var vol = 0.0
        draft.exercises.forEach { e ->
            e.sets.forEach { s ->
                total++
                if (s.done) {
                    done++
                    if (e.type == RecordType.WR) vol += (Fmt.parse(s.value(FieldKey.KG)) ?: 0.0) * (Fmt.parse(s.value(FieldKey.REPS)) ?: 0.0)
                }
            }
        }
        return Progress(done, total, vol)
    }

    // ---------- Salvar (regra 6) ----------

    fun durationMin(startedAt: Long, now: Long): Int = max(1, ((now - startedAt) / 60_000.0).roundToInt())

    /** Converte o rascunho em sessão: só séries marcadas; exercícios vazios e sem anotação saem. */
    fun buildSession(draft: Draft, now: Long, program: ProgramSource, zone: ZoneId = ZoneId.systemDefault()): Session {
        val exercises = draft.exercises.map { e ->
            LoggedExercise(
                exerciseId = e.exerciseId,
                name = e.name,
                type = e.type,
                note = e.note.trim(),
                sets = e.sets.filter { it.done }.map { it.toLogged() },
            )
        }.filter { it.sets.isNotEmpty() || it.note.isNotEmpty() }
        return Session(
            id = draft.id,
            treino = draft.treino,
            title = program.treino(draft.treino)?.name.orEmpty(),
            date = localDate(draft.startedAt, zone).toString(),
            startedAt = draft.startedAt,
            finishedAt = now,
            durationMin = durationMin(draft.startedAt, now),
            exercises = exercises,
        )
    }

    private fun DraftSet.toLogged(): LoggedSet {
        fun d(k: FieldKey) = Fmt.parse(value(k))
        fun i(k: FieldKey) = Fmt.parse(value(k))?.roundToInt()
        return LoggedSet(
            kg = d(FieldKey.KG), reps = i(FieldKey.REPS), seconds = i(FieldKey.SECONDS),
            meters = i(FieldKey.METERS), minutes = i(FieldKey.MINUTES), incline = d(FieldKey.INCLINE),
        )
    }

    // ---------- Semana (regra 9) ----------

    data class WeekDay(val date: LocalDate, val label: String, val treino: String?, val isToday: Boolean)
    data class Week(val days: List<WeekDay>, val count: Int)

    /** Semana de segunda a domingo que contém [today], com o treino (mais recente) de cada dia. */
    fun week(sessions: List<Session>, today: LocalDate): Week {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val dates = (0..6).map { monday.plusDays(it.toLong()) }
        val keys = dates.map { it.toString() }.toSet()
        val byDay = sessions.groupBy { it.date }
        val days = dates.mapIndexed { i, d ->
            WeekDay(d, Fmt.WEEK_LETTERS[i], byDay[d.toString()]?.firstOrNull()?.treino, d == today)
        }
        return Week(days, sessions.count { it.date in keys })
    }

    // ---------- Histórico e gráfico ----------

    fun volume(session: Session): Double = session.exercises.filter { it.type == RecordType.WR }
        .sumOf { e -> e.sets.sumOf { (it.kg ?: 0.0) * (it.reps ?: 0) } }

    /** [month]: dias diferentes com treino no mês de hoje (dois treinos no mesmo dia contam 1). */
    data class Stats(val month: Int, val total: Int, val avgMin: Int?)

    fun stats(sessions: List<Session>, today: LocalDate): Stats {
        val ym = today.toString().take(7)
        val recent = sessions.take(10).filter { it.durationMin > 0 }
        val avg = if (recent.isEmpty()) null else (recent.sumOf { it.durationMin } / recent.size.toDouble()).roundToInt()
        return Stats(sessions.filter { it.date.startsWith(ym) }.map { it.date }.distinct().size, sessions.size, avg)
    }

    // ---------- Calendário do mês ----------

    /** Um dia do calendário com os treinos feitos nele (do mais antigo ao mais novo). */
    data class MonthDay(val date: LocalDate, val treinos: List<String>, val isToday: Boolean)

    /** Datas em que um treino foi feito no mês (para "em que dia da semana fiz cada treino"). */
    data class TreinoDates(val treino: String, val dates: List<LocalDate>)

    data class Month(
        val month: YearMonth,
        /** Semanas de segunda a domingo; null = dia de fora do mês (preenche a grade). */
        val cells: List<MonthDay?>,
        val trainedDays: Int,
        val sessions: Int,
        val byTreino: List<TreinoDates>,
    )

    fun month(sessions: List<Session>, month: YearMonth, today: LocalDate, program: ProgramSource): Month {
        val inMonth = sessions.filter { it.date.startsWith(month.toString()) }.asReversed()
        val byDay = inMonth.groupBy { it.date }
        val first = month.atDay(1)
        val lead = first.dayOfWeek.value - 1 // segunda = 0
        val days = (1..month.lengthOfMonth()).map { d ->
            val date = month.atDay(d)
            MonthDay(date, byDay[date.toString()].orEmpty().map { it.treino }, date == today)
        }
        val cells = List<MonthDay?>(lead) { null } + days
        val padded = cells + List((7 - cells.size % 7) % 7) { null }
        val order = program.order
        val byTreino = inMonth.groupBy { it.treino }
            .map { (t, list) -> TreinoDates(t, list.map { LocalDate.parse(it.date) }) }
            .sortedBy { order.indexOf(it.treino).let { i -> if (i < 0) Int.MAX_VALUE else i } }
        return Month(month, padded, byDay.size, inMonth.size, byTreino)
    }

    data class ChartOption(val exercise: PlannedExercise, val treinoId: String)
    data class ChartPoint(val startedAt: Long, val kg: Double, val reps: Int)

    /** Exercícios `wr` que já têm alguma série registrada, na ordem da ficha. */
    fun chartOptions(sessions: List<Session>, program: ProgramSource): List<ChartOption> {
        val logged = sessions.flatMap { s -> s.exercises.filter { it.sets.isNotEmpty() }.map { it.exerciseId } }.toSet()
        return program.treinos.flatMap { t ->
            t.exercises.filter { it.type == RecordType.WR && it.id in logged }.map { ChartOption(it, t.id) }
        }
    }

    /** Maior carga de cada treino (empate: mais reps), do mais antigo ao mais novo, últimos [limit]. */
    fun chartPoints(sessions: List<Session>, exerciseId: String, limit: Int = 12): List<ChartPoint> =
        sessions.asReversed().mapNotNull { s ->
            val e = s.exercises.firstOrNull { it.exerciseId == exerciseId } ?: return@mapNotNull null
            e.sets.filter { it.kg != null }
                .maxWithOrNull(compareBy<LoggedSet> { it.kg }.thenBy { it.reps ?: 0 })
                ?.let { ChartPoint(s.startedAt, it.kg!!, it.reps ?: 0) }
        }.takeLast(limit)

    fun bestPoint(points: List<ChartPoint>): ChartPoint? =
        points.maxWithOrNull(compareBy<ChartPoint> { it.kg }.thenBy { it.reps })

    fun localDate(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
}
