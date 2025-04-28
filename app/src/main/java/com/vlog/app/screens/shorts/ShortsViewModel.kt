package com.vlog.app.screens.shorts

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.R
import com.vlog.app.VlogApp
import com.vlog.app.data.api.NetworkModule
import com.vlog.app.data.videos.Video
import com.vlog.app.data.videos.VideoLocalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShortsViewModel(application: Application) : AndroidViewModel(application) {

    private val videoLocalRepository: VideoLocalRepository = VlogApp.getInstance().videoLocalRepository
    private val context = application.applicationContext

    private val TAG = "ShortsViewModel"

    // 当前页面索引变化时加载视频详情
    private var currentPageIndex = -1

    // 预加载的阈值，当用户滚动到距离列表末尾这么远时，开始加载下一页
    private val PRELOAD_THRESHOLD = 3

    private val _uiState = MutableStateFlow(ShortsUiState())
    val uiState: StateFlow<ShortsUiState> = _uiState.asStateFlow()

    init {
        loadShorts()
    }

    /**
     * 停止视频播放
     */
    fun stopVideoPlayback() {
        _uiState.update { it.copy(shouldPlayVideo = false) }
        Log.d("ShortsViewModel", "Video playback stopped")
    }

    /**
     * 当页面变化时调用，加载当前页面的视频详情
     */
    fun onPageChanged(page: Int) {
        // 先停止所有视频播放
        stopVideoPlayback()

        if (page == currentPageIndex) return
        currentPageIndex = page

        val shorts = _uiState.value.shorts
        if (shorts.isEmpty() || page >= shorts.size) return

        // 更新当前视频索引
        _uiState.update { it.copy(currentShortIndex = page) }

        // 加载当前视频详情
        val videoId = shorts[page].id
        loadVideoDetail(videoId) // 传入 true 表示加载完成后自动播放

        // 如果滚动到距离列表末尾 PRELOAD_THRESHOLD 个项目，则自动加载更多
        if (page >= shorts.size - PRELOAD_THRESHOLD && _uiState.value.canLoadMore && !_uiState.value.isLoadingMore) {
            Log.d("ShortsViewModel", "Triggering loadMoreShorts at page $page, list size: ${shorts.size}")
            loadMoreShorts()
        }
    }

    /**
     * 加载解说视频列表（首次加载）
     */
    fun loadShorts() {
        _uiState.update { it.copy(
            isLoading = true,
            error = null,
            currentPage = 1, // 重置页码
            canLoadMore = true // 重置加载状态
        ) }

        viewModelScope.launch {
            try {
                // 只从网络获取数据，不使用本地数据
                Log.d(TAG, "Loading data from API only")
                loadShortsFromNetwork()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading shorts: ${e.message}", e)
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: context.getString(R.string.unknown_error),
                        canLoadMore = false
                    )
                }
            }
        }
    }

    /**
     * 从网络加载解说视频
     */
    private suspend fun loadShortsFromNetwork() {
        try {
            Log.d(TAG, "Loading shorts from network")

            // 直接从网络获取数据
            val shorts = withContext(Dispatchers.IO) {
                NetworkModule.apiService.getVideoList(8, 0, 1).data
            }

            Log.d(TAG, "Shorts fetched from network, size: ${shorts.size}")

            // 检查数据是否为空
            if (shorts.isEmpty()) {
                // 网络数据为空，尝试从本地加载
                Log.d(TAG, "Network data is empty, trying to load from local")
                loadShortsFromLocal()
                return
            }

            // 保存到本地
            Log.d(TAG, "Saving shorts to local database")
            videoLocalRepository.saveShortsVideos(shorts)

            // 更新 UI
            Log.d(TAG, "Updating UI with network data")
            _uiState.update {
                it.copy(
                    shorts = shorts,
                    isLoading = false,
                    canLoadMore = shorts.isNotEmpty()
                )
            }

            // 如果加载成功且有视频，自动加载第一个视频的详情
            if (shorts.isNotEmpty()) {
                loadVideoDetail(shorts[0].id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading data from network: ${e.message}", e)
            // 网络请求失败，尝试从本地加载
            loadShortsFromLocal()
        }
    }

    /**
     * 从本地加载解说视频
     */
    private suspend fun loadShortsFromLocal() {
        Log.d(TAG, "Loading shorts from local database")
        val localShorts = videoLocalRepository.getLocalShorts()
        Log.d(TAG, "Local shorts loaded, size: ${localShorts.size}")

        // 检查本地数据是否为空
        if (localShorts.isEmpty()) {
            // 本地数据为空，显示错误
            Log.d(TAG, "Local data is empty, showing error")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = context.getString(R.string.no_data_available),
                    canLoadMore = false
                )
            }
            return
        }

        // 更新 UI
        Log.d(TAG, "Updating UI with local data")
        _uiState.update {
            it.copy(
                shorts = localShorts,
                isLoading = false,
                canLoadMore = localShorts.isNotEmpty()
            )
        }

        // 如果加载成功且有视频，自动加载第一个视频的详情
        if (localShorts.isNotEmpty()) {
            loadVideoDetail(localShorts[0].id)
        }
    }

    /**
     * 加载更多解说视频（下一页）
     */
    fun loadMoreShorts() {
        // 如果已经在加载中或者不能加载更多，则直接返回
        if (_uiState.value.isLoadingMore || !_uiState.value.canLoadMore) {
            Log.d("ShortsViewModel", "Skip loadMoreShorts: isLoadingMore=${_uiState.value.isLoadingMore}, canLoadMore=${_uiState.value.canLoadMore}")
            return
        }

        Log.d("ShortsViewModel", "Starting loadMoreShorts, current page: ${_uiState.value.currentPage}")

        _uiState.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch {
            try {
                val nextPage = _uiState.value.currentPage + 1

                // 加载下一页数据
                val newShorts = videoLocalRepository.loadMoreShorts(nextPage)

                Log.d("ShortsViewModel", "Loaded page $nextPage, got ${newShorts.size} new items")

                _uiState.update {
                    it.copy(
                        shorts = it.shorts + newShorts, // 将新数据添加到现有列表中
                        currentPage = nextPage,
                        isLoadingMore = false,
                        canLoadMore = newShorts.isNotEmpty() // 如果返回的数据为空，则不能加载更多
                    )
                }

                Log.d("ShortsViewModel", "After update: total items=${_uiState.value.shorts.size}, currentPage=${_uiState.value.currentPage}, canLoadMore=${_uiState.value.canLoadMore}")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        error = e.message ?: "Failed to load more videos",
                        canLoadMore = false
                    )
                }
            }
        }
    }

    /**
     * 加载视频详情
     */
    private fun loadVideoDetail(videoId: String) {
        _uiState.update { it.copy(isLoadingDetail = true) }

        viewModelScope.launch {
            try {
                val videoDetail = videoLocalRepository.getVideoDetail(videoId)

                // 只有当前页面的视频才允许播放
                val isCurrentPage = _uiState.value.currentShortIndex == currentPageIndex

                _uiState.update {
                    it.copy(
                        currentVideoDetail = videoDetail,
                        isLoadingDetail = false,
                        shouldPlayVideo = isCurrentPage // 只有当前页面的视频才允许播放
                    )
                }

                Log.d("ShortsViewModel", "Video detail loaded, shouldPlayVideo=$isCurrentPage, currentIndex=${_uiState.value.currentShortIndex}, pageIndex=$currentPageIndex")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingDetail = false,
                        error = e.message ?: "Failed to load video detail"
                    )
                }
            }
        }
    }

    fun setCurrentShort(index: Int) {
        if (index in _uiState.value.shorts.indices) {
            _uiState.update { it.copy(currentShortIndex = index) }
        }
    }
}

data class ShortsUiState(
    val shorts: List<Video> = emptyList(),
    val currentShortIndex: Int = 0,
    val currentVideoDetail: Video? = null,
    val isLoading: Boolean = false,
    val isLoadingDetail: Boolean = false,
    val isLoadingMore: Boolean = false, // 是否正在加载更多数据
    val error: String? = null,
    val shouldPlayVideo: Boolean = false, // 控制是否应该播放视频
    val currentPage: Int = 1, // 当前页码
    val canLoadMore: Boolean = true // 是否可以加载更多数据
)
