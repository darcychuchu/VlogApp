package com.vlog.app.data.videos

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 视频实体类，用于本地存储视频数据
 */
@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val cover: String, // 兼容旧版本，新版本使用coverUrl
    val score: String,
    val description: String?,
    val director: String?,
    val actors: String?,
    val region: String?,
    val language: String?,
    val alias: String?,
    val categoryId: String?,
    val tags: String?,
    val author: String?,
    val playerUrl: String?, // 兼容旧版本，新版本使用videoPlayList中的播放地址
    val typeName: String?,
    val attachmentId: String?,
    val coverUrl: String?,
    val videoType: Int, // 视频类型：0-未知，1-电影，2-电视剧，3-动漫，4-综艺，5-体育赛事，8-影视解说，9-预告片
    val lastUpdated: Long = System.currentTimeMillis(), // 最后更新时间
    val pageSource: String // 来源页面：home-首页，filter-筛选页，shorts-解说页
) {
    companion object {
        /**
         * 将 Video 对象转换为 VideoEntity 对象
         */
        fun fromVideo(video: Video, videoType: Int, pageSource: String): VideoEntity {
            return VideoEntity(
                id = video.id,
                title = video.title,
                cover = video.cover,
                score = video.score,
                description = video.description,
                director = video.director,
                actors = video.actors,
                region = video.region,
                language = video.language,
                alias = video.alias,
                categoryId = video.categoryId,
                tags = video.tags,
                author = video.author,
                playerUrl = video.playerUrl,
                typeName = video.typeName,
                attachmentId = video.attachmentId,
                coverUrl = video.coverUrl,
                videoType = videoType,
                pageSource = pageSource
            )
        }
    }

    /**
     * 将 VideoEntity 对象转换为 Video 对象
     */
    fun toVideo(): Video {
        return Video(
            id = id,
            title = title,
            cover = cover,
            score = score,
            description = description,
            director = director,
            actors = actors,
            region = region,
            language = language,
            alias = alias,
            categoryId = categoryId,
            tags = tags,
            author = author,
            playerUrl = playerUrl,
            typeName = typeName,
            attachmentId = attachmentId ?: "",
            coverUrl = coverUrl
        )
    }
}