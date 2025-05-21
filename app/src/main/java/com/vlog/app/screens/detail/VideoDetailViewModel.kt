package com.vlog.app.screens.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vlog.app.VlogApp
import com.vlog.app.data.videos.Gather
import com.vlog.app.data.videos.Player
import com.vlog.app.data.videos.Video
import com.vlog.app.data.comments.CommentRepository
import com.vlog.app.data.videos.VideoRepository
import com.vlog.app.data.histories.watch.WatchHistoryRepository
import com.vlog.app.data.histories.watch.WatchHistory
import com.vlog.app.data.comments.Comment
import com.vlog.app.data.videos.PlayItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.TreeMap

class VideoDetailViewModel(
    private val videoId: String,
    private val videoRepository: VideoRepository = VlogApp.getInstance().videoRepository,
    private val watchHistoryRepository: WatchHistoryRepository = VlogApp.getInstance().watchHistoryRepository,
    private val commentRepository: CommentRepository = VlogApp.getInstance().commentRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(VideoDetailUiState())
    val uiState: StateFlow<VideoDetailUiState> = _uiState.asStateFlow()

    init {
        loadVideoDetail()
        loadGathers()
        loadWatchHistory()
        loadComments()
        loadRecommendedVideos()
    }

    fun loadVideoDetail() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val result = videoRepository.getVideoDetail(videoId)

            if (result.isSuccess) {
                val videoDetail = result.getOrNull()!!
                ////Log.d("VideoDetailViewModel", "Video detail loaded: $videoDetail")
                _uiState.update {
                    it.copy(
                        videoDetail = videoDetail,
                        isLoading = false
                    )
                }
            } else {
                val exception = result.exceptionOrNull()
                Log.e("VideoDetailViewModel", "Error loading video detail", exception)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = exception?.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    /**
     * 加载观看历史
     */
    private fun loadWatchHistory() {
        viewModelScope.launch {
            try {
                val watchHistory = watchHistoryRepository.getWatchHistoryById(videoId)
                ////Log.d("VideoDetailViewModel", "Watch history loaded: $watchHistory")

                if (watchHistory != null) {
                    _uiState.update {
                        it.copy(
                            watchHistory = watchHistory,
                            lastPlayedPosition = watchHistory.lastPlayedPosition
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("VideoDetailViewModel", "Error loading watch history", e)
            }
        }
    }

    /**
     * 加载视频的服务商列表
     * 从视频详情中获取服务商列表
     */
    fun loadGathers() {
        _uiState.update { it.copy(isLoadingGathers = true) }

        viewModelScope.launch {
            // 从视频详情中获取服务商列表
            val videoDetail = uiState.value.videoDetail
            if (videoDetail != null && videoDetail.isDetail()) {
                // 从视频详情中获取服务商列表
                val gathers = videoDetail.getGathers()
                ////Log.d("VideoDetailViewModel", "Gathers loaded from video detail: ${gathers.size}")

                // 获取历史记录中的服务商ID
                val historyGatherId = uiState.value.watchHistory?.gatherId

                // 如果有历史记录且服务商存在，选择该服务商
                val selectedGatherId = if (historyGatherId != null && gathers.any { it.id == historyGatherId }) {
                    historyGatherId
                } else {
                    // 如果没有历史记录或服务商不存在，选择第一个服务商
                    gathers.firstOrNull()?.id
                }

                _uiState.update {
                    it.copy(
                        gathers = gathers,
                        selectedGatherId = selectedGatherId,
                        isLoadingGathers = false
                    )
                }

                // 如果有选中的服务商，自动加载播放地址
                selectedGatherId?.let { gatherId ->
                    loadPlayers(gatherId)
                }
            } else {
                // 如果视频详情为空或不包含服务商列表，则使用新API
                val result = videoRepository.getGathersFromVideoDetail(videoId)

                if (result.isSuccess) {
                    val gathers = result.getOrNull() ?: emptyList()
                    ////Log.d("VideoDetailViewModel", "Gathers loaded from API: ${gathers.size}")

                    // 获取历史记录中的服务商ID
                    val historyGatherId = uiState.value.watchHistory?.gatherId

                    // 如果有历史记录且服务商存在，选择该服务商
                    val selectedGatherId = if (historyGatherId != null && gathers.any { it.id == historyGatherId }) {
                        historyGatherId
                    } else {
                        // 如果没有历史记录或服务商不存在，选择第一个服务商
                        gathers.firstOrNull()?.id
                    }

                    _uiState.update {
                        it.copy(
                            gathers = gathers,
                            selectedGatherId = selectedGatherId,
                            isLoadingGathers = false
                        )
                    }

                    // 如果有选中的服务商，自动加载播放地址
                    selectedGatherId?.let { gatherId ->
                        loadPlayers(gatherId)
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e("VideoDetailViewModel", "Error loading gathers", exception)
                    _uiState.update {
                        it.copy(
                            isLoadingGathers = false,
                            error = exception?.message ?: "Failed to load video sources"
                        )
                    }
                }
            }
        }
    }

    /**
     * 加载服务商的播放地址列表
     * 从视频详情中获取播放地址列表
     */
    fun loadPlayers(gatherId: String) {
        _uiState.update { it.copy(isLoadingPlayers = true, selectedGatherId = gatherId) }

        viewModelScope.launch {
            // 从视频详情中获取播放地址列表
            val videoDetail = uiState.value.videoDetail
            if (videoDetail != null && videoDetail.isDetail()) {
                // 从视频详情中获取播放地址列表
                val playList = videoDetail.getPlayListByGatherId(gatherId)

                // 将 PlayItem 转换为 Player 列表
                val players = playList.map { playItem ->
                    Player(
                        id = "0", // 新API中没有id字段，使用默认值
                        videoTitle = playItem.title,
                        playerUrl = playItem.playUrl,
                        path = playItem.path,
                        gatherId = gatherId,
                        videoId = videoId
                    )
                }

                ////Log.d("VideoDetailViewModel", "Players loaded from video detail: ${players.size}")

                // 获取历史记录中的播放地址
                val historyPlayerUrl = uiState.value.watchHistory?.playerUrl
                val historyGatherId = uiState.value.watchHistory?.gatherId

                // 如果有历史记录且是当前服务商的播放地址，选择该地址
                val selectedPlayerUrl = if (historyPlayerUrl != null && historyGatherId == gatherId && players.any { it.playerUrl == historyPlayerUrl }) {
                    historyPlayerUrl
                } else {
                    // 如果没有历史记录或播放地址不存在，选择第一个地址
                    players.firstOrNull()?.playerUrl
                }

                // 获取选中播放地址的标题
                val selectedPlayer = players.find { it.playerUrl == selectedPlayerUrl }
                val gatherName = uiState.value.gathers.find { it.id == gatherId }?.title

//                val playUrlToIndexMap = playList.mapIndexed { index, mapping ->
//                    mapping.playUrl to index
//                }.toMap()
//
//                val currentIndex = playUrlToIndexMap[selectedPlayerUrl] ?: -1
//                var previousPlayerUrl: String? = null
//                var nextPlayerUrl: String? = null
//                var nextPlayerList: MutableList<String>? = null
//                if (currentIndex != -1) {
//                    val currentMapping = playList[currentIndex]
//
//                    previousPlayerUrl = if (currentIndex > 0) {
//                        playList[currentIndex - 1].playUrl
//                    } else null
//
//                    nextPlayerUrl = if (currentIndex < playList.size - 1) {
//                        playList[currentIndex + 1].playUrl
//                    } else null
//
//                    nextPlayerList = if (currentIndex < playList.size - 1) {
//                        f
//
//                    }
//                }

                _uiState.update {
                    it.copy(
                        players = players,
                        selectedPlayerUrl = selectedPlayerUrl,
//                        previousPlayerUrl = previousPlayerUrl,
//                        nextPlayerUrl = nextPlayerUrl,
                        isLoadingPlayers = false
                    )
                }

                // 如果有选中的播放地址，自动开始播放
                selectedPlayerUrl?.let { playerUrl ->

                    // 记录观看历史
                    uiState.value.videoDetail?.let { videoDetail ->
                        watchHistoryRepository.addWatchHistoryFromVideoDetail(
                            videoDetail = videoDetail,
                            playPosition = uiState.value.watchHistory?.lastPlayedPosition ?: 0,
                            duration = uiState.value.watchHistory?.duration ?: 0,
                            episodeTitle = selectedPlayer?.videoTitle,
                            gatherId = gatherId,
                            gatherName = gatherName,
                            playerUrl = playerUrl
                        )
                    }
                }
            } else {
                // 如果视频详情为空或不包含播放地址列表，则使用新API
                val result = videoRepository.getPlayersFromVideoDetail(gatherId, videoId)

                if (result.isSuccess) {
                    val players = result.getOrNull() ?: emptyList()
                    ////Log.d("VideoDetailViewModel", "Players loaded from API: ${players.size}")

                    // 获取历史记录中的播放地址
                    val historyPlayerUrl = uiState.value.watchHistory?.playerUrl
                    val historyGatherId = uiState.value.watchHistory?.gatherId

                    // 如果有历史记录且是当前服务商的播放地址，选择该地址
                    val selectedPlayerUrl = if (historyPlayerUrl != null && historyGatherId == gatherId && players.any { it.playerUrl == historyPlayerUrl }) {
                        historyPlayerUrl
                    } else {
                        // 如果没有历史记录或播放地址不存在，选择第一个地址
                        players.firstOrNull()?.playerUrl
                    }

                    // 获取选中播放地址的标题
                    val selectedPlayer = players.find { it.playerUrl == selectedPlayerUrl }
                    val gatherName = uiState.value.gathers.find { it.id == gatherId }?.title

                    _uiState.update {
                        it.copy(
                            players = players,
                            selectedPlayerUrl = selectedPlayerUrl,
                            isLoadingPlayers = false
                        )
                    }

                    // 如果有选中的播放地址，自动开始播放
                    selectedPlayerUrl?.let { playerUrl ->

                        // 记录观看历史
                        uiState.value.videoDetail?.let { videoDetail ->
                            watchHistoryRepository.addWatchHistoryFromVideoDetail(
                                videoDetail = videoDetail,
                                playPosition = uiState.value.watchHistory?.lastPlayedPosition ?: 0,
                                duration = uiState.value.watchHistory?.duration ?: 0,
                                episodeTitle = selectedPlayer?.videoTitle,
                                gatherId = gatherId,
                                gatherName = gatherName,
                                playerUrl = playerUrl
                            )
                        }
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e("VideoDetailViewModel", "Error loading players", exception)
                    _uiState.update {
                        it.copy(
                            isLoadingPlayers = false,
                            error = exception?.message ?: "Failed to load video episodes"
                        )
                    }
                }
            }
        }
    }

    /**
     * 选择播放地址
     */
    fun selectPlayerUrl(playerUrl: String, playerTitle: String? = null) {
        _uiState.update { it.copy(selectedPlayerUrl = playerUrl) }

        // 记录观看历史
        uiState.value.videoDetail?.let { videoDetail ->
            viewModelScope.launch {
                // 获取当前服务商信息
                val gatherId = uiState.value.selectedGatherId
                val gatherName = uiState.value.gathers.find { it.id == gatherId }?.title

                watchHistoryRepository.addWatchHistoryFromVideoDetail(
                    videoDetail = videoDetail,
                    playPosition = 0, // 新选择的剧集从头开始播放
                    duration = 0,
                    episodeTitle = playerTitle,
                    gatherId = gatherId,
                    gatherName = gatherName,
                    playerUrl = playerUrl
                )
            }
        }
    }


    /**
     * 更新播放进度
     */
    fun updatePlayProgress(playPosition: Long, duration: Long) {
        viewModelScope.launch {
            // 获取当前服务商和播放地址信息
            val gatherId = uiState.value.selectedGatherId
            val gatherName = uiState.value.gathers.find { it.id == gatherId }?.title
            val playerUrl = uiState.value.selectedPlayerUrl

            watchHistoryRepository.updatePlayProgress(
                videoId = videoId,
                playPosition = playPosition,
                duration = duration,
                gatherId = gatherId,
                gatherName = gatherName,
                playerUrl = playerUrl
            )
        }
    }

    /**
     * 加载评论
     */
    fun loadComments() {
        _uiState.update { it.copy(isLoadingComments = true) }

        viewModelScope.launch {
            try {
                val comments = commentRepository.getComments(videoId, 0)
                ////Log.d("VideoDetailViewModel", "Comments loaded: ${comments.size}")

                _uiState.update {
                    it.copy(
                        comments = comments,
                        isLoadingComments = false
                    )
                }
            } catch (e: Exception) {
                Log.e("VideoDetailViewModel", "Error loading comments", e)
                _uiState.update {
                    it.copy(
                        isLoadingComments = false,
                        error = e.message ?: "Failed to load comments"
                    )
                }
            }
        }
    }

    /**
     * 发表评论
     */
    fun postComment(content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            try {
                val success = commentRepository.postComment(videoId, 0, content)
                if (success) {
                    // 发表成功后重新加载评论
                    loadComments()
                }
            } catch (e: Exception) {
                Log.e("VideoDetailViewModel", "Error posting comment", e)
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to post comment")
                }
            }
        }
    }

    /**
     * 加载推荐视频
     */
    fun loadRecommendedVideos() {
        _uiState.update { it.copy(isLoadingRecommendations = true) }

        viewModelScope.launch {
            val result = videoRepository.getMoreLiked(videoId, 0)

            if (result.isSuccess) {
                val videos = result.getOrNull() ?: emptyList()
                ////Log.d("VideoDetailViewModel", "Recommended videos loaded: ${videos.size}")

                _uiState.update {
                    it.copy(
                        recommendedVideos = videos,
                        isLoadingRecommendations = false
                    )
                }
            } else {
                val exception = result.exceptionOrNull()
                Log.e("VideoDetailViewModel", "Error loading recommended videos", exception)
                _uiState.update {
                    it.copy(
                        isLoadingRecommendations = false,
                        error = exception?.message ?: "Failed to load recommendations"
                    )
                }
            }
        }
    }

    /**
     * 显示服务商对话框
     */
    fun showGatherDialog() {
        _uiState.update { it.copy(showGatherDialog = true) }
    }

    /**
     * 隐藏服务商对话框
     */
    fun hideGatherDialog() {
        _uiState.update { it.copy(showGatherDialog = false) }
    }

    /**
     * 显示播放列表对话框
     */
    fun showPlayerDialog() {
        _uiState.update { it.copy(showPlayerDialog = true) }
    }

    /**
     * 隐藏播放列表对话框
     */
    fun hidePlayerDialog() {
        _uiState.update { it.copy(showPlayerDialog = false) }
    }

    /**
     * 显示服务商和播放列表整合对话框
     */
    fun showGatherAndPlayerDialog() {
        _uiState.update { it.copy(showGatherAndPlayerDialog = true) }
    }

    /**
     * 隐藏服务商和播放列表整合对话框
     */
    fun hideGatherAndPlayerDialog() {
        _uiState.update { it.copy(showGatherAndPlayerDialog = false) }
    }

    /**
     * 切换标签页
     */
    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }
}

data class VideoDetailUiState(
    val videoDetail: Video? = null,
    val gathers: List<Gather> = emptyList(),
    val selectedGatherId: String? = null,
    val players: List<Player> = emptyList(),
    val selectedPlayerUrl: String? = null, // 当前选中的播放地址
    val isLoading: Boolean = false,
    val isLoadingGathers: Boolean = false,
    val isLoadingPlayers: Boolean = false,
    val isLoadingComments: Boolean = false,
    val isLoadingRecommendations: Boolean = false,
    val error: String? = null,
    val watchHistory: WatchHistory? = null, // 观看历史
    val lastPlayedPosition: Long = 0, // 上次播放位置
    val comments: List<Comment> = emptyList(), // 评论列表
    val recommendedVideos: List<Video> = emptyList(), // 推荐视频列表
    val showGatherDialog: Boolean = false, // 是否显示服务商对话框
    val showPlayerDialog: Boolean = false, // 是否显示播放列表对话框
    val showGatherAndPlayerDialog: Boolean = false, // 是否显示服务商和播放列表整合对话框
    val selectedTab: Int = 0 // 0: 详情, 1: 评论
)

class VideoDetailViewModelFactory(private val videoId: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoDetailViewModel::class.java)) {
            return VideoDetailViewModel(videoId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
