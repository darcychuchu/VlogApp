package com.vlog.app.screens.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
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
 * 全屏播放器屏幕
 */
@UnstableApi
@Composable
fun FullPlayerScreen(
    videoUrl: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // 播放器状态
    var isPlaying by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    // 设置屏幕方向
    LaunchedEffect(isFullScreen) {
        activity?.requestedOrientation = if (isFullScreen) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // 创建 ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .build()
            .apply {
                val dataSourceFactory = DefaultDataSource.Factory(context)
                val mediaItem = MediaItem.fromUri(videoUrl)
                val mediaSource = if (videoUrl.contains(".m3u8")) {
                    HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(mediaItem)
                } else {
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(mediaItem)
                }

                setMediaSource(mediaSource)
                prepare()

                // 检查是否有保存的播放位置
                val savedPosition = if (videoUrl == VideoPlayerManager.currentUrl.value) {
                    VideoPlayerManager.currentPosition.value
                } else {
                    0L
                }

                // 设置播放位置
                if (savedPosition > 0) {
                    seekTo(savedPosition)
                    Log.d("FullPlayerScreen", "Seeking to saved position: $savedPosition")
                }

                // 更新播放器管理器
                VideoPlayerManager.setCurrentUrl(videoUrl)

                playWhenReady = true
            }
    }

    // 监听播放器状态
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = exoPlayer.duration
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                // 更新播放状态
                VideoPlayerManager.setPlaying(playing)
            }
        }

        exoPlayer.addListener(listener)

        // 定时更新播放进度
        val progressUpdateRunnable = object : Runnable {
            override fun run() {
                if (exoPlayer.isPlaying) {
                    currentPosition = exoPlayer.currentPosition
                    // 更新播放进度
                    VideoPlayerManager.updatePosition(currentPosition)
                }
                // 使用 Handler 定时器替代 createMessage
                Handler(Looper.getMainLooper()).postDelayed({
                    this.run()
                }, 1000) // 每秒更新一次
            }
        }

        // 开始定时更新
        Handler(Looper.getMainLooper()).postDelayed({
            progressUpdateRunnable.run()
        }, 1000) // 1秒后执行

        onDispose {
            // 保存当前播放状态
            if (exoPlayer.currentMediaItem != null) {
                val currentUrl = exoPlayer.currentMediaItem?.localConfiguration?.uri.toString()
                val position = exoPlayer.currentPosition
                val isPlaying = exoPlayer.isPlaying
                VideoPlayerManager.savePlaybackState(currentUrl, position, isPlaying)
                Log.d("FullPlayerScreen", "Saved state on dispose: pos=$position, playing=$isPlaying")
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
            // 确保在组件销毁时重置全屏状态
            VideoPlayerManager.setFullScreen(false)
            // 恢复屏幕方向
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // 自动隐藏控制器
    LaunchedEffect(showControls) {
        if (showControls && !isLocked) {
            delay(5000)
            showControls = false
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                if (!isLocked) {
                    showControls = !showControls
                }
            }
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

        // 自定义控制器
        if (showControls || isLocked) {
            // 锁定按钮 (始终显示)
            IconButton(
                onClick = {
                    isLocked = !isLocked
                    if (isLocked) {
                        showControls = false
                    } else {
                        showControls = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = if (isLocked) "解锁" else "锁定",
                    tint = if (isLocked) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // 其他控制器 (仅在未锁定时显示)
        if (showControls && !isLocked) {
            // 顶部控制栏
            IconButton(
                onClick = {
                    // 退出全屏
                    VideoPlayerManager.setFullScreen(false)
                    // 恢复屏幕方向
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    // 调用返回回调
                    onBackClick()
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
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
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

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
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

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
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // 底部控制栏
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
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
                        color = Color.White
                    )

                    Text(
                        text = " / ",
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    // 总时长
                    Text(
                        text = formatDuration(duration),
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f)
                    )

                    // 全屏按钮
                    IconButton(
                        onClick = {
                            isFullScreen = !isFullScreen
                            if (!isFullScreen) {
                                // 退出全屏
                                VideoPlayerManager.setFullScreen(false)
                                // 恢复屏幕方向
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                // 调用返回回调
                                onBackClick()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullScreen) "退出全屏" else "全屏",
                            tint = Color.White
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
