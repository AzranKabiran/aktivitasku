package com.aktivitasku.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktivitasku.data.local.dao.ActivityDao
import com.aktivitasku.data.repository.ActivityRepository
import com.aktivitasku.domain.model.Activity
import com.aktivitasku.domain.model.ActivityCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class DayStats(val date: LocalDate, val completed: Int, val total: Int)
data class CategoryStats(val category: ActivityCategory, val count: Int, val percentage: Float)

data class StatsUiState(
    val totalCompleted: Int         = 0,
    val totalPending: Int           = 0,
    val weeklyStats: List<DayStats> = emptyList(),
    val categoryBreakdown: List<CategoryStats> = emptyList(),
    val currentStreak: Int          = 0,
    val longestStreak: Int          = 0,
    val completionRate: Float       = 0f,
    val isLoading: Boolean          = true
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: ActivityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init { loadStats() }

    private fun loadStats() {
        viewModelScope.launch {
            repository.observeAll().collect { activities ->
                val completed = activities.count { it.isCompleted }
                val total     = activities.size
                val rate      = if (total == 0) 0f else completed.toFloat() / total

                _uiState.update {
                    it.copy(
                        totalCompleted    = completed,
                        totalPending      = activities.count { a -> !a.isCompleted },
                        weeklyStats       = buildWeeklyStats(activities),
                        categoryBreakdown = buildCategoryStats(activities),
                        currentStreak     = calcCurrentStreak(activities),
                        longestStreak     = calcLongestStreak(activities),
                        completionRate    = rate,
                        isLoading         = false
                    )
                }
            }
        }
    }

    private fun buildWeeklyStats(activities: List<Activity>): List<DayStats> {
        val today = LocalDate.now()
        return (6 downTo 0).map { daysBack ->
            val date  = today.minusDays(daysBack.toLong())
            val inDay = activities.filter { it.startDateTime.toLocalDate() == date }
            DayStats(
                date      = date,
                completed = inDay.count { it.isCompleted },
                total     = inDay.size
            )
        }
    }

    private fun buildCategoryStats(activities: List<Activity>): List<CategoryStats> {
        val total = activities.size.takeIf { it > 0 } ?: return emptyList()
        return ActivityCategory.values().mapNotNull { cat ->
            val count = activities.count { it.category == cat }
            if (count == 0) null
            else CategoryStats(cat, count, count.toFloat() / total)
        }.sortedByDescending { it.count }
    }

    private fun calcCurrentStreak(activities: List<Activity>): Int {
        var streak = 0
        var date   = LocalDate.now()
        while (true) {
            val hadCompleted = activities.any {
                it.isCompleted && it.startDateTime.toLocalDate() == date
            }
            if (!hadCompleted) break
            streak++
            date = date.minusDays(1)
        }
        return streak
    }

    private fun calcLongestStreak(activities: List<Activity>): Int {
        val completedDates = activities
            .filter { it.isCompleted }
            .map { it.startDateTime.toLocalDate() }
            .toSortedSet()
        var longest = 0
        var current = 0
        var prev: LocalDate? = null
        for (date in completedDates) {
            current = if (prev != null && date == prev.plusDays(1)) current + 1 else 1
            longest = maxOf(longest, current)
            prev    = date
        }
        return longest
    }
}
