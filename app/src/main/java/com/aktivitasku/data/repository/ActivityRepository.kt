package com.aktivitasku.data.repository

import com.aktivitasku.data.local.dao.ActivityDao
import com.aktivitasku.data.local.entity.toEntity
import com.aktivitasku.domain.model.Activity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepository @Inject constructor(
    private val dao: ActivityDao
) {

    fun observeAll(): Flow<List<Activity>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeById(id: Long): Flow<Activity?> =
        dao.observeById(id).map { it?.toDomain() }

    fun observeToday(): Flow<List<Activity>> =
        dao.observeByDate(LocalDate.now().atStartOfDay())
           .map { list -> list.map { it.toDomain() } }

    fun observeByDateRange(start: LocalDateTime, end: LocalDateTime): Flow<List<Activity>> =
        dao.observeByDateRange(start, end).map { it.map { e -> e.toDomain() } }

    fun observeUpcoming(): Flow<List<Activity>> =
        dao.observeUpcoming().map { it.map { e -> e.toDomain() } }

    fun observeNext(limit: Int = 5): Flow<List<Activity>> =
        dao.observeNextActivities(LocalDateTime.now(), limit)
           .map { it.map { e -> e.toDomain() } }

    fun search(query: String): Flow<List<Activity>> =
        dao.search(query).map { it.map { e -> e.toDomain() } }

    fun observeByCategory(category: String): Flow<List<Activity>> =
        dao.observeByCategory(category).map { it.map { e -> e.toDomain() } }

    fun countCompleted(): Flow<Int> = dao.countCompleted()
    fun countPending(): Flow<Int>   = dao.countPending()

    suspend fun getById(id: Long): Activity? =
        dao.getById(id)?.toDomain()

    suspend fun save(activity: Activity): Long =
        dao.insert(activity.toEntity())

    suspend fun update(activity: Activity) =
        dao.update(activity.toEntity())

    suspend fun delete(activity: Activity) =
        dao.delete(activity.toEntity())

    suspend fun deleteById(id: Long) =
        dao.deleteById(id)

    suspend fun setCompleted(id: Long, completed: Boolean) =
        dao.setCompleted(id, completed)
}
