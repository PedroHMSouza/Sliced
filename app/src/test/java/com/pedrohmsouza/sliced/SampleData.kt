package com.pedrohmsouza.sliced

import com.pedrohmsouza.sliced.model.LoggedExercise
import com.pedrohmsouza.sliced.model.LoggedSet
import com.pedrohmsouza.sliced.model.RecordType
import com.pedrohmsouza.sliced.model.Session
import java.time.LocalDate
import java.time.ZoneId

/** Dados de exemplo para testes e screenshots. */
object SampleData {
    val zone: ZoneId = ZoneId.of("America/Sao_Paulo")

    fun at(date: LocalDate, hour: Int = 7, minute: Int = 0): Long =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    fun wr(kg: Double, vararg reps: Int) = reps.map { LoggedSet(kg = kg, reps = it) }

    fun session(
        treino: String,
        date: LocalDate,
        exercises: List<LoggedExercise>,
        durationMin: Int = 52,
        title: String = mapOf("A" to "Superior 1", "B" to "Inferior 1", "C" to "Condicionamento e core", "D" to "Superior 2", "E" to "Inferior 2")[treino]!!,
    ): Session {
        val start = at(date)
        return Session(
            id = "${date}_${treino}_test01",
            treino = treino, title = title, date = date.toString(),
            startedAt = start, finishedAt = start + durationMin * 60_000L, durationMin = durationMin,
            exercises = exercises,
        )
    }

    fun ex(id: String, name: String, sets: List<LoggedSet>, note: String = "", type: RecordType = RecordType.WR) =
        LoggedExercise(id, name, type, note, sets)

    /** Duas semanas de treinos, do mais recente para o mais antigo. Hoje = segunda 09/11/2026. */
    val today: LocalDate = LocalDate.of(2026, 11, 9)

    val history: List<Session> = listOf(
        session("E", today.minusDays(3), listOf(
            ex("agachamento-smith", "Agachamento no Smith ou hack", wr(50.0, 12, 11, 10)),
            ex("stiff-halteres", "Stiff com halteres", wr(16.0, 12, 12, 12)),
        ), durationMin = 55),
        session("D", today.minusDays(4), listOf(
            ex("remada-maquina", "Remada na máquina", wr(45.0, 12, 12, 12)),
            ex("supino-inclinado-halteres", "Supino inclinado com halteres", wr(18.0, 10, 9, 8)),
        )),
        session("C", today.minusDays(5), listOf(
            ex("cardio-continuo", "Cardio contínuo", listOf(LoggedSet(minutes = 30, incline = 8.0)), type = RecordType.CARDIO),
            ex("prancha-frontal", "Prancha frontal", listOf(LoggedSet(seconds = 35), LoggedSet(seconds = 30)), type = RecordType.S),
        ), durationMin = 48),
        session("B", today.minusDays(6), listOf(
            ex("leg-press-45", "Leg press 45°", wr(120.0, 12, 12, 11)),
        )),
        session("A", today.minusDays(7), listOf(
            ex("supino-maquina", "Supino reto na máquina", wr(40.0, 12, 12, 12), note = "banco no furo 4"),
            ex("puxada-frontal", "Puxada frontal (pulley)", wr(45.0, 12, 11, 10)),
        ), durationMin = 58),
        session("A", today.minusDays(14), listOf(
            ex("supino-maquina", "Supino reto na máquina", wr(37.5, 12, 11, 10)),
        )),
        session("A", today.minusDays(21), listOf(
            ex("supino-maquina", "Supino reto na máquina", wr(35.0, 12, 12, 11)),
        )),
    )
}
