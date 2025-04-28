package com.vlog.app.screens.filter

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.R
import com.vlog.app.VlogApp
import com.vlog.app.data.api.NetworkModule
import com.vlog.app.data.videos.Video
import com.vlog.app.data.categories.CategoryRepository
import com.vlog.app.data.videos.VideoLocalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FilterViewModel(application: Application) : AndroidViewModel(application) {

    private val videoLocalRepository: VideoLocalRepository = VlogApp.getInstance().videoLocalRepository
    private val categoryRepository: CategoryRepository = VlogApp.getInstance().categoryRepository
    private val context = application.applicationContext

    private val TAG = "FilterViewModel"

    private val _uiState = MutableStateFlow(FilterUiState())
    val uiState: StateFlow<FilterUiState> = _uiState.asStateFlow()

    init {
        // 检查并更新分类数据
        checkAndUpdateCategories()

        // 加载默认分类的子分类
        val defaultCategoryId = _uiState.value.selectedCategory.id
        loadSubCategories(defaultCategoryId)

        // 加载筛选列表
        loadFilteredVideos()
    }

    /**
     * 检查并更新分类数据
     */
    private fun checkAndUpdateCategories() {
        viewModelScope.launch {
            try {
                if (categoryRepository.shouldUpdateCategories()) {
                    categoryRepository.refreshCategories()
                }

                // 加载主分类
                loadMainCategories()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to update categories")
                }
            }
        }
    }

    /**
     * 加载主分类
     */
    private fun loadMainCategories() {
        viewModelScope.launch {
            try {
                // 从数据库获取主分类
                val mainCategories = categoryRepository.getMainCategories().first()

                // 将主分类转换为 FilterItem 列表
                val filterItems = mainCategories.map {
                    FilterItem(it.id, it.title)
                }

                // 更新 UI 状态
                _uiState.update {
                    it.copy(
                        mainCategories = filterItems,
                        // 如果当前选中的分类不在新的分类列表中，则选择第一个分类
                        selectedCategory = if (filterItems.any { item -> item.id == it.selectedCategory.id }) {
                            it.selectedCategory
                        } else {
                            filterItems.firstOrNull() ?: it.selectedCategory
                        }
                    )
                }

                // 加载选中分类的子分类
                loadSubCategories(_uiState.value.selectedCategory.id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to load main categories")
                }
            }
        }
    }

    fun updateFilter(section: FilterSection, item: FilterItem) {
        _uiState.update { state ->
            when (section.param) {
                "typed" -> {
                    // 当选择了新的分类时，加载子分类
                    loadSubCategories(item.id)
                    state.copy(selectedCategory = item, selectedSubCategory = null)
                }
                "year" -> state.copy(selectedYear = item)
                "order_by" -> state.copy(selectedOrderBy = item)
                "cate" -> state.copy(selectedSubCategory = item)
                "code" -> state.copy(selectedCode = item)
                else -> state
            }
        }

        // 自动应用筛选条件
        loadFilteredVideos()
    }

    /**
     * 加载子分类
     */
    private fun loadSubCategories(parentId: String) {
        _uiState.update { it.copy(isLoadingCategories = true) }

        viewModelScope.launch {
            try {
                // 从数据库获取子分类
                val subCategories = categoryRepository.getSubCategories(parentId).first()

                // 将子分类转换为 FilterItem 列表
                val filterItems = mutableListOf<FilterItem>()

                // 只有当有子分类时才添加“全部”选项
                if (subCategories.isNotEmpty()) {
                    filterItems.add(FilterItem("0", "全部")) // 添加“全部”选项
                    filterItems.addAll(subCategories.map { FilterItem(it.id, it.title) })
                }

                _uiState.update {
                    it.copy(
                        subCategories = filterItems,
                        isLoadingCategories = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingCategories = false,
                        error = e.message ?: "Failed to load sub categories"
                    )
                }
            }
        }
    }

    fun resetFilters() {
        _uiState.update { state ->
            state.copy(
                selectedCategory = state.mainCategories.firstOrNull() ?: DefaultFilterConfig.categories.items.first(),
                selectedYear = DefaultFilterConfig.years.items.first(),
                selectedOrderBy = DefaultFilterConfig.orderBy.items.first(),
                selectedCode = DefaultFilterConfig.codes.items.first(),
                selectedSubCategory = null
            )
        }

        // 加载选中分类的子分类
        loadSubCategories(_uiState.value.selectedCategory.id)

        // 加载筛选列表
        loadFilteredVideos()
    }

    fun applyFilters() {
        loadFilteredVideos()
    }

    /**
     * 刷新数据
     * 同时刷新分类数据和视频列表
     */
    fun refreshData() {
        _uiState.update { it.copy(isRefreshing = true) }

        viewModelScope.launch {
            try {
                // 刷新分类数据
                categoryRepository.refreshCategories()

                // 重新加载主分类
                loadMainCategories()

                // 重新加载视频列表
                loadFilteredVideos()

                _uiState.update { it.copy(isRefreshing = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        error = e.message ?: "Failed to refresh data"
                    )
                }
            }
        }
    }

    private fun loadFilteredVideos() {
        _uiState.update { it.copy(
            isLoading = true,
            error = null,
            currentPage = 1, // 重置当前页码
            canLoadMore = true // 重置加载更多状态
        ) }

        viewModelScope.launch {
            try {
                // 只从网络获取数据，不使用本地数据
                Log.d(TAG, "Loading data from API only")
                loadFilteredVideosFromNetwork()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading filtered videos: ${e.message}", e)
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
     * 从网络加载筛选数据
     */
    private suspend fun loadFilteredVideosFromNetwork() {
        try {
            // 获取选中分类的 ID
            val categoryId = _uiState.value.selectedCategory.id

            // 从数据库获取分类实体，以获取 isTyped 字段
            val categoryEntity = categoryRepository.getCategoryById(categoryId)

            // 使用 isTyped 字段作为 typed 参数，如果为空则使用 ID
            val typed = categoryEntity?.isTyped?.toIntOrNull() ?: categoryId.toIntOrNull()

            val year = _uiState.value.selectedYear.id.toIntOrNull()
            val orderBy = _uiState.value.selectedOrderBy.id.toIntOrNull()
            val cate = _uiState.value.selectedSubCategory?.let { if (it.id == "0") null else it.id }

            // 只有当 typed = 1 (电影) 时才使用 code 参数
            val code = if (typed == 1) {
                _uiState.value.selectedCode.let { if (it.id == "0") null else it.id }
            } else null

            Log.d(TAG, "Loading filtered videos from network - categoryId: $categoryId, isTyped: ${categoryEntity?.isTyped}, typed: $typed, year: $year, orderBy: $orderBy, cate: $cate, code: $code")

            // 直接从网络获取数据
            val videos = withContext(Dispatchers.IO) {
                NetworkModule.apiService.getFilteredList(
                    typed = if (typed == 0) null else typed,
                    year = if (year == 0) null else year,
                    orderBy = if (orderBy == 0) null else orderBy,
                    page = 1,
                    cate = cate,
                    code = code
                ).data.items
            }

            Log.d(TAG, "Filtered videos fetched from network, size: ${videos.size}")

            // 检查数据是否为空
            if (videos.isEmpty()) {
                // 网络数据为空，尝试从本地加载
                Log.d(TAG, "Network data is empty, trying to load from local")
                loadFilteredVideosFromLocal()
                return
            }

            // 保存到本地
            Log.d(TAG, "Saving filtered videos to local database")
            videoLocalRepository.saveFilteredVideos(
                videos = videos,
                videoType = typed ?: 0
            )

            // 更新 UI
            Log.d(TAG, "Updating UI with network data")
            _uiState.update {
                it.copy(
                    videos = videos,
                    isLoading = false,
                    canLoadMore = videos.isNotEmpty()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading data from network: ${e.message}", e)
            // 网络请求失败，尝试从本地加载
            loadFilteredVideosFromLocal()
        }
    }

    /**
     * 从本地加载筛选数据
     */
    private suspend fun loadFilteredVideosFromLocal() {
        Log.d(TAG, "Loading filtered videos from local database")

        // 获取选中分类的 ID
        val categoryId = _uiState.value.selectedCategory.id

        // 从数据库获取分类实体，以获取 isTyped 字段
        val categoryEntity = categoryRepository.getCategoryById(categoryId)

        // 使用 isTyped 字段作为 typed 参数，如果为空则使用 ID
        val typed = categoryEntity?.isTyped?.toIntOrNull() ?: categoryId.toIntOrNull() ?: 0

        Log.d(TAG, "Loading local filtered videos - categoryId: $categoryId, isTyped: ${categoryEntity?.isTyped}, typed: $typed")

        val localVideos = videoLocalRepository.getLocalFilteredVideos(typed)
        Log.d(TAG, "Local filtered videos loaded, size: ${localVideos.size}")

        // 检查本地数据是否为空
        if (localVideos.isEmpty()) {
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
                videos = localVideos,
                isLoading = false,
                canLoadMore = localVideos.isNotEmpty()
            )
        }
    }

    /**
     * 加载更多视频（下一页）
     */
    fun loadMoreVideos() {
        // 如果已经在加载中或者不能加载更多，则直接返回
        if (_uiState.value.isLoadingMore || !_uiState.value.canLoadMore) {
            return
        }

        _uiState.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch {
            try {
                val nextPage = _uiState.value.currentPage + 1

                // 获取选中分类的 ID
                val categoryId = _uiState.value.selectedCategory.id

                // 从数据库获取分类实体，以获取 isTyped 字段
                val categoryEntity = categoryRepository.getCategoryById(categoryId)

                // 使用 isTyped 字段作为 typed 参数，如果为空则使用 ID
                val typed = categoryEntity?.isTyped?.toIntOrNull() ?: categoryId.toIntOrNull()

                val year = _uiState.value.selectedYear.id.toIntOrNull()
                val orderBy = _uiState.value.selectedOrderBy.id.toIntOrNull()
                val cate = _uiState.value.selectedSubCategory?.let { if (it.id == "0") null else it.id }

                // 只有当 typed = 1 (电影) 时才使用 code 参数
                val code = if (typed == 1) {
                    _uiState.value.selectedCode.let { if (it.id == "0") null else it.id }
                } else null

                //Log.d(TAG, "Loading more filtered videos - categoryId: $categoryId, isTyped: ${categoryEntity?.isTyped}, typed: $typed, year: $year, orderBy: $orderBy, page: $nextPage, cate: $cate, code: $code")

                val newVideos = videoLocalRepository.loadMoreFilteredVideos(
                    typed = if (typed == 0) null else typed,
                    year = if (year == 0) null else year,
                    orderBy = if (orderBy == 0) null else orderBy,
                    page = nextPage,
                    cate = cate,
                    code = code
                )

                _uiState.update {
                    it.copy(
                        videos = it.videos + newVideos, // 将新视频添加到现有列表中
                        currentPage = nextPage,
                        isLoadingMore = false,
                        canLoadMore = newVideos.isNotEmpty() // 如果返回的数据为空，则不能加载更多
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        error = e.message ?: "Unknown error",
                        canLoadMore = false
                    )
                }
            }
        }
    }
}

data class FilterUiState(
    val videos: List<Video> = emptyList(),
    val mainCategories: List<FilterItem> = DefaultFilterConfig.categories.items,
    val selectedCategory: FilterItem = DefaultFilterConfig.categories.items.first(),
    val selectedYear: FilterItem = DefaultFilterConfig.years.items.first(),
    val selectedOrderBy: FilterItem = DefaultFilterConfig.orderBy.items.first(),
    val selectedCode: FilterItem = DefaultFilterConfig.codes.items.first(),
    val selectedSubCategory: FilterItem? = null,
    val subCategories: List<FilterItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLoadingCategories: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val canLoadMore: Boolean = true
)

data class FilterSection(
    val title: String,
    val items: List<FilterItem>,
    val param: String
)

data class FilterItem(
    val id: String,
    val name: String
)

object DefaultFilterConfig {
    val categories = FilterSection(
        title = "分类",
        items = listOf(
            FilterItem("1", "电影"),
            FilterItem("2", "电视剧"),
            FilterItem("3", "动漫"),
            FilterItem("4", "综艺"),
            FilterItem("5", "体育赛事"),
            FilterItem("9", "预告片")
        ),
        param = "typed"
    )

    val codes = FilterSection(
        title = "地区",
        items = listOf(
            FilterItem("0", "地区"),
            FilterItem("100156", "中国"),
            FilterItem("100410", "韩国"),
            FilterItem("100392", "日本"),
            FilterItem("100840", "美国"),
            FilterItem("100356", "印度"),
            FilterItem("100764", "泰国"),
            FilterItem("100250", "法国"),
            FilterItem("100826", "英国"),
            FilterItem("100276", "德国"),
            FilterItem("100380", "意大利"),
            FilterItem("100124", "加拿大"),
            FilterItem("100484", "墨西哥")
        ),
        param = "code"
    )

    val years = FilterSection(
        title = "年份",
        items = listOf(
            FilterItem("0", "年份"),
            FilterItem("2025", "2025"),
            FilterItem("2024", "2024"),
            FilterItem("2023", "2023"),
            FilterItem("2022", "2022")
        ),
        param = "year"
    )

    val orderBy = FilterSection(
        title = "排序",
        items = listOf(
            FilterItem("0", "创建时间"),
            FilterItem("1", "更新时间"),
            FilterItem("2", "热度"),
            FilterItem("3", "推荐")
        ),
        param = "order_by"
    )
}
