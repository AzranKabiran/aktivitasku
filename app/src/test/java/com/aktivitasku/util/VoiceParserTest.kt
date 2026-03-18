package com.aktivitasku.util

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class VoiceParserTest {

    // ── Date parsing ──────────────────────────────────────

    @Test
    fun `parse hari ini returns today`() {
        val result = VoiceParser.parse("hari ini jam 9 meeting")
        assertEquals(LocalDate.now(), result.dateTime?.toLocalDate())
    }

    @Test
    fun `parse besok returns tomorrow`() {
        val result = VoiceParser.parse("besok jam 10 olahraga")
        assertEquals(LocalDate.now().plusDays(1), result.dateTime?.toLocalDate())
    }

    @Test
    fun `parse lusa returns day after tomorrow`() {
        val result = VoiceParser.parse("lusa jam 8 antar anak sekolah")
        assertEquals(LocalDate.now().plusDays(2), result.dateTime?.toLocalDate())
    }

    @Test
    fun `parse named day senin returns next monday`() {
        val result = VoiceParser.parse("senin jam 9 rapat")
        val parsed = result.dateTime?.toLocalDate()
        assertNotNull(parsed)
        assertEquals(java.time.DayOfWeek.MONDAY, parsed?.dayOfWeek)
    }

    @Test
    fun `parse named day jumat returns next friday`() {
        val result = VoiceParser.parse("jumat jam 15 antar anak dokter")
        val parsed = result.dateTime?.toLocalDate()
        assertNotNull(parsed)
        assertEquals(java.time.DayOfWeek.FRIDAY, parsed?.dayOfWeek)
    }

    // ── Time parsing ──────────────────────────────────────

    @Test
    fun `parse jam 9 returns 09 00`() {
        val result = VoiceParser.parse("hari ini jam 9 meeting")
        assertEquals(9, result.dateTime?.hour)
        assertEquals(0, result.dateTime?.minute)
    }

    @Test
    fun `parse jam 9 pagi keeps morning hour`() {
        val result = VoiceParser.parse("besok jam 9 pagi rapat")
        assertEquals(9, result.dateTime?.hour)
    }

    @Test
    fun `parse jam 3 sore converts to 15`() {
        val result = VoiceParser.parse("hari ini jam 3 sore olahraga")
        assertEquals(15, result.dateTime?.hour)
    }

    @Test
    fun `parse jam 8 malam converts to 20`() {
        val result = VoiceParser.parse("besok jam 8 malam belajar")
        assertEquals(20, result.dateTime?.hour)
    }

    @Test
    fun `parse jam with minutes returns correct time`() {
        val result = VoiceParser.parse("hari ini jam 9.30 meeting klien")
        assertEquals(9, result.dateTime?.hour)
        assertEquals(30, result.dateTime?.minute)
    }

    @Test
    fun `parse jam with colon separator`() {
        val result = VoiceParser.parse("besok jam 14:30 presentasi")
        assertEquals(14, result.dateTime?.hour)
        assertEquals(30, result.dateTime?.minute)
    }

    // ── Title extraction ──────────────────────────────────

    @Test
    fun `title removes date and time markers`() {
        val result = VoiceParser.parse("besok jam 9 pagi meeting dengan klien")
        val title = result.title.lowercase()
        assertFalse("Title should not contain 'besok'", title.contains("besok"))
        assertFalse("Title should not contain 'jam'", title.contains("jam"))
        assertFalse("Title should not contain 'pagi'", title.contains("pagi"))
        assertTrue("Title should contain meeting", title.contains("meeting"))
    }

    @Test
    fun `title removes filler phrase ingatkan saya`() {
        val result = VoiceParser.parse("ingatkan saya olahraga sore ini jam 5")
        assertFalse(result.title.lowercase().contains("ingatkan"))
        assertFalse(result.title.lowercase().contains("saya"))
    }

    @Test
    fun `title is capitalized`() {
        val result = VoiceParser.parse("hari ini jam 9 meeting")
        assertTrue(result.title[0].isUpperCase())
    }

    @Test
    fun `empty input returns default title`() {
        val result = VoiceParser.parse("")
        assertEquals("Kegiatan baru", result.title)
    }

    // ── Category detection ────────────────────────────────

    @Test
    fun `detects work category from meeting`() {
        val result = VoiceParser.parse("besok jam 9 meeting dengan klien")
        assertEquals("WORK", result.detectedCategory)
    }

    @Test
    fun `detects health category from olahraga`() {
        val result = VoiceParser.parse("hari ini jam 5 sore olahraga")
        assertEquals("HEALTH", result.detectedCategory)
    }

    @Test
    fun `detects study category from belajar`() {
        val result = VoiceParser.parse("malam jam 8 belajar untuk ujian")
        assertEquals("STUDY", result.detectedCategory)
    }

    @Test
    fun `detects health from dokter`() {
        val result = VoiceParser.parse("jumat jam 3 antar anak ke dokter")
        assertEquals("HEALTH", result.detectedCategory)
    }

    @Test
    fun `no category for unrelated input`() {
        val result = VoiceParser.parse("hari ini jam 7 makan siang")
        // "makan" matches PERSONAL
        assertNotNull(result.detectedCategory)
    }

    // ── Edge cases ────────────────────────────────────────

    @Test
    fun `full natural sentence parsed correctly`() {
        val result = VoiceParser.parse("Besok jam 9 pagi meeting dengan klien baru")
        assertEquals(LocalDate.now().plusDays(1), result.dateTime?.toLocalDate())
        assertEquals(9, result.dateTime?.hour)
        assertTrue(result.title.lowercase().contains("meeting"))
    }

    @Test
    fun `parse with only time of day no explicit hour`() {
        val result = VoiceParser.parse("hari ini pagi olahraga")
        // Should get 08:00 from "pagi" mapping
        assertEquals(8, result.dateTime?.hour)
    }

    @Test
    fun `parse midnight subuh`() {
        val result = VoiceParser.parse("besok subuh sholat")
        assertEquals(5, result.dateTime?.hour)
    }
}
