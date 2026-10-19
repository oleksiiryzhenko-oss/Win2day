package com.taichiflow.app.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val name: String,
    val type: String,
    val category: String,
    val timestamp: Long,
    val durationSec: Int,
    val score: Int,
    val xp: Int,
    val balance: Int,
    val stability: Int,
    val mobility: Int,
    val coordination: Int,
    val breathing: Int,
    val rhythm: Int,
    val focus: Int,
    val flow: Int,
    val details: String,
    val movementIds: String,
    val completion: Int,
    val isRecord: Boolean,
    val recordLabel: String,
    val daily: Boolean
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val unlockedAt: Long
)

@Entity(tableName = "custom_sessions")
data class CustomSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val movementIds: String,
    val workSec: Int,
    val restSec: Int,
    val rounds: Int,
    val inhale: Int,
    val hold: Int,
    val exhale: Int,
    val holdOut: Int,
    val createdAt: Long
)

@Entity(tableName = "movement_progress")
data class MovementProgressEntity(
    @PrimaryKey val movementId: String,
    val timesCompleted: Int,
    val totalSec: Int,
    val lastPracticed: Long
)

@Entity(tableName = "daily_completions")
data class DailyCompletionEntity(
    @PrimaryKey val epochDay: Long,
    val challengeId: String,
    val completedAt: Long,
    val xp: Int
)

@Entity(tableName = "weekly_claims", primaryKeys = ["weekKey", "goalId"])
data class WeeklyClaimEntity(
    val weekKey: Long,
    val goalId: String,
    val claimedAt: Long,
    val xp: Int
)

@Dao
interface TaiChiDao {
    // History
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun history(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    suspend fun historyOnce(): List<HistoryEntity>

    @Insert
    suspend fun insertHistory(entity: HistoryEntity): Long

    @Insert
    suspend fun insertHistoryAll(entities: List<HistoryEntity>)

    // Achievements
    @Query("SELECT * FROM achievements")
    fun achievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun achievementsOnce(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievements(entities: List<AchievementEntity>)

    // Custom sessions
    @Query("SELECT * FROM custom_sessions ORDER BY createdAt DESC")
    fun customSessions(): Flow<List<CustomSessionEntity>>

    @Query("SELECT * FROM custom_sessions ORDER BY createdAt DESC")
    suspend fun customSessionsOnce(): List<CustomSessionEntity>

    @Query("SELECT * FROM custom_sessions WHERE id = :id")
    suspend fun customSession(id: Long): CustomSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomSession(entity: CustomSessionEntity): Long

    @Query("DELETE FROM custom_sessions WHERE id = :id")
    suspend fun deleteCustomSession(id: Long)

    // Movement progress
    @Query("SELECT * FROM movement_progress")
    fun movementProgress(): Flow<List<MovementProgressEntity>>

    @Query("SELECT * FROM movement_progress")
    suspend fun movementProgressOnce(): List<MovementProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMovementProgress(entities: List<MovementProgressEntity>)

    // Daily
    @Query("SELECT * FROM daily_completions ORDER BY epochDay DESC")
    fun dailyCompletions(): Flow<List<DailyCompletionEntity>>

    @Query("SELECT * FROM daily_completions ORDER BY epochDay DESC")
    suspend fun dailyCompletionsOnce(): List<DailyCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDaily(entity: DailyCompletionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDailyAll(entities: List<DailyCompletionEntity>)

    // Weekly
    @Query("SELECT * FROM weekly_claims")
    fun weeklyClaims(): Flow<List<WeeklyClaimEntity>>

    @Query("SELECT * FROM weekly_claims")
    suspend fun weeklyClaimsOnce(): List<WeeklyClaimEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWeeklyClaim(entity: WeeklyClaimEntity): Long
}

@Database(
    entities = [
        HistoryEntity::class,
        AchievementEntity::class,
        CustomSessionEntity::class,
        MovementProgressEntity::class,
        DailyCompletionEntity::class,
        WeeklyClaimEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): TaiChiDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "taichiflow.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
