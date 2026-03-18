package com.aktivitasku.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aktivitasku.domain.model.Activity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    companion object {
        const val ACTION_ALARM    = "com.aktivitasku.ACTION_ALARM"
        const val EXTRA_ACTIVITY_ID    = "activity_id"
        const val EXTRA_ACTIVITY_TITLE = "activity_title"
        const val EXTRA_MINUTES_BEFORE = "minutes_before"
    }

    /**
     * Schedule all reminder alarms for an activity.
     * Each reminder gets a unique requestCode = activityId * 100 + reminderIndex
     */
    fun schedule(activity: Activity) {
        if (!canScheduleExactAlarms()) return

        activity.reminders.forEachIndexed { index, minutesBefore ->
            val triggerAt = activity.startDateTime
                .minusMinutes(minutesBefore.toLong())

            if (triggerAt.isAfter(LocalDateTime.now())) {
                val requestCode = (activity.id * 100 + index).toInt()
                scheduleExact(
                    triggerAt      = triggerAt,
                    requestCode    = requestCode,
                    activityId     = activity.id,
                    activityTitle  = activity.title,
                    minutesBefore  = minutesBefore
                )
            }
        }
    }

    /** Cancel all alarms for an activity (up to 10 reminders per activity) */
    fun cancel(activityId: Long) {
        for (i in 0..9) {
            val requestCode = (activityId * 100 + i).toInt()
            val intent = buildIntent(activityId, "", 0)
            val pi = buildPendingIntent(requestCode, intent)
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    /** Reschedule all activities — call this on boot */
    fun rescheduleAll(activities: List<Activity>) {
        activities
            .filter { !it.isCompleted && it.startDateTime.isAfter(LocalDateTime.now()) }
            .forEach { schedule(it) }
    }

    // ── Private Helpers ───────────────────────────────────

    private fun scheduleExact(
        triggerAt: LocalDateTime,
        requestCode: Int,
        activityId: Long,
        activityTitle: String,
        minutesBefore: Int
    ) {
        val triggerMs = triggerAt
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val intent = buildIntent(activityId, activityTitle, minutesBefore)
        val pi     = buildPendingIntent(requestCode, intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMs,
                pi
            )
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMs, pi)
        }
    }

    private fun buildIntent(
        activityId: Long,
        activityTitle: String,
        minutesBefore: Int
    ) = Intent(context, AlarmReceiver::class.java).apply {
        action = ACTION_ALARM
        putExtra(EXTRA_ACTIVITY_ID, activityId)
        putExtra(EXTRA_ACTIVITY_TITLE, activityTitle)
        putExtra(EXTRA_MINUTES_BEFORE, minutesBefore)
    }

    private fun buildPendingIntent(requestCode: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            alarmManager.canScheduleExactAlarms()
        else true
}
