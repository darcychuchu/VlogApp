package com.vlog.app.utils

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.request.ImageResult
import coil.request.SuccessResult
import com.vlog.app.data.images.ImageType
import com.vlog.app.data.images.ImageCacheRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.core.graphics.drawable.toDrawable

/**
 * Coil图片加载拦截器
 * 用于拦截图片加载请求，实现缓存功能
 */
class CachedImageInterceptor(
    private val imageCacheRepository: ImageCacheRepository
) : Interceptor {

    // 使用互斥锁防止同一URL的并发下载
    private val downloadMutexes = mutableMapOf<String, Mutex>()
    private val mutexesLock = Mutex()

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val data = request.data

        // 只处理URL请求
        if (data is String && data.startsWith("http")) {
            val url = data
            // 从请求头部获取参数
            val headers = request.headers
            val videoId = headers["X-VideoId"]
            val imageTypeStr = headers["X-ImageType"]
            val description = headers["X-Description"]

            Log.d("CachedImageInterceptor", "Headers: $headers, videoId: $videoId, imageType: $imageTypeStr")

            val imageType = if (imageTypeStr != null) {
                try {
                    ImageType.valueOf(imageTypeStr)
                } catch (e: Exception) {
                    Log.w("CachedImageInterceptor", "Invalid image type: $imageTypeStr, using OTHER")
                    ImageType.OTHER
                }
            } else {
                ImageType.OTHER
            }

            Log.d("CachedImageInterceptor", "Intercepting image request: $url, videoId: $videoId, type: $imageType")

            // 尝试从缓存获取
            val cachedFile = imageCacheRepository.getCachedImageFile(url)
            if (cachedFile != null) {
                Log.d("CachedImageInterceptor", "Using cached image: $url")
                // 使用缓存的图片
                return SuccessResult(
                    drawable = cachedFile.inputStream().use {
                        BitmapFactory.decodeStream(it)
                    }.toDrawable(request.context.resources),
                    request = request,
                    dataSource = DataSource.DISK // 表示图片来自磁盘缓存
                )
            }

            // 获取或创建此URL的互斥锁
            val mutex = mutexesLock.withLock {
                downloadMutexes.getOrPut(url) { Mutex() }
            }

            // 使用互斥锁确保同一URL只下载一次
            return mutex.withLock {
                // 再次检查缓存（可能在等待锁的过程中已被其他线程下载）
                val cachedFileAfterLock = imageCacheRepository.getCachedImageFile(url)
                if (cachedFileAfterLock != null) {
                    Log.d("CachedImageInterceptor", "Using cached image after lock: $url")
                    return@withLock SuccessResult(
                        drawable = cachedFileAfterLock.inputStream().use {
                            BitmapFactory.decodeStream(it)
                        }.toDrawable(request.context.resources),
                        request = request,
                        dataSource = DataSource.DISK // 表示图片来自磁盘缓存
                    )
                }

                // 下载并缓存
                val downloadedFile = imageCacheRepository.downloadAndCacheImage(
                    url = url,
                    videoId = videoId,
                    imageType = imageType,
                    description = description
                )

                if (downloadedFile != null) {
                    Log.d("CachedImageInterceptor", "Downloaded and cached image: $url")
                    return@withLock SuccessResult(
                        drawable = downloadedFile.inputStream().use {
                            BitmapFactory.decodeStream(it)
                        }.toDrawable(request.context.resources),
                        request = request,
                        dataSource = DataSource.NETWORK // 表示图片来自网络
                    )
                }

                // 如果下载失败，继续原始请求
                Log.w("CachedImageInterceptor", "Failed to download image, proceeding with original request: $url")
                chain.proceed(request)
            }
        }

        // 默认处理
        return chain.proceed(request)
    }
}
