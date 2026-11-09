package com.pedrohmsouza.sliced.domain

import com.pedrohmsouza.sliced.model.PlannedExercise
import com.pedrohmsouza.sliced.model.Program
import com.pedrohmsouza.sliced.model.Treino
import java.text.Normalizer
import kotlin.random.Random

/**
 * Edição da ficha: funções puras que recebem a ficha e devolvem a ficha alterada.
 *
 * O histórico liga cada série ao exercício pelo `id`, nunca pelo nome. Por isso renomear um
 * exercício mantém o "Anterior", o gráfico e o "Subir carga"; e um exercício novo ganha um
 * id novo, que nunca é reaproveitado.
 */
object ProgramRules {

    val WEEKDAYS = listOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo")

    fun updateTreino(p: Program, treinoId: String, f: (Treino) -> Treino): Program =
        p.copy(treinos = p.treinos.map { if (it.id == treinoId) f(it) else it })

    /** Substitui o exercício com o mesmo id; se ele ainda não está no treino, entra no fim. */
    fun saveExercise(p: Program, treinoId: String, e: PlannedExercise): Program = updateTreino(p, treinoId) { t ->
        if (t.exercises.any { it.id == e.id }) t.copy(exercises = t.exercises.map { if (it.id == e.id) e else it })
        else t.copy(exercises = t.exercises + e)
    }

    fun removeExercise(p: Program, treinoId: String, exerciseId: String): Program =
        updateTreino(p, treinoId) { t -> t.copy(exercises = t.exercises.filterNot { it.id == exerciseId }) }

    /** Move o exercício [delta] posições (−1 sobe, +1 desce). Fora dos limites, não faz nada. */
    fun moveExercise(p: Program, treinoId: String, exerciseId: String, delta: Int): Program = updateTreino(p, treinoId) { t ->
        val from = t.exercises.indexOfFirst { it.id == exerciseId }
        val to = from + delta
        if (from < 0 || to !in t.exercises.indices) return@updateTreino t
        val list = t.exercises.toMutableList()
        list.add(to, list.removeAt(from))
        t.copy(exercises = list)
    }

    /** "Rosca Scott" → "rosca-scott-k3x9": legível e sem colisão com ids já usados. */
    fun newExerciseId(name: String, taken: Set<String>, random: Random = Random.Default): String {
        val base = slug(name).ifEmpty { "exercicio" }.take(40)
        while (true) {
            val suffix = (1..4).map { "abcdefghijklmnopqrstuvwxyz0123456789"[random.nextInt(36)] }.joinToString("")
            val id = "$base-$suffix"
            if (id !in taken) return id
        }
    }

    fun slug(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
}
