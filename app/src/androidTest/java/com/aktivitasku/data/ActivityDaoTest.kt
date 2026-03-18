package com.aktivitasku.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aktivitasku.data.local.dao.ActivityDao
import com.aktivitasku.data.local.database.AppDatabase
import com.aktivitasku.data.local.entity.ActivityEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class ActivityDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ActivityDao

    private fun makeEntity(
        id: Long = 0,
        title: String = "Test",
        completed: Boolean = false,
        startDt: LocalDateTime = LocalDateTime.now().plusHours(1)
    ) = ActivityEntity(
        id            = id,
        title         = title,
        description   = "",
        startDateTime = startDt,
        endDateTime   = null,
        category      = "OTHER",
        priority      = "MEDIUM",
        isCompleted   = completed,
        isAllDay      = false,
        repeatType    = "NONE",
        repeatDays    = "",
        reminders     = "15",
        colorHex      = null,
        createdAt     = LocalDateTime.now()
    )

    @Before
    fun createDb() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db  = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
        dao = db.activityDao()
    }

    @After
    fun closeDb() = db.close()

    // ── Insert & read ──────────────────────────────────

    @Test
    fun insertAndReadActivity() = runTest {
        val id = dao.insert(makeEntity(title = "Meeting pagi"))
        val result = dao.getById(id)
        assertNotNull(result)
        assertEquals("Meeting pagi", result?.title)
    }

    @Test
    fun insertMultipleReturnsSortedByTime() = runTest {
        dao.insert(makeEntity(title = "C", startDt = LocalDateTime.now().plusHours(3)))
        dao.insert(makeEntity(title = "A", startDt = LocalDateTime.now().plusHours(1)))
        dao.insert(makeEntity(title = "B", startDt = LocalDateTime.now().plusHours(2)))

        val all = dao.observeAll().first()
        assertEquals("A", all[0].title)
        assertEquals("B", all[1].title)
        assertEquals("C", all[2].title)
    }

    // ── Update ─────────────────────────────────────────

    @Test
    fun updateChangesTitle() = runTest {
        val id = dao.insert(makeEntity(title = "Original"))
        val entity = dao.getById(id)!!
        dao.update(entity.copy(title = "Updated"))
        assertEquals("Updated", dao.getById(id)?.title)
    }

    // ── Delete ─────────────────────────────────────────

    @Test
    fun deleteRemovesActivity() = runTest {
        val id = dao.insert(makeEntity(title = "To delete"))
        val entity = dao.getById(id)!!
        dao.delete(entity)
        assertNull(dao.getById(id))
    }

    @Test
    fun deleteByIdRemovesActivity() = runTest {
        val id = dao.insert(makeEntity(title = "Delete by ID"))
        dao.deleteById(id)
        assertNull(dao.getById(id))
    }

    // ── Complete ───────────────────────────────────────

    @Test
    fun setCompletedUpdatesFlag() = runTest {
        val id = dao.insert(makeEntity(completed = false))
        dao.setCompleted(id, true)
        assertTrue(dao.getById(id)?.isCompleted == true)
    }

    @Test
    fun setCompletedFalseUnchecks() = runTest {
        val id = dao.insert(makeEntity(completed = true))
        dao.setCompleted(id, false)
        assertFalse(dao.getById(id)?.isCompleted == true)
    }

    // ── Counts ─────────────────────────────────────────

    @Test
    fun countCompletedAndPending() = runTest {
        dao.insert(makeEntity(completed = true))
        dao.insert(makeEntity(completed = true))
        dao.insert(makeEntity(completed = false))

        assertEquals(2, dao.countCompleted().first())
        assertEquals(1, dao.countPending().first())
    }

    // ── Search ─────────────────────────────────────────

    @Test
    fun searchFindsMatchingTitle() = runTest {
        dao.insert(makeEntity(title = "Meeting dengan klien"))
        dao.insert(makeEntity(title = "Olahraga pagi"))

        val results = dao.search("meeting").first()
        assertEquals(1, results.size)
        assertEquals("Meeting dengan klien", results[0].title)
    }

    @Test
    fun searchIsCaseInsensitive() = runTest {
        dao.insert(makeEntity(title = "OLAHRAGA SORE"))
        val results = dao.search("olahraga").first()
        assertEquals(1, results.size)
    }

    @Test
    fun searchEmptyReturnNothing() = runTest {
        dao.insert(makeEntity(title = "Meeting"))
        val results = dao.search("xyz_no_match").first()
        assertTrue(results.isEmpty())
    }

    // ── Upcoming ───────────────────────────────────────

    @Test
    fun observeUpcomingExcludesCompletedAndPast() = runTest {
        dao.insert(makeEntity(title = "Past", startDt = LocalDateTime.now().minusHours(1)))
        dao.insert(makeEntity(title = "Done", startDt = LocalDateTime.now().plusHours(1), completed = true))
        dao.insert(makeEntity(title = "Future", startDt = LocalDateTime.now().plusHours(2), completed = false))

        val upcoming = dao.observeUpcoming().first()
        assertEquals(1, upcoming.size)
        assertEquals("Future", upcoming[0].title)
    }

    // ── Category filter ────────────────────────────────

    @Test
    fun filterByCategory() = runTest {
        dao.insert(makeEntity(title = "Work").also { /* category = OTHER by default */ })
        val workEntity = makeEntity(title = "Rapat").copy(category = "WORK")
        dao.insert(workEntity)

        val workItems = dao.observeByCategory("WORK").first()
        assertEquals(1, workItems.size)
        assertEquals("Rapat", workItems[0].title)
    }
}
