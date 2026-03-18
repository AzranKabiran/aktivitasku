package com.aktivitasku.data

import com.aktivitasku.data.local.entity.ActivityEntity
import com.aktivitasku.data.local.entity.toEntity
import com.aktivitasku.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class ActivityMappingTest {

    private val now = LocalDateTime.of(2025, 6, 15, 9, 0)

    private fun makeActivity(
        id: Long = 1L,
        title: String = "Test",
        category: ActivityCategory = ActivityCategory.WORK,
        priority: Priority = Priority.HIGH,
        reminders: List<Int> = listOf(5, 15),
        repeatDays: List<Int> = listOf(1, 3, 5),
        repeatType: RepeatType = RepeatType.CUSTOM
    ) = Activity(
        id            = id,
        title         = title,
        description   = "desc",
        startDateTime = now,
        category      = category,
        priority      = priority,
        reminders     = reminders,
        repeatType    = repeatType,
        repeatDays    = repeatDays,
        isCompleted   = false,
        isAllDay      = false,
        createdAt     = now
    )

    // ── Domain → Entity ────────────────────────────────────

    @Test
    fun `toEntity stores category as string name`() {
        val entity = makeActivity(category = ActivityCategory.HEALTH).toEntity()
        assertEquals("HEALTH", entity.category)
    }

    @Test
    fun `toEntity stores priority as string name`() {
        val entity = makeActivity(priority = Priority.LOW).toEntity()
        assertEquals("LOW", entity.priority)
    }

    @Test
    fun `toEntity joins reminders as csv`() {
        val entity = makeActivity(reminders = listOf(5, 15, 60)).toEntity()
        assertEquals("5,15,60", entity.reminders)
    }

    @Test
    fun `toEntity joins repeatDays as csv`() {
        val entity = makeActivity(repeatDays = listOf(1, 3, 5)).toEntity()
        assertEquals("1,3,5", entity.repeatDays)
    }

    @Test
    fun `toEntity preserves empty reminders`() {
        val entity = makeActivity(reminders = emptyList()).toEntity()
        assertEquals("", entity.reminders)
    }

    @Test
    fun `toEntity preserves empty repeatDays`() {
        val entity = makeActivity(repeatDays = emptyList()).toEntity()
        assertEquals("", entity.repeatDays)
    }

    // ── Entity → Domain ────────────────────────────────────

    private fun makeEntity(
        id: Long = 1L,
        title: String = "Test",
        category: String = "WORK",
        priority: String = "MEDIUM",
        reminders: String = "15,30",
        repeatDays: String = "",
        repeatType: String = "NONE",
        completed: Boolean = false
    ) = ActivityEntity(
        id            = id,
        title         = title,
        description   = "desc",
        startDateTime = now,
        endDateTime   = null,
        category      = category,
        priority      = priority,
        isCompleted   = completed,
        isAllDay      = false,
        repeatType    = repeatType,
        repeatDays    = repeatDays,
        reminders     = reminders,
        colorHex      = null,
        createdAt     = now
    )

    @Test
    fun `toDomain parses category enum`() {
        val domain = makeEntity(category = "STUDY").toDomain()
        assertEquals(ActivityCategory.STUDY, domain.category)
    }

    @Test
    fun `toDomain parses priority enum`() {
        val domain = makeEntity(priority = "HIGH").toDomain()
        assertEquals(Priority.HIGH, domain.priority)
    }

    @Test
    fun `toDomain splits reminders csv`() {
        val domain = makeEntity(reminders = "5,15,60").toDomain()
        assertEquals(listOf(5, 15, 60), domain.reminders)
    }

    @Test
    fun `toDomain splits repeatDays csv`() {
        val domain = makeEntity(repeatDays = "1,3,5").toDomain()
        assertEquals(listOf(1, 3, 5), domain.repeatDays)
    }

    @Test
    fun `toDomain handles empty reminders string`() {
        val domain = makeEntity(reminders = "").toDomain()
        assertTrue(domain.reminders.isEmpty())
    }

    @Test
    fun `toDomain handles empty repeatDays string`() {
        val domain = makeEntity(repeatDays = "").toDomain()
        assertTrue(domain.repeatDays.isEmpty())
    }

    // ── Round-trip ─────────────────────────────────────────

    @Test
    fun `round-trip domain to entity and back preserves fields`() {
        val original = makeActivity(
            title      = "Presentasi klien",
            category   = ActivityCategory.WORK,
            priority   = Priority.HIGH,
            reminders  = listOf(15, 60),
            repeatDays = listOf(1, 3),
            repeatType = RepeatType.CUSTOM
        )

        val back = original.toEntity().toDomain()

        assertEquals(original.title,      back.title)
        assertEquals(original.category,   back.category)
        assertEquals(original.priority,   back.priority)
        assertEquals(original.reminders,  back.reminders)
        assertEquals(original.repeatDays, back.repeatDays)
        assertEquals(original.repeatType, back.repeatType)
    }
}
