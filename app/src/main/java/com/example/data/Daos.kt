package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelProgressDao {

    @Query("SELECT * FROM level_progress ORDER BY levelId ASC")
    fun getAllProgress(): Flow<List<LevelProgressEntity>>

    @Query("SELECT * FROM level_progress WHERE levelId = :levelId")
    suspend fun getProgressForLevel(levelId: Int): LevelProgressEntity?

    @Query("SELECT * FROM level_progress WHERE levelId = :levelId")
    fun observeProgressForLevel(levelId: Int): Flow<LevelProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: LevelProgressEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(list: List<LevelProgressEntity>)

    @Query("SELECT COUNT(*) FROM level_progress WHERE isCompleted = 1")
    fun observeCompletedCount(): Flow<Int>

    @Query("SELECT * FROM level_progress WHERE isCompleted = 1")
    fun observeCompletedLevels(): Flow<List<LevelProgressEntity>>

    @Query("SELECT * FROM level_progress WHERE isCompleted = 1")
    suspend fun getCompletedLevels(): List<LevelProgressEntity>

    @Query("SELECT MAX(levelId) FROM level_progress WHERE isUnlocked = 1")
    fun observeHighestUnlockedLevel(): Flow<Int?>

    @Query("SELECT MAX(levelId) FROM level_progress WHERE isUnlocked = 1")
    suspend fun getHighestUnlockedLevel(): Int?

    @Query("SELECT SUM(stars) FROM level_progress")
    fun observeTotalStars(): Flow<Int?>
}

@Dao
interface UserSettingsDao {

    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun observeSettings(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getSettings(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: UserSettingsEntity)
}
