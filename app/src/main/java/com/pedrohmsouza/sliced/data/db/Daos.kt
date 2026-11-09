package com.pedrohmsouza.sliced.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * DAOs: interfaces em que cada função é uma consulta SQL. O Room gera a implementação
 * em tempo de compilação (via KSP) e confere o SQL contra as tabelas — um erro de digitação
 * numa coluna quebra o build, não o app.
 */
@Dao
interface SessionDao {
    /** Retorna um Flow: emite de novo sempre que alguma das tabelas envolvidas mudar. */
    @Transaction
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<SessionWithExercises>>

    @Insert
    suspend fun insertSession(session: SessionEntity)

    @Insert
    suspend fun insertExercise(exercise: SessionExerciseEntity): Long

    @Insert
    suspend fun insertSets(sets: List<SessionSetEntity>)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: String)

    /**
     * Grava a sessão inteira de forma atômica: ou entra tudo, ou nada (se o app morrer no
     * meio, não fica sessão pela metade). Se já existir uma sessão com o mesmo id, ela é
     * substituída — os filhos saem junto pelo ON DELETE CASCADE.
     */
    @Transaction
    suspend fun replace(session: SessionEntity, exercises: List<Pair<SessionExerciseEntity, List<SessionSetEntity>>>) {
        delete(session.id)
        insertSession(session)
        exercises.forEach { (exercise, sets) ->
            val exerciseId = insertExercise(exercise)
            insertSets(sets.map { it.copy(sessionExerciseId = exerciseId) })
        }
    }
}

@Dao
interface DraftDao {
    @Query("SELECT * FROM active_workout WHERE id = 0")
    fun observe(): Flow<ActiveWorkoutEntity?>

    @Upsert
    suspend fun upsert(entity: ActiveWorkoutEntity)

    @Query("DELETE FROM active_workout")
    suspend fun clear()
}

@Dao
interface ProgramDao {
    @Query("SELECT * FROM program WHERE id = 0")
    fun observe(): Flow<ProgramEntity?>

    @Upsert
    suspend fun upsert(entity: ProgramEntity)
}
