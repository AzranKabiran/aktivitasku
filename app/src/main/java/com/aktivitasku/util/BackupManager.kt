package com.aktivitasku.util

import android.content.Context
import android.net.Uri
import com.aktivitasku.data.repository.ActivityRepository
import com.aktivitasku.domain.model.Activity
import com.aktivitasku.domain.model.ActivityCategory
import com.aktivitasku.domain.model.Priority
import com.aktivitasku.domain.model.RepeatType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

sealed class BackupResult {
    data class Success(val count: Int) : BackupResult()
    data class Error(val message: String) : BackupResult()
}

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ActivityRepository
) {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    // ── Export ────────────────────────────────────────────

    suspend fun exportToUri(uri: Uri): BackupResult {
        return try {
            val activities = repository.observeAll().first()
            val json       = buildJson(activities)

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toString(2).toByteArray())
            } ?: return BackupResult.Error("Tidak bisa membuka file output")

            BackupResult.Success(activities.size)
        } catch (e: Exception) {
            BackupResult.Error("Ekspor gagal: ${e.localizedMessage}")
        }
    }

    // ── Import ────────────────────────────────────────────

    suspend fun importFromUri(uri: Uri): BackupResult {
        return try {
            val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            } ?: return BackupResult.Error("Tidak bisa membaca file")

            val activities = parseJson(text)
            activities.forEach { activity ->
                repository.save(activity.copy(id = 0)) // reset id for re-import
            }

            BackupResult.Success(activities.size)
        } catch (e: Exception) {
            BackupResult.Error("Import gagal: ${e.localizedMessage}")
        }
    }

    // ── JSON builders ─────────────────────────────────────

    private fun buildJson(activities: List<Activity>): JSONObject {
        val arr = JSONArray()
        activities.forEach { a ->
            arr.put(JSONObject().apply {
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
            })
        }
        return JSONObject().apply {
            put("version",   1)
            put("exportedAt", LocalDateTime.now().format(formatter))
            put("count",     activities.size)
            put("activities", arr)
        }
    }

    private fun parseJson(text: String): List<Activity> {
        val root = JSONObject(text)
        val arr  = root.getJSONArray("activities")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Activity(
                id            = 0,
                title         = o.getString("title"),
                description   = o.optString("description", ""),
                startDateTime = LocalDateTime.parse(o.getString("startDateTime"), formatter),
                endDateTime   = o.optString("endDateTime", "").takeIf { it.isNotBlank() }
                                 ?.let { LocalDateTime.parse(it, formatter) },
                category      = runCatching { ActivityCategory.valueOf(o.getString("category")) }
                                 .getOrDefault(ActivityCategory.OTHER),
                priority      = runCatching { Priority.valueOf(o.getString("priority")) }
                                 .getOrDefault(Priority.MEDIUM),
                isCompleted   = o.optBoolean("isCompleted", false),
                isAllDay      = o.optBoolean("isAllDay", false),
                repeatType    = runCatching { RepeatType.valueOf(o.getString("repeatType")) }
                                 .getOrDefault(RepeatType.NONE),
                repeatDays    = o.optString("repeatDays", "").split(",")
                                 .mapNotNull { it.trim().toIntOrNull() },
                reminders     = o.optString("reminders", "15").split(",")
                                 .mapNotNull { it.trim().toIntOrNull() },
                colorHex      = o.optString("colorHex", "").ifBlank { null },
                createdAt     = runCatching {
                                    LocalDateTime.parse(o.getString("createdAt"), formatter)
                                }.getOrDefault(LocalDateTime.now())
            )
        }
    }
}
