package com.vlog.app.data.histories.watch

import com.vlog.app.data.videos.Video
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 观看历史仓库
 */
class WatchHistoryRepository(private val watchHistoryDao: WatchHistoryDao) {

    /**
     * 获取所有观看历史
     */
    fun getAllWatchHistory(): Flow<List<WatchHistory>> {
        return watchHistoryDao.getAllWatchHistory()
    }

    /**
     * 获取最近观看的历史记录
     */
    fun getRecentWatchHistory(limit: Int = 10): Flow<List<WatchHistory>> {
        return watchHistoryDao.getRecentWatchHistory(limit)
    }

    /**
     * 按类型获取观看历史
     */
    fun getWatchHistoryByType(videoType: Int): Flow<List<WatchHistory>> {
        return watchHistoryDao.getWatchHistoryByType(videoType)
    }

    /**
     * 获取指定视频的观看历史
     */
    suspend fun getWatchHistoryById(videoId: String): WatchHistory? {
        return watchHistoryDao.getWatchHistoryById(videoId)
    }

    /**
     * 添加或更新观看历史
     */
    suspend fun addOrUpdateWatchHistory(
        videoId: String,
        title: String,
        coverUrl: String?,
        playPosition: Long = 0,
        duration: Long = 0,
        videoType: Int = 0,
        episodeTitle: String? = null,
        episodeIndex: Int = 0,
        totalEpisodes: Int = 0,
        gatherId: String? = null,
        gatherName: String? = null,
        playerUrl: String? = null
    ) {
        val watchHistory = WatchHistory(
            videoId = videoId,
            title = title,
            coverUrl = coverUrl,
            lastPlayedPosition = playPosition,
            duration = duration,
            lastWatchTime = Date(),
            videoType = videoType,
            episodeTitle = episodeTitle,
            episodeIndex = episodeIndex,
            totalEpisodes = totalEpisodes,
            gatherId = gatherId,
            gatherName = gatherName,
            playerUrl = playerUrl
        )
        watchHistoryDao.insertWatchHistory(watchHistory)
    }

    /**
     * 从 Video 添加观看历史
     * 同时支持列表项和详情
     */
    suspend fun addWatchHistoryFromVideo(
        video: Video,
        playPosition: Long = 0,
        duration: Long = 0,
        episodeTitle: String? = null,
        episodeIndex: Int = 0,
        gatherId: String? = null,
        gatherName: String? = null,
        playerUrl: String? = null
    ) {
        val videoType = when {
            video.typeName?.contains("电影") == true -> 1
            video.typeName?.contains("电视") == true -> 2
            video.typeName?.contains("动漫") == true -> 3
            else -> 0
        }

        addOrUpdateWatchHistory(
            videoId = video.id,
            title = video.title,
            coverUrl = video.coverUrl,
            playPosition = playPosition,
            duration = duration,
            videoType = videoType,
            episodeTitle = episodeTitle,
            episodeIndex = episodeIndex,
            totalEpisodes = video.episodeCount ?: 0,
            gatherId = gatherId,
            gatherName = gatherName,
            playerUrl = playerUrl
        )
    }

    /**
     * 从 Video 添加观看历史（兼容旧版本）
     */
    suspend fun addWatchHistoryFromVideoDetail(
        videoDetail: Video,
        playPosition: Long = 0,
        duration: Long = 0,
        episodeTitle: String? = null,
        episodeIndex: Int = 0,
        gatherId: String? = null,
        gatherName: String? = null,
        playerUrl: String? = null
    ) {
        addWatchHistoryFromVideo(
            video = videoDetail,
            playPosition = playPosition,
            duration = duration,
            episodeTitle = episodeTitle,
            episodeIndex = episodeIndex,
            gatherId = gatherId,
            gatherName = gatherName,
            playerUrl = playerUrl
        )
    }

    /**
     * 更新播放进度
     */
    suspend fun updatePlayProgress(
        videoId: String,
        playPosition: Long,
        duration: Long,
        gatherId: String? = null,
        gatherName: String? = null,
        playerUrl: String? = null
    ) {
        val watchHistory = watchHistoryDao.getWatchHistoryById(videoId)
        watchHistory?.let {
            val updated = it.copy(
                lastPlayedPosition = playPosition,
                duration = duration,
                lastWatchTime = Date(),
                gatherId = gatherId ?: it.gatherId,
                gatherName = gatherName ?: it.gatherName,
                playerUrl = playerUrl ?: it.playerUrl
            )
            watchHistoryDao.updateWatchHistory(updated)
        }
    }

    /**
     * 删除观看历史
     */
    suspend fun deleteWatchHistory(videoId: String) {
        watchHistoryDao.deleteWatchHistoryById(videoId)
    }

    /**
     * 清空所有观看历史
     */
    suspend fun clearAllWatchHistory() {
        watchHistoryDao.clearAllWatchHistory()
    }
}
