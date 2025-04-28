package com.vlog.app.data.videos

import android.util.Log
import com.vlog.app.data.api.ApiService
import com.vlog.app.data.base.BaseRepository
import com.vlog.app.data.base.Resource
import com.vlog.app.data.categories.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 视频数据仓库
 * 负责处理视频相关的网络请求和数据缓存
 */
class VideoRepository(private val apiService: ApiService) : BaseRepository {

    companion object {
        private const val TAG = "VideoRepository"
    }

    /**
     * 获取视频列表
     * @param typed 视频类型
     * @param released 发布状态
     * @param orderBy 排序方式
     * @return 视频列表结果流
     */
    suspend fun getVideoList(typed: Int, released: Int, orderBy: Int): Result<List<Video>> {
        return safeApiCall {
            apiService.getVideoList(typed, released, orderBy).data
        }
    }

    /**
     * 获取分页视频列表
     * @param typed 视频类型
     * @param released 发布状态
     * @param orderBy 排序方式
     * @param page 页码
     * @return 视频列表结果流
     */
    suspend fun getVideoListWithPage(typed: Int, released: Int, orderBy: Int, page: Int): Result<List<Video>> {
        return safeApiCall {
            apiService.getFilteredList(typed, 0, released, orderBy, page).data.items
        }
    }

    /**
     * 获取筛选视频列表
     * @param typed 视频类型
     * @param year 年份
     * @param released 发布状态
     * @param orderBy 排序方式
     * @param page 页码
     * @param cate 分类ID
     * @param code 代码
     * @return 视频列表结果流
     */
    suspend fun getFilteredList(
        typed: Int? = null,
        year: Int? = null,
        released: Int? = null,
        orderBy: Int? = null,
        page: Int = 1,
        cate: String? = null,
        code: String? = null
    ): Result<List<Video>> {
        return safeApiCall {
            apiService.getFilteredList(typed, year, released, orderBy, page, cate, code).data.items
        }
    }

    /**
     * 获取分类列表
     * @return 分类列表结果流
     */
    suspend fun getCategories(): Result<List<Category>> {
        return safeApiCall {
            apiService.getCategories().data ?: emptyList()
        }
    }

    /**
     * 获取视频服务商列表
     * @param videoId 视频ID
     * @return 服务商列表结果流
     */
    suspend fun getGathersFromVideoDetail(videoId: String): Result<List<Gather>> {
        // 从视频详情中获取服务商列表
        val detailResult = getVideoDetail(videoId)
        if (detailResult.isSuccess) {
            val detail = detailResult.getOrNull()
            if (detail != null && detail.videoPlayList.isNotEmpty()) {
                // 使用Video模型中的getGathers方法获取服务商列表
                return Result.success(detail.getGathers())
            }
        }

        return Result.failure(Exception("无法获取视频服务商列表"))
    }

    /**
     * 获取视频详情
     * @param videoId 视频ID
     * @param gather 服务商ID
     * @return 视频详情结果流
     */
    suspend fun getVideoDetail(videoId: String, gather: String? = null): Result<Video> {
        return safeApiCall {
            apiService.getVideoDetail(videoId, gather).data
        }
    }

    /**
     * 从视频详情中获取演员列表
     * @param videoId 视频ID
     * @return 演员列表结果流
     */
    suspend fun getActorsFromVideoDetail(videoId: String): Result<List<Actor>> {
        // 从视频详情中获取演员列表
        val detailResult = getVideoDetail(videoId)
        if (detailResult.isSuccess) {
            val detail = detailResult.getOrNull()
            if (detail != null && !detail.actors.isNullOrEmpty()) {
                // 这里需要根据实际情况解析actors字段
                // 假设actors字段是JSON字符串或者逗号分隔的字符串
                // 这里简单返回一个空列表，实际实现需要根据API返回格式调整
                return Result.success(emptyList())
            }
        }

        return Result.failure(Exception("无法获取演员列表"))
    }

    /**
     * 从视频详情中获取类型列表
     * @param videoId 视频ID
     * @return 类型列表结果流
     */
    suspend fun getGenresFromVideoDetail(videoId: String): Result<List<Genre>> {
        // 从视频详情中获取类型列表
        val detailResult = getVideoDetail(videoId)
        if (detailResult.isSuccess) {
            val detail = detailResult.getOrNull()
            if (detail != null && !detail.tags.isNullOrEmpty()) {
                // 这里需要根据实际情况解析tags字段
                // 假设tags字段是JSON字符串或者逗号分隔的字符串
                // 这里简单返回一个空列表，实际实现需要根据API返回格式调整
                return Result.success(emptyList())
            }
        }

        return Result.failure(Exception("无法获取类型列表"))
    }

    /**
     * 获取服务商列表
     * @return 服务商列表结果流
     */
    suspend fun getGatherList(): Result<List<Gather>> {
        return safeApiCall {
            apiService.getGatherList().data
        }
    }

    /**
     * 从视频详情中获取播放器列表
     * @param gather 服务商ID
     * @param videoId 视频ID
     * @return 播放器列表结果流
     */
    suspend fun getPlayersFromVideoDetail(gather: String, videoId: String): Result<List<Player>> {
        // 从视频详情中获取播放列表
        val detailResult = getVideoDetail(videoId)
        if (detailResult.isSuccess) {
            val detail = detailResult.getOrNull()
            if (detail != null) {
                // 查找指定服务商的播放列表
                val playList = detail.getPlayListByGatherId(gather)
                if (playList.isNotEmpty()) {
                    // 将PlayItem转换为Player列表
                    val players = playList.map {
                        Player(
                            id = "0", // 新API中没有id字段，使用默认值
                            videoTitle = it.title,
                            playerUrl = it.playUrl,
                            path = it.path
                        )
                    }
                    return Result.success(players)
                }
            }
        }

        return Result.failure(Exception("无法获取播放列表"))
    }

    /**
     * 获取相关推荐视频
     * @param videoId 视频ID
     * @param orderBy 排序方式
     * @return 视频列表结果流
     */
    suspend fun getMoreLiked(videoId: String, orderBy: Int): Result<List<Video>> {
        return safeApiCall {
            apiService.getMoreLiked(videoId, orderBy).data
        }
    }

    /**
     * 搜索视频
     * @param key 搜索关键词，为空时返回热门搜索
     * @return 搜索结果列表结果流
     */
    suspend fun searchVideos(key: String? = null): Result<List<Video>> {
        return safeApiCall {
            apiService.searchVideos(key).data
        }
    }

    /**
     * 获取视频详情及相关数据
     * 包括视频详情、演员、类型和相关推荐
     * @param videoId 视频ID
     * @param gather 服务商ID (已废弃)
     * @return 视频详情数据流
     */
    fun getVideoDetailWithRelated(videoId: String, gather: String? = null): Flow<Resource<VideoDetailData>> = flow {
        emit(Resource.Loading())

        try {
            // 获取视频详情
            val detailResult = getVideoDetail(videoId, gather)
            if (detailResult.isFailure) {
                emit(Resource.Error("获取视频详情失败: ${detailResult.exceptionOrNull()?.message}"))
                return@flow
            }

            val detail = detailResult.getOrNull()!!

            // 获取相关推荐视频
            val moreLikedResult = getMoreLiked(videoId, 0)

            // 构建结果
            // 注意：演员和类型信息现在已经包含在视频详情中，不需要单独获取
            val videoDetailData = VideoDetailData(
                detail = detail,
                // 这里可以根据实际情况解析detail.actors和detail.tags字段
                actors = emptyList(), // 实际实现需要根据API返回格式调整
                genres = emptyList(), // 实际实现需要根据API返回格式调整
                relatedVideos = moreLikedResult.getOrDefault(emptyList())
            )

            emit(Resource.Success(videoDetailData))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting video detail with related data", e)
            emit(Resource.Error("获取视频详情失败: ${e.message}"))
        }
    }
}