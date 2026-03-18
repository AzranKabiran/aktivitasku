package com.aktivitasku.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.aktivitasku.MainActivity
import com.aktivitasku.R
import com.aktivitasku.presentation.settings.dataStore
import com.aktivitasku.service.AlarmScheduler.Companion.EXTRA_ACTIVITY_ID
import com.aktivitasku.service.AlarmScheduler.Companion.EXTRA_ACTIVITY_TITLE
import com.aktivitasku.service.AlarmScheduler.Companion.EXTRA_MINUTES_BEFORE
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalTime

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID   = "aktivitasku_alarm"
        const val CHANNEL_NAME = "Pengingat Kegiatan"
        private const val ACTION_SNOOZE   = "com.aktivitasku.ACTION_SNOOZE"
        private const val ACTION_COMPLETE = "com.aktivitasku.ACTION_COMPLETE"

        // DataStore keys (mirrors PrefKeys in SettingsViewModel)
        private val KEY_DND_ENABLED    = booleanPreferencesKey("dnd_enabled")
        private val KEY_DND_START_HOUR = intPreferencesKey("dnd_start_hour")
        private val KEY_DND_END_HOUR   = intPreferencesKey("dnd_end_hour")
        private val KEY_SOUND_ENABLED  = booleanPreferencesKey("sound_enabled")
        private val KEY_VIBRATE        = booleanPreferencesKey("vibrate_enabled")
    }

    override fun onReceive(context: Context, intent: Intent) {
        val activityId    = intent.getLongExtra(EXTRA_ACTIVITY_ID, -1)
        val activityTitle = intent.getStringExtra(EXTRA_ACTIVITY_TITLE) ?: "Kegiatan"
        val minutesBefore = intent.getIntExtra(EXTRA_MINUTES_BEFORE, 15)

        if (activityId == -1L) return

        createChannel(context)

        // Read user preferences synchronously (BroadcastReceiver has no coroutine scope)
        val prefs = runBlocking { context.dataStore.data.first() }
        val dndEnabled   = prefs[KEY_DND_ENABLED]    ?: false
        val dndStart     = prefs[KEY_DND_START_HOUR] ?: 22
        val dndEnd       = prefs[KEY_DND_END_HOUR]   ?: 7
        val soundEnabled = prefs[KEY_SOUND_ENABLED]  ?: true
        val vibrateOn    = prefs[KEY_VIBRATE]         ?: true

        // Skip notification if inside DND window
        if (dndEnabled && isInsideDndWindow(dndStart, dndEnd)) return

        showNotification(
            context       = context,
            activityId    = activityId,
            title         = activityTitle,
            minutesBefore = minutesBefore,
            soundEnabled  = soundEnabled,
            vibrateOn     = vibrateOn
        )
    }

    // ── DND window check ──────────────────────────────────
    // Supports overnight windows e.g. 22:00 – 07:00
    private fun isInsideDndWindow(startHour: Int, endHour: Int): Boolean {
        val now = LocalTime.now().hour
        return if (startHour <= endHour) {
            now in startHour until endHour
        } else {
            // Overnight: e.g. 22 until 7 → now >= 22 OR now < 7
            now >= startHour || now < endHour
        }
    }

    // ── Notification builder ──────────────────────────────

    private fun showNotification(
        context: Context,
        activityId: Long,
        title: String,
        minutesBefore: Int,
        soundEnabled: Boolean,
        vibrateOn: Boolean
    ) {
        val nm = context.getSystemService(NotificationManager::class.java)

        val contentText = when {
            minutesBefore == 0   -> "Kegiatan dimulai sekarang!"
            minutesBefore < 60   -> "$minutesBefore menit lagi"
            minutesBefore == 60  -> "1 jam lagi"
            minutesBefore == 120 -> "2 jam lagi"
            minutesBefore == 1440 -> "1 hari lagi"
            else                  -> "${minutesBefore / 60} jam lagi"
        }

        // Tap → open detail
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_ACTIVITY_ID, activityId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPi = PendingIntent.getActivity(
            context, activityId.toInt(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action
        val snoozePi   = buildActionPendingIntent(context, ACTION_SNOOZE,   activityId, 100)
        val completePi = buildActionPendingIntent(context, ACTION_COMPLETE,  activityId, 200)

        val alarmSound = if (soundEnabled)
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        else null

        val vibPattern = if (vibrateOn) longArrayOf(0, 500, 200, 500) else null

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$contentText · Ketuk untuk membuka detail kegiatan")
            )
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColorized(true)
            .setColor(0xFF1565C0.toInt())
            .addAction(0, "Tunda 5 menit", snoozePi)
            .addAction(0, "Selesai ✓",     completePi)

        if (alarmSound != null) builder.setSound(alarmSound)
        if (vibPattern != null) builder.setVibrate(vibPattern)

        nm.notify(activityId.toInt(), builder.build())
    }

    // ── Helpers ───────────────────────────────────────────

    private fun buildActionPendingIntent(
        context: Context,
        action: String,
        activityId: Long,
        requestOffset: Int
    ): PendingIntent {
        val intent = Intent(context, AlarmActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_ACTIVITY_ID, activityId)
        }
        return PendingIntent.getBroadcast(
            context,
            (activityId + requestOffset).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val audioAttr  = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
            description    = "Notifikasi pengingat kegiatan AktivitasKu"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500)
            setSound(alarmSound, audioAttr)
        }
        nm.createNotificationChannel(channel)
    }
}
