package com.vlog.app.data.videos

import android.content.Context
import android.util.Log
import com.vlog.app.data.api.ApiService
import com.vlog.app.data.videos.VideoDao
import com.vlog.app.data.videos.VideoEntity
import com.vlog.app.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

/**
 * 视频本地仓库
 * 负责处理视频数据的本地存储和网络请求
 */
class VideoLocalRepository(
    private val context: Context,
    private val apiService: ApiService,
    private val videoDao: VideoDao
) {
    private val TAG = "VideoLocalRepository"

    // 网络请求超时时间（毫秒）
    private val NETWORK_TIMEOUT = 15000L // 增加超时时间到 15 秒

    // 数据刷新间隔（毫秒）
    private val REFRESH_INTERVAL = TimeUnit.HOURS.toMillis(1) // 1小时

    /**
     * 获取首页数据
     * 先尝试从网络获取，如果超时或失败则从本地获取
     */
    fun getHomeData(): Flow<HomeData> = flow {
        Log.d(TAG, "getHomeData: Starting to fetch home data")

        try {
            // 只从网络获取数据，不使用本地数据
            Log.d(TAG, "getHomeData: Fetching from network only")
            Log.d(TAG, "getHomeData: Starting network request with timeout $NETWORK_TIMEOUT ms")

            val networkData = withTimeoutOrNull(NETWORK_TIMEOUT) {
                Log.d(TAG, "getHomeData: Inside network request coroutine")
                val data = getNetworkHomeData()
                Log.d(TAG, "getHomeData: Network data fetched - banners: ${data.banners.size}, movies: ${data.recommendedMovies.size}, tv: ${data.tvSeries.size}, comics: ${data.comics.size}")
                data
            }

            if (networkData != null) {
                Log.d(TAG, "getHomeData: Network request completed successfully")
                // 检查网络数据是否为空
                val isNetworkDataEmpty = networkData.banners.isEmpty() &&
                                       networkData.recommendedMovies.isEmpty() &&
                                       networkData.tvSeries.isEmpty() &&
                                       networkData.comics.isEmpty()

                if (!isNetworkDataEmpty) {
                    // 保存到本地
                    Log.d(TAG, "getHomeData: Saving network data to local database")
                    saveHomeData(networkData)
                    // 发送网络数据
                    Log.d(TAG, "getHomeData: Emitting network data")
                    emit(networkData)
                } else {
                    // 网络数据为空，发送空数据
                    Log.d(TAG, "getHomeData: Network data is empty, emitting empty data")
                    emit(HomeData())
                }
            } else {
                // 网络请求失败，发送空数据
                Log.d(TAG, "getHomeData: Network request failed, emitting empty data")
                emit(HomeData())
            }

            // 注释掉从本地获取数据的代码
            /*
            // 从本地获取数据
            val localData = getLocalHomeData()
            Log.d(TAG, "getHomeData: Local data fetched - banners: ${localData.banners.size}, movies: ${localData.recommendedMovies.size}, tv: ${localData.tvSeries.size}, comics: ${localData.comics.size}")

            // 判断本地数据是否为空
            val isLocalDataEmpty = localData.banners.isEmpty() &&
                                  localData.recommendedMovies.isEmpty() &&
                                  localData.tvSeries.isEmpty() &&
                                  localData.comics.isEmpty()

            if (!isLocalDataEmpty) {
                // 本地有数据，发送本地数据
                Log.d(TAG, "getHomeData: Emitting local data")
                emit(localData)
            } else {
                // 本地数据为空，发送空数据
                Log.d(TAG, "getHomeData: Local data is empty, emitting empty data")
                emit(HomeData())
            }
            */

        } catch (e: Exception) {
            Log.e(TAG, "getHomeData: Error fetching data: ${e.message}", e)
            e.printStackTrace()
            emit(HomeData())

            // 注释掉从本地获取数据的代码
            /*
            // 发生异常，尝试从本地获取数据
            try {
                val localData = getLocalHomeData()
                val isLocalDataEmpty = localData.banners.isEmpty() &&
                                      localData.recommendedMovies.isEmpty() &&
                                      localData.tvSeries.isEmpty() &&
                                      localData.comics.isEmpty()

                if (!isLocalDataEmpty) {
                    // 本地有数据，发送本地数据
                    Log.d(TAG, "getHomeData: Emitting local data after exception")
                    emit(localData)
                } else {
                    // 本地数据为空，发送空数据
                    Log.d(TAG, "getHomeData: Local data is empty after exception, emitting empty data")
                    emit(HomeData())
                }
            } catch (localException: Exception) {
                Log.e(TAG, "getHomeData: Error fetching local data: ${localException.message}", localException)
                emit(HomeData())
            }
            */
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 获取筛选页面数据
     */
    fun getFilteredVideos(
        typed: Int? = null,
        year: Int? = null,
        released: Int? = null,
        orderBy: Int? = null,
        page: Int = 1,
        cate: String? = null,
        code: String? = null
    ): Flow<List<Video>> = flow {
        val videoType = typed ?: 0
        Log.d(TAG, "getFilteredVideos: Starting with params - typed: $typed, year: $year, orderBy: $orderBy, page: $page, cate: $cate, code: $code")

        try {
            // 只从网络获取数据，不使用本地数据
            Log.d(TAG, "getFilteredVideos: Fetching from network only for type $videoType")

            // 从网络获取数据，设置超时
            val networkData = withTimeoutOrNull(NETWORK_TIMEOUT) {
                withContext(Dispatchers.IO) {
                    val response = apiService.getFilteredList(typed, year, released, orderBy, page, cate, code)
                    Log.d(TAG, "getFilteredVideos: Network response - code: ${response.code}, message: ${response.message}")
                    response.data.items
                }
            }

            if (networkData != null && networkData.isNotEmpty()) {
                Log.d(TAG, "getFilteredVideos: Network data fetched successfully, size: ${networkData.size}")
                // 保存到本地
                saveFilteredVideos(networkData, videoType)
                // 发送网络数据
                emit(networkData)
            } else {
                // 网络数据为空或请求失败，发送空列表
                Log.d(TAG, "getFilteredVideos: Network data is empty or request failed, emitting empty list")
                emit(emptyList())
            }

            // 注释掉从本地获取数据的代码
            /*
            // 从本地获取数据
            val localVideos = getLocalFilteredVideos(videoType)
            Log.d(TAG, "getFilteredVideos: Local data fetched, size: ${localVideos.size}")

            if (localVideos.isNotEmpty()) {
                // 本地有数据，发送本地数据
                Log.d(TAG, "getFilteredVideos: Emitting local data")
                emit(localVideos)
            } else {
                // 本地数据为空，发送空列表
                Log.d(TAG, "getFilteredVideos: Local data is empty, emitting empty list")
                emit(emptyList())
            }
            */

        } catch (e: Exception) {
            Log.e(TAG, "getFilteredVideos: Error fetching data: ${e.message}", e)
            e.printStackTrace()
            emit(emptyList())

            // 注释掉从本地获取数据的代码
            /*
            // 发生异常，尝试从本地获取数据
            try {
                val localVideos = getLocalFilteredVideos(videoType)

                if (localVideos.isNotEmpty()) {
                    // 本地有数据，发送本地数据
                    Log.d(TAG, "getFilteredVideos: Emitting local data after exception")
                    emit(localVideos)
                } else {
                    // 本地数据为空，发送空列表
                    Log.d(TAG, "getFilteredVideos: Local data is empty after exception, emitting empty list")
                    emit(emptyList())
                }
            } catch (localException: Exception) {
                Log.e(TAG, "getFilteredVideos: Error fetching local data: ${localException.message}", localException)
                emit(emptyList())
            }
            */
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 获取解说页面数据
     */
    fun getShorts(): Flow<List<Video>> = flow {
        Log.d(TAG, "getShorts: Starting to fetch shorts data")

        try {
            // 只从网络获取数据，不使用本地数据
            Log.d(TAG, "getShorts: Fetching from network only")

            // 从网络获取数据，设置超时
            val networkData = withTimeoutOrNull(NETWORK_TIMEOUT) {
                withContext(Dispatchers.IO) {
                    val response = apiService.getVideoList(8, 0, 1)
                    Log.d(TAG, "getShorts: Network response - code: ${response.code}, message: ${response.message}")
                    response.data
                }
            }

            if (networkData != null && networkData.isNotEmpty()) {
                Log.d(TAG, "getShorts: Network data fetched successfully, size: ${networkData.size}")
                // 保存到本地
                saveShortsVideos(networkData)
                // 发送网络数据
                emit(networkData)
            } else {
                // 网络数据为空或请求失败，发送空列表
                Log.d(TAG, "getShorts: Network data is empty or request failed, emitting empty list")
                emit(emptyList())
            }

            // 注释掉从本地获取数据的代码
            /*
            // 从本地获取数据
            val localVideos = getLocalShorts()
            Log.d(TAG, "getShorts: Local data fetched, size: ${localVideos.size}")

            if (localVideos.isNotEmpty()) {
                // 本地有数据，发送本地数据
                Log.d(TAG, "getShorts: Emitting local data")
                emit(localVideos)
            } else {
                // 本地数据为空，发送空列表
                Log.d(TAG, "getShorts: Local data is empty, emitting empty list")
                emit(emptyList())
            }
            */

        } catch (e: Exception) {
            Log.e(TAG, "getShorts: Error fetching data: ${e.message}", e)
            e.printStackTrace()
            emit(emptyList())

            // 注释掉从本地获取数据的代码
            /*
            // 发生异常，尝试从本地获取数据
            try {
                val localVideos = getLocalShorts()

                if (localVideos.isNotEmpty()) {
                    // 本地有数据，发送本地数据
                    Log.d(TAG, "getShorts: Emitting local data after exception")
                    emit(localVideos)
                } else {
                    // 本地数据为空，发送空列表
                    Log.d(TAG, "getShorts: Local data is empty after exception, emitting empty list")
                    emit(emptyList())
                }
            } catch (localException: Exception) {
                Log.e(TAG, "getShorts: Error fetching local data: ${localException.message}", localException)
                emit(emptyList())
            }
            */
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 加载更多解说视频
     */
    suspend fun loadMoreShorts(page: Int): List<Video> {
        return try {
            withContext(Dispatchers.IO) {
                apiService.getVideoListWithPage(8, 0, 1, page).data
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading more shorts", e)
            emptyList()
        }
    }

    /**
     * 加载更多筛选视频
     */
    suspend fun loadMoreFilteredVideos(
        typed: Int? = null,
        year: Int? = null,
        released: Int? = null,
        orderBy: Int? = null,
        page: Int = 1,
        cate: String? = null,
        code: String? = null
    ): List<Video> {
        return try {
            withContext(Dispatchers.IO) {
                apiService.getFilteredList(typed, year, released, orderBy, page, cate, code).data.items
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading more filtered videos", e)
            emptyList()
        }
    }

    /**
     * 获取视频详情
     */
    suspend fun getVideoDetail(videoId: String, gather: String? = null) = withContext(Dispatchers.IO) {
        apiService.getVideoDetail(videoId, gather).data
    }

    /**
     * 获取服务商列表
     */
    suspend fun getGatherList() = withContext(Dispatchers.IO) {
        apiService.getGatherList().data
    }

    /**
     * 从视频详情中获取服务商列表
     */
    suspend fun getGathersFromVideoDetail(videoId: String) = withContext(Dispatchers.IO) {
        val videoDetail = apiService.getVideoDetail(videoId).data
        videoDetail.getGathers()
    }

    /**
     * 从视频详情中获取播放列表
     */
    suspend fun getPlayersFromVideoDetail(gather: String, videoId: String) = withContext(Dispatchers.IO) {
        val videoDetail = apiService.getVideoDetail(videoId).data
        val playList = videoDetail.getPlayListByGatherId(gather)
        playList.map { playItem ->
            Player(
                id = "0",
                videoTitle = playItem.title,
                playerUrl = playItem.playUrl,
                path = playItem.path,
                gatherId = gather,
                videoId = videoId
            )
        }
    }

    /**
     * 获取相关推荐
     */
    suspend fun getMoreLiked(videoId: String, orderBy: Int) = withContext(Dispatchers.IO) {
        apiService.getMoreLiked(videoId, orderBy).data
    }

    /**
     * 从本地获取首页数据
     */
    suspend fun getLocalHomeData(): HomeData {
        val banners = mutableListOf<Video>()
        videoDao.getBanners().map { entities ->
            entities.map { it.toVideo() }
        }.catch {
            emit(emptyList())
        }.flowOn(Dispatchers.IO).collect {
            banners.addAll(it)
        }

        val recommendedMovies = mutableListOf<Video>()
        videoDao.getRecommendedMovies().map { entities ->
            entities.map { it.toVideo() }
        }.catch {
            emit(emptyList())
        }.flowOn(Dispatchers.IO).collect {
            recommendedMovies.addAll(it)
        }

        val tvSeries = mutableListOf<Video>()
        videoDao.getTvSeries().map { entities ->
            entities.map { it.toVideo() }
        }.catch {
            emit(emptyList())
        }.flowOn(Dispatchers.IO).collect {
            tvSeries.addAll(it)
        }

        val anime = mutableListOf<Video>()
        videoDao.getAnime().map { entities ->
            entities.map { it.toVideo() }
        }.catch {
            emit(emptyList())
        }.flowOn(Dispatchers.IO).collect {
            anime.addAll(it)
        }

        return HomeData(
            banners = banners,
            recommendedMovies = recommendedMovies,
            tvSeries = tvSeries,
            comics = anime
        )
    }

    /**
     * 从网络获取首页数据
     */
    private suspend fun getNetworkHomeData(): HomeData = withContext(Dispatchers.IO) {
        Log.d(TAG, "getNetworkHomeData: Starting to fetch network data")

        // 检查网络连接
        if (!NetworkUtils.checkNetworkConnection(context)) {
            Log.e(TAG, "getNetworkHomeData: Network is not available")
            return@withContext HomeData()
        }

        try {
            // 获取轮播图数据（使用推荐电影作为轮播图）
            Log.d(TAG, "getNetworkHomeData: Fetching banners")
            val banners = apiService.getVideoList(0, 2025, 3).data
            Log.d(TAG, "getNetworkHomeData: Banners fetched, size: ${banners.size}")

            // 获取推荐电影列表
            Log.d(TAG, "getNetworkHomeData: Fetching recommended movies")
            val recommendedMovies = apiService.getVideoList(1, 2025, 3).data
            Log.d(TAG, "getNetworkHomeData: Recommended movies fetched, size: ${recommendedMovies.size}")

            // 获取电视剧列表
            Log.d(TAG, "getNetworkHomeData: Fetching TV series")
            val tvSeries = apiService.getVideoList(2, 0, 3).data
            Log.d(TAG, "getNetworkHomeData: TV series fetched, size: ${tvSeries.size}")

            // 获取动漫列表
            Log.d(TAG, "getNetworkHomeData: Fetching anime")
            val comics = apiService.getVideoList(3, 0, 3).data
            Log.d(TAG, "getNetworkHomeData: Anime fetched, size: ${comics.size}")

            return@withContext HomeData(
                banners = banners.take(5), // 只取前5个作为轮播图
                recommendedMovies = recommendedMovies,
                tvSeries = tvSeries,
                comics = comics
            )
        } catch (e: Exception) {
            Log.e(TAG, "getNetworkHomeData: Error fetching network data: ${e.message}", e)
            e.printStackTrace()
            return@withContext HomeData()
        }
    }

    /**
     * 保存首页数据到本地
     */
    suspend fun saveHomeData(homeData: HomeData) {
        // 转换为实体类
        val bannerEntities = homeData.banners.map { video ->
            VideoEntity.fromVideo(video, 0, "home_banner")
        }

        val movieEntities = homeData.recommendedMovies.map { video ->
            VideoEntity.fromVideo(video, 1, "home_movie")
        }

        val tvEntities = homeData.tvSeries.map { video ->
            VideoEntity.fromVideo(video, 2, "home_tv")
        }

        val animeEntities = homeData.comics.map { video ->
            VideoEntity.fromVideo(video, 3, "home_anime")
        }

        // 保存到数据库
        videoDao.refreshHomeData(
            banners = bannerEntities,
            recommendedMovies = movieEntities,
            tvSeries = tvEntities,
            anime = animeEntities
        )
    }

    /**
     * 从本地获取筛选页面数据
     */
    suspend fun getLocalFilteredVideos(videoType: Int): List<Video> {
        val videos = mutableListOf<Video>()
        videoDao.getFilteredVideos(videoType).map { entities ->
            entities.map { it.toVideo() }
        }.catch {
            emit(emptyList())
        }.flowOn(Dispatchers.IO).collect {
            videos.addAll(it)
        }
        return videos
    }

    /**
     * 保存筛选页面数据到本地
     */
    suspend fun saveFilteredVideos(videos: List<Video>, videoType: Int) {
        // 转换为实体类
        val entities = videos.map { video ->
            VideoEntity.fromVideo(video, videoType, "filter")
        }

        // 保存到数据库
        videoDao.refreshFilterData(entities, videoType)
    }

    /**
     * 从本地获取解说视频
     */
    suspend fun getLocalShorts(): List<Video> {
        val videos = mutableListOf<Video>()
        videoDao.getShorts().map { entities ->
            entities.map { it.toVideo() }
        }.catch {
            emit(emptyList())
        }.flowOn(Dispatchers.IO).collect {
            videos.addAll(it)
        }
        return videos
    }

    /**
     * 保存解说页面数据到本地
     */
    suspend fun saveShortsVideos(videos: List<Video>) {
        // 转换为实体类
        val entities = videos.map { video ->
            VideoEntity.fromVideo(video, 8, "shorts")
        }

        // 保存到数据库
        videoDao.refreshShortsData(entities)
    }

    /**
     * 首页数据类
     */
    data class HomeData(
        val banners: List<Video> = emptyList(),
        val recommendedMovies: List<Video> = emptyList(),
        val tvSeries: List<Video> = emptyList(),
        val comics: List<Video> = emptyList()
    )
}
