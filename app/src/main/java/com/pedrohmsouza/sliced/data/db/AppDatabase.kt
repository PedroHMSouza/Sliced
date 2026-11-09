package com.pedrohmsouza.sliced.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * O banco SQLite do app. `version` precisa subir a cada mudança nas tabelas, junto com uma
 * migração (o esquema de cada versão fica salvo em app/schemas/ pelo exportSchema).
 */
@Database(
    entities = [SessionEntity::class, SessionExerciseEntity::class, SessionSetEntity::class, ActiveWorkoutEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun draftDao(): DraftDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "diario-treino.db").build()
    }
}
