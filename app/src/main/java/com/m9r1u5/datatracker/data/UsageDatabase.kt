package com.m9r1u5.datatracker.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** One row = one app's data use on one day. Dates are stored as yyyy-MM-dd. */
@Entity(tableName = "daily_usage", primaryKeys = ["date", "appKey"])
data class DailyUsage(
    val date: String,
    val appKey: String,
    val label: String,
    val isSystem: Boolean,
    val mobileBytes: Long = 0,
    val wifiBytes: Long = 0,
    val mobileFgBytes: Long = 0,
    val mobileBgBytes: Long = 0,
    val wifiFgBytes: Long = 0,
    val wifiBgBytes: Long = 0,
)

@Dao
interface UsageDao {
    @Query("SELECT * FROM daily_usage WHERE date = :date")
    suspend fun forDate(date: String): List<DailyUsage>

    @Query("SELECT * FROM daily_usage WHERE date = :date")
    fun observeDate(date: String): Flow<List<DailyUsage>>

    @Query("SELECT * FROM daily_usage WHERE date BETWEEN :from AND :to")
    suspend fun range(from: String, to: String): List<DailyUsage>

    @Query("SELECT * FROM daily_usage WHERE date BETWEEN :from AND :to")
    fun observeRange(from: String, to: String): Flow<List<DailyUsage>>

    @Query("SELECT * FROM daily_usage WHERE appKey = :key AND date BETWEEN :from AND :to ORDER BY date")
    fun observeApp(key: String, from: String, to: String): Flow<List<DailyUsage>>

    @Query("SELECT * FROM daily_usage ORDER BY date, appKey")
    suspend fun all(): List<DailyUsage>

    @Upsert
    suspend fun upsertAll(rows: List<DailyUsage>)

    @Query("DELETE FROM daily_usage")
    suspend fun clear()
}

@Database(entities = [DailyUsage::class], version = 1, exportSchema = false)
abstract class UsageDatabase : RoomDatabase() {
    abstract fun dao(): UsageDao

    companion object {
        @Volatile
        private var instance: UsageDatabase? = null

        fun get(context: Context): UsageDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    UsageDatabase::class.java,
                    "usage.db"
                ).build().also { instance = it }
            }
    }
}

/** Adds up rows for the same app across several days. */
fun List<DailyUsage>.sumByApp(): List<DailyUsage> =
    groupBy { it.appKey }.map { (key, list) ->
        val latest = list.maxBy { it.date }
        DailyUsage(
            date = "",
            appKey = key,
            label = latest.label,
            isSystem = latest.isSystem,
            mobileBytes = list.sumOf { it.mobileBytes },
            wifiBytes = list.sumOf { it.wifiBytes },
            mobileFgBytes = list.sumOf { it.mobileFgBytes },
            mobileBgBytes = list.sumOf { it.mobileBgBytes },
            wifiFgBytes = list.sumOf { it.wifiFgBytes },
            wifiBgBytes = list.sumOf { it.wifiBgBytes },
        )
    }
