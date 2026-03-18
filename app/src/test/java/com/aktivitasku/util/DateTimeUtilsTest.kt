package com.aktivitasku.util

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DateTimeUtilsTest {

    @Test
    fun `toRelativeLabel returns hari ini for today`() {
        val dt = LocalDateTime.now().withHour(9).withMinute(0)
        val label = dt.toRelativeLabel()
        assertTrue(label.startsWith("Hari ini"))
    }

    @Test
    fun `toRelativeLabel returns besok for tomorrow`() {
        val dt = LocalDate.now().plusDays(1).atTime(10, 30)
        val label = dt.toRelativeLabel()
        assertTrue(label.startsWith("Besok"))
    }

    @Test
    fun `toRelativeLabel returns formatted date for other days`() {
        val dt = LocalDate.now().plusDays(5).atTime(14, 0)
        val label = dt.toRelativeLabel()
        assertFalse(label.startsWith("Hari ini"))
        assertFalse(label.startsWith("Besok"))
        assertTrue(label.contains("·"))
        assertTrue(label.contains("14:00"))
    }

    @Test
    fun `toReminderLabel for 0 minutes`() {
        assertEquals("Saat kegiatan mulai", 0.toReminderLabel())
    }

    @Test
    fun `toReminderLabel for 15 minutes`() {
        assertEquals("15 menit sebelum", 15.toReminderLabel())
    }

    @Test
    fun `toReminderLabel for 60 minutes`() {
        assertEquals("1 jam sebelum", 60.toReminderLabel())
    }

    @Test
    fun `toReminderLabel for 120 minutes`() {
        assertEquals("2 jam sebelum", 120.toReminderLabel())
    }

    @Test
    fun `isToday returns true for today`() {
        assertTrue(LocalDate.now().isToday())
    }

    @Test
    fun `isToday returns false for tomorrow`() {
        assertFalse(LocalDate.now().plusDays(1).isToday())
    }

    @Test
    fun `isTomorrow returns true for tomorrow`() {
        assertTrue(LocalDate.now().plusDays(1).isTomorrow())
    }
}
