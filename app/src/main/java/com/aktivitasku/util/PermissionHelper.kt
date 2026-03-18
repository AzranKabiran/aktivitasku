package com.aktivitasku.util

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.aktivitasku.presentation.theme.Blue700
import com.aktivitasku.presentation.theme.Teal400

// ── Permission state holder ───────────────────────────────

data class AppPermissions(
    val hasNotification: Boolean    = false,
    val hasRecordAudio: Boolean     = false,
    val hasExactAlarm: Boolean      = false
)

fun Context.checkAppPermissions(): AppPermissions {
    val nm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    } else true

    val audio = checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    val alarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    } else true

    return AppPermissions(nm, audio, alarm)
}

// ── Permission request composable ────────────────────────

@Composable
fun PermissionRequestScreen(
    onAllGranted: () -> Unit
) {
    val context = LocalContext.current
    var permissions by remember { mutableStateOf(context.checkAppPermissions()) }

    // Notification permission launcher (Android 13+)
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissions = context.checkAppPermissions()
        if (permissions.hasNotification && permissions.hasRecordAudio && permissions.hasExactAlarm) {
            onAllGranted()
        }
    }

    // Audio permission launcher
    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        permissions = context.checkAppPermissions()
        if (permissions.hasNotification && permissions.hasRecordAudio && permissions.hasExactAlarm) {
            onAllGranted()
        }
    }

    LaunchedEffect(permissions) {
        if (permissions.hasNotification && permissions.hasRecordAudio && permissions.hasExactAlarm) {
            onAllGranted()
        }
    }

    // Request permissions in sequence
    if (!permissions.hasNotification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        PermissionCard(
            icon        = Icons.Rounded.NotificationsActive,
            title       = "Izin Notifikasi",
            description = "AktivitasKu perlu mengirim notifikasi untuk mengingatkan kegiatan kamu tepat waktu.",
            buttonLabel = "Izinkan Notifikasi",
            iconTint    = Blue700,
            onRequest   = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
        )
    } else if (!permissions.hasExactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PermissionCard(
            icon        = Icons.Rounded.Schedule,
            title       = "Izin Alarm Tepat Waktu",
            description = "Diperlukan agar alarm berbunyi di waktu yang tepat, bahkan saat layar mati.",
            buttonLabel = "Buka Pengaturan",
            iconTint    = Teal400,
            onRequest   = {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                )
            }
        )
    } else if (!permissions.hasRecordAudio) {
        PermissionCard(
            icon        = Icons.Rounded.Mic,
            title       = "Izin Mikrofon",
            description = "Dibutuhkan untuk fitur input suara. Kamu bisa melewati ini dan tetap menggunakan input teks.",
            buttonLabel = "Izinkan Mikrofon",
            buttonSecondaryLabel = "Lewati",
            iconTint    = Blue700,
            onRequest   = { audioLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            onSkip      = onAllGranted
        )
    }
}

@Composable
private fun PermissionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    buttonLabel: String,
    buttonSecondaryLabel: String? = null,
    iconTint: androidx.compose.ui.graphics.Color,
    onRequest: () -> Unit,
    onSkip: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = {},
        icon  = { Icon(icon, null, tint = iconTint) },
        title = { Text(title) },
        text  = { Text(description, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onRequest,
                colors  = ButtonDefaults.buttonColors(containerColor = iconTint)
            ) { Text(buttonLabel) }
        },
        dismissButton = buttonSecondaryLabel?.let {{
            TextButton(onClick = onSkip!!) { Text(buttonSecondaryLabel) }
        }}
    )
}
