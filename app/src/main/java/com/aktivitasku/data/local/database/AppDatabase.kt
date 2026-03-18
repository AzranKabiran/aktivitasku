package com.aktivitasku.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.aktivitasku.data.local.dao.ActivityDao
import com.aktivitasku.data.local.entity.ActivityEntity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Database(
    entities = [ActivityEntity::class],
    version  = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao

    companion object {
        const val DATABASE_NAME = "aktivitasku.db"
    }
}

class Converters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? =
        value?.format(formatter)

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? =
        value?.let { LocalDateTime.parse(it, formatter) }
}
