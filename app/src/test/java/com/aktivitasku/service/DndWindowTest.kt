package com.aktivitasku.service

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for DND window logic (extracted as pure function for testability).
 */
class DndWindowTest {

    // Mirrors isInsideDndWindow from AlarmReceiver
    private fun isInsideDndWindow(startHour: Int, endHour: Int, nowHour: Int): Boolean {
        return if (startHour <= endHour) {
            nowHour in startHour until endHour
        } else {
            nowHour >= startHour || nowHour < endHour
        }
    }

    // ── Same-day window e.g. 08:00 – 18:00 ───────────────

    @Test
    fun `daytime window 8 to 18 - inside at 12`() {
        assertTrue(isInsideDndWindow(8, 18, 12))
    }

    @Test
    fun `daytime window 8 to 18 - outside at 7`() {
        assertFalse(isInsideDndWindow(8, 18, 7))
    }

    @Test
    fun `daytime window 8 to 18 - outside at 18`() {
        assertFalse(isInsideDndWindow(8, 18, 18))
    }

    @Test
    fun `daytime window 8 to 18 - outside at 22`() {
        assertFalse(isInsideDndWindow(8, 18, 22))
    }

    // ── Overnight window e.g. 22:00 – 07:00 ──────────────

    @Test
    fun `overnight window 22 to 7 - inside at 23`() {
        assertTrue(isInsideDndWindow(22, 7, 23))
    }

    @Test
    fun `overnight window 22 to 7 - inside at 0`() {
        assertTrue(isInsideDndWindow(22, 7, 0))
    }

    @Test
    fun `overnight window 22 to 7 - inside at 3`() {
        assertTrue(isInsideDndWindow(22, 7, 3))
    }

    @Test
    fun `overnight window 22 to 7 - outside at 7`() {
        assertFalse(isInsideDndWindow(22, 7, 7))
    }

    @Test
    fun `overnight window 22 to 7 - outside at 12`() {
        assertFalse(isInsideDndWindow(22, 7, 12))
    }

    @Test
    fun `overnight window 22 to 7 - outside at 21`() {
        assertFalse(isInsideDndWindow(22, 7, 21))
    }

    @Test
    fun `overnight window exactly at start 22`() {
        assertTrue(isInsideDndWindow(22, 7, 22))
    }

    // ── Edge: midnight window 0 to 6 ─────────────────────

    @Test
    fun `midnight window 0 to 6 - inside at 3`() {
        assertTrue(isInsideDndWindow(0, 6, 3))
    }

    @Test
    fun `midnight window 0 to 6 - outside at 8`() {
        assertFalse(isInsideDndWindow(0, 6, 8))
    }
}
