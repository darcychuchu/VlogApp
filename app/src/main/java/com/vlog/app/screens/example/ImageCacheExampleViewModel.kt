package com.vlog.app.screens.example

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.VlogApp
import com.vlog.app.data.images.ImageType
import com.vlog.app.data.images.ImageCacheRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * 图片缓存示例ViewModel
 * 展示如何在业务逻辑中使用图片缓存仓库
 */
class ImageCacheExampleViewModel : ViewModel() {
    
    private val imageCacheRepository: ImageCacheRepository = VlogApp.getInstance().imageCacheRepository
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    /**
     * UI状态
     */
    data class UiState(
        val isLoading: Boolean = false,
        val cacheStats: ImageCacheRepository.CacheStats? = null,
        val videoPosters: Map<String, List<File>> = emptyMap(),
        val error: String? = null
    )
    
    /**
     * 加载缓存统计信息
     */
    fun loadCacheStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val stats = imageCacheRepository.getCacheStats()
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        cacheStats = stats
                    )
                }
                Log.d("ImageCacheExample", "Cache stats: $stats")
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to load cache stats: ${e.message}"
                    )
                }
                Log.e("ImageCacheExample", "Error loading cache stats", e)
            }
        }
    }
    
    /**
     * 加载指定视频的海报图片
     */
    fun loadVideoPosters(videoIds: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val posters = mutableMapOf<String, List<File>>()
                
                videoIds.forEach { videoId ->
                    val posterFiles = imageCacheRepository.getCachedImagesByVideoIdAndType(
                        videoId = videoId,
                        imageType = ImageType.POSTER
                    )
                    posters[videoId] = posterFiles
                }
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        videoPosters = posters
                    )
                }
                Log.d("ImageCacheExample", "Loaded posters for ${videoIds.size} videos")
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to load video posters: ${e.message}"
                    )
                }
                Log.e("ImageCacheExample", "Error loading video posters", e)
            }
        }
    }
    
    /**
     * 清除指定视频的所有图片缓存
     */
    fun clearVideoCache(videoId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                imageCacheRepository.clearVideoCache(videoId)
                
                // 刷新缓存统计和海报列表
                loadCacheStats()
                loadVideoPosters(listOf(videoId))
                
                Log.d("ImageCacheExample", "Cleared cache for video: $videoId")
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to clear video cache: ${e.message}"
                    )
                }
                Log.e("ImageCacheExample", "Error clearing video cache", e)
            }
        }
    }
    
    /**
     * 预加载视频的所有图片
     */
    fun preloadVideoImages(videoId: String, imageUrls: Map<ImageType, List<String>>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                var totalImages = 0
                var successCount = 0
                
                imageUrls.forEach { (imageType, urls) ->
                    urls.forEach { url ->
                        totalImages++
                        val file = imageCacheRepository.downloadAndCacheImage(
                            url = url,
                            videoId = videoId,
                            imageType = imageType
                        )
                        if (file != null) {
                            successCount++
                        }
                    }
                }
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = null
                    )
                }
                
                // 刷新缓存统计
                loadCacheStats()
                
                Log.d("ImageCacheExample", "Preloaded $successCount/$totalImages images for video: $videoId")
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to preload video images: ${e.message}"
                    )
                }
                Log.e("ImageCacheExample", "Error preloading video images", e)
            }
        }
    }
}
