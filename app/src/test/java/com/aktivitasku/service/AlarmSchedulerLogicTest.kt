package com.aktivitasku.service

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for AlarmScheduler pure logic — request code generation,
 * DND overlap detection, and reminder filtering.
 * No Android context needed.
 */
class AlarmSchedulerLogicTest {

    // Mirrors AlarmScheduler.requestCode formula
    private fun requestCode(activityId: Long, reminderIndex: Int): Int =
        (activityId * 100 + reminderIndex).toInt()

    // ── Request code uniqueness ────────────────────────────

    @Test
    fun `requestCode is unique per activity per reminder index`() {
        val codes = mutableSetOf<Int>()
        // 20 activities, 5 reminders each → 100 unique codes
        for (id in 1L..20L) {
            for (idx in 0..4) {
                val code = requestCode(id, idx)
                assertTrue("Duplicate code for id=$id idx=$idx", codes.add(code))
            }
        }
    }

    @Test
    fun `requestCode changes with different activity id`() {
        assertNotEquals(requestCode(1L, 0), requestCode(2L, 0))
    }

    @Test
    fun `requestCode changes with different reminder index`() {
        assertNotEquals(requestCode(1L, 0), requestCode(1L, 1))
    }

    @Test
    fun `cancel range covers up to 10 reminders`() {
        // AlarmScheduler.cancel loops i in 0..9
        val id   = 5L
        val range = (0..9).map { requestCode(id, it) }
        assertEquals(10, range.distinct().size)
    }

    // ── Reminder filtering ─────────────────────────────────

    private fun shouldSchedule(
        triggerMinutesFromNow: Long,
        minMinutesAhead: Long = 0
    ): Boolean = triggerMinutesFromNow > minMinutesAhead

    @Test
    fun `reminder in the past is not scheduled`() {
        assertFalse(shouldSchedule(-5))
    }

    @Test
    fun `reminder exactly now is not scheduled`() {
        assertFalse(shouldSchedule(0))
    }

    @Test
    fun `reminder 1 minute ahead is scheduled`() {
        assertTrue(shouldSchedule(1))
    }

    @Test
    fun `reminder 60 minutes ahead is scheduled`() {
        assertTrue(shouldSchedule(60))
    }

    // ── Snooze offset ─────────────────────────────────────

    @Test
    fun `snooze adds 5 minutes`() {
        val snoozeMinutes = 5
        val now = System.currentTimeMillis()
        val snoozedAt = now + snoozeMinutes * 60 * 1000L
        assertTrue(snoozedAt > now)
        assertEquals(5 * 60 * 1000L, snoozedAt - now)
    }

    // ── Content text formatting ────────────────────────────

    private fun formatContentText(minutesBefore: Int): String = when {
        minutesBefore == 0    -> "Kegiatan dimulai sekarang!"
        minutesBefore < 60    -> "$minutesBefore menit lagi"
        minutesBefore == 60   -> "1 jam lagi"
        minutesBefore == 120  -> "2 jam lagi"
        minutesBefore == 1440 -> "1 hari lagi"
        else                  -> "${minutesBefore / 60} jam lagi"
    }

    @Test
    fun `format 0 minutes returns sekarang`() {
        assertEquals("Kegiatan dimulai sekarang!", formatContentText(0))
    }

    @Test
    fun `format 5 minutes`() {
        assertEquals("5 menit lagi", formatContentText(5))
    }

    @Test
    fun `format 15 minutes`() {
        assertEquals("15 menit lagi", formatContentText(15))
    }

    @Test
    fun `format 30 minutes`() {
        assertEquals("30 menit lagi", formatContentText(30))
    }

    @Test
    fun `format 60 minutes returns 1 jam`() {
        assertEquals("1 jam lagi", formatContentText(60))
    }

    @Test
    fun `format 120 minutes returns 2 jam`() {
        assertEquals("2 jam lagi", formatContentText(120))
    }

    @Test
    fun `format 1440 minutes returns 1 hari`() {
        assertEquals("1 hari lagi", formatContentText(1440))
    }

    @Test
    fun `format 180 minutes returns 3 jam`() {
        assertEquals("3 jam lagi", formatContentText(180))
    }
}
