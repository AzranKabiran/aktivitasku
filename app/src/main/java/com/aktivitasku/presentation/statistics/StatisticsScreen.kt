package com.aktivitasku.presentation.statistics

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktivitasku.domain.model.ActivityCategory
import com.aktivitasku.presentation.components.color
import com.aktivitasku.presentation.theme.*
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistik", style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Blue700)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Streak cards ────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StreakCard(
                    value    = uiState.currentStreak,
                    label    = "Streak Sekarang",
                    icon     = Icons.Rounded.Whatshot,
                    color    = Error,
                    modifier = Modifier.weight(1f)
                )
                StreakCard(
                    value    = uiState.longestStreak,
                    label    = "Streak Terpanjang",
                    icon     = Icons.Rounded.EmojiEvents,
                    color    = Warning,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Summary row ──────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(
                    value    = uiState.totalCompleted.toString(),
                    label    = "Selesai",
                    color    = Teal400,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    value    = uiState.totalPending.toString(),
                    label    = "Tertunda",
                    color    = Warning,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    value    = "${(uiState.completionRate * 100).toInt()}%",
                    label    = "Tingkat Selesai",
                    color    = Blue700,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Weekly bar chart ─────────────────────────
            Card(
                shape  = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "7 Hari Terakhir",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(16.dp))
                    WeeklyBarChart(stats = uiState.weeklyStats)
                }
            }

            // ── Category breakdown ───────────────────────
            if (uiState.categoryBreakdown.isNotEmpty()) {
                Card(
                    shape  = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Berdasarkan Kategori",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(12.dp))
                        uiState.categoryBreakdown.forEach { stat ->
                            CategoryRow(stat = stat)
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Sub-components ────────────────────────────────────────

@Composable
private fun StreakCard(
    value: Int,
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                text  = value.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SummaryCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier  = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text  = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WeeklyBarChart(stats: List<DayStats>) {
    val maxTotal = stats.maxOfOrNull { it.total }?.coerceAtLeast(1) ?: 1
    val dayAbbr  = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.Bottom
    ) {
        stats.forEach { day ->
            val totalFrac     = day.total.toFloat() / maxTotal
            val completedFrac = if (day.total == 0) 0f else day.completed.toFloat() / day.total
            val dayIdx        = day.date.dayOfWeek.value - 1

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier            = Modifier.weight(1f)
            ) {
                // Bar
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    // Background bar (total)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(totalFrac.coerceAtLeast(0.04f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(Blue50)
                            .align(Alignment.BottomCenter)
                    )
                    // Completed portion
                    if (day.completed > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight((totalFrac * completedFrac).coerceAtLeast(0.04f))
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    Brush.verticalGradient(listOf(Teal400, Blue700))
                                )
                                .align(Alignment.BottomCenter)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = dayAbbr[dayIdx],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (day.completed > 0) {
                    Text(
                        text  = "${day.completed}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Teal400
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(stat: CategoryStats) {
    val color = stat.category.color()
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${stat.category.emoji} ${stat.category.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                "${stat.count} · ${(stat.percentage * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress   = { stat.percentage },
            modifier   = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color      = color,
            trackColor = color.copy(alpha = 0.12f)
        )
    }
}
