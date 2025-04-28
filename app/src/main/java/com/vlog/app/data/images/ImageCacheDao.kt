package com.vlog.app.data.images

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * 图片缓存数据访问对象
 */
@Dao
interface ImageCacheDao {
    @Query("SELECT * FROM image_cache WHERE url = :url")
    suspend fun getByUrl(url: String): ImageCache?

    @Query("SELECT * FROM image_cache WHERE videoId = :videoId AND imageType = :imageType")
    suspend fun getByVideoIdAndType(videoId: String, imageType: String): List<ImageCache>

    @Query("SELECT * FROM image_cache WHERE videoId = :videoId")
    suspend fun getByVideoId(videoId: String): List<ImageCache>

    @Query("SELECT * FROM image_cache WHERE imageType = :imageType")
    suspend fun getByImageType(imageType: String): List<ImageCache>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(imageCache: ImageCache)

    @Update
    suspend fun update(imageCache: ImageCache)

    @Query("DELETE FROM image_cache WHERE url = :url")
    suspend fun delete(url: String)

    @Query("DELETE FROM image_cache WHERE videoId = :videoId")
    suspend fun deleteByVideoId(videoId: String)

    @Query("DELETE FROM image_cache WHERE videoId = :videoId AND imageType = :imageType")
    suspend fun deleteByVideoIdAndType(videoId: String, imageType: String)

    @Query("SELECT * FROM image_cache ORDER BY lastAccessTime ASC LIMIT :limit")
    suspend fun getOldestAccessed(limit: Int): List<ImageCache>

    @Query("SELECT SUM(size) FROM image_cache")
    suspend fun getTotalSize(): Long
}