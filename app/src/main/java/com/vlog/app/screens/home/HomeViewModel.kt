package com.vlog.app.screens.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.VlogApp
import com.vlog.app.R
import com.vlog.app.data.api.NetworkModule
import com.vlog.app.data.videos.Video
// HomeData is an inner class of VideoLocalRepository
import com.vlog.app.data.videos.VideoLocalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val videoLocalRepository: VideoLocalRepository = VlogApp.getInstance().videoLocalRepository
    private val context = application.applicationContext

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                // 只从网络获取数据，不使用本地数据
                Log.d("HomeViewModel", "Loading data from API only")
                loadDataFromNetwork()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading home data: ${e.message}", e)
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: context.getString(R.string.unknown_error)
                    )
                }
            }
        }
    }

    /**
     * 从网络加载数据
     */
    private suspend fun loadDataFromNetwork() {
        try {
            Log.d("HomeViewModel", "Starting to load home data from network")

            // 从网络获取轮播图数据
            Log.d("HomeViewModel", "Fetching banners from network")
            val banners = withContext(Dispatchers.IO) {
                NetworkModule.apiService.getVideoList(0, 2025, 3).data
            }
            Log.d("HomeViewModel", "Banners fetched from network, size: ${banners.size}")

            // 从网络获取推荐电影列表
            Log.d("HomeViewModel", "Fetching recommended movies from network")
            val recommendedMovies = withContext(Dispatchers.IO) {
                NetworkModule.apiService.getVideoList(1, 2025, 3).data
            }
            Log.d("HomeViewModel", "Recommended movies fetched from network, size: ${recommendedMovies.size}")

            // 从网络获取电视剧列表
            Log.d("HomeViewModel", "Fetching TV series from network")
            val tvSeries = withContext(Dispatchers.IO) {
                NetworkModule.apiService.getVideoList(2, 0, 3).data
            }
            Log.d("HomeViewModel", "TV series fetched from network, size: ${tvSeries.size}")

            // 从网络获取动漫列表
            Log.d("HomeViewModel", "Fetching anime from network")
            val comics = withContext(Dispatchers.IO) {
                NetworkModule.apiService.getVideoList(3, 0, 3).data
            }
            Log.d("HomeViewModel", "Anime fetched from network, size: ${comics.size}")

            // 检查数据是否为空
            val isDataEmpty = banners.isEmpty() &&
                             recommendedMovies.isEmpty() &&
                             tvSeries.isEmpty() &&
                             comics.isEmpty()

            if (isDataEmpty) {
                // 网络数据为空，尝试从本地加载
                Log.d("HomeViewModel", "Network data is empty, trying to load from local")
                loadDataFromLocal()
                return
            }

            // 保存到本地
            Log.d("HomeViewModel", "Saving network data to local database")
            videoLocalRepository.saveHomeData(
                VideoLocalRepository.HomeData(
                    banners = banners.take(5),
                    recommendedMovies = recommendedMovies,
                    tvSeries = tvSeries,
                    comics = comics
                )
            )

            // 更新 UI
            Log.d("HomeViewModel", "Updating UI with network data")
            _uiState.update {
                it.copy(
                    banners = banners.take(5),
                    recommendedMovies = recommendedMovies,
                    tvSeries = tvSeries,
                    comics = comics,
                    isLoading = false,
                    error = null
                )
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error loading data from network: ${e.message}", e)
            // 网络请求失败，尝试从本地加载
            loadDataFromLocal()
        }
    }

    /**
     * 从本地加载数据
     */
    private suspend fun loadDataFromLocal() {
        Log.d("HomeViewModel", "Loading data from local database")
        val localData = videoLocalRepository.getLocalHomeData()
        Log.d("HomeViewModel", "Local data loaded - banners: ${localData.banners.size}, movies: ${localData.recommendedMovies.size}, tv: ${localData.tvSeries.size}, comics: ${localData.comics.size}")

        // 检查本地数据是否为空
        val isLocalDataEmpty = localData.banners.isEmpty() &&
                             localData.recommendedMovies.isEmpty() &&
                             localData.tvSeries.isEmpty() &&
                             localData.comics.isEmpty()

        if (isLocalDataEmpty) {
            // 本地数据为空，显示错误
            Log.d("HomeViewModel", "Local data is empty, showing error")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = context.getString(R.string.no_data_available)
                )
            }
            return
        }

        // 更新 UI
        Log.d("HomeViewModel", "Updating UI with local data")
        _uiState.update {
            it.copy(
                banners = localData.banners,
                recommendedMovies = localData.recommendedMovies,
                tvSeries = localData.tvSeries,
                comics = localData.comics,
                isLoading = false,
                error = null
            )
        }
    }
}

data class HomeUiState(
    val banners: List<Video> = emptyList(),  // 轮播图数据
    val recommendedMovies: List<Video> = emptyList(),
    val tvSeries: List<Video> = emptyList(),
    val comics: List<Video> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
