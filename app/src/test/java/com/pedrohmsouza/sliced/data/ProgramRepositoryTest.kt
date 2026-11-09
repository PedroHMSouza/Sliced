package com.pedrohmsouza.sliced.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pedrohmsouza.sliced.data.db.AppDatabase
import com.pedrohmsouza.sliced.domain.ProgramRules
import com.pedrohmsouza.sliced.model.Muscle
import com.pedrohmsouza.sliced.model.RecordType
import com.pedrohmsouza.sliced.model.StaticProgram
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ProgramRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private lateinit var repo: RoomProgramRepository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = RoomProgramRepository(db.programDao(), now = { 1L })
    }

    @After fun tearDown() = db.close()

    @Test fun withoutEditsUsesStartingProgram() = runTest {
        assertSame(StaticProgram, repo.program.first())
    }

    @Test fun editsSurviveRoundTripWithEveryField() = runTest {
        val custom = StaticProgram.exercise("rosca-direta")!!.copy(
            id = "rosca-scott-ab12", name = "Rosca Scott", sets = 4, repRange = 6..8, restSec = 105,
            type = RecordType.WR, tip = "Cotovelo apoiado", target = "4 × 6–8", libraryId = "Preacher_Curl",
            media = "/data/x.gif", primary = listOf(Muscle.BICEPS), secondary = listOf(Muscle.FOREARMS),
        )
        repo.update { ProgramRules.saveExercise(it, "A", custom) }
        repo.update { ProgramRules.updateTreino(it, "A") { t -> t.copy(name = "Peito e costas", day = "Sábado") } }

        val read = repo.program.first()
        assertEquals("Peito e costas", read.treino("A")!!.name)
        assertEquals("Sábado", read.treino("A")!!.day)
        assertEquals(custom, read.exercise("rosca-scott-ab12"))
        // O resto da ficha continua igual à inicial.
        assertEquals(StaticProgram.treinos.drop(1), read.treinos.drop(1))
    }

    /**
     * Um celular com o app 1.0 tem o banco na versão 1, com histórico. Ao atualizar, o Room
     * aplica a AutoMigration 1 → 2; o histórico tem que continuar lá.
     */
    @Test fun migrationFrom1KeepsHistory() = runTest {
        val name = "migration-test.db"
        context.deleteDatabase(name)
        val schema = Json.parseToJsonElement(File("schemas/com.pedrohmsouza.sliced.data.db.AppDatabase/1.json").readText())
            .jsonObject["database"]!!.jsonObject
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name).apply { parentFile?.mkdirs() }, null).use { old ->
            schema["entities"]!!.jsonArray.forEach { e ->
                val table = e.jsonObject["tableName"]!!.jsonPrimitive.content
                old.execSQL(e.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                e.jsonObject["indices"]?.jsonArray?.forEach { i ->
                    old.execSQL(i.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                }
            }
            schema["setupQueries"]!!.jsonArray.forEach { old.execSQL(it.jsonPrimitive.content) }
            old.execSQL("INSERT INTO sessions VALUES ('s1', 'A', 'Superior 1', '2026-11-02', 1000, 2000, 52)")
            old.execSQL("INSERT INTO session_exercises VALUES (1, 's1', 'supino-maquina', 'Supino', 'wr', '', 0)")
            old.execSQL("INSERT INTO session_sets VALUES (1, 1, 0, 40.0, 12, NULL, NULL, NULL, NULL)")
            old.version = 1
        }

        val migrated = AppDatabase.create(context, name)
        try {
            val sessions = RoomSessionRepository(migrated.sessionDao()).sessions.first()
            assertEquals(1, sessions.size)
            assertEquals(40.0, sessions[0].exercises[0].sets[0].kg!!, 0.0)
            assertSame(StaticProgram, RoomProgramRepository(migrated.programDao(), { 1L }).program.first())
        } finally {
            migrated.close()
            context.deleteDatabase(name)
        }
    }
}
