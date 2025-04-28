package com.vlog.app.data.videos

import com.squareup.moshi.Json

/**
 * 视频数据模型
 * 包含所有视频相关的数据类
 */

/**
 * 视频模型
 * 用于视频列表和详情展示
 * 在列表中部分字段可能为空，在详情中会包含完整信息
 */
data class Video(
    val id: String,
    val title: String,
    @Json(name = "categoryId")
    val categoryId: String? = null,
    @Json(name = "attachmentId")
    val attachmentId: String = "", // 保留用于多张海报功能
    val cover: String = "", // 兼容旧版本，新版本使用coverUrl
    val coverUrl: String? = null, // 直接使用，无需再次请求API
    val score: String,
    val alias: String? = null,
    val director: String? = null,
    val actors: String? = null,
    val region: String? = null,
    val language: String? = null,
    val description: String? = null,
    val tags: String? = null,
    val author: String? = null,
    val typeName: String? = null,
    // 以下字段主要在详情中使用
    val videoPlayList: List<VideoPlayList> = emptyList(),
    val typed: Int? = null,
    val duration: String? = null,
    val released: String? = null,
    val playerUrl: String? = null, // 兼容旧版本，新版本使用videoPlayList中的播放地址
    val episodeCount: Int? = null
) {
    /**
     * 获取第一个可用的播放地址
     * @return 播放地址，如果没有则返回null
     */
    fun getFirstPlayUrl(): String? {
        return videoPlayList.firstOrNull()?.playList?.firstOrNull()?.playUrl
    }

    /**
     * 获取指定服务商的播放列表
     * @param gatherId 服务商ID
     * @return 播放列表，如果没有则返回空列表
     */
    fun getPlayListByGatherId(gatherId: String): List<PlayItem> {
        return videoPlayList.find { it.gatherId == gatherId }?.playList ?: emptyList()
    }

    /**
     * 获取所有服务商
     * @return 服务商列表
     */
    fun getGathers(): List<Gather> {
        return videoPlayList.map {
            Gather(
                id = it.gatherId,
                title = it.title,
                playerHost = it.playerHost,
                playerPort = it.playerPort,
                remarks = it.remarks,
                countVideo = it.playList.size.toString()
            )
        }
    }

    /**
     * 判断是否为详情数据
     * 通过检查videoPlayList是否存在来判断
     * @return 是否为详情数据
     */
    fun isDetail(): Boolean {
        return videoPlayList.isNotEmpty()
    }
}

/**
 * 视频详情数据类
 * 包含视频详情、演员、类型和相关推荐视频
 */
data class VideoDetailData(
    val detail: Video,
    val actors: List<Actor> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val relatedVideos: List<Video> = emptyList()
)

/**
 * 视频播放列表
 * 包含服务商信息和播放列表
 */
data class VideoPlayList(
    val gatherId: String,
    @Json(name = "gatherTitle")
    val title: String,
    val playerHost: String = "",
    val playerPort: String = "0",
    val remarks: String = "",
    val playList: List<PlayItem> = emptyList()
)

/**
 * 播放项
 * 包含播放标题和播放地址
 */
data class PlayItem(
    val title: String,
    val path: String,
    val playUrl: String
)

/**
 * 服务商
 * 提供视频播放服务的平台
 */
data class Gather(
    val id: String,
    val title: String,
    val playerHost: String = "",
    val playerPort: String = "0",
    val remarks: String = "",
    val countVideo: String = "0"
)

/**
 * 播放器
 * 包含播放地址和标题
 */
data class Player(
    val id: String,
    val videoTitle: String,
    val playerUrl: String,
    val path: String = "",
    val gatherId: String = "", // 兼容旧版本，新版本使用videoPlayList中的gatherId
    val videoId: String = "" // 兼容旧版本，新版本使用video的id
)

/**
 * 演员
 */
data class Actor(
    val id: String,
    val name: String,
    val avatar: String? = null,
    val role: String? = null
)

/**
 * 类型/标签
 */
data class Genre(
    val id: String,
    val name: String
)

/**
 * API响应包装类
 */
data class VideoResponse(
    val data: List<Video>
)

data class VideoDetailResponse(
    val data: Video
)

data class GatherResponse(
    val data: List<Gather>
)

data class PlayerResponse(
    val data: List<Player>
)

data class ActorResponse(
    val data: List<Actor>
)

data class GenreResponse(
    val data: List<Genre>
)
