package com.pedrohmsouza.sliced.model

/**
 * Campo de uma série. Cada [RecordType] usa um subconjunto destes campos.
 * [decimal] diz se o campo aceita vírgula (kg, inclinação) ou só inteiros (reps, segundos...).
 */
enum class FieldKey(val label: String, val decimal: Boolean) {
    KG("kg", true),
    REPS("reps", false),
    SECONDS("seg", false),
    METERS("metros", false),
    MINUTES("min", false),
    INCLINE("incl. %", true),
}

/** Tipo de registro: define quais campos cada série tem (igual à coluna "Tipo" da ficha). */
enum class RecordType(val code: String, val fields: List<FieldKey>) {
    WR("wr", listOf(FieldKey.KG, FieldKey.REPS)),
    R("r", listOf(FieldKey.REPS)),
    S("s", listOf(FieldKey.SECONDS)),
    WM("wm", listOf(FieldKey.KG, FieldKey.METERS)),
    CARDIO("cardio", listOf(FieldKey.MINUTES, FieldKey.INCLINE));

    companion object {
        fun fromCode(code: String): RecordType = entries.firstOrNull { it.code == code } ?: WR
    }
}

/** Exercício planejado na ficha (não é o que foi feito; isso é [LoggedExercise]). */
data class PlannedExercise(
    val id: String,
    val name: String,
    val sets: Int,
    /** Faixa de repetições para a dupla progressão; null quando o alvo não é em reps. */
    val repRange: IntRange?,
    val restSec: Int,
    val type: RecordType,
    val tip: String,
    /** Texto livre do alvo quando "séries × faixa" não descreve bem (ex.: "30 min"). */
    val target: String? = null,
) {
    /** Ex.: "3 × 8–12", "2 × 10", ou o [target] explícito. */
    val targetText: String
        get() = target ?: repRange?.let { r ->
            val reps = if (r.first == r.last) "${r.first}" else "${r.first}–${r.last}"
            "$sets × $reps"
        } ?: if (sets > 1) "$sets séries" else "$sets série"
}

/** Um dos treinos da divisão (A–E). */
data class Treino(
    val id: String,
    val name: String,
    val day: String,
    val exercises: List<PlannedExercise>,
)

/**
 * Fonte da ficha de treino. No MVP é estática ([StaticProgram]), mas o resto do app só
 * conhece esta interface: quando a ficha puder ser editada pelo app, basta criar uma
 * implementação que lê do Room, sem mexer nas telas nem nas regras.
 */
interface ProgramSource {
    val treinos: List<Treino>

    /** Ordem da sequência A → B → C → D → E → A. */
    val order: List<String> get() = treinos.map { it.id }

    fun treino(id: String): Treino? = treinos.firstOrNull { it.id == id }

    fun exercise(id: String): PlannedExercise? =
        treinos.asSequence().flatMap { it.exercises }.firstOrNull { it.id == id }

    fun treinoOfExercise(id: String): Treino? = treinos.firstOrNull { t -> t.exercises.any { it.id == id } }
}
