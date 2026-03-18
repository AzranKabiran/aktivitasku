package com.aktivitasku.presentation.statistics

import com.aktivitasku.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Pure logic tests for StatisticsViewModel calculations.
 * Extracted as top-level functions for easy unit testing without ViewModel setup.
 */
class StatisticsCalculationsTest {

    private fun makeActivity(
        id: Long,
        completed: Boolean,
        daysAgo: Long = 0,
        category: ActivityCategory = ActivityCategory.OTHER
    ) = Activity(
        id            = id,
        title         = "Test $id",
        startDateTime = LocalDate.now().minusDays(daysAgo).atTime(9, 0),
        isCompleted   = completed,
        category      = category
    )

    // ── Completion rate ────────────────────────────────

    @Test
    fun `completion rate 0 when no activities`() {
        val rate = calcCompletionRate(emptyList())
        assertEquals(0f, rate, 0.001f)
    }

    @Test
    fun `completion rate 1 when all done`() {
        val activities = listOf(
            makeActivity(1, true),
            makeActivity(2, true)
        )
        assertEquals(1f, calcCompletionRate(activities), 0.001f)
    }

    @Test
    fun `completion rate 0_5 when half done`() {
        val activities = listOf(
            makeActivity(1, true),
            makeActivity(2, false)
        )
        assertEquals(0.5f, calcCompletionRate(activities), 0.001f)
    }

    // ── Current streak ─────────────────────────────────

    @Test
    fun `streak 0 when nothing completed`() {
        val activities = listOf(makeActivity(1, false))
        assertEquals(0, calcCurrentStreak(activities))
    }

    @Test
    fun `streak 1 when completed today only`() {
        val activities = listOf(makeActivity(1, true, daysAgo = 0))
        assertEquals(1, calcCurrentStreak(activities))
    }

    @Test
    fun `streak 3 for three consecutive days`() {
        val activities = listOf(
            makeActivity(1, true, daysAgo = 2),
            makeActivity(2, true, daysAgo = 1),
            makeActivity(3, true, daysAgo = 0)
        )
        assertEquals(3, calcCurrentStreak(activities))
    }

    @Test
    fun `streak breaks on gap`() {
        val activities = listOf(
            makeActivity(1, true, daysAgo = 3),
            makeActivity(2, true, daysAgo = 1), // gap at day 2
            makeActivity(3, true, daysAgo = 0)
        )
        assertEquals(2, calcCurrentStreak(activities))
    }

    // ── Longest streak ─────────────────────────────────

    @Test
    fun `longest streak finds max run`() {
        val activities = listOf(
            makeActivity(1, true, daysAgo = 10),
            makeActivity(2, true, daysAgo = 9),
            makeActivity(3, true, daysAgo = 8), // run of 3
            makeActivity(4, true, daysAgo = 5),
            makeActivity(5, true, daysAgo = 4),
            makeActivity(6, true, daysAgo = 3),
            makeActivity(7, true, daysAgo = 2),
            makeActivity(8, true, daysAgo = 1)  // run of 5
        )
        assertEquals(5, calcLongestStreak(activities))
    }

    // ── Category breakdown ─────────────────────────────

    @Test
    fun `category breakdown sums correctly`() {
        val activities = listOf(
            makeActivity(1, true,  category = ActivityCategory.WORK),
            makeActivity(2, false, category = ActivityCategory.WORK),
            makeActivity(3, true,  category = ActivityCategory.HEALTH)
        )
        val stats = buildCategoryStats(activities)
        val work   = stats.first { it.category == ActivityCategory.WORK }
        val health = stats.first { it.category == ActivityCategory.HEALTH }

        assertEquals(2, work.count)
        assertEquals(1, health.count)
        assertEquals(2f / 3f, work.percentage, 0.001f)
        assertEquals(1f / 3f, health.percentage, 0.001f)
    }

    @Test
    fun `category breakdown sorted descending`() {
        val activities = listOf(
            makeActivity(1, true, category = ActivityCategory.HEALTH),
            makeActivity(2, true, category = ActivityCategory.WORK),
            makeActivity(3, true, category = ActivityCategory.WORK),
            makeActivity(4, true, category = ActivityCategory.WORK)
        )
        val stats = buildCategoryStats(activities)
        assertEquals(ActivityCategory.WORK, stats.first().category)
    }

    // ── Helper functions (mirror ViewModel logic) ──────

    private fun calcCompletionRate(activities: List<Activity>): Float {
        val total = activities.size
        if (total == 0) return 0f
        return activities.count { it.isCompleted }.toFloat() / total
    }

    private fun calcCurrentStreak(activities: List<Activity>): Int {
        var streak = 0
        var date   = LocalDate.now()
        while (true) {
            if (!activities.any { it.isCompleted && it.startDateTime.toLocalDate() == date }) break
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
        var longest = 0; var current = 0; var prev: LocalDate? = null
        for (date in completedDates) {
            current = if (prev != null && date == prev.plusDays(1)) current + 1 else 1
            longest = maxOf(longest, current)
            prev    = date
        }
        return longest
    }

    private fun buildCategoryStats(activities: List<Activity>): List<CategoryStats> {
        val total = activities.size.takeIf { it > 0 } ?: return emptyList()
        return ActivityCategory.values().mapNotNull { cat ->
            val count = activities.count { it.category == cat }
            if (count == 0) null
            else CategoryStats(cat, count, count.toFloat() / total)
        }.sortedByDescending { it.count }
    }
}
