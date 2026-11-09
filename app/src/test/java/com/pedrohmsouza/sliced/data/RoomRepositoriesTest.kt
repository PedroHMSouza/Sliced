package com.pedrohmsouza.sliced.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pedrohmsouza.sliced.SampleData
import com.pedrohmsouza.sliced.data.db.AppDatabase
import com.pedrohmsouza.sliced.domain.WorkoutRules
import com.pedrohmsouza.sliced.model.FieldKey
import com.pedrohmsouza.sliced.model.StaticProgram
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Testa o Room de verdade (SQLite em memória) rodando na JVM via Robolectric. */
@RunWith(AndroidJUnit4::class)
class RoomRepositoriesTest {
    private lateinit var db: AppDatabase
    private lateinit var sessions: RoomSessionRepository
    private lateinit var drafts: RoomDraftRepository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        sessions = RoomSessionRepository(db.sessionDao())
        drafts = RoomDraftRepository(db.draftDao())
    }

    @After fun tearDown() = db.close()

    @Test fun savesAndReadsSessionsNewestFirstKeepingOrder() = runTest {
        SampleData.history.reversed().forEach { sessions.save(it) }
        val read = sessions.sessions.first()
        assertEquals(SampleData.history, read)
    }

    @Test fun saveWithSameIdReplaces() = runTest {
        val s = SampleData.history.first()
        sessions.save(s)
        sessions.save(s.copy(durationMin = 99, exercises = s.exercises.take(1)))
        val read = sessions.sessions.first()
        assertEquals(1, read.size)
        assertEquals(99, read[0].durationMin)
        assertEquals(1, read[0].exercises.size)
    }

    @Test fun deleteCascades() = runTest {
        SampleData.history.forEach { sessions.save(it) }
        sessions.delete(SampleData.history[0].id)
        assertEquals(SampleData.history.size - 1, sessions.sessions.first().size)
        val orphanSets = db.query("SELECT COUNT(*) FROM session_sets s LEFT JOIN session_exercises e ON e.id = s.sessionExerciseId WHERE e.id IS NULL", null)
        orphanSets.moveToFirst()
        assertEquals(0, orphanSets.getInt(0))
    }

    @Test fun draftRoundTrip() = runTest {
        val d = WorkoutRules.newDraft(StaticProgram.treino("A")!!, emptyList(), 1_000L)
            .let { WorkoutRules.run { it.updateSet(0, 1) { s -> s.copy(values = mapOf(FieldKey.KG to "22,5"), done = true) } } }
        drafts.save(d)
        assertEquals(d, drafts.draft.first())
        drafts.clear()
        assertNull(drafts.draft.first())
    }
}
