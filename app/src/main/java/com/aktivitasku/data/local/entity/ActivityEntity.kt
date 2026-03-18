package com.aktivitasku.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aktivitasku.domain.model.Activity
import com.aktivitasku.domain.model.ActivityCategory
import com.aktivitasku.domain.model.Priority
import com.aktivitasku.domain.model.RepeatType
import java.time.LocalDateTime

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime?,
    val category: String,          // ActivityCategory.name
    val priority: String,          // Priority.name
    val isCompleted: Boolean,
    val isAllDay: Boolean,
    val repeatType: String,        // RepeatType.name
    val repeatDays: String,        // CSV: "1,3,5"
    val reminders: String,         // CSV: "15,30"
    val colorHex: String?,
    val createdAt: LocalDateTime
) {
    fun toDomain() = Activity(
        id            = id,
        title         = title,
        description   = description,
        startDateTime = startDateTime,
        endDateTime   = endDateTime,
        category      = ActivityCategory.valueOf(category),
        priority      = Priority.valueOf(priority),
        isCompleted   = isCompleted,
        isAllDay      = isAllDay,
        repeatType    = RepeatType.valueOf(repeatType),
        repeatDays    = if (repeatDays.isBlank()) emptyList()
                        else repeatDays.split(",").map { it.toInt() },
        reminders     = if (reminders.isBlank()) emptyList()
                        else reminders.split(",").map { it.toInt() },
        colorHex      = colorHex,
        createdAt     = createdAt
    )
}

fun Activity.toEntity() = ActivityEntity(
    id            = id,
    title         = title,
    description   = description,
    startDateTime = startDateTime,
    endDateTime   = endDateTime,
    category      = category.name,
    priority      = priority.name,
    isCompleted   = isCompleted,
    isAllDay      = isAllDay,
    repeatType    = repeatType.name,
    repeatDays    = repeatDays.joinToString(","),
    reminders     = reminders.joinToString(","),
    colorHex      = colorHex,
    createdAt     = createdAt
)
