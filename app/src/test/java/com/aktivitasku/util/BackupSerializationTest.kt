package com.aktivitasku.util

import com.aktivitasku.domain.model.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Tests for JSON serialization logic extracted from BackupManager.
 * Pure JVM tests, no Android context needed.
 */
class BackupSerializationTest {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    private fun makeActivity(
        id: Long = 1L,
        title: String = "Test Kegiatan",
        category: ActivityCategory = ActivityCategory.WORK,
        priority: Priority = Priority.MEDIUM,
        reminders: List<Int> = listOf(15, 30),
        repeatType: RepeatType = RepeatType.NONE,
        isCompleted: Boolean = false
    ) = Activity(
        id            = id,
        title         = title,
        description   = "Deskripsi test",
        startDateTime = LocalDateTime.of(2025, 6, 15, 9, 0),
        category      = category,
        priority      = priority,
        reminders     = reminders,
        repeatType    = repeatType,
        isCompleted   = isCompleted,
        createdAt     = LocalDateTime.of(2025, 6, 1, 10, 0)
    )

    // ── Serialization helpers (mirror BackupManager logic) ──

    private fun activityToJson(a: Activity): JSONObject = JSONObject().apply {
        put("id",            a.id)
        put("title",         a.title)
        put("description",   a.description)
        put("startDateTime", a.startDateTime.format(formatter))
        put("endDateTime",   a.endDateTime?.format(formatter) ?: "")
        put("category",      a.category.name)
        put("priority",      a.priority.name)
        put("isCompleted",   a.isCompleted)
        put("isAllDay",      a.isAllDay)
        put("repeatType",    a.repeatType.name)
        put("repeatDays",    a.repeatDays.joinToString(","))
        put("reminders",     a.reminders.joinToString(","))
        put("colorHex",      a.colorHex ?: "")
        put("createdAt",     a.createdAt.format(formatter))
    }

    private fun jsonToActivity(o: JSONObject): Activity = Activity(
        id            = 0,
        title         = o.getString("title"),
        description   = o.optString("description", ""),
        startDateTime = LocalDateTime.parse(o.getString("startDateTime"), formatter),
        endDateTime   = o.optString("endDateTime","").takeIf { it.isNotBlank() }
                         ?.let { LocalDateTime.parse(it, formatter) },
        category      = runCatching { ActivityCategory.valueOf(o.getString("category")) }
                         .getOrDefault(ActivityCategory.OTHER),
        priority      = runCatching { Priority.valueOf(o.getString("priority")) }
                         .getOrDefault(Priority.MEDIUM),
        isCompleted   = o.optBoolean("isCompleted", false),
        repeatType    = runCatching { RepeatType.valueOf(o.getString("repeatType")) }
                         .getOrDefault(RepeatType.NONE),
        reminders     = o.optString("reminders","15").split(",")
                         .mapNotNull { it.trim().toIntOrNull() },
        createdAt     = LocalDateTime.parse(o.getString("createdAt"), formatter)
    )

    // ── Tests ──────────────────────────────────────────────

    @Test
    fun `serialize title is preserved`() {
        val json = activityToJson(makeActivity(title = "Meeting pagi"))
        assertEquals("Meeting pagi", json.getString("title"))
    }

    @Test
    fun `serialize category name is stored`() {
        val json = activityToJson(makeActivity(category = ActivityCategory.HEALTH))
        assertEquals("HEALTH", json.getString("category"))
    }

    @Test
    fun `serialize priority name is stored`() {
        val json = activityToJson(makeActivity(priority = Priority.HIGH))
        assertEquals("HIGH", json.getString("priority"))
    }

    @Test
    fun `serialize reminders joined as csv`() {
        val json = activityToJson(makeActivity(reminders = listOf(5, 15, 60)))
        assertEquals("5,15,60", json.getString("reminders"))
    }

    @Test
    fun `serialize datetime in ISO format`() {
        val a = makeActivity()
        val json = activityToJson(a)
        val parsed = LocalDateTime.parse(json.getString("startDateTime"), formatter)
        assertEquals(a.startDateTime, parsed)
    }

    @Test
    fun `serialize completed flag`() {
        val json = activityToJson(makeActivity(isCompleted = true))
        assertTrue(json.getBoolean("isCompleted"))
    }

    @Test
    fun `deserialize restores title`() {
        val a = makeActivity(title = "Olahraga sore")
        val result = jsonToActivity(activityToJson(a))
        assertEquals("Olahraga sore", result.title)
    }

    @Test
    fun `deserialize restores category`() {
        val a = makeActivity(category = ActivityCategory.STUDY)
        val result = jsonToActivity(activityToJson(a))
        assertEquals(ActivityCategory.STUDY, result.category)
    }

    @Test
    fun `deserialize restores reminders list`() {
        val a = makeActivity(reminders = listOf(10, 30))
        val result = jsonToActivity(activityToJson(a))
        assertEquals(listOf(10, 30), result.reminders)
    }

    @Test
    fun `deserialize unknown category defaults to OTHER`() {
        val json = activityToJson(makeActivity())
        json.put("category", "INVALID_CATEGORY")
        val result = jsonToActivity(json)
        assertEquals(ActivityCategory.OTHER, result.category)
    }

    @Test
    fun `deserialize unknown priority defaults to MEDIUM`() {
        val json = activityToJson(makeActivity())
        json.put("priority", "ULTRA_HIGH")
        val result = jsonToActivity(json)
        assertEquals(Priority.MEDIUM, result.priority)
    }

    @Test
    fun `round-trip preserves all core fields`() {
        val original = makeActivity(
            title      = "Rapat tahunan",
            category   = ActivityCategory.WORK,
            priority   = Priority.HIGH,
            reminders  = listOf(15, 60),
            repeatType = RepeatType.WEEKLY,
            isCompleted = false
        )
        val result = jsonToActivity(activityToJson(original))

        assertEquals(original.title,         result.title)
        assertEquals(original.description,   result.description)
        assertEquals(original.startDateTime, result.startDateTime)
        assertEquals(original.category,      result.category)
        assertEquals(original.priority,      result.priority)
        assertEquals(original.reminders,     result.reminders)
        assertEquals(original.repeatType,    result.repeatType)
        assertEquals(original.isCompleted,   result.isCompleted)
    }
}
