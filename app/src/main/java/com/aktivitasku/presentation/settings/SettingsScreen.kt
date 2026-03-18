package com.aktivitasku.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktivitasku.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Export file picker
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportBackup(it) }
    }

    // Import file picker
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importBackup(it) }
    }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Pengaturan", style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost    = { SnackbarHost(snackbarHostState) },
        containerColor  = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Notifikasi ────────────────────────────────
            SettingsSection(title = "Notifikasi") {
                SettingsToggle(
                    icon    = Icons.Rounded.Vibration,
                    title   = "Getar saat alarm",
                    checked = uiState.vibrateEnabled,
                    onToggle = viewModel::toggleVibrate
                )
                SettingsToggle(
                    icon    = Icons.Rounded.VolumeUp,
                    title   = "Suara alarm",
                    checked = uiState.soundEnabled,
                    onToggle = viewModel::toggleSound
                )
                SettingsToggle(
                    icon    = Icons.Rounded.DoNotDisturb,
                    title   = "Mode Jangan Ganggu",
                    subtitle = "Tidak ada alarm dalam rentang waktu tertentu",
                    checked  = uiState.dndEnabled,
                    onToggle = viewModel::toggleDnd
                )
                AnimatedVisibility(visible = uiState.dndEnabled) {
                    DndTimeRow(
                        startHour = uiState.dndStartHour,
                        endHour   = uiState.dndEndHour,
                        onStartChange = viewModel::setDndStart,
                        onEndChange   = viewModel::setDndEnd
                    )
                }
            }

            // ── Tampilan ──────────────────────────────────
            SettingsSection(title = "Tampilan") {
                SettingsToggle(
                    icon    = Icons.Rounded.DarkMode,
                    title   = "Mode Gelap",
                    checked = uiState.darkModeEnabled,
                    onToggle = viewModel::toggleDarkMode
                )
            }

            // ── Backup & Restore ──────────────────────────
            SettingsSection(title = "Backup & Restore") {
                SettingsItem(
                    icon     = Icons.Rounded.Upload,
                    title    = "Ekspor Data",
                    subtitle = "Simpan semua kegiatan sebagai file JSON",
                    iconTint = Teal400,
                    isLoading = uiState.isExporting,
                    onClick  = {
                        val timestamp = java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                        exportLauncher.launch("aktivitasku_backup_$timestamp.json")
                    }
                )
                SettingsItem(
                    icon     = Icons.Rounded.Download,
                    title    = "Import Data",
                    subtitle = "Muat kegiatan dari file backup JSON",
                    iconTint = Blue700,
                    isLoading = uiState.isImporting,
                    onClick  = { importLauncher.launch(arrayOf("application/json")) }
                )
            }

            // ── Data ──────────────────────────────────────
            SettingsSection(title = "Data") {
                var showClearDialog by remember { mutableStateOf(false) }
                SettingsItem(
                    icon     = Icons.Rounded.DeleteSweep,
                    title    = "Hapus Semua Kegiatan",
                    subtitle = "Aksi ini tidak dapat dibatalkan",
                    iconTint = Error,
                    onClick  = { showClearDialog = true }
                )
                if (showClearDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearDialog = false },
                        icon    = { Icon(Icons.Rounded.Warning, null, tint = Error) },
                        title   = { Text("Hapus Semua?") },
                        text    = { Text("Semua kegiatan akan dihapus permanen. Backup terlebih dahulu jika perlu.") },
                        confirmButton = {
                            Button(
                                onClick = { viewModel.clearAll(); showClearDialog = false },
                                colors  = ButtonDefaults.buttonColors(containerColor = Error)
                            ) { Text("Hapus Semua") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearDialog = false }) { Text("Batal") }
                        }
                    )
                }
            }

            // ── Tentang ───────────────────────────────────
            SettingsSection(title = "Tentang") {
                SettingsInfoRow(
                    icon  = Icons.Rounded.Info,
                    title = "Versi Aplikasi",
                    value = "1.0.0"
                )
                SettingsInfoRow(
                    icon  = Icons.Rounded.Storage,
                    title = "Total Kegiatan",
                    value = "${uiState.totalActivities} kegiatan"
                )
                SettingsItem(
                    icon    = Icons.Rounded.Star,
                    title   = "Beri Bintang",
                    subtitle = "Dukung pengembangan AktivitasKu",
                    iconTint = Warning,
                    onClick  = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=${context.packageName}"))
                                .also { it.flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                        )
                    }
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ── Section wrapper ───────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text     = title.uppercase(),
            style    = MaterialTheme.typography.labelSmall,
            color    = Blue700,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp, top = 8.dp)
        )
        Card(
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column { content() }
        }
    }
}

// ── Toggle row ────────────────────────────────────────────

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIcon(icon = icon, tint = if (checked) Blue700 else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked  = checked,
            onCheckedChange = onToggle,
            colors   = SwitchDefaults.colors(
                checkedThumbColor   = White,
                checkedTrackColor   = Blue700,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
    HorizontalDivider(
        color    = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = 56.dp)
    )
}

// ── Clickable item row ────────────────────────────────────

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIcon(icon = icon, tint = iconTint)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color       = Blue700
            )
        } else {
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint     = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
    HorizontalDivider(
        color    = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = 56.dp)
    )
}

// ── Info row (non-clickable) ──────────────────────────────

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIcon(icon = icon, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider(
        color    = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = 56.dp)
    )
}

// ── DnD time selector ────────────────────────────────────

@Composable
private fun DndTimeRow(
    startHour: Int,
    endHour: Int,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp, end = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(
            "Mulai" to startHour to onStartChange,
            "Selesai" to endHour to onEndChange
        ).forEach { (pair, action) ->
            val (label, hour) = pair
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value         = "%02d:00".format(hour),
                    onValueChange = {},
                    readOnly      = true,
                    shape         = RoundedCornerShape(10.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Blue700,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ── Icon helper ───────────────────────────────────────────

@Composable
private fun SettingsIcon(icon: ImageVector, tint: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.10f))
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
    }
}
