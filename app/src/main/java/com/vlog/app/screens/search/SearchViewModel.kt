package com.vlog.app.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.VlogApp
import com.vlog.app.data.histories.search.SearchHistoryEntity
import com.vlog.app.data.videos.Video
import com.vlog.app.data.histories.search.SearchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 搜索页面 ViewModel
 */
class SearchViewModel : ViewModel() {
    private val searchRepository: SearchRepository = VlogApp.getInstance().searchRepository

    // UI 状态
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        // 加载搜索历史
        loadSearchHistory()
        // 加载热门搜索
        loadHotSearches()
    }

    /**
     * 加载搜索历史
     */
    private fun loadSearchHistory() {
        viewModelScope.launch {
            searchRepository.getRecentSearches().collect { searchHistory ->
                _uiState.update { it.copy(searchHistory = searchHistory) }
            }
        }
    }

    /**
     * 加载热门搜索
     */
    private fun loadHotSearches() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val hotSearches = searchRepository.searchVideos()
                _uiState.update {
                    it.copy(
                        hotSearches = hotSearches,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "加载热门搜索失败",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * 搜索视频
     */
    fun searchVideos(query: String) {
        if (query.isBlank()) {
            // 如果搜索关键词为空，显示热门搜索
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    isSearching = false,
                    currentQuery = ""
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        isSearching = true,
                        isLoading = true,
                        currentQuery = query
                    )
                }

                // 保存搜索历史
                searchRepository.saveSearchQuery(query)

                // 搜索视频
                val results = searchRepository.searchVideos(query)

                _uiState.update {
                    it.copy(
                        searchResults = results,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "搜索失败",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * 清除搜索历史
     */
    fun clearSearchHistory() {
        viewModelScope.launch {
            searchRepository.clearAllSearchHistory()
        }
    }

    /**
     * 删除单条搜索历史
     */
    fun deleteSearchHistory(searchHistory: SearchHistoryEntity) {
        viewModelScope.launch {
            searchRepository.deleteSearchQuery(searchHistory)
        }
    }

    /**
     * 更新搜索框文本
     */
    fun updateSearchText(text: String) {
        _uiState.update { it.copy(searchText = text) }
    }
}

/**
 * 搜索页面 UI 状态
 */
data class SearchUiState(
    val searchText: String = "",
    val currentQuery: String = "",
    val searchHistory: List<SearchHistoryEntity> = emptyList(),
    val hotSearches: List<Video> = emptyList(),
    val searchResults: List<Video> = emptyList(),
    val isSearching: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
