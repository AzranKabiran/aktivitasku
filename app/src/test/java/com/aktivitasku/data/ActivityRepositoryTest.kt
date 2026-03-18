package com.aktivitasku.data

import com.aktivitasku.data.local.dao.ActivityDao
import com.aktivitasku.data.local.entity.ActivityEntity
import com.aktivitasku.data.repository.ActivityRepository
import com.aktivitasku.domain.model.*
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class ActivityRepositoryTest {

    private lateinit var dao: ActivityDao
    private lateinit var repository: ActivityRepository

    private fun makeEntity(
        id: Long = 1L,
        title: String = "Test",
        completed: Boolean = false
    ) = ActivityEntity(
        id            = id,
        title         = title,
        description   = "",
        startDateTime = LocalDateTime.now().plusHours(1),
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
    fun setup() {
        dao = mockk(relaxed = true)
        repository = ActivityRepository(dao)
    }

    @Test
    fun `observeAll maps entities to domain`() = runTest {
        val entity = makeEntity(title = "Meeting")
        every { dao.observeAll() } returns flowOf(listOf(entity))

        val activities = repository.observeAll().first()

        assertEquals(1, activities.size)
        assertEquals("Meeting", activities[0].title)
        assertEquals(ActivityCategory.OTHER, activities[0].category)
    }

    @Test
    fun `observeAll returns empty list when no data`() = runTest {
        every { dao.observeAll() } returns flowOf(emptyList())

        val activities = repository.observeAll().first()

        assertTrue(activities.isEmpty())
    }

    @Test
    fun `save calls dao insert and returns id`() = runTest {
        val activity = Activity(
            title         = "New activity",
            startDateTime = LocalDateTime.now().plusHours(2),
            category      = ActivityCategory.WORK,
            priority      = Priority.HIGH
        )
        coEvery { dao.insert(any()) } returns 42L

        val id = repository.save(activity)

        assertEquals(42L, id)
        coVerify { dao.insert(any()) }
    }

    @Test
    fun `update calls dao update`() = runTest {
        val activity = Activity(
            id            = 5L,
            title         = "Updated",
            startDateTime = LocalDateTime.now().plusHours(1)
        )
        coEvery { dao.update(any()) } just Runs

        repository.update(activity)

        coVerify { dao.update(any()) }
    }

    @Test
    fun `delete calls dao delete`() = runTest {
        val activity = Activity(
            id            = 3L,
            title         = "Delete me",
            startDateTime = LocalDateTime.now()
        )
        coEvery { dao.delete(any()) } just Runs

        repository.delete(activity)

        coVerify { dao.delete(any()) }
    }

    @Test
    fun `setCompleted delegates to dao`() = runTest {
        coEvery { dao.setCompleted(any(), any()) } just Runs

        repository.setCompleted(7L, true)

        coVerify { dao.setCompleted(7L, true) }
    }

    @Test
    fun `search maps results to domain`() = runTest {
        val entity = makeEntity(title = "Olahraga pagi")
        every { dao.search("olahraga") } returns flowOf(listOf(entity))

        val results = repository.search("olahraga").first()

        assertEquals(1, results.size)
        assertEquals("Olahraga pagi", results[0].title)
    }

    @Test
    fun `countCompleted delegates to dao`() = runTest {
        every { dao.countCompleted() } returns flowOf(5)

        val count = repository.countCompleted().first()

        assertEquals(5, count)
    }

    @Test
    fun `countPending delegates to dao`() = runTest {
        every { dao.countPending() } returns flowOf(3)

        val count = repository.countPending().first()

        assertEquals(3, count)
    }
}
