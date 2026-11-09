package com.pedrohmsouza.sliced.data

import com.pedrohmsouza.sliced.data.db.ProgramDao
import com.pedrohmsouza.sliced.data.db.ProgramEntity
import com.pedrohmsouza.sliced.model.Muscle
import com.pedrohmsouza.sliced.model.PlannedExercise
import com.pedrohmsouza.sliced.model.Program
import com.pedrohmsouza.sliced.model.ProgramSource
import com.pedrohmsouza.sliced.model.RecordType
import com.pedrohmsouza.sliced.model.StaticProgram
import com.pedrohmsouza.sliced.model.Treino
import com.pedrohmsouza.sliced.model.toProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Ficha guardada como JSON numa única linha. Sem linha (ou JSON ilegível), vale a ficha inicial. */
class RoomProgramRepository(
    private val dao: ProgramDao,
    private val now: () -> Long,
    private val json: Json = DraftJson,
) : ProgramRepository {

    private val mutex = Mutex()

    override val program: Flow<ProgramSource> = dao.observe()
        .map { row -> row?.let { decode(it.json) } ?: StaticProgram }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    override suspend fun update(transform: (Program) -> Program) = mutex.withLock {
        val next = transform(program.first().toProgram())
        dao.upsert(ProgramEntity(json = json.encodeToString(StoredProgram.serializer(), next.toStored()), updatedAt = now()))
    }

    private fun decode(text: String): Program? =
        runCatching { json.decodeFromString(StoredProgram.serializer(), text).toModel() }.getOrNull()
}

/*
 * Formato do JSON. Fica separado do modelo (PlannedExercise tem IntRange e enums que não
 * queremos amarrar ao formato salvo): assim o modelo pode mudar sem quebrar fichas antigas.
 */

@Serializable
private data class StoredProgram(val treinos: List<StoredTreino>)

@Serializable
private data class StoredTreino(val id: String, val name: String, val day: String, val exercises: List<StoredExercise>)

@Serializable
private data class StoredExercise(
    val id: String,
    val name: String,
    val sets: Int,
    val repMin: Int? = null,
    val repMax: Int? = null,
    val restSec: Int,
    val type: String,
    val tip: String = "",
    val target: String? = null,
    val libraryId: String? = null,
    val media: String? = null,
    val primary: List<String> = emptyList(),
    val secondary: List<String> = emptyList(),
)

private fun Program.toStored() = StoredProgram(
    treinos.map { t ->
        StoredTreino(
            t.id, t.name, t.day,
            t.exercises.map { e ->
                StoredExercise(
                    id = e.id, name = e.name, sets = e.sets, repMin = e.repRange?.first, repMax = e.repRange?.last,
                    restSec = e.restSec, type = e.type.code, tip = e.tip, target = e.target, libraryId = e.libraryId,
                    media = e.media, primary = e.primary.map { it.name }, secondary = e.secondary.map { it.name },
                )
            },
        )
    },
)

private fun StoredProgram.toModel() = Program(
    treinos.map { t ->
        Treino(
            t.id, t.name, t.day,
            t.exercises.map { e ->
                PlannedExercise(
                    id = e.id, name = e.name, sets = e.sets,
                    repRange = if (e.repMin != null && e.repMax != null) e.repMin..e.repMax else null,
                    restSec = e.restSec, type = RecordType.fromCode(e.type), tip = e.tip, target = e.target,
                    libraryId = e.libraryId, media = e.media,
                    primary = e.primary.toMuscles(), secondary = e.secondary.toMuscles(),
                )
            },
        )
    },
)

/** Nomes desconhecidos (de uma versão mais nova do app) são ignorados. */
private fun List<String>.toMuscles() = mapNotNull { n -> Muscle.entries.firstOrNull { it.name == n } }
