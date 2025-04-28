package com.vlog.app.data.api

/**
 * API常量
 * 包含API基础URL、图片URL和所有API端点
 */
object ApiConstants {
    // 基础URL
    const val BASE_URL = "https://66log.com/api/json/v1/"
    const val VLOG_APP = "https://66log.com"
    const val VLOG_APP_BLOG = "$VLOG_APP/blog/"

    // 图片相关URL
    const val IMAGE_BASE_URL = "$VLOG_APP/file/attachments/"
    const val IMAGE_BASE_URL_SMALL = "$IMAGE_BASE_URL/image/s/"

    // 图片尺寸
    const val IMAGE_SIZE_SMALL = "s/"
    const val IMAGE_SIZE_MEDIUM = "m/"
    const val IMAGE_SIZE_BIG = "l/"

    // API版本
    const val APP_VERSION = "1.0.4"

    // API端点 - 应用更新
    const val ENDPOINT_CHECK_UPDATE = "app/version"

    // API端点 - 用户相关
    const val ENDPOINT_USER_CHECK_NAME = "users/stated-name"
    const val ENDPOINT_USER_CHECK_NICKNAME = "users/stated-nickname"
    const val ENDPOINT_USER_LOGIN = "users/login"
    const val ENDPOINT_USER_REGISTER = "users/register"
    const val ENDPOINT_USER_INFO = "users/stated-me/{name}/{token}"
    const val ENDPOINT_USER_UPDATE = "users/updated/{name}/{token}"

    // API端点 - 视频相关
    const val ENDPOINT_COMMENTS_POST = "videos/comments-post/{id}/{typed}" // 发表评论

    const val ENDPOINT_COMMENTS = "videos/comments/{id}/{typed}" // 获取评论列表

    const val ENDPOINT_GATHER = "videos/gather" // 获取所有服务商列表

    // 视频详情API，现包含演员、标签、服务商和播放列表等信息
    const val ENDPOINT_VIDEO_DETAIL = "videos/detail/{id}"

    const val ENDPOINT_MORE_LIKED = "videos/more-liked/{id}/{orderBy}" // 获取相关推荐视频
    const val ENDPOINT_VIDEO_LIST = "videos/list/{typed}/{released}/{orderBy}" // 首页视频列表
    const val ENDPOINT_FILTER_LIST = "videos/list" // 筛选页面视频列表
    const val ENDPOINT_CATEGORIES = "videos/categories" // 获取分类列表
    const val ENDPOINT_SEARCH = "videos/search" // 搜索视频
}
