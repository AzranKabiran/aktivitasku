package com.aktivitasku.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.aktivitasku.MainActivity
import com.aktivitasku.data.repository.ActivityRepository
import com.aktivitasku.domain.model.ActivityCategory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable
data class WidgetItem(
    val id: Long,
    val title: String,
    val time: String,
    val categoryColor: Int,
    val isCompleted: Boolean
)

private val KEY_ITEMS = stringPreferencesKey("widget_items")

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun activityRepository(): ActivityRepository
}

class AktivitasKuWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val json  = prefs[KEY_ITEMS] ?: "[]"
            val items = runCatching {
                Json.decodeFromString<List<WidgetItem>>(json)
            }.getOrDefault(emptyList())

            val dateLabel = LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEE, d MMM", Locale("id"))
            )

            WidgetContent(items = items, dateLabel = dateLabel, context = context)
        }
    }
}

@Composable
private fun WidgetContent(items: List<WidgetItem>, dateLabel: String, context: Context) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFFF5F9FF)))
            .padding(12.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {

            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text  = "AktivitasKu",
                    style = TextStyle(
                        color      = ColorProvider(Color(0xFF1565C0)),
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text  = dateLabel,
                    style = TextStyle(
                        color    = ColorProvider(Color(0xFF8B8FA8)),
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(GlanceModifier.height(6.dp))

            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ColorProvider(Color(0xFFDFECFF)))
            )

            Spacer(GlanceModifier.height(8.dp))

            if (items.isEmpty()) {
                Text(
                    text  = "Tidak ada kegiatan hari ini",
                    style = TextStyle(
                        color    = ColorProvider(Color(0xFF8B8FA8)),
                        fontSize = 12.sp
                    )
                )
            } else {
                items.take(3).forEach { item ->
                    WidgetItemRow(item)
                    Spacer(GlanceModifier.height(6.dp))
                }
            }

            Spacer(GlanceModifier.defaultWeight())

            val done  = items.count { it.isCompleted }
            val total = items.size
            if (total > 0) {
                Text(
                    text  = "$done/$total selesai",
                    style = TextStyle(
                        color      = ColorProvider(Color(0xFF00C9A7)),
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun WidgetItemRow(item: WidgetItem) {
    val catColor  = Color(item.categoryColor)
    val textColor = if (item.isCompleted) Color(0xFF8B8FA8) else Color(0xFF1A1C2A)

    Row(
        modifier          = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .width(3.dp)
                .height(32.dp)
                .background(ColorProvider(catColor))
        )
        Spacer(GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text     = item.title,
                style    = TextStyle(
                    color      = ColorProvider(textColor),
                    fontSize   = 12.sp,
                    fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.Medium
                ),
                maxLines = 1
            )
            Text(
                text  = item.time,
                style = TextStyle(
                    color    = ColorProvider(Color(0xFF8B8FA8)),
                    fontSize = 10.sp
                )
            )
        }
        if (item.isCompleted) {
            Text(
                "✓",
                style = TextStyle(
                    color      = ColorProvider(Color(0xFF00C9A7)),
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

class AktivitasKuWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = AktivitasKuWidget()
}

// Called from WidgetRefreshWorker
suspend fun refreshWidgetData(context: Context) {
    val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        WidgetEntryPoint::class.java
    )
    val repo  = entryPoint.activityRepository()
    val today = LocalDate.now()

    val activities = repo.observeByDateRange(
        start = today.atStartOfDay(),
        end   = today.plusDays(1).atStartOfDay()
    ).first()

    val items = activities.map { a ->
        WidgetItem(
            id            = a.id,
            title         = a.title,
            time          = a.startDateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
            categoryColor = a.category.widgetColor(),
            isCompleted   = a.isCompleted
        )
    }

    val jsonStr = Json.encodeToString(items)

    GlanceAppWidgetManager(context)
        .getGlanceIds(AktivitasKuWidget::class.java)
        .forEach { id ->
            updateAppWidgetState<Preferences>(context, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs.toMutablePreferences().apply { set(KEY_ITEMS, jsonStr) }
            }
            AktivitasKuWidget().update(context, id)
        }
}

private fun ActivityCategory.widgetColor(): Int = when (this) {
    ActivityCategory.WORK     -> 0xFF1565C0.toInt()
    ActivityCategory.PERSONAL -> 0xFF00C9A7.toInt()
    ActivityCategory.HEALTH   -> 0xFFE91E63.toInt()
    ActivityCategory.STUDY    -> 0xFF9C27B0.toInt()
    ActivityCategory.OTHER    -> 0xFFFFA726.toInt()
}
