package com.vlog.app.data.histories.watch

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchHistoryEntityDao {

    @Query("SELECT * FROM watch_history ORDER BY lastWatchedTime DESC")
    fun getAllWatchHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE videoId = :videoId")
    suspend fun getWatchHistoryById(videoId: String): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertWatchHistory(watchHistory: WatchHistoryEntity)

    @Delete
    suspend fun deleteWatchHistory(watchHistory: WatchHistoryEntity)

    @Query("DELETE FROM watch_history")
    suspend fun clearAllWatchHistory()
}