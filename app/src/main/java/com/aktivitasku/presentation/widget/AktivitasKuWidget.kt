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
import androidx.glance.appwidget.*
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
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

// ── Widget data model (JSON-serializable) ─────────────────

@Serializable
data class WidgetItem(
    val id: Long,
    val title: String,
    val time: String,
    val colorInt: Long,       // ARGB as Long (avoids Int overflow issues)
    val isCompleted: Boolean
)

private val KEY_ITEMS = stringPreferencesKey("widget_items_v2")

// ── Hilt entry point ──────────────────────────────────────

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun activityRepository(): ActivityRepository
}

// ── Widget ────────────────────────────────────────────────

class AktivitasKuWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> =
        PreferencesGlanceStateDefinition

    @Composable
    override fun Content() {
        val prefs = currentState<Preferences>()
        val items = runCatching {
            Json.decodeFromString<List<WidgetItem>>(prefs[KEY_ITEMS] ?: "[]")
        }.getOrDefault(emptyList())

        val dateLabel = LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, d MMM", Locale("id"))
        )

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFFF5F9FF)))
                .appWidgetBackground()
                .cornerRadius(20.dp)
                .padding(14.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {

                // Header
                Row(
                    modifier              = GlanceModifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "AktivitasKu",
                        style = TextStyle(
                            color      = ColorProvider(Color(0xFF1565C0)),
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Text(
                        dateLabel,
                        style = TextStyle(
                            color    = ColorProvider(Color(0xFF8B8FA8)),
                            fontSize = 10.sp
                        )
                    )
                }

                Spacer(GlanceModifier.height(6.dp))
                Box(
                    GlanceModifier.fillMaxWidth().height(1.dp)
                        .background(ColorProvider(Color(0xFFDFECFF)))
                )
                Spacer(GlanceModifier.height(8.dp))

                // Items
                if (items.isEmpty()) {
                    Text(
                        "Tidak ada kegiatan hari ini.\nKetuk untuk menambah.",
                        style = TextStyle(
                            color    = ColorProvider(Color(0xFF8B8FA8)),
                            fontSize = 12.sp
                        )
                    )
                } else {
                    LazyColumn {
                        items(items.take(5)) { item ->
                            WidgetRow(item)
                            Spacer(GlanceModifier.height(6.dp))
                        }
                    }
                }

                Spacer(GlanceModifier.defaultWeight())

                // Footer progress
                if (items.isNotEmpty()) {
                    val done  = items.count { it.isCompleted }
                    val total = items.size
                    Text(
                        "$done/$total selesai",
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
}

@Composable
private fun WidgetRow(item: WidgetItem) {
    val catColor  = Color(item.colorInt)
    val textColor = if (item.isCompleted) Color(0xFF8B8FA8) else Color(0xFF1A1C2A)

    Row(
        modifier          = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            GlanceModifier.width(3.dp).height(32.dp)
                .background(ColorProvider(catColor))
                .cornerRadius(2.dp)
        )
        Spacer(GlanceModifier.width(8.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(
                item.title,
                style    = TextStyle(
                    color      = ColorProvider(textColor),
                    fontSize   = 12.sp,
                    fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.Medium
                ),
                maxLines = 1
            )
            Text(
                item.time,
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

// ── Receiver ──────────────────────────────────────────────

class AktivitasKuWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = AktivitasKuWidget()

    override suspend fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        refreshWidgetData(context)
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }
}

// ── Data refresh ──────────────────────────────────────────

suspend fun refreshWidgetData(context: Context) {
    val repo = EntryPointAccessors
        .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        .activityRepository()

    val today = LocalDate.now()
    val activities = repo.observeByDateRange(
        start = today.atStartOfDay(),
        end   = today.plusDays(1).atStartOfDay()
    ).first()

    val items = activities.map { a ->
        WidgetItem(
            id          = a.id,
            title       = a.title,
            time        = a.startDateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
            colorInt    = a.category.widgetColorLong(),
            isCompleted = a.isCompleted
        )
    }

    val json = Json.encodeToString(items)

    GlanceAppWidgetManager(context)
        .getGlanceIds(AktivitasKuWidget::class.java)
        .forEach { id ->
            updateAppWidgetState<Preferences>(
                context, PreferencesGlanceStateDefinition, id
            ) { it.toMutablePreferences().apply { set(KEY_ITEMS, json) } }
            AktivitasKuWidget().update(context, id)
        }
}

private fun ActivityCategory.widgetColorLong(): Long = when (this) {
    ActivityCategory.WORK     -> 0xFF1565C0L
    ActivityCategory.PERSONAL -> 0xFF00C9A7L
    ActivityCategory.HEALTH   -> 0xFFE91E63L
    ActivityCategory.STUDY    -> 0xFF9C27B0L
    ActivityCategory.OTHER    -> 0xFFFFA726L
}
