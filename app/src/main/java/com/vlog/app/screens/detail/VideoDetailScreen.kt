package com.vlog.app.screens.detail

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.vlog.app.navigation.NavigationRoutes
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vlog.app.R
import com.vlog.app.data.api.ApiConstants
import com.vlog.app.data.videos.Player
import com.vlog.app.data.videos.Video
import com.vlog.app.screens.components.ErrorView
import com.vlog.app.screens.components.LoadingView


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(
    navController: NavController,
    videoId: String,
    viewModel: VideoDetailViewModel = viewModel(factory = VideoDetailViewModelFactory(videoId))
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var isFullScreen by remember { mutableStateOf(false) }
    // 创建 ExoPlayer 实例
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    // 处理屏幕方向切换
    LaunchedEffect(isFullScreen) {
        val activity = context as Activity
        activity.requestedOrientation = if (isFullScreen) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // 释放ExoPlayer资源
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }


    // 显示服务商对话框
    if (uiState.showGatherDialog) {
        GatherDialog(
            gathers = uiState.gathers,
            selectedGatherId = uiState.selectedGatherId,
            onGatherSelected = { gatherId ->
                viewModel.loadPlayers(gatherId)
                viewModel.hideGatherDialog()
            },
            onDismiss = { viewModel.hideGatherDialog() }
        )
    }

    // 显示播放列表对话框
    if (uiState.showPlayerDialog) {
        val gatherName = uiState.gathers.find { it.id == uiState.selectedGatherId }?.title
        PlayerDialog(
            players = uiState.players,
            selectedPlayerUrl = uiState.selectedPlayerUrl,
            gatherName = gatherName,
            onPlayerSelected = { playerUrl, playerTitle ->
                viewModel.selectPlayerUrl(playerUrl, playerTitle)
                viewModel.hidePlayerDialog()
            },
            onDismiss = { viewModel.hidePlayerDialog() }
        )
    }

    // 显示服务商和播放列表整合对话框
    if (uiState.showGatherAndPlayerDialog) {
        GatherAndPlayerDialog(
            gathers = uiState.gathers,
            players = uiState.players,
            selectedGatherId = uiState.selectedGatherId,
            selectedPlayerUrl = uiState.selectedPlayerUrl,
            isLoadingPlayers = uiState.isLoadingPlayers,
            onGatherSelected = { gatherId ->
                viewModel.loadPlayers(gatherId)
            },
            onPlayerSelected = { playerUrl, playerTitle ->
                viewModel.selectPlayerUrl(playerUrl, playerTitle)
            },
            onDismiss = { viewModel.hideGatherAndPlayerDialog() }
        )
    }

    val videoDetail = uiState.videoDetail
    val currentPlayerUrl = uiState.selectedPlayerUrl ?: videoDetail?.getFirstPlayUrl() ?: videoDetail?.playerUrl
    if (videoDetail != null) {
        LaunchedEffect(currentPlayerUrl) {
            val mediaItems = mutableListOf<MediaItem>()
            var currentPlayerIndex = 0
            if (uiState.players.isNotEmpty()) {
                uiState.players.forEachIndexed { index, player ->
                    val mediaItem = MediaItem.Builder()
                        .setUri(player.playerUrl.toUri())
                        .setMediaId(player.videoTitle)
                        //.setTag(player.videoTitle)
                        .build()
                    mediaItems.add(mediaItem)
                    if (player.playerUrl == currentPlayerUrl) {
                        currentPlayerIndex = index
                    }
                }
            }
            exoPlayer.setMediaItems(mediaItems)
            exoPlayer.seekTo(currentPlayerIndex, 0)
            exoPlayer.prepare()
        }
    }

    if (isFullScreen) {
        // 全屏模式下只显示播放器
        VideoPlayer(
            exoPlayer = exoPlayer,
            isFullScreen = isFullScreen,
            onFullScreenChange = { newValue -> isFullScreen = newValue }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = uiState.videoDetail?.title ?: stringResource(R.string.video_detail)) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
            ) {
                when {
                    uiState.isLoading -> {
                        LoadingView(modifier = Modifier.padding(paddingValues))
                    }
                    uiState.error != null -> {
                        ErrorView(
                            message = uiState.error ?: stringResource(R.string.loading_error),
                            onRetry = { viewModel.loadVideoDetail() },
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                    uiState.videoDetail != null -> {



                        // 主内容
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                key(currentPlayerUrl) {
                                    // 视频播放器
                                    VideoPlayer(
                                        exoPlayer = exoPlayer,
                                        isFullScreen = isFullScreen,
                                        onFullScreenChange = { newValue -> isFullScreen = newValue }
                                    )
                                }


                            }


                            // 播放控制区域
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 左侧：详情/评论切换
                                TabRow(
                                    selectedTabIndex = uiState.selectedTab,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Tab(
                                        selected = uiState.selectedTab == 0,
                                        onClick = { viewModel.selectTab(0) },
                                        text = { Text("详情") }
                                    )
                                    Tab(
                                        selected = uiState.selectedTab == 1,
                                        onClick = { viewModel.selectTab(1) },
                                        text = { Text("评论 (${uiState.comments.size})") }
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // 线路和选集整合按钮
                                Button(
                                    onClick = { viewModel.showGatherAndPlayerDialog() },
                                    modifier = Modifier.wrapContentWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.List,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("选集/线路")
                                }
                            }

                            // 内容区域：详情或评论
                            when (uiState.selectedTab) {
                                0 -> {
                                    // 详情
                                    VideoDetailContent(videoDetail = videoDetail!!)

                                    // 当前服务商的播放列表
                                    if (uiState.players.isNotEmpty()) {
                                        CurrentPlaylist(
                                            players = uiState.players,
                                            selectedPlayerUrl = uiState.selectedPlayerUrl,
                                            gatherName = uiState.gathers.find { it.id == uiState.selectedGatherId }?.title,
                                            onPlayerSelected = { playerUrl, playerTitle ->
                                                viewModel.selectPlayerUrl(playerUrl, playerTitle)
                                            }
                                        )
                                    }
                                }
                                1 -> {
                                    // 评论
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(400.dp) // 固定高度防止无限约束
                                    ) {
                                        CommentSection(
                                            comments = uiState.comments,
                                            isLoading = uiState.isLoadingComments,
                                            onPostComment = { content ->
                                                viewModel.postComment(content)
                                            }
                                        )
                                    }
                                }
                            }

                            // 推荐视频
                            RecommendedVideos(
                                videos = uiState.recommendedVideos,
                                isLoading = uiState.isLoadingRecommendations,
                                onVideoClick = { videoId ->
                                    navController.navigate("video/$videoId")
                                }
                            )

                            // 底部间距
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }


    }


}


/**
 * 视频详情内容
 */
@Composable
fun VideoDetailContent(
    videoDetail: Video
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = videoDetail.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 基本信息
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "评分: ${videoDetail.score}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            videoDetail.region?.let {
                Text(
                    text = "地区: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            videoDetail.released?.let {
                Text(
                    text = "年份: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 详细描述
        Text(
            text = "简介",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = videoDetail.description ?: "",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * 当前播放列表
 */
@Composable
fun CurrentPlaylist(
    players: List<Player>,
    selectedPlayerUrl: String?,
    gatherName: String?,
    onPlayerSelected: (String, String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "剧集列表" + (gatherName?.let { " - $it" } ?: ""),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 播放列表横向滚动
        // 使用 rememberLazyListState 来控制滚动位置
        val listState = rememberLazyListState()

        // 自动滚动到选中的剧集
        LaunchedEffect(selectedPlayerUrl) {
            selectedPlayerUrl?.let { url ->
                val selectedIndex = players.indexOfFirst { it.playerUrl == url }
                if (selectedIndex >= 0) {
                    listState.animateScrollToItem(selectedIndex)
                }
            }
        }

        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(players) { player ->
                val isSelected = player.playerUrl == selectedPlayerUrl

                Surface(
                    modifier = Modifier
                        .height(36.dp)
                        .wrapContentWidth()
                        .clickable { onPlayerSelected(player.playerUrl, player.videoTitle) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = if (isSelected) 4.dp else 0.dp
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = player.videoTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("SetTextI18n")
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    exoPlayer: ExoPlayer,
    isFullScreen: Boolean,
    onFullScreenChange: (Boolean) -> Unit
) {
    // 跟踪当前媒体项ID的状态
    var currentMediaId by remember { mutableStateOf(exoPlayer.currentMediaItem?.mediaId ?: "未知") }

    // 媒体项变化时的回调
    val onMediaItemChanged = { id: String ->
        currentMediaId = id
    }
    Box(modifier = Modifier
        .fillMaxWidth()
        .then(if (isFullScreen) Modifier.fillMaxSize() else Modifier.aspectRatio(16f/9f))) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setFullscreenButtonClickListener { onFullScreenChange(!isFullScreen) }
                    setShowPreviousButton(true)
                    setShowNextButton(true)



                    // 添加监听器以便在Compose中更新标题
                    exoPlayer.addListener(object : androidx.media3.common.Player.Listener {
                        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                            // 当媒体项变化时通知Compose更新
                            onMediaItemChanged(mediaItem?.mediaId ?: "未知")
                        }
                    })

//                    // 创建一个TextView并添加到PlayerView
//                    val titleTextView = TextView(ctx).apply {
//                        text = "当前播放: ${exoPlayer.currentMediaItem?.mediaId}"
//                        gravity = Gravity.CENTER
//                        setPadding(16, 8, 16, 8)
//                    }
//
//                    // 设置TextView的布局参数
//                    val params = FrameLayout.LayoutParams(
//                        FrameLayout.LayoutParams.MATCH_PARENT,
//                        FrameLayout.LayoutParams.WRAP_CONTENT
//                    ).apply {
//                        gravity = Gravity.TOP // 放在顶部
//                    }
//
//                    // 添加到PlayerView
//                    addView(titleTextView, params)
//
//
//                    exoPlayer.addListener(object : androidx.media3.common.Player.Listener {
//                        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
//                            // 当媒体项变化时更新文本
//                            titleTextView.text = "当前播放: ${mediaItem?.mediaId ?: "未知"}"
//                        }
//
//                        override fun onPlaybackStateChanged(playbackState: Int) {
//                            // 当播放状态变化时也更新文本(例如初始加载完成)
//                            if (playbackState == androidx.media3.common.Player.STATE_READY) {
//                                titleTextView.text = "当前播放: ${exoPlayer.currentMediaItem?.mediaId ?: "未知"}"
//                            }
//                        }
//                    })
//
//                    // 立即更新一次文本(如果ExoPlayer已经加载了媒体项)
//                    post {
//                        titleTextView.text = "当前播放: ${exoPlayer.currentMediaItem?.mediaId ?: "未知"}"
//                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 添加全屏返回按钮
        if (isFullScreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    //.background(Color.Black.copy(alpha = 0.5f)) // 半透明背景
                    .padding(4.dp)
                    .align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 返回按钮
                IconButton(
                    onClick = { onFullScreenChange(false) }
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "退出全屏",
                        tint = Color.White
                    )
                }

                // 标题文本
                Text(
                    text = "当前播放: ${currentMediaId}",
                    color = Color.White,
                    modifier = Modifier.padding(start = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
