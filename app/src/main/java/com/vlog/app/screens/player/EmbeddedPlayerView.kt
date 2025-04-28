package com.vlog.app.screens.player

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/**
 * 内嵌播放器视图
 */
@OptIn(UnstableApi::class)
@UnstableApi
@Composable
fun EmbeddedPlayerView(
    url: String,
    title: String = "",
    initialPosition: Long = 0,
    initialPlaying: Boolean = true,
    onFullScreenClick: () -> Unit,
    onProgressUpdate: (position: Long, duration: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 播放器状态
    var isPlaying by remember { mutableStateOf(initialPlaying) }
    var showControls by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(initialPosition) }
    var duration by remember { mutableLongStateOf(0L) }

    // 创建 ExoPlayer
    val exoPlayer = remember(url) { // 添加 url 作为 key，确保 URL 变化时重新创建播放器
        Log.d("EmbeddedPlayerView", "Creating new ExoPlayer for URL: $url")
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .build()
            .apply {
                val dataSourceFactory = DefaultDataSource.Factory(context)
                val mediaItem = MediaItem.fromUri(url)
                val mediaSource = if (url.contains(".m3u8") || url.endsWith("/index.m3u8")) {
                    HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(mediaItem)
                } else {
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(mediaItem)
                }

                setMediaSource(mediaSource)
                prepare()

                // 设置初始播放位置
                if (initialPosition > 0) {
                    seekTo(initialPosition)
                    Log.d("EmbeddedPlayerView", "Seeking to initial position: $initialPosition")
                }

                // 更新播放器管理器
                VideoPlayerManager.setCurrentUrl(url)
                VideoPlayerManager.updatePosition(initialPosition)

                playWhenReady = initialPlaying
            }
    }

    // 当 URL 变化时，记录日志
    LaunchedEffect(url) {
        Log.d("EmbeddedPlayerView", "URL changed: $url")
    }

    // 监听播放器状态
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = exoPlayer.duration
                    Log.d("EmbeddedPlayerView", "Video duration: $duration ms")
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                VideoPlayerManager.setPlaying(playing)
            }
        }

        exoPlayer.addListener(listener)

        // 定时更新播放进度
        val progressUpdateRunnable = object : Runnable {
            override fun run() {
                if (exoPlayer.isPlaying) {
                    currentPosition = exoPlayer.currentPosition
                    VideoPlayerManager.updatePosition(currentPosition)
                    onProgressUpdate(currentPosition, exoPlayer.duration)
                }
                Handler(Looper.getMainLooper()).postDelayed(
                    this,
                    1000
                ) // 每秒更新一次
            }
        }

        // 开始定时更新
        Handler(Looper.getMainLooper()).postDelayed(
            progressUpdateRunnable,
            1000
        ) // 1秒后执行

        onDispose {
            // 保存当前播放状态
            if (exoPlayer.currentMediaItem != null) {
                val currentUrl = exoPlayer.currentMediaItem?.localConfiguration?.uri.toString()
                val position = exoPlayer.currentPosition
                val isPlaying = exoPlayer.isPlaying
                VideoPlayerManager.savePlaybackState(currentUrl, position, isPlaying)
                Log.d("EmbeddedPlayerView", "Saved state on dispose: pos=$position, playing=$isPlaying")
            }

            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // 监听生命周期
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isPlaying) {
                        exoPlayer.play()
                    }
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 自动隐藏控制器
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000)
            showControls = false
        }
    }

    // UI
    Box(
        modifier = modifier
            .clickable { showControls = !showControls }
    ) {
        // 播放器视图
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // 使用自定义控制器
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 自定义控制器 (仅在显示控制器时显示)
        if (showControls) {
            // 顶部控制栏 (标题)
            if (title.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(8.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // 中间控制栏
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 快退按钮
                IconButton(
                    onClick = {
                        exoPlayer.seekBack()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "快退",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 播放/暂停按钮
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 快进按钮
                IconButton(
                    onClick = {
                        exoPlayer.seekForward()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "快进",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // 底部控制栏
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                // 进度条
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = {
                        currentPosition = it.toLong()
                        exoPlayer.seekTo(it.toLong())
                    },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 时间和全屏按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 当前时间
                    Text(
                        text = formatDuration(currentPosition),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = " / ",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )

                    // 总时长
                    Text(
                        text = formatDuration(duration),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )

                    // 全屏按钮
                    IconButton(
                        onClick = onFullScreenClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "全屏",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 格式化时长
 */
private fun formatDuration(durationMs: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
