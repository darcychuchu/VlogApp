package com.vlog.app.screens.player

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.Surface
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.vlog.app.ui.theme.VlogAppTheme
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/**
 * 统一播放器活动 - 可以在全屏和非全屏模式之间切换
 */
class UnifiedPlayerActivity : ComponentActivity() {

    private var isFullScreen = false
    private var videoUrl = ""
    private var videoTitle = ""
    private var initialPosition = 0L
    private var currentPosition = 0L
    private var wasPlaying = true

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取视频 URL 和全屏模式
        if (savedInstanceState != null) {
            // 从 savedInstanceState 恢复状态
            videoUrl = savedInstanceState.getString(EXTRA_VIDEO_URL, "")
            isFullScreen = savedInstanceState.getBoolean(EXTRA_FULLSCREEN, false)
            videoTitle = savedInstanceState.getString(EXTRA_VIDEO_TITLE, "")
            initialPosition = savedInstanceState.getLong(EXTRA_POSITION, 0L)
            currentPosition = savedInstanceState.getLong("current_position", 0L)
            wasPlaying = savedInstanceState.getBoolean("was_playing", true)
        } else {
            // 从 intent 获取状态
            videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL) ?: ""
            isFullScreen = intent.getBooleanExtra(EXTRA_FULLSCREEN, false)
            videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: ""
            initialPosition = intent.getLongExtra(EXTRA_POSITION, 0L)
            currentPosition = initialPosition
            wasPlaying = true
        }

        // 设置全屏模式
        if (isFullScreen) {
            setupFullScreen()
        }

        setContent {
            VlogAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UnifiedPlayerScreen(
                        videoUrl = videoUrl,
                        videoTitle = videoTitle,
                        initialPosition = if (savedInstanceState != null) currentPosition else initialPosition,
                        initialPlaying = if (savedInstanceState != null) wasPlaying else true,
                        initialFullScreen = isFullScreen,
                        onBackClick = { finish() },
                        onFullScreenChanged = { fullScreen ->
                            isFullScreen = fullScreen
                            if (fullScreen) {
                                setupFullScreen()
                            } else {
                                exitFullScreen()
                            }
                        }
                    )
                }
            }
        }
    }

    /**
     * 设置全屏
     */
    private fun setupFullScreen() {
        // 设置横屏
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // 隐藏系统 UI
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    /**
     * 退出全屏
     */
    private fun exitFullScreen() {
        // 设置竖屏
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // 显示系统 UI
        WindowCompat.setDecorFitsSystemWindows(window, true)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // 保存播放器状态
        outState.putString(EXTRA_VIDEO_URL, videoUrl)
        outState.putBoolean(EXTRA_FULLSCREEN, isFullScreen)
        outState.putString(EXTRA_VIDEO_TITLE, videoTitle)
        outState.putLong(EXTRA_POSITION, initialPosition)
        outState.putLong("current_position", VideoPlayerManager.currentPosition.value)
        outState.putBoolean("was_playing", VideoPlayerManager.isPlaying.value)
    }

    override fun onResume() {
        super.onResume()
        // 确保全屏状态正确
        if (isFullScreen) {
            setupFullScreen()
        } else {
            exitFullScreen()
        }
        VideoPlayerManager.setFullScreen(isFullScreen)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 重置全屏状态
        if (isFinishing) {
            VideoPlayerManager.setFullScreen(false)
        }
    }

    companion object {
        const val EXTRA_VIDEO_URL = "extra_video_url"
        const val EXTRA_FULLSCREEN = "extra_fullscreen"
        const val EXTRA_VIDEO_TITLE = "extra_video_title"
        const val EXTRA_POSITION = "extra_position"
    }
}

/**
 * 统一播放器屏幕
 */
@UnstableApi
@Composable
fun UnifiedPlayerScreen(
    videoUrl: String,
    videoTitle: String = "",
    initialPosition: Long = 0,
    initialPlaying: Boolean = true,
    initialFullScreen: Boolean = false,
    onBackClick: () -> Unit,
    onFullScreenChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current

    // 播放器状态
    var isPlaying by remember { mutableStateOf(initialPlaying) }
    var isLocked by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(initialFullScreen) }
    var showControls by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(initialPosition) }
    var duration by remember { mutableLongStateOf(0L) }

    // 创建 ExoPlayer
    val exoPlayer = remember(videoUrl) { // 添加 videoUrl 作为 key，确保 URL 变化时重新创建播放器
        Log.d("UnifiedPlayerScreen", "Creating new ExoPlayer for URL: $videoUrl")
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .build()
            .apply {
                val dataSourceFactory = DefaultDataSource.Factory(context)
                val mediaItem = MediaItem.fromUri(videoUrl)
                val mediaSource = if (videoUrl.contains(".m3u8") || videoUrl.endsWith("/index.m3u8")) {
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
                    Log.d("UnifiedPlayerScreen", "Seeking to initial position: $initialPosition")
                }

                // 更新播放器管理器
                VideoPlayerManager.setCurrentUrl(videoUrl)
                VideoPlayerManager.updatePosition(initialPosition)

                playWhenReady = initialPlaying
            }
    }

    // 监听全屏状态变化
    LaunchedEffect(isFullScreen) {
        onFullScreenChanged(isFullScreen)
        VideoPlayerManager.setFullScreen(isFullScreen)
    }

    // 监听播放器状态
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = exoPlayer.duration
                    Log.d("UnifiedPlayerScreen", "Video duration: $duration ms")
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
                Log.d("UnifiedPlayerScreen", "Saved state on dispose: pos=$position, playing=$isPlaying")
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (videoTitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = videoTitle,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
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
                    .background(Color.Black.copy(alpha = 0.5f))
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
