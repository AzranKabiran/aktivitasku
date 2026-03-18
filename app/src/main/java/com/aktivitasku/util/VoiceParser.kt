package com.aktivitasku.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.DayOfWeek

data class ParsedActivity(
    val title: String,
    val dateTime: LocalDateTime?,
    val detectedCategory: String? = null
)

object VoiceParser {

    // ── Day name mapping (Indonesia) ─────────────────────
    private val dayNames = mapOf(
        "senin"   to DayOfWeek.MONDAY,
        "selasa"  to DayOfWeek.TUESDAY,
        "rabu"    to DayOfWeek.WEDNESDAY,
        "kamis"   to DayOfWeek.THURSDAY,
        "jumat"   to DayOfWeek.FRIDAY,
        "jumat"   to DayOfWeek.FRIDAY,
        "sabtu"   to DayOfWeek.SATURDAY,
        "minggu"  to DayOfWeek.SUNDAY
    )

    // ── Time-of-day anchors ───────────────────────────────
    private val timeOfDay = mapOf(
        "pagi"   to 8,
        "siang"  to 12,
        "sore"   to 16,
        "malam"  to 20,
        "tengah malam" to 0,
        "subuh"  to 5
    )

    // ── Category keywords ─────────────────────────────────
    private val categoryKeywords = mapOf(
        "WORK"     to listOf("meeting", "rapat", "kerja", "kantor", "presentasi", "klien", "deadline", "laporan"),
        "HEALTH"   to listOf("olahraga", "gym", "dokter", "rumah sakit", "lari", "yoga", "minum obat", "periksa"),
        "STUDY"    to listOf("belajar", "kuliah", "sekolah", "ujian", "pr", "tugas", "les", "kursus"),
        "PERSONAL" to listOf("antar", "jemput", "belanja", "makan", "tidur", "keluarga", "teman")
    )

    /**
     * Main entry point. Parse a natural language string in Bahasa Indonesia.
     * Example: "Besok jam 9 pagi meeting dengan klien"
     */
    fun parse(raw: String): ParsedActivity {
        val text  = raw.lowercase().trim()
        val today = LocalDate.now()

        val date  = extractDate(text, today)
        val time  = extractTime(text)
        val dt    = if (date != null && time != null) LocalDateTime.of(date, time) else null
        val title = extractTitle(text)
        val cat   = detectCategory(text)

        return ParsedActivity(title = title, dateTime = dt, detectedCategory = cat)
    }

    // ── Date Extraction ───────────────────────────────────

    private fun extractDate(text: String, today: LocalDate): LocalDate? {
        return when {
            text.contains("hari ini")  -> today
            text.contains("besok")     -> today.plusDays(1)
            text.contains("lusa")      -> today.plusDays(2)
            text.contains("kemarin")   -> today.minusDays(1)
            else -> extractDayName(text, today) ?: extractExplicitDate(text, today)
        }
    }

    private fun extractDayName(text: String, today: LocalDate): LocalDate? {
        dayNames.forEach { (name, dow) ->
            if (text.contains(name)) {
                var date = today
                // Find next occurrence of that day
                while (date.dayOfWeek != dow) date = date.plusDays(1)
                return if (date == today) date.plusWeeks(1) else date
            }
        }
        return null
    }

    private fun extractExplicitDate(text: String, today: LocalDate): LocalDate? {
        // Pattern: "tanggal 15" or "15 april"
        val tanggalRegex = Regex("""tanggal\s+(\d{1,2})""")
        tanggalRegex.find(text)?.let { m ->
            val day = m.groupValues[1].toIntOrNull() ?: return null
            return today.withDayOfMonth(day).let {
                if (it.isBefore(today)) it.plusMonths(1) else it
            }
        }
        // Pattern: "15 maret" etc
        val monthNames = mapOf(
            "januari" to 1, "februari" to 2, "maret" to 3,
            "april"   to 4, "mei"      to 5, "juni"  to 6,
            "juli"    to 7, "agustus"  to 8, "september" to 9,
            "oktober" to 10, "november" to 11, "desember" to 12
        )
        monthNames.forEach { (name, month) ->
            val regex = Regex("""(\d{1,2})\s+$name""")
            regex.find(text)?.let { m ->
                val day = m.groupValues[1].toIntOrNull() ?: return null
                return LocalDate.of(today.year, month, day).let {
                    if (it.isBefore(today)) it.plusYears(1) else it
                }
            }
        }
        return null
    }

    // ── Time Extraction ───────────────────────────────────

    private fun extractTime(text: String): LocalTime? {
        // "jam 9", "jam 09", "jam 9.30", "jam 9:30"
        val jamRegex = Regex("""jam\s+(\d{1,2})(?:[.:,](\d{2}))?""")
        jamRegex.find(text)?.let { m ->
            var hour   = m.groupValues[1].toIntOrNull() ?: return null
            val minute = m.groupValues[2].toIntOrNull() ?: 0
            // Adjust hour based on time-of-day context
            if (hour < 12) {
                val tod = extractTimeOfDay(text)
                when (tod) {
                    "siang", "sore" -> if (hour < 6) hour += 12
                    "malam"         -> if (hour < 9) hour += 12
                }
            }
            return LocalTime.of(hour.coerceIn(0, 23), minute)
        }
        // Fall back to time-of-day only
        val tod = extractTimeOfDay(text)
        if (tod != null) {
            return LocalTime.of(timeOfDay[tod] ?: return null, 0)
        }
        return null
    }

    private fun extractTimeOfDay(text: String): String? =
        timeOfDay.keys.firstOrNull { text.contains(it) }

    // ── Title Extraction ──────────────────────────────────

    private fun extractTitle(text: String): String {
        var cleaned = text

        // Remove time markers
        cleaned = cleaned.replace(Regex("""jam\s+\d{1,2}(?:[.:]\d{2})?"""), "")
        cleaned = cleaned.replace(Regex("""tanggal\s+\d{1,2}"""), "")

        // Remove date keywords
        listOf("hari ini", "besok", "lusa", "kemarin").forEach { cleaned = cleaned.replace(it, "") }
        dayNames.keys.forEach { cleaned = cleaned.replace(it, "") }

        // Remove time-of-day words
        timeOfDay.keys.forEach { cleaned = cleaned.replace(it, "") }

        // Remove filler phrases
        listOf("tolong", "ingatkan saya", "ingatkan aku", "jadwalkan", "set alarm", "catat").forEach {
            cleaned = cleaned.replace(it, "")
        }

        // Capitalize first letter
        return cleaned.trim()
            .replace(Regex("\\s+"), " ")
            .replaceFirstChar { it.uppercase() }
            .ifBlank { "Kegiatan baru" }
    }

    // ── Category Detection ────────────────────────────────

    private fun detectCategory(text: String): String? {
        var maxScore = 0
        var bestCat: String? = null
        categoryKeywords.forEach { (cat, keywords) ->
            val score = keywords.count { text.contains(it) }
            if (score > maxScore) { maxScore = score; bestCat = cat }
        }
        return bestCat
    }
}
