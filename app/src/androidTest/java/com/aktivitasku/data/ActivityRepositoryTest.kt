package com.aktivitasku.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aktivitasku.data.local.database.AppDatabase
import com.aktivitasku.data.repository.ActivityRepository
import com.aktivitasku.domain.model.Activity
import com.aktivitasku.domain.model.ActivityCategory
import com.aktivitasku.domain.model.Priority
import com.aktivitasku.domain.model.RepeatType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class ActivityRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: ActivityRepository

    private fun activity(
        title: String = "Test",
        category: ActivityCategory = ActivityCategory.WORK,
        completed: Boolean = false,
        daysFromNow: Long = 1
    ) = Activity(
        title         = title,
        startDateTime = LocalDateTime.now().plusDays(daysFromNow),
        category      = category,
        isCompleted   = completed,
        priority      = Priority.MEDIUM,
        reminders     = listOf(15),
        repeatType    = RepeatType.NONE
    )

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db   = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
        repo = ActivityRepository(db.activityDao())
    }

    @After
    fun teardown() = db.close()

    // ── Basic CRUD ──────────────────────────────────────

    @Test
    fun save_and_getById_roundtrip() = runTest {
        val id = repo.save(activity("Rapat klien"))
        val found = repo.getById(id)
        assertNotNull(found)
        assertEquals("Rapat klien", found!!.title)
    }

    @Test
    fun update_changes_title() = runTest {
        val id = repo.save(activity("Lama"))
        val old = repo.getById(id)!!
        repo.update(old.copy(title = "Baru"))
        assertEquals("Baru", repo.getById(id)!!.title)
    }

    @Test
    fun delete_removes_activity() = runTest {
        val id = repo.save(activity())
        repo.deleteById(id)
        assertNull(repo.getById(id))
    }

    // ── Reactive flows ──────────────────────────────────

    @Test
    fun observeAll_emits_on_insert() = runTest {
        assertTrue(repo.observeAll().first().isEmpty())
        repo.save(activity())
        assertEquals(1, repo.observeAll().first().size)
    }

    @Test
    fun observeAll_sorted_by_startDateTime() = runTest {
        repo.save(activity("C", daysFromNow = 3))
        repo.save(activity("A", daysFromNow = 1))
        repo.save(activity("B", daysFromNow = 2))
        val list = repo.observeAll().first()
        assertEquals("A", list[0].title)
        assertEquals("B", list[1].title)
        assertEquals("C", list[2].title)
    }

    // ── Set completed ───────────────────────────────────

    @Test
    fun setCompleted_true_updates_flag() = runTest {
        val id = repo.save(activity(completed = false))
        repo.setCompleted(id, true)
        assertTrue(repo.getById(id)!!.isCompleted)
    }

    @Test
    fun setCompleted_false_unsets_flag() = runTest {
        val id = repo.save(activity(completed = true))
        repo.setCompleted(id, false)
        assertFalse(repo.getById(id)!!.isCompleted)
    }

    // ── Counts ──────────────────────────────────────────

    @Test
    fun countCompleted_counts_correctly() = runTest {
        repo.save(activity(completed = true))
        repo.save(activity(completed = true))
        repo.save(activity(completed = false))
        assertEquals(2, repo.countCompleted().first())
        assertEquals(1, repo.countPending().first())
    }

    // ── Search ──────────────────────────────────────────

    @Test
    fun search_finds_by_title_substring() = runTest {
        repo.save(activity("Meeting dengan tim"))
        repo.save(activity("Olahraga sore"))
        val results = repo.search("meeting").first()
        assertEquals(1, results.size)
        assertEquals("Meeting dengan tim", results[0].title)
    }

    @Test
    fun search_is_case_insensitive() = runTest {
        repo.save(activity("RAPAT PAGI"))
        val results = repo.search("rapat").first()
        assertEquals(1, results.size)
    }

    // ── Upcoming ────────────────────────────────────────

    @Test
    fun observeUpcoming_excludes_completed() = runTest {
        repo.save(activity("Done",   completed = true,  daysFromNow = 1))
        repo.save(activity("Active", completed = false, daysFromNow = 2))
        val upcoming = repo.observeUpcoming().first()
        assertEquals(1, upcoming.size)
        assertEquals("Active", upcoming[0].title)
    }

    // ── Date range ──────────────────────────────────────

    @Test
    fun observeByDateRange_filters_correctly() = runTest {
        val today = LocalDate.now()
        repo.save(activity("Yesterday", daysFromNow = -1))
        repo.save(activity("Today",     daysFromNow = 0))
        repo.save(activity("Tomorrow",  daysFromNow = 1))

        val results = repo.observeByDateRange(
            start = today.atStartOfDay(),
            end   = today.plusDays(1).atStartOfDay()
        ).first()

        assertEquals(1, results.size)
        assertEquals("Today", results[0].title)
    }

    // ── Category filter ─────────────────────────────────

    @Test
    fun observeByCategory_filters_to_category() = runTest {
        repo.save(activity("Work task",     category = ActivityCategory.WORK))
        repo.save(activity("Health task",   category = ActivityCategory.HEALTH))
        repo.save(activity("Another work",  category = ActivityCategory.WORK))

        val workItems = repo.observeByCategory("WORK").first()
        assertEquals(2, workItems.size)
        assertTrue(workItems.all { it.category == ActivityCategory.WORK })
    }

    // ── observeById ─────────────────────────────────────

    @Test
    fun observeById_emits_updates() = runTest {
        val id    = repo.save(activity("Original"))
        val first = repo.observeById(id).first()
        assertEquals("Original", first?.title)

        repo.update(first!!.copy(title = "Updated"))
        val second = repo.observeById(id).first()
        assertEquals("Updated", second?.title)
    }

    @Test
    fun observeById_returns_null_after_delete() = runTest {
        val id = repo.save(activity())
        repo.deleteById(id)
        assertNull(repo.observeById(id).first())
    }
}
