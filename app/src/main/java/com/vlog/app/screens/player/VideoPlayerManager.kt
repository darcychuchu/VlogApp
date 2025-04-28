package com.vlog.app.screens.player

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 视频播放器管理器 - 单例类，用于管理播放状态
 */
object VideoPlayerManager {

    // 当前播放的视频 URL
    private val _currentUrl = MutableStateFlow<String?>(null)
    val currentUrl: StateFlow<String?> = _currentUrl.asStateFlow()

    // 当前播放位置（毫秒）
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    // 是否正在播放
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // 是否处于全屏模式
    private val _isFullScreen = MutableStateFlow(false)
    val isFullScreen: StateFlow<Boolean> = _isFullScreen.asStateFlow()

    /**
     * 设置当前播放的视频 URL
     */
    fun setCurrentUrl(url: String?) {
        _currentUrl.value = url
        Log.d("VideoPlayerManager", "Set current URL: $url")
    }

    /**
     * 更新播放位置
     */
    fun updatePosition(position: Long) {
        _currentPosition.value = position
    }

    /**
     * 设置播放状态
     */
    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
        Log.d("VideoPlayerManager", "Set playing: $playing")
    }

    /**
     * 设置全屏状态
     */
    fun setFullScreen(fullScreen: Boolean) {
        _isFullScreen.value = fullScreen
        Log.d("VideoPlayerManager", "Set fullscreen: $fullScreen")
    }

    /**
     * 保存当前播放状态
     */
    fun savePlaybackState(url: String, position: Long, isPlaying: Boolean) {
        _currentUrl.value = url
        _currentPosition.value = position
        _isPlaying.value = isPlaying
        Log.d("VideoPlayerManager", "Saved playback state: url=$url, position=$position, isPlaying=$isPlaying")
    }

    /**
     * 重置播放状态
     */
    fun reset() {
        _currentUrl.value = null
        _currentPosition.value = 0L
        _isPlaying.value = false
        _isFullScreen.value = false
        Log.d("VideoPlayerManager", "Reset playback state")
    }

    /**
     * 更新播放地址
     * 当选择新的剧集时调用此方法
     */
    fun updatePlayerUrl(url: String) {
        // 如果 URL 与当前 URL 不同，则更新
        if (_currentUrl.value != url) {
            _currentUrl.value = url
            _currentPosition.value = 0L // 从头开始播放
            _isPlaying.value = true // 自动开始播放
            Log.d("VideoPlayerManager", "Updated player URL: $url")
        }
    }
}
