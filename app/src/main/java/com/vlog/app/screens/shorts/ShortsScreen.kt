package com.vlog.app.screens.shorts

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vlog.app.R
import com.vlog.app.data.api.ApiConstants
import com.vlog.app.data.videos.Video
import com.vlog.app.navigation.NavigationRoutes
import com.vlog.app.navigation.NavigationRoutes.FullScreenRoute.VideoPlayer
import com.vlog.app.screens.components.CommonTopBar
import com.vlog.app.screens.components.ErrorView
import com.vlog.app.screens.components.LoadingView

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: ShortsViewModel = viewModel(factory = ShortsViewModelFactory(context.applicationContext as Application))
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CommonTopBar(
                title = "影视解说",
                navController = navController,
                currentRoute = NavigationRoutes.MainRoute.Shorts.route
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingView(modifier = Modifier.padding(paddingValues))
            }
            uiState.error != null -> {
                ErrorView(
                    message = uiState.error ?: stringResource(R.string.loading_error),
                    onRetry = { viewModel.loadShorts() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            uiState.shorts.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(R.string.no_results))
                }
            }
            else -> {
                ShortsContent(
                    shorts = uiState.shorts,
                    currentIndex = uiState.currentShortIndex,
                    onIndexChange = viewModel::setCurrentShort,
                    modifier = Modifier.padding(paddingValues),
                    viewModel = viewModel,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortsContent(
    shorts: List<Video>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShortsViewModel
) {
    val pagerState = rememberPagerState(
        initialPage = currentIndex,
        pageCount = { shorts.size }
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onIndexChange(page)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true // 允许用户滑动
        ) { page ->
            // 当页面变化时通知 ViewModel
            LaunchedEffect(page) {
                viewModel.onPageChanged(page)
            }
            ShortItem(
                video = shorts[page],
                currentPageIndex = page,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 加载更多指示器
        val uiState by viewModel.uiState.collectAsState()
        if (uiState.isLoadingMore) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ShortItem(
    video: Video,
    currentPageIndex: Int, // 添加当前页面索引参数
    modifier: Modifier = Modifier
) {
    // 获取当前的 ViewModel 和状态
    val viewModel = viewModel<ShortsViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val videoDetail = uiState.currentVideoDetail
    val context = LocalContext.current
    // 创建 ExoPlayer 实例
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    // 当页面变化时停止视频播放
    DisposableEffect(currentPageIndex) {
        onDispose {
            exoPlayer.release()
        }
    }


    val currentPlayerUrl = videoDetail?.getFirstPlayUrl() ?: videoDetail?.playerUrl

    if (currentPlayerUrl != null) {
        LaunchedEffect(currentPlayerUrl) {
            val mediaItem = MediaItem.Builder()
                .setUri(currentPlayerUrl.toUri())
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }

    Box(modifier = modifier) {
        // 背景图片
        AsyncImage(
            model = video.coverUrl ?: "${ApiConstants.IMAGE_BASE_URL}${video.cover}",
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 半透明遮罩，增强文字可读性
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        ),
                        startY = 300f
                    )
                )
        )

        // 视频播放器（如果有播放 URL 并且允许播放）
        if (currentPlayerUrl != null) {
            // 在页面中间放置视频播放器
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize()
                    //.fillMaxWidth(0.9f) // 占据 90% 宽度
                    .aspectRatio(16f/9f) // 16:9 的宽高比
            ) {
                // 使用 EmbeddedPlayerView 播放视频
                if (uiState.shouldPlayVideo && uiState.currentShortIndex == currentPageIndex) {
                    // 只在当前页面是活动页面时播放视频
                    key(videoDetail?.id) { // 使用 key 确保在视频变化时重新创建播放器

                        Box(modifier = Modifier
                            .fillMaxWidth().aspectRatio(16f/9f)) {
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        player = exoPlayer
                                        useController = true
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                    }
                } else {
                    // 显示视频的封面图
                    AsyncImage(
                        model = video.coverUrl ?: if (video.cover.isNotEmpty()) "${ApiConstants.IMAGE_BASE_URL}${video.cover}" else null,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // 显示播放按钮
                    IconButton(
                        onClick = { viewModel.onPageChanged(currentPageIndex) },
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "播放",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }

        // 视频信息在底部显示
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 描述
            if (video.description != null) {
                Text(
                    text = video.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 地区和评分
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.Yellow,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = video.score,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )

                if (video.region != null) {
                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "|",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = video.region,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // 加载中指示器
        if (uiState.isLoadingDetail) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}


