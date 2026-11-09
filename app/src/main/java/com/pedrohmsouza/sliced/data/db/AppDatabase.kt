package com.pedrohmsouza.sliced.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * O banco SQLite do app. `version` precisa subir a cada mudança nas tabelas, junto com uma
 * migração (o esquema de cada versão fica salvo em app/schemas/ pelo exportSchema).
 *
 * Versão 2: tabela `program` (ficha editável). Só acrescenta uma tabela, então o Room gera
 * a migração sozinho comparando os esquemas 1 e 2 (AutoMigration); o histórico fica intacto.
 */
@Database(
    entities = [
        SessionEntity::class, SessionExerciseEntity::class, SessionSetEntity::class,
        ActiveWorkoutEntity::class, ProgramEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun draftDao(): DraftDao
    abstract fun programDao(): ProgramDao

    companion object {
        fun create(context: Context, name: String = "diario-treino.db"): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, name).build()
    }
}
