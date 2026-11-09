package com.pedrohmsouza.sliced.model

import kotlinx.serialization.Serializable

/**
 * Treino em andamento ("rascunho"). Fica salvo como JSON numa linha do Room: é um
 * objeto que muda a cada toque e só vira histórico de verdade ao finalizar. Guardar
 * como JSON evita várias tabelas para algo temporário e deixa o salvamento atômico.
 *
 * Os valores dos campos são guardados como texto exatamente como foram digitados
 * ("22,5", "" etc.) para a tela não "corrigir" o que a pessoa está digitando.
 */
@Serializable
data class Draft(
    val id: String,
    val treino: String,
    val startedAt: Long,
    val updatedAt: Long,
    val exercises: List<DraftExercise>,
)

@Serializable
data class DraftExercise(
    val exerciseId: String,
    val name: String,
    val type: RecordType,
    val note: String = "",
    val sets: List<DraftSet>,
)

@Serializable
data class DraftSet(
    val values: Map<FieldKey, String> = emptyMap(),
    val done: Boolean = false,
) {
    fun value(key: FieldKey): String = values[key].orEmpty()
}
