package com.aktivitasku.presentation.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktivitasku.domain.model.Activity
import com.aktivitasku.domain.model.Priority
import com.aktivitasku.domain.model.ReminderOptions
import com.aktivitasku.domain.model.RepeatType
import com.aktivitasku.presentation.components.color
import com.aktivitasku.presentation.theme.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    activityId: Long,
    onNavigateBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(activityId) { viewModel.load(activityId) }
    LaunchedEffect(uiState.deleted) { if (uiState.deleted) onNavigateBack() }

    val activity = uiState.activity ?: return

    var showDeleteDialog by remember { mutableStateOf(false) }
    val categoryColor = activity.category.color()
    val completedColor by animateColorAsState(
        if (activity.isCompleted) Teal400 else Blue700, label = "completedColor"
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon    = { Icon(Icons.Rounded.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title   = { Text("Hapus Kegiatan?") },
            text    = { Text("\"${activity.title}\" akan dihapus permanen dan tidak bisa dikembalikan.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.delete(); showDeleteDialog = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBackIosNew, "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, "Edit", tint = Blue700)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Rounded.DeleteOutline, "Hapus", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick  = { showDeleteDialog = true },
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border   = ButtonDefaults.outlinedButtonBorder.copy(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Hapus")
                    }
                    Button(
                        onClick  = viewModel::toggleComplete,
                        colors   = ButtonDefaults.buttonColors(containerColor = completedColor),
                        modifier = Modifier.weight(2f)
                    ) {
                        Icon(
                            if (activity.isCompleted) Icons.Rounded.Replay else Icons.Rounded.CheckCircle,
                            null, Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (activity.isCompleted) "Tandai Belum Selesai" else "Tandai Selesai")
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            // ── Hero header with gradient ─────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                categoryColor.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column {
                    // Status badge
                    if (activity.isCompleted) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Teal400.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null,
                                tint = Teal400, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Selesai", style = MaterialTheme.typography.labelSmall, color = Teal400)
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Text(
                        text  = activity.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (activity.description.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text  = activity.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Info cards ───────────────────────────
                DetailInfoRow(
                    icon  = Icons.Rounded.CalendarToday,
                    label = "Tanggal & Waktu",
                    value = activity.startDateTime.format(
                        DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy · HH:mm", Locale("id"))
                    )
                )

                DetailInfoRow(
                    icon  = Icons.Rounded.Category,
                    label = "Kategori",
                    value = "${activity.category.emoji} ${activity.category.label}",
                    valueColor = categoryColor
                )

                DetailInfoRow(
                    icon  = Icons.Rounded.Flag,
                    label = "Prioritas",
                    value = activity.priority.label,
                    valueColor = activity.priority.color()
                )

                if (activity.repeatType != RepeatType.NONE) {
                    DetailInfoRow(
                        icon  = Icons.Rounded.Repeat,
                        label = "Pengulangan",
                        value = activity.repeatType.label
                    )
                }

                // ── Reminders ────────────────────────────
                if (activity.reminders.isNotEmpty()) {
                    Card(
                        shape  = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.NotificationsActive,
                                    null,
                                    tint     = Blue700,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Pengingat",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            activity.reminders.forEach { minutes ->
                                val label = ReminderOptions.OPTIONS
                                    .firstOrNull { it.first == minutes }?.second
                                    ?: "$minutes menit sebelum"
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Teal400)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Created at
                Text(
                    text  = "Dibuat pada ${activity.createdAt.format(
                        DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale("id"))
                    )}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DetailInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Blue50)
            ) {
                Icon(icon, null, tint = Blue700, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = valueColor
                )
            }
        }
    }
}
