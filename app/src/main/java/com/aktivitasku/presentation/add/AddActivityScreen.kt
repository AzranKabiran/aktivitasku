package com.aktivitasku.presentation.add

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktivitasku.domain.model.*
import com.aktivitasku.presentation.components.CategoryChip
import com.aktivitasku.presentation.components.color
import com.aktivitasku.presentation.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivityScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate back after save
    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.id == null) "Kegiatan Baru" else "Edit Kegiatan",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBackIosNew, "Kembali")
                    }
                },
                actions = {
                    TextButton(
                        onClick  = viewModel::save,
                        enabled  = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Simpan", color = Blue700)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Voice Input Section ───────────────────────
            VoiceInputSection(
                voiceState    = uiState.voiceState,
                transcript    = uiState.voiceTranscript,
                onStartVoice  = viewModel::startVoiceInput,
                onStopVoice   = viewModel::stopVoiceInput,
                onResetVoice  = viewModel::resetVoiceState
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ── Title ─────────────────────────────────────
            OutlinedTextField(
                value         = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label         = { Text("Judul kegiatan *") },
                isError       = uiState.titleError != null,
                supportingText = uiState.titleError?.let { { Text(it) } },
                leadingIcon   = { Icon(Icons.Rounded.Title, null) },
                shape         = RoundedCornerShape(14.dp),
                colors        = inputColors(),
                modifier      = Modifier.fillMaxWidth()
            )

            // ── Description ───────────────────────────────
            OutlinedTextField(
                value         = uiState.description,
                onValueChange = viewModel::onDescChange,
                label         = { Text("Deskripsi (opsional)") },
                leadingIcon   = { Icon(Icons.Rounded.Notes, null) },
                shape         = RoundedCornerShape(14.dp),
                colors        = inputColors(),
                minLines      = 2,
                modifier      = Modifier.fillMaxWidth()
            )

            // ── Date & Time ───────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DatePickerField(
                    date     = uiState.date,
                    onSelect = viewModel::onDateChange,
                    modifier = Modifier.weight(1f)
                )
                TimePickerField(
                    time     = uiState.time,
                    onSelect = viewModel::onTimeChange,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Category ──────────────────────────────────
            FormSection(title = "Kategori") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActivityCategory.values().forEach { cat ->
                        val selected = uiState.category == cat
                        FilterChip(
                            selected = selected,
                            onClick  = { viewModel.onCategoryChange(cat) },
                            label    = { Text("${cat.emoji} ${cat.label}") },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cat.color().copy(alpha = 0.15f),
                                selectedLabelColor     = cat.color()
                            )
                        )
                    }
                }
            }

            // ── Priority ──────────────────────────────────
            FormSection(title = "Prioritas") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.values().forEach { prio ->
                        val selected = uiState.priority == prio
                        FilterChip(
                            selected = selected,
                            onClick  = { viewModel.onPriorityChange(prio) },
                            label    = { Text(prio.label) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(prio.color())
                                )
                            }
                        )
                    }
                }
            }

            // ── Reminders ─────────────────────────────────
            FormSection(title = "Ingatkan Saya") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReminderOptions.OPTIONS.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (minutes, label) ->
                                val selected = minutes in uiState.selectedReminders
                                FilterChip(
                                    selected = selected,
                                    onClick  = { viewModel.toggleReminder(minutes) },
                                    label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = if (selected) ({
                                        Icon(Icons.Rounded.Check, null, Modifier.size(14.dp))
                                    }) else null,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // ── Repeat ────────────────────────────────────
            FormSection(title = "Pengulangan") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RepeatType.values().forEach { type ->
                        FilterChip(
                            selected = uiState.repeatType == type,
                            onClick  = { viewModel.onRepeatTypeChange(type) },
                            label    = { Text(type.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                AnimatedVisibility(visible = uiState.repeatType == RepeatType.CUSTOM) {
                    val dayLabels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        dayLabels.forEachIndexed { idx, label ->
                            val day      = idx + 1
                            val selected = day in uiState.repeatDays
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) Blue700 else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { viewModel.toggleRepeatDay(day) }
                            ) {
                                Text(
                                    text  = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Voice Input Section ───────────────────────────────────

@Composable
private fun VoiceInputSection(
    voiceState: VoiceState,
    transcript: String,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onResetVoice: () -> Unit
) {
    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Input Suara",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Ucapkan kegiatan secara natural, contoh:\n\"Besok jam 9 pagi meeting dengan klien\"",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            // Mic button with animated state
            val btnScale by animateFloatAsState(
                targetValue = if (voiceState == VoiceState.LISTENING) 1.1f else 1f,
                label       = "micScale"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                // Pulse rings when listening
                if (voiceState == VoiceState.LISTENING) {
                    PulseRing(delay = 0)
                    PulseRing(delay = 400)
                }

                FilledIconButton(
                    onClick  = when (voiceState) {
                        VoiceState.IDLE,
                        VoiceState.SUCCESS,
                        VoiceState.ERROR -> onStartVoice
                        VoiceState.LISTENING   -> onStopVoice
                        VoiceState.PROCESSING  -> ({})
                    },
                    shape    = CircleShape,
                    colors   = IconButtonDefaults.filledIconButtonColors(
                        containerColor = when (voiceState) {
                            VoiceState.LISTENING  -> Error
                            VoiceState.SUCCESS    -> Teal400
                            VoiceState.PROCESSING -> Blue500
                            else                   -> Blue700
                        }
                    ),
                    modifier = Modifier
                        .size(64.dp)
                        .scale(btnScale)
                ) {
                    Icon(
                        imageVector = when (voiceState) {
                            VoiceState.LISTENING  -> Icons.Rounded.Stop
                            VoiceState.SUCCESS    -> Icons.Rounded.Check
                            VoiceState.PROCESSING -> Icons.Rounded.HourglassTop
                            VoiceState.ERROR      -> Icons.Rounded.Refresh
                            else                   -> Icons.Rounded.Mic
                        },
                        contentDescription = "Mic",
                        tint     = White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            AnimatedContent(targetState = voiceState, label = "voiceLabel") { state ->
                Text(
                    text  = when (state) {
                        VoiceState.IDLE       -> "Ketuk untuk mulai"
                        VoiceState.LISTENING  -> "Mendengarkan..."
                        VoiceState.PROCESSING -> "Memproses..."
                        VoiceState.SUCCESS    -> "Berhasil dikenali!"
                        VoiceState.ERROR      -> "Coba lagi"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = when (state) {
                        VoiceState.SUCCESS -> Teal400
                        VoiceState.ERROR   -> Error
                        VoiceState.LISTENING -> Blue500
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            if (transcript.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Card(
                    shape  = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.FormatQuote,
                            null,
                            tint     = Teal400,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text  = transcript,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PulseRing(delay: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_$delay")
    val scale by infiniteTransition.animateFloat(
        initialValue   = 1f,
        targetValue    = 1.6f,
        animationSpec  = infiniteRepeatable(
            animation  = androidx.compose.animation.core.tween(800, delayMillis = delay),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "pulseScale_$delay"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue   = 0.6f,
        targetValue    = 0f,
        animationSpec  = infiniteRepeatable(
            animation  = androidx.compose.animation.core.tween(800, delayMillis = delay),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "pulseAlpha_$delay"
    )
    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Error.copy(alpha = alpha))
    )
}

// ── Small helpers ─────────────────────────────────────────

@Composable
private fun FormSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text  = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(date: LocalDate, onSelect: (LocalDate) -> Unit, modifier: Modifier) {
    val fmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id"))
    var showPicker by remember { mutableStateOf(false) }

    // Prepare state with current date pre-selected
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = date
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    )

    OutlinedTextField(
        value         = date.format(fmt),
        onValueChange = {},
        readOnly      = true,
        label         = { Text("Tanggal") },
        leadingIcon   = { Icon(Icons.Rounded.CalendarToday, null) },
        shape         = RoundedCornerShape(14.dp),
        colors        = inputColors(),
        modifier      = modifier.clickable { showPicker = true }
    )

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton    = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val picked = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        onSelect(picked)
                    }
                    showPicker = false
                }) { Text("OK", color = Blue700) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Batal") }
            },
            colors = DatePickerDefaults.colors(
                containerColor           = MaterialTheme.colorScheme.surface,
                selectedDayContainerColor = Blue700,
                selectedDayContentColor  = White,
                todayDateBorderColor     = Blue700,
                todayContentColor        = Blue700
            )
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerField(time: LocalTime, onSelect: (LocalTime) -> Unit, modifier: Modifier) {
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    var showPicker by remember { mutableStateOf(false) }

    val timeState = rememberTimePickerState(
        initialHour   = time.hour,
        initialMinute = time.minute,
        is24Hour      = true
    )

    OutlinedTextField(
        value         = time.format(fmt),
        onValueChange = {},
        readOnly      = true,
        label         = { Text("Waktu") },
        leadingIcon   = { Icon(Icons.Rounded.Schedule, null) },
        shape         = RoundedCornerShape(14.dp),
        colors        = inputColors(),
        modifier      = modifier.clickable { showPicker = true }
    )

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Pilih Waktu", style = MaterialTheme.typography.titleMedium) },
            text  = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier.fillMaxWidth()
                ) {
                    TimePicker(
                        state  = timeState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor             = Blue50,
                            clockDialSelectedContentColor = White,
                            selectorColor              = Blue700,
                            periodSelectorSelectedContainerColor = Blue700,
                            periodSelectorSelectedContentColor   = White
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSelect(LocalTime.of(timeState.hour, timeState.minute))
                    showPicker = false
                }) { Text("OK", color = Blue700) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun inputColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor    = Blue700,
    unfocusedBorderColor  = MaterialTheme.colorScheme.outlineVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface
)


