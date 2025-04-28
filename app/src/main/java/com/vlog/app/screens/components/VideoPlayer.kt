package com.vlog.app.screens.components

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.vlog.app.screens.player.VideoPlayerManager

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = false,  // 新增全屏状态参数
    onFullScreenToggle: (() -> Unit)? = null,  // 全屏切换回调
    onProgressUpdate: ((position: Long, duration: Long) -> Unit)? = null,  // 进度更新回调
    initialPosition: Long = 0  // 初始播放位置
) {
    val context = LocalContext.current

    // 检查是否有保存的播放位置
    val savedPosition = if (url == VideoPlayerManager.currentUrl.value) {
        VideoPlayerManager.currentPosition.value
    } else {
        initialPosition
    }

    // 创建 ExoPlayer 实例
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // 设置播放器属性
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF

            try {
                // 打印 URL 信息便于调试
                Log.d("VideoPlayer", "Attempting to play URL: $url")

                // 根据 URL 类型创建不同的 MediaSource
                val mediaItem = MediaItem.fromUri(url)
                val dataSourceFactory = DefaultDataSource.Factory(context)

                val mediaSource = if (url.contains(".m3u8") || url.endsWith("/index.m3u8")) {
                    Log.d("VideoPlayer", "Using HlsMediaSource for m3u8 format")
                    // 对于 HLS (m3u8) 格式使用 HlsMediaSource
                    HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(mediaItem)
                } else {
                    Log.d("VideoPlayer", "Using ProgressiveMediaSource for other format")
                    // 对于其他格式使用 ProgressiveMediaSource
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(mediaItem)
                }

                // 重置播放器状态
                stop()
                clearMediaItems()

                setMediaSource(mediaSource)
                prepare()

                // 设置播放位置
                if (savedPosition > 0) {
                    seekTo(savedPosition)
                    Log.d("VideoPlayer", "Seeking to saved position: $savedPosition")
                }

                // 更新播放器管理器
                VideoPlayerManager.setCurrentUrl(url)

                play() // 确保开始播放
                Log.d("VideoPlayer", "Player prepared successfully")
            } catch (e: Exception) {
                Log.e("VideoPlayer", "Error initializing player: ${e.message}", e)
            }
        }
    }

    // 添加播放进度监听器
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    // 当播放器准备就绪时，获取总时长
                    val duration = exoPlayer.duration
                    Log.d("VideoPlayer", "Video duration: $duration ms")
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // 更新播放状态
                VideoPlayerManager.setPlaying(isPlaying)

                if (isPlaying) {
                    // 当开始播放时，启动定期更新进度
                    Log.d("VideoPlayer", "Video started playing")
                }
            }
        }

        exoPlayer.addListener(listener)

        // 创建定时器，每 1 秒更新一次进度
        val progressUpdateRunnable = object : Runnable {
            override fun run() {
                if (exoPlayer.isPlaying) {
                    val position = exoPlayer.currentPosition
                    val duration = exoPlayer.duration
                    if (duration > 0) { // 避免除以零错误
                        // 更新播放进度
                        VideoPlayerManager.updatePosition(position)
                        onProgressUpdate?.invoke(position, duration)
                        Log.d("VideoPlayer", "Progress: $position/$duration")
                    }
                }
                // 使用 Handler 定时器替代 createMessage
                Handler(Looper.getMainLooper()).postDelayed({
                    this.run()
                }, 1000) // 1 秒后再次执行
            }
        }

        // 开始定时更新
        Handler(Looper.getMainLooper()).postDelayed({
            progressUpdateRunnable.run()
        }, 1000) // 1 秒后执行

        onDispose {
            // 保存当前播放状态
            if (exoPlayer.currentMediaItem != null) {
                val currentUrl = exoPlayer.currentMediaItem?.localConfiguration?.uri.toString()
                val position = exoPlayer.currentPosition
                val isPlaying = exoPlayer.isPlaying
                VideoPlayerManager.savePlaybackState(currentUrl, position, isPlaying)
                Log.d("VideoPlayer", "Saved state on dispose: pos=$position, playing=$isPlaying")
            }

            exoPlayer.removeListener(listener)
        }
    }

    // 使用 AndroidView 包装 PlayerView
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)

                // 设置 PlayerView 属性
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL

                setFullscreenButtonClickListener {
                    onFullScreenToggle?.invoke()
                }
            }
        },

        modifier = if (isFullScreen) {
            modifier
                .fillMaxSize()
                .rotate(90f) // 旋转90度实现横屏效果
        } else {
            modifier
                .fillMaxWidth()
                .aspectRatio(16f/9f)
        }
    )

    // 当组件离开组合时释放资源
    DisposableEffect(key1 = Unit) {
        onDispose {
            Log.d("VideoPlayer", "Releasing player resources")
            exoPlayer.release()
        }
    }
}
