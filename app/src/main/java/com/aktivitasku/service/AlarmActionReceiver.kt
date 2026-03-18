package com.aktivitasku.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aktivitasku.data.repository.ActivityRepository
import com.aktivitasku.service.AlarmScheduler.Companion.EXTRA_ACTIVITY_ID
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class AlarmActionReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: ActivityRepository
    @Inject lateinit var scheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val activityId = intent.getLongExtra(EXTRA_ACTIVITY_ID, -1)
        if (activityId == -1L) return

        // Dismiss the notification
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.cancel(activityId.toInt())

        when (intent.action) {
            "com.aktivitasku.ACTION_SNOOZE" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val activity = repository.getById(activityId) ?: return@launch
                    // Re-schedule alarm 5 minutes from now
                    val snoozed = activity.copy(
                        reminders = listOf(0),
                        startDateTime = LocalDateTime.now().plusMinutes(5)
                    )
                    scheduler.cancel(activityId)
                    scheduler.schedule(snoozed)
                }
            }
            "com.aktivitasku.ACTION_COMPLETE" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    repository.setCompleted(activityId, true)
                    scheduler.cancel(activityId)
                }
            }
        }
    }
}
