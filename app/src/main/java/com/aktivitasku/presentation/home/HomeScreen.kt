package com.aktivitasku.presentation.home

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktivitasku.domain.model.Activity
import com.aktivitasku.presentation.components.ActivityCard
import com.aktivitasku.presentation.components.SectionHeader
import com.aktivitasku.presentation.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddActivity: () -> Unit,
    onActivityClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()
    val isScrolled by remember { derivedStateOf { scrollState.firstVisibleItemScrollOffset > 0 } }

    Scaffold(
        topBar = {
            HomeTopBar(
                searchQuery    = uiState.searchQuery,
                onQueryChange  = viewModel::onSearchQueryChange,
                onClearSearch  = viewModel::clearSearch,
                isScrolled     = isScrolled
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick           = onAddActivity,
                expanded          = !isScrolled,
                icon              = { Icon(Icons.Rounded.Add, "Tambah kegiatan") },
                text              = { Text("Tambah Kegiatan") },
                containerColor    = Blue700,
                contentColor      = White
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        if (uiState.isSearching) {
            SearchResultsContent(
                results  = uiState.searchResults,
                query    = uiState.searchQuery,
                onClick  = onActivityClick,
                onComplete = viewModel::toggleCompleted,
                onDelete   = viewModel::deleteActivity,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                state           = scrollState,
                contentPadding  = PaddingValues(
                    top    = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 80.dp,
                    start  = 16.dp,
                    end    = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Stats header
                item {
                    StatsHeader(
                        completed = uiState.completedCount,
                        pending   = uiState.pendingCount
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // Date strip
                item {
                    WeekDateStrip(
                        selected = uiState.selectedDate,
                        onSelect = viewModel::selectDate
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // Today's activities
                item {
                    SectionHeader(
                        title  = "Hari Ini",
                        action = {
                            Text(
                                text  = "${uiState.todayActivities.size} kegiatan",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (uiState.todayActivities.isEmpty()) {
                    item { EmptyState(message = "Tidak ada kegiatan hari ini\nTambah kegiatan baru!") }
                } else {
                    items(uiState.todayActivities, key = { it.id }) { activity ->
                        ActivityCard(
                            activity   = activity,
                            onClick    = { onActivityClick(activity.id) },
                            onComplete = { viewModel.toggleCompleted(activity) },
                            onDelete   = { viewModel.deleteActivity(activity) }
                        )
                    }
                }

                // Upcoming section
                if (uiState.upcomingActivities.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        SectionHeader(title = "Akan Datang")
                        Spacer(Modifier.height(8.dp))
                    }
                    items(uiState.upcomingActivities.take(5), key = { "upcoming_${it.id}" }) { activity ->
                        ActivityCard(
                            activity   = activity,
                            onClick    = { onActivityClick(activity.id) },
                            onComplete = { viewModel.toggleCompleted(activity) },
                            onDelete   = { viewModel.deleteActivity(activity) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    isScrolled: Boolean
) {
    val today = LocalDate.now()
    val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("id"))
    val dateStr = today.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("id")))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isScrolled) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.background
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (searchQuery.isEmpty()) {
            Text(
                text  = dayName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text  = dateStr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
        }

        SearchBar(
            query          = searchQuery,
            onQueryChange  = onQueryChange,
            onClearSearch  = onClearSearch
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    OutlinedTextField(
        value         = query,
        onValueChange = onQueryChange,
        placeholder   = { Text("Cari kegiatan...") },
        leadingIcon   = {
            Icon(Icons.Rounded.Search, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingIcon  = if (query.isNotBlank()) ({
            IconButton(onClick = onClearSearch) {
                Icon(Icons.Rounded.Close, contentDescription = "Hapus pencarian")
            }
        }) else null,
        shape  = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Blue700,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor   = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        singleLine = true,
        modifier   = Modifier.fillMaxWidth()
    )
}

@Composable
private fun StatsHeader(completed: Int, pending: Int) {
    val total = completed + pending
    val progress = if (total == 0) 0f else completed.toFloat() / total

    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(Blue700, Blue600))
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text  = "Progres Hari Ini",
                    style = MaterialTheme.typography.labelMedium,
                    color = Blue100
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text  = "$completed dari $total",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = White
                        )
                        Text(
                            text  = "kegiatan selesai",
                            style = MaterialTheme.typography.bodySmall,
                            color = Blue100
                        )
                    }
                    // Circular progress
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress          = { progress },
                            modifier          = Modifier.size(56.dp),
                            color             = Teal400,
                            trackColor        = White.copy(alpha = 0.2f),
                            strokeWidth       = 6.dp
                        )
                        Text(
                            text  = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = White
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress      = { progress },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color         = Teal400,
                    trackColor    = White.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun WeekDateStrip(
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val days  = (-2..4).map { today.plusDays(it.toLong()) }
    val dayAbbr = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

    Row(
        modifier            = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { date ->
            val isSelected = date == selected
            val isToday    = date == today
            val dayIdx     = date.dayOfWeek.value - 1  // 0=Mon..6=Sun

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            isSelected -> Blue700
                            isToday    -> Blue50
                            else       -> Color.Transparent
                        }
                    )
                    .clickable { onSelect(date) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text  = dayAbbr[dayIdx],
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isSelected -> White
                        else       -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = when {
                        isSelected -> White
                        isToday    -> Blue700
                        else       -> MaterialTheme.colorScheme.onSurface
                    }
                )
                if (isToday) {
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) White else Blue700)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultsContent(
    results: List<Activity>,
    query: String,
    onClick: (Long) -> Unit,
    onComplete: (Activity) -> Unit,
    onDelete: (Activity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier        = modifier.padding(horizontal = 16.dp),
        contentPadding  = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text  = "${results.size} hasil untuk \"$query\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        if (results.isEmpty()) {
            item { EmptyState(message = "Tidak ada kegiatan yang cocok") }
        } else {
            items(results, key = { it.id }) { activity ->
                ActivityCard(
                    activity   = activity,
                    onClick    = { onClick(activity.id) },
                    onComplete = { onComplete(activity) },
                    onDelete   = { onDelete(activity) }
                )
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.EventAvailable,
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text      = message,
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
