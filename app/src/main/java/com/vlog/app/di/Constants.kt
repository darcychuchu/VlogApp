package com.vlog.app.di

object Constants {

    const val VLOG_APP = "https://66log.com"
    const val API_BASE_URL = "${VLOG_APP}/api/json/v1/"
    const val IMAGE_BASE_URL = "${VLOG_APP}/file/attachments/"
    const val VLOG_APP_BLOG= "${VLOG_APP}/blog/"

    const val IMAGE_SIZE_SMALL = "s/"
    const val IMAGE_SIZE_MEDIUM = "m/"
    const val IMAGE_SIZE_BIG = "l/"

    const val APP_VERSION = "1.0.4"


    // update
    const val ENDPOINT_CHECK_UPDATE = "app/version"

    // users
    const val ENDPOINT_USER_CHECK_NAME = "users/stated-name"
    const val ENDPOINT_USER_CHECK_NICKNAME = "users/stated-nickname"
    const val ENDPOINT_USER_LOGIN = "users/login"
    const val ENDPOINT_USER_REGISTER = "users/register"
    const val ENDPOINT_USER_INFO = "users/stated-me/{name}/{token}"
    const val ENDPOINT_USER_UPDATE = "users/updated/{name}/{token}"


    // videos
    const val ENDPOINT_COMMENTS_POST = "videos/comments-post/{id}/{typed}"
    const val ENDPOINT_ACTORS = "videos/actors/{id}"
    const val ENDPOINT_GENRES = "videos/genres/{id}"
    const val ENDPOINT_COMMENTS = "videos/comments/{id}/{typed}"
    const val ENDPOINT_GATHER = "videos/gather"
    const val ENDPOINT_VIDEO_DETAIL = "videos/detail/{id}"
    const val ENDPOINT_GATHERS = "videos/gathers/{videoId}"
    const val ENDPOINT_PLAYERS = "videos/players/{gatherId}/{videoId}"
    const val ENDPOINT_MORE_LIKED = "videos/more-liked/{id}/{orderBy}"
    const val ENDPOINT_VIDEO_LIST = "videos/list/{typed}/{released}/{orderBy}"
    const val ENDPOINT_FILTER_LIST = "videos/list"
    const val ENDPOINT_CATEGORIES = "videos/categories"
    const val ENDPOINT_SEARCH = "videos/search"
}