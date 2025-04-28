package com.vlog.app.data.videos

import android.util.Log
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * 视频数据访问对象
 */
@Dao
interface VideoDao {
    /**
     * 插入视频数据
     */
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    /**
     * 批量插入视频数据
     */
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    /**
     * 获取首页轮播图数据
     */
    @Query("SELECT * FROM videos WHERE pageSource = 'home_banner' ORDER BY lastUpdated DESC LIMIT :limit")
    fun getBanners(limit: Int = 5): Flow<List<VideoEntity>>

    /**
     * 获取首页推荐电影数据
     */
    @Query("SELECT * FROM videos WHERE pageSource = 'home_movie' ORDER BY lastUpdated DESC")
    fun getRecommendedMovies(): Flow<List<VideoEntity>>

    /**
     * 获取首页电视剧数据
     */
    @Query("SELECT * FROM videos WHERE pageSource = 'home_tv' ORDER BY lastUpdated DESC")
    fun getTvSeries(): Flow<List<VideoEntity>>

    /**
     * 获取首页动漫数据
     */
    @Query("SELECT * FROM videos WHERE pageSource = 'home_anime' ORDER BY lastUpdated DESC")
    fun getAnime(): Flow<List<VideoEntity>>

    /**
     * 获取筛选页面数据
     */
    @Query("SELECT * FROM videos WHERE pageSource = 'filter' AND videoType = :videoType ORDER BY lastUpdated DESC")
    fun getFilteredVideos(videoType: Int): Flow<List<VideoEntity>>

    /**
     * 获取解说页面数据
     */
    @Query("SELECT * FROM videos WHERE pageSource = 'shorts' ORDER BY lastUpdated DESC")
    fun getShorts(): Flow<List<VideoEntity>>

    /**
     * 获取指定类型的视频数据
     */
    @Query("SELECT * FROM videos WHERE videoType = :videoType ORDER BY lastUpdated DESC")
    fun getVideosByType(videoType: Int): Flow<List<VideoEntity>>

    /**
     * 获取指定ID的视频数据
     */
    @Query("SELECT * FROM videos WHERE id = :videoId")
    suspend fun getVideoById(videoId: String): VideoEntity?

    /**
     * 删除指定来源的所有视频数据
     */
    @Query("DELETE FROM videos WHERE pageSource = :pageSource")
    suspend fun deleteVideosByPageSource(pageSource: String)

    /**
     * 删除指定来源和类型的所有视频数据
     */
    @Query("DELETE FROM videos WHERE pageSource = :pageSource AND videoType = :videoType")
    suspend fun deleteVideosByPageSourceAndType(pageSource: String, videoType: Int)

    /**
     * 获取指定来源的最后更新时间
     */
    @Query("SELECT MAX(lastUpdated) FROM videos WHERE pageSource = :pageSource")
    suspend fun getLastUpdateTime(pageSource: String): Long?

    /**
     * 获取指定来源和类型的最后更新时间
     */
    @Query("SELECT MAX(lastUpdated) FROM videos WHERE pageSource = :pageSource AND videoType = :videoType")
    suspend fun getLastUpdateTimeByType(pageSource: String, videoType: Int): Long?

    /**
     * 刷新首页数据
     */
    @Transaction
    suspend fun refreshHomeData(
        banners: List<VideoEntity>,
        recommendedMovies: List<VideoEntity>,
        tvSeries: List<VideoEntity>,
        anime: List<VideoEntity>
    ) {
        try {
            // 删除旧数据
            deleteVideosByPageSource("home_banner")
            deleteVideosByPageSource("home_movie")
            deleteVideosByPageSource("home_tv")
            deleteVideosByPageSource("home_anime")

            // 插入新数据
            if (banners.isNotEmpty()) insertVideos(banners)
            if (recommendedMovies.isNotEmpty()) insertVideos(recommendedMovies)
            if (tvSeries.isNotEmpty()) insertVideos(tvSeries)
            if (anime.isNotEmpty()) insertVideos(anime)
        } catch (e: Exception) {
            Log.e("VideoDao", "Error refreshing home data: ${e.message}", e)
            throw e
        }
    }

    /**
     * 刷新筛选页面数据
     */
    @Transaction
    suspend fun refreshFilterData(videos: List<VideoEntity>, videoType: Int) {
        try {
            // 删除旧数据
            deleteVideosByPageSourceAndType("filter", videoType)

            // 插入新数据
            if (videos.isNotEmpty()) insertVideos(videos)
        } catch (e: Exception) {
            Log.e("VideoDao", "Error refreshing filter data: ${e.message}", e)
            throw e
        }
    }

    /**
     * 刷新解说页面数据
     */
    @Transaction
    suspend fun refreshShortsData(videos: List<VideoEntity>) {
        try {
            // 删除旧数据
            deleteVideosByPageSource("shorts")

            // 插入新数据
            if (videos.isNotEmpty()) insertVideos(videos)
        } catch (e: Exception) {
            Log.e("VideoDao", "Error refreshing shorts data: ${e.message}", e)
            throw e
        }
    }
}