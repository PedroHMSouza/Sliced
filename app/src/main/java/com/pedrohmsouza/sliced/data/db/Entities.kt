package com.pedrohmsouza.sliced.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/*
 * Tabelas do Room.
 *
 * O histórico é normalizado em três tabelas (sessão → exercícios → séries) em vez de um
 * JSON por sessão. Assim dá para fazer consultas por exercício no futuro (ex.: "todas as
 * séries de supino") direto em SQL, e apagar uma sessão apaga tudo em cascata.
 *
 * As séries têm uma coluna por campo possível (kg, reps, segundos...). Fica mais claro do
 * que um par "chave/valor" e o SQLite guarda colunas vazias (NULL) praticamente de graça.
 */

@Entity(tableName = "sessions", indices = [Index("startedAt"), Index("date")])
data class SessionEntity(
    @PrimaryKey val id: String,
    val treino: String,
    val title: String,
    val date: String,
    val startedAt: Long,
    val finishedAt: Long,
    val durationMin: Int,
)

@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class SessionExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val exerciseId: String,
    val name: String,
    /** Código do RecordType: wr | r | s | wm | cardio. */
    val type: String,
    val note: String,
    val position: Int,
)

@Entity(
    tableName = "session_sets",
    foreignKeys = [
        ForeignKey(
            entity = SessionExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionExerciseId")],
)
data class SessionSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionExerciseId: Long,
    val setIndex: Int,
    val kg: Double? = null,
    val reps: Int? = null,
    val seconds: Int? = null,
    val meters: Int? = null,
    val minutes: Int? = null,
    val incline: Double? = null,
)

/** Rascunho do treino em andamento: uma linha só (id = 0) com o estado em JSON. */
@Entity(tableName = "active_workout")
data class ActiveWorkoutEntity(
    @PrimaryKey val id: Int = 0,
    val json: String,
    val updatedAt: Long,
)

/**
 * Ficha de treino editada no app: uma linha só (id = 0) com a ficha em JSON, como o
 * rascunho. A ficha é pequena, muda pouco e é sempre lida inteira; tabelas próprias não
 * trariam nada. Sem linha = ficha inicial ([com.pedrohmsouza.sliced.model.StaticProgram]).
 */
@Entity(tableName = "program")
data class ProgramEntity(
    @PrimaryKey val id: Int = 0,
    val json: String,
    val updatedAt: Long,
)

/* Classes de leitura: o Room monta a árvore sessão → exercícios → séries com @Relation. */

data class ExerciseWithSets(
    @Embedded val exercise: SessionExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionExerciseId")
    val sets: List<SessionSetEntity>,
)

data class SessionWithExercises(
    @Embedded val session: SessionEntity,
    @Relation(entity = SessionExerciseEntity::class, parentColumn = "id", entityColumn = "sessionId")
    val exercises: List<ExerciseWithSets>,
)
