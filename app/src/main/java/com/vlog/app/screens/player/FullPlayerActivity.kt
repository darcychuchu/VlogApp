package com.vlog.app.screens.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.vlog.app.ui.theme.VlogAppTheme

/**
 * 全屏播放器 Activity
 */
class FullPlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置全屏
        setupFullScreen()

        // 获取视频 URL
        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL) ?: DEFAULT_VIDEO_URL

        // 设置全屏状态
        VideoPlayerManager.setFullScreen(true)

        setContent {
            VlogAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FullPlayerScreen(
                        videoUrl = videoUrl,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }

    /**
     * 设置全屏
     */
    private fun setupFullScreen() {
        // 隐藏系统 UI
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onResume() {
        super.onResume()
        // 确保全屏
        setupFullScreen()
        VideoPlayerManager.setFullScreen(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 重置全屏状态
        VideoPlayerManager.setFullScreen(false)
    }

    companion object {
        const val EXTRA_VIDEO_URL = "extra_video_url"
        private const val DEFAULT_VIDEO_URL = "https://d1gnaphp93fop2.cloudfront.net/videos/multiresolution/rendition_new10.m3u8"
    }
}
