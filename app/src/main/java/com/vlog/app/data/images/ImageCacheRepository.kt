package com.vlog.app.data.images

import android.content.Context
import android.util.Log
import com.vlog.app.utils.ImageFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * 图片缓存仓库
 * 负责图片的缓存、获取和更新
 */
class ImageCacheRepository(
    private val context: Context,
    private val imageCacheDao: ImageCacheDao,
    private val imageFileManager: ImageFileManager,
    private val okHttpClient: OkHttpClient
) {
    private val maxCacheSize = 100 * 1024 * 1024 // 100MB

    /**
     * 根据URL获取缓存的图片文件
     */
    suspend fun getCachedImageFile(url: String): File? {
        val cachedImage = imageCacheDao.getByUrl(url)

        return if (cachedImage != null) {
            val file = File(cachedImage.localPath)
            if (file.exists()) {
                // 更新最后访问时间
                imageCacheDao.update(cachedImage.copy(lastAccessTime = System.currentTimeMillis()))
                file
            } else {
                // 文件不存在，删除数据库记录
                imageCacheDao.delete(url)
                null
            }
        } else {
            null
        }
    }

    /**
     * 根据视频ID和图片类型获取缓存的图片文件列表
     */
    suspend fun getCachedImagesByVideoIdAndType(videoId: String, imageType: ImageType): List<File> {
        val cachedImages = imageCacheDao.getByVideoIdAndType(videoId, imageType.name)
        val result = mutableListOf<File>()

        cachedImages.forEach { cachedImage ->
            val file = File(cachedImage.localPath)
            if (file.exists()) {
                // 更新最后访问时间
                imageCacheDao.update(cachedImage.copy(lastAccessTime = System.currentTimeMillis()))
                result.add(file)
            } else {
                // 文件不存在，删除数据库记录
                imageCacheDao.delete(cachedImage.url)
            }
        }

        return result
    }

    /**
     * 根据视频ID获取所有缓存的图片文件，按类型分组
     */
    suspend fun getCachedImagesByVideoId(videoId: String): Map<ImageType, List<File>> {
        val cachedImages = imageCacheDao.getByVideoId(videoId)
        val result = mutableMapOf<ImageType, MutableList<File>>()

        cachedImages.forEach { cachedImage ->
            val file = File(cachedImage.localPath)
            if (file.exists()) {
                // 更新最后访问时间
                imageCacheDao.update(cachedImage.copy(lastAccessTime = System.currentTimeMillis()))

                // 根据图片类型分组
                val imageType = try {
                    ImageType.valueOf(cachedImage.imageType)
                } catch (e: Exception) {
                    ImageType.OTHER
                }

                if (!result.containsKey(imageType)) {
                    result[imageType] = mutableListOf()
                }
                result[imageType]?.add(file)
            } else {
                // 文件不存在，删除数据库记录
                imageCacheDao.delete(cachedImage.url)
            }
        }

        return result
    }

    /**
     * 下载并缓存图片
     */
    suspend fun downloadAndCacheImage(
        url: String,
        videoId: String? = null,
        imageType: ImageType = ImageType.OTHER,
        description: String? = null
    ): File? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val cachedImage = imageCacheDao.getByUrl(url)

                // 添加条件请求头
                val requestBuilder = request.newBuilder()
                cachedImage?.lastModified?.let {
                    requestBuilder.header("If-Modified-Since", it)
                }

                val response = okHttpClient.newCall(requestBuilder.build()).execute()

                when (response.code) {
                    304 -> {
                        // 未修改，使用缓存
                        Log.d("ImageCacheRepository", "Image not modified, using cache: $url")
                        return@withContext File(cachedImage!!.localPath)
                    }

                    200 -> {
                        // 有更新或首次下载
                        val responseBody = response.body ?: return@withContext null
                        val imageBytes = responseBody.bytes()

                        // 保存到文件
                        val file = imageFileManager.saveImageToFile(url, imageBytes)

                        // 获取Last-Modified头部
                        val lastModified = response.header("Last-Modified")
                        Log.d(
                            "ImageCacheRepository",
                            "Downloaded image with Last-Modified: $lastModified"
                        )

                        // 保存到数据库
                        val imageCache = ImageCache(
                            url = url,
                            localPath = file.absolutePath,
                            videoId = videoId,
                            imageType = imageType.name,
                            downloadTime = System.currentTimeMillis(),
                            lastModified = lastModified,
                            size = file.length(),
                            description = description
                        )
                        imageCacheDao.insert(imageCache)

                        // 检查并清理缓存
                        cleanupCacheIfNeeded()

                        return@withContext file
                    }

                    else -> {
                        Log.e(
                            "ImageCacheRepository",
                            "Error downloading image, status code: ${response.code}"
                        )
                        return@withContext null
                    }
                }
            } catch (e: Exception) {
                Log.e("ImageCacheRepository", "Error downloading image", e)
                return@withContext null
            }
        }
    }

    /**
     * 清除指定视频相关的所有图片缓存
     */
    suspend fun clearVideoCache(videoId: String) {
        val cachedImages = imageCacheDao.getByVideoId(videoId)
        cachedImages.forEach { cache ->
            imageFileManager.deleteFile(cache.localPath)
        }
        imageCacheDao.deleteByVideoId(videoId)
        Log.d("ImageCacheRepository", "Cleared cache for video: $videoId")
    }

    /**
     * 清除指定视频的指定类型图片缓存
     */
    suspend fun clearVideoCacheByType(videoId: String, imageType: ImageType) {
        val cachedImages = imageCacheDao.getByVideoIdAndType(videoId, imageType.name)
        cachedImages.forEach { cache ->
            imageFileManager.deleteFile(cache.localPath)
        }
        imageCacheDao.deleteByVideoIdAndType(videoId, imageType.name)
        Log.d("ImageCacheRepository", "Cleared cache for video: $videoId, type: ${imageType.name}")
    }

    /**
     * 清理过旧缓存
     */
    private suspend fun cleanupCacheIfNeeded() {
        val totalSize = imageCacheDao.getTotalSize()
        if (totalSize > maxCacheSize) {
            val sizeToFree = totalSize - (maxCacheSize * 0.8).toLong() // 释放20%空间
            val filesToDelete = imageCacheDao.getOldestAccessed((sizeToFree / 50000).toInt()) // 假设平均图片大小50KB

            Log.d("ImageCacheRepository", "Cleaning up cache, freeing ${sizeToFree / 1024}KB")

            filesToDelete.forEach { cache ->
                imageFileManager.deleteFile(cache.localPath)
                imageCacheDao.delete(cache.url)
            }
        }
    }

    /**
     * 获取缓存统计信息
     */
    suspend fun getCacheStats(): CacheStats {
        val totalEntries = imageCacheDao.getOldestAccessed(Int.MAX_VALUE).size
        val totalSize = imageCacheDao.getTotalSize()
        return CacheStats(totalEntries, totalSize)
    }

    /**
     * 缓存统计信息
     */
    data class CacheStats(
        val totalEntries: Int,
        val totalSize: Long
    )
}