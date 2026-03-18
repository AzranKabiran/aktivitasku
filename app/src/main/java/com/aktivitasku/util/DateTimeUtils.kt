package com.aktivitasku.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val locale = Locale("id")

fun LocalDateTime.toDisplayDate(): String =
    format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale))

fun LocalDateTime.toDisplayTime(): String =
    format(DateTimeFormatter.ofPattern("HH:mm"))

fun LocalDateTime.toDisplayFull(): String =
    format(DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", locale))

fun LocalDate.toDisplayShort(): String =
    format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))

fun LocalDate.isToday()    = this == LocalDate.now()
fun LocalDate.isTomorrow() = this == LocalDate.now().plusDays(1)

fun LocalDateTime.toRelativeLabel(): String {
    val date = toLocalDate()
    val time = toDisplayTime()
    return when {
        date.isToday()    -> "Hari ini · $time"
        date.isTomorrow() -> "Besok · $time"
        else              -> "${date.toDisplayShort()} · $time"
    }
}

fun Int.toReminderLabel(): String = when {
    this == 0    -> "Saat kegiatan mulai"
    this < 60    -> "$this menit sebelum"
    this == 60   -> "1 jam sebelum"
    this % 60 == 0 -> "${this / 60} jam sebelum"
    else         -> "$this menit sebelum"
}
