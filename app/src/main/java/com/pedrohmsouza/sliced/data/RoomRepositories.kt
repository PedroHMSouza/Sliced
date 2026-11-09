package com.pedrohmsouza.sliced.data

import com.pedrohmsouza.sliced.data.db.ActiveWorkoutEntity
import com.pedrohmsouza.sliced.data.db.DraftDao
import com.pedrohmsouza.sliced.data.db.SessionDao
import com.pedrohmsouza.sliced.data.db.SessionEntity
import com.pedrohmsouza.sliced.data.db.SessionExerciseEntity
import com.pedrohmsouza.sliced.data.db.SessionSetEntity
import com.pedrohmsouza.sliced.data.db.SessionWithExercises
import com.pedrohmsouza.sliced.model.Draft
import com.pedrohmsouza.sliced.model.LoggedExercise
import com.pedrohmsouza.sliced.model.LoggedSet
import com.pedrohmsouza.sliced.model.RecordType
import com.pedrohmsouza.sliced.model.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/** Histórico guardado no Room. Converte entidades ⇄ modelos de domínio. */
class RoomSessionRepository(private val dao: SessionDao) : SessionRepository {

    override val sessions: Flow<List<Session>> = dao.observeAll()
        .map { rows -> rows.map { it.toModel() } }
        .flowOn(Dispatchers.Default)

    override suspend fun save(session: Session) {
        val entity = SessionEntity(
            id = session.id, treino = session.treino, title = session.title, date = session.date,
            startedAt = session.startedAt, finishedAt = session.finishedAt, durationMin = session.durationMin,
        )
        val children = session.exercises.mapIndexed { pos, e ->
            SessionExerciseEntity(
                sessionId = session.id, exerciseId = e.exerciseId, name = e.name,
                type = e.type.code, note = e.note, position = pos,
            ) to e.sets.mapIndexed { i, s ->
                SessionSetEntity(
                    sessionExerciseId = 0, setIndex = i, kg = s.kg, reps = s.reps, seconds = s.seconds,
                    meters = s.meters, minutes = s.minutes, incline = s.incline,
                )
            }
        }
        dao.replace(entity, children)
    }

    override suspend fun delete(id: String) = dao.delete(id)
}

internal fun SessionWithExercises.toModel() = Session(
    id = session.id, treino = session.treino, title = session.title, date = session.date,
    startedAt = session.startedAt, finishedAt = session.finishedAt, durationMin = session.durationMin,
    // @Relation não garante ordem: ordenamos pela posição/índice gravados.
    exercises = exercises.sortedBy { it.exercise.position }.map { ex ->
        LoggedExercise(
            exerciseId = ex.exercise.exerciseId,
            name = ex.exercise.name,
            type = RecordType.fromCode(ex.exercise.type),
            note = ex.exercise.note,
            sets = ex.sets.sortedBy { it.setIndex }.map {
                LoggedSet(it.kg, it.reps, it.seconds, it.meters, it.minutes, it.incline)
            },
        )
    },
)

/** Rascunho guardado como JSON numa única linha. */
class RoomDraftRepository(private val dao: DraftDao, private val json: Json = DraftJson) : DraftRepository {

    override val draft: Flow<Draft?> = dao.observe()
        .map { row -> row?.let { runCatching { json.decodeFromString(Draft.serializer(), it.json) }.getOrNull() } }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    override suspend fun save(draft: Draft) =
        dao.upsert(ActiveWorkoutEntity(json = json.encodeToString(Draft.serializer(), draft), updatedAt = draft.updatedAt))

    override suspend fun clear() = dao.clear()
}

/** `ignoreUnknownKeys`: um rascunho salvo por uma versão mais nova do app não quebra a leitura. */
val DraftJson = Json { ignoreUnknownKeys = true }
