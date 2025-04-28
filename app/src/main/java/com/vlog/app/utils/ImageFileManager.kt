package com.vlog.app.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.absoluteValue

/**
 * 图片文件管理器
 * 负责图片文件的存储和删除
 */
class ImageFileManager(private val context: Context) {
    
    private val cacheDir = File(context.cacheDir, "image_cache").apply { 
        if (!exists()) mkdirs() 
    }
    
    /**
     * 根据URL获取图片文件
     */
    fun getImageFile(url: String): File {
        val fileName = generateFileName(url)
        return File(cacheDir, fileName)
    }
    
    /**
     * 根据URL生成唯一的文件名
     */
    private fun generateFileName(url: String): String {
        return "${url.hashCode().absoluteValue}.jpg"
    }
    
    /**
     * 将图片数据保存到文件
     */
    suspend fun saveImageToFile(url: String, imageBytes: ByteArray): File {
        val file = getImageFile(url)
        withContext(Dispatchers.IO) {
            try {
                file.outputStream().use { it.write(imageBytes) }
            } catch (e: Exception) {
                Log.e("ImageFileManager", "Error saving image to file: ${e.message}")
                throw e
            }
        }
        return file
    }
    
    /**
     * 删除图片文件
     */
    fun deleteFile(path: String): Boolean {
        val file = File(path)
        return if (file.exists()) {
            try {
                file.delete()
            } catch (e: Exception) {
                Log.e("ImageFileManager", "Error deleting file: ${e.message}")
                false
            }
        } else {
            // 文件不存在，视为删除成功
            true
        }
    }
    
    /**
     * 清空缓存目录
     */
    fun clearCache(): Boolean {
        return try {
            cacheDir.listFiles()?.forEach { it.delete() }
            true
        } catch (e: Exception) {
            Log.e("ImageFileManager", "Error clearing cache: ${e.message}")
            false
        }
    }
    
    /**
     * 获取缓存目录大小
     */
    fun getCacheSize(): Long {
        var size = 0L
        cacheDir.listFiles()?.forEach { size += it.length() }
        return size
    }
}
