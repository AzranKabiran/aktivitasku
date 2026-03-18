package com.aktivitasku.domain.model

import java.time.LocalDateTime

data class Activity(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime? = null,
    val category: ActivityCategory = ActivityCategory.OTHER,
    val priority: Priority = Priority.MEDIUM,
    val isCompleted: Boolean = false,
    val isAllDay: Boolean = false,
    val repeatType: RepeatType = RepeatType.NONE,
    val repeatDays: List<Int> = emptyList(), // 1=Mon..7=Sun
    val reminders: List<Int> = listOf(15),   // minutes before
    val colorHex: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class ActivityCategory(val label: String, val emoji: String) {
    WORK("Kerja", "💼"),
    PERSONAL("Pribadi", "🏠"),
    HEALTH("Kesehatan", "💪"),
    STUDY("Belajar", "📚"),
    OTHER("Lainnya", "📌")
}

enum class Priority(val label: String, val level: Int) {
    LOW("Rendah", 1),
    MEDIUM("Sedang", 2),
    HIGH("Tinggi", 3)
}

enum class RepeatType(val label: String) {
    NONE("Tidak berulang"),
    DAILY("Setiap hari"),
    WEEKLY("Setiap minggu"),
    MONTHLY("Setiap bulan"),
    CUSTOM("Kustom")
}

// Reminder options in minutes
object ReminderOptions {
    val OPTIONS = listOf(
        5   to "5 menit sebelum",
        10  to "10 menit sebelum",
        15  to "15 menit sebelum",
        30  to "30 menit sebelum",
        60  to "1 jam sebelum",
        120 to "2 jam sebelum",
        1440 to "1 hari sebelum"
    )
}
