package com.vlog.app.data.histories.watch

import com.vlog.app.data.histories.search.SearchHistoryDao
import com.vlog.app.data.histories.search.SearchHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Date

class HistoryRepository(
    private val watchHistoryEntityDao: WatchHistoryEntityDao,
    private val searchHistoryDao: SearchHistoryDao
) {

    // Watch History
    fun getAllWatchHistory(): Flow<List<WatchHistoryEntity>> {
        return this@HistoryRepository.watchHistoryEntityDao.getAllWatchHistory()
    }

    suspend fun getWatchHistoryById(videoId: String): WatchHistoryEntity? {
        return withContext(Dispatchers.IO) {
            this@HistoryRepository.watchHistoryEntityDao.getWatchHistoryById(videoId)
        }
    }

    suspend fun saveWatchHistory(
        videoId: String,
        title: String,
        cover: String,
        duration: String?,
        watchedDuration: Long,
        typed: Int
    ) {
        withContext(Dispatchers.IO) {
            val watchHistory = WatchHistoryEntity(
                videoId = videoId,
                title = title,
                cover = cover,
                duration = duration,
                watchedDuration = watchedDuration,
                lastWatchedTime = Date(),
                typed = typed
            )
            this@HistoryRepository.watchHistoryEntityDao.insertWatchHistory(watchHistory)
        }
    }

    suspend fun deleteWatchHistory(watchHistory: WatchHistoryEntity) {
        withContext(Dispatchers.IO) {
            this@HistoryRepository.watchHistoryEntityDao.deleteWatchHistory(watchHistory)
        }
    }

    suspend fun clearAllWatchHistory() {
        withContext(Dispatchers.IO) {
            this@HistoryRepository.watchHistoryEntityDao.clearAllWatchHistory()
        }
    }

    // Search History
    fun getRecentSearches(): Flow<List<SearchHistoryEntity>> {
        return searchHistoryDao.getRecentSearches()
    }

    suspend fun saveSearchQuery(query: String) {
        withContext(Dispatchers.IO) {
            val searchHistory = SearchHistoryEntity(
                query = query,
                timestamp = Date()
            )
            searchHistoryDao.insertSearchQuery(searchHistory)
        }
    }

    suspend fun deleteSearchQuery(searchHistory: SearchHistoryEntity) {
        withContext(Dispatchers.IO) {
            searchHistoryDao.deleteSearchQuery(searchHistory)
        }
    }

    suspend fun clearAllSearchHistory() {
        withContext(Dispatchers.IO) {
            searchHistoryDao.clearAllSearchHistory()
        }
    }
}
