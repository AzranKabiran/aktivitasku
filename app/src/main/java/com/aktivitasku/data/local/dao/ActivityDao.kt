package com.aktivitasku.data.local.dao

import androidx.room.*
import com.aktivitasku.data.local.entity.ActivityEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ActivityDao {

    // ── Insert / Update / Delete ──────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: ActivityEntity): Long

    @Update
    suspend fun update(activity: ActivityEntity)

    @Delete
    suspend fun delete(activity: ActivityEntity)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteById(id: Long)

    // ── Single Queries ────────────────────────────────────

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getById(id: Long): ActivityEntity?

    @Query("SELECT * FROM activities WHERE id = :id")
    fun observeById(id: Long): Flow<ActivityEntity?>

    // ── List Queries (Flow = live / reactive) ─────────────

    @Query("SELECT * FROM activities ORDER BY startDateTime ASC")
    fun observeAll(): Flow<List<ActivityEntity>>

    @Query("""
        SELECT * FROM activities
        WHERE startDateTime >= :start AND startDateTime < :end
        ORDER BY startDateTime ASC
    """)
    fun observeByDateRange(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<ActivityEntity>>

    @Query("""
        SELECT * FROM activities
        WHERE DATE(startDateTime) = DATE(:date)
        ORDER BY startDateTime ASC
    """)
    fun observeByDate(date: LocalDateTime): Flow<List<ActivityEntity>>

    @Query("""
        SELECT * FROM activities
        WHERE isCompleted = 0
        ORDER BY startDateTime ASC
    """)
    fun observeUpcoming(): Flow<List<ActivityEntity>>

    @Query("""
        SELECT * FROM activities
        WHERE isCompleted = 0 
          AND startDateTime >= :from
        ORDER BY startDateTime ASC
        LIMIT :limit
    """)
    fun observeNextActivities(from: LocalDateTime, limit: Int = 5): Flow<List<ActivityEntity>>

    @Query("""
        SELECT * FROM activities
        WHERE category = :category
        ORDER BY startDateTime ASC
    """)
    fun observeByCategory(category: String): Flow<List<ActivityEntity>>

    // ── Search ────────────────────────────────────────────

    @Query("""
        SELECT * FROM activities
        WHERE title LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
        ORDER BY startDateTime ASC
    """)
    fun search(query: String): Flow<List<ActivityEntity>>

    // ── Mark Complete ─────────────────────────────────────

    @Query("UPDATE activities SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    // ── Stats ─────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM activities WHERE isCompleted = 1")
    fun countCompleted(): Flow<Int>

    @Query("SELECT COUNT(*) FROM activities WHERE isCompleted = 0")
    fun countPending(): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM activities
        WHERE isCompleted = 1
          AND DATE(startDateTime) = DATE(:date)
    """)
    suspend fun countCompletedOnDate(date: LocalDateTime): Int
}
