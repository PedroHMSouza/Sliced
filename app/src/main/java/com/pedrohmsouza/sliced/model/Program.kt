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
    /** Exercício do [ExerciseLibrary] de onde vêm as fotos do movimento. */
    val libraryId: String? = null,
    /** Foto ou GIF escolhido no celular (arquivo copiado para o app). Tem prioridade sobre o catálogo. */
    val media: String? = null,
    /** Músculos trabalhados: cada série conta 1 para os principais e ½ para os secundários. */
    val primary: List<Muscle> = emptyList(),
    val secondary: List<Muscle> = emptyList(),
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
 * Fonte da ficha de treino. A ficha inicial é [StaticProgram]; depois de editada no app,
 * ela vem do Room como um [Program]. As telas e as regras só conhecem esta interface.
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

/** Ficha como dado (a editada pelo app). */
data class Program(override val treinos: List<Treino>) : ProgramSource

fun ProgramSource.toProgram(): Program = this as? Program ?: Program(treinos)
