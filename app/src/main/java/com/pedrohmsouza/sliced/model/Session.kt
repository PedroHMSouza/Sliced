package com.pedrohmsouza.sliced.model

/**
 * Modelos de domínio do histórico: o que de fato foi feito. São classes Kotlin puras,
 * sem anotações do Room. A tradução de/para as tabelas fica na camada de dados; assim
 * as telas e as regras de negócio não dependem de como o banco está organizado.
 */
data class Session(
    val id: String,
    val treino: String,
    val title: String,
    /** Data local no formato YYYY-MM-DD (o dia em que o treino começou). */
    val date: String,
    val startedAt: Long,
    val finishedAt: Long,
    val durationMin: Int,
    val exercises: List<LoggedExercise>,
)

data class LoggedExercise(
    val exerciseId: String,
    val name: String,
    val type: RecordType,
    val note: String,
    val sets: List<LoggedSet>,
)

/** Uma série registrada. Só os campos do tipo do exercício são preenchidos. */
data class LoggedSet(
    val kg: Double? = null,
    val reps: Int? = null,
    val seconds: Int? = null,
    val meters: Int? = null,
    val minutes: Int? = null,
    val incline: Double? = null,
) {
    fun get(key: FieldKey): Number? = when (key) {
        FieldKey.KG -> kg
        FieldKey.REPS -> reps
        FieldKey.SECONDS -> seconds
        FieldKey.METERS -> meters
        FieldKey.MINUTES -> minutes
        FieldKey.INCLINE -> incline
    }
}
