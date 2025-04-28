package com.vlog.app.data.api

import com.vlog.app.data.categories.Category
import com.vlog.app.data.ApiResponse
import com.vlog.app.data.versions.AppVersion
import com.vlog.app.data.FilterListResponse
import com.vlog.app.data.videos.Gather
import com.vlog.app.data.videos.Player
import com.vlog.app.data.users.User
import com.vlog.app.data.videos.Actor
import com.vlog.app.data.comments.Comment
import com.vlog.app.data.comments.CommentRequest
import com.vlog.app.data.videos.Genre
import com.vlog.app.data.videos.Video
import retrofit2.http.*

interface ApiService {

    // 用户相关接口
    @GET(ApiConstants.ENDPOINT_USER_CHECK_NAME)
    suspend fun checkUserExists(
        @Query("username") username: String
    ): ApiResponse<Boolean>

    @GET(ApiConstants.ENDPOINT_USER_CHECK_NICKNAME)
    suspend fun checkNicknameExists(
        @Query("nickname") nickname: String
    ): ApiResponse<Boolean>

    @POST(ApiConstants.ENDPOINT_USER_LOGIN)
    suspend fun login(
        @Query("username") username: String,
        @Query("password") password: String
    ): ApiResponse<User>

    @POST(ApiConstants.ENDPOINT_USER_REGISTER)
    suspend fun register(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("nickname") nickname: String
    ): ApiResponse<User>

    @GET(ApiConstants.ENDPOINT_USER_INFO)
    suspend fun getUserInfo(
        @Path("name") username: String,
        @Path("token") token: String
    ): ApiResponse<User>

    @Multipart
    @POST(ApiConstants.ENDPOINT_USER_UPDATE)
    suspend fun updateUserInfo(
        @Path("name") username: String,
        @Path("token") token: String,
        @Query("nickname") nickname: String?,
        @Part avatar_file: okhttp3.MultipartBody.Part?
    ): ApiResponse<User>

    // 应用更新接口
    @GET(ApiConstants.ENDPOINT_CHECK_UPDATE)
    suspend fun checkUpdate(): ApiResponse<AppVersion>

    @POST(ApiConstants.ENDPOINT_COMMENTS_POST)
    suspend fun postComment(
        @Path("id") videoId: String,
        @Path("typed") typed: Int,
        @Body comment: CommentRequest
    ): ApiResponse<Boolean>

    @GET(ApiConstants.ENDPOINT_COMMENTS)
    suspend fun getComments(
        @Path("id") videoId: String,
        @Path("typed") typed: Int
    ): ApiResponse<List<Comment>>

    @GET(ApiConstants.ENDPOINT_GATHER)
    suspend fun getGatherList(): ApiResponse<List<Gather>>

    @GET(ApiConstants.ENDPOINT_VIDEO_DETAIL)
    suspend fun getVideoDetail(
        @Path("id") videoId: String,
        @Query("gather") gather: String? = null
    ): ApiResponse<Video>


    @GET(ApiConstants.ENDPOINT_MORE_LIKED)
    suspend fun getMoreLiked(
        @Path("id") videoId: String,
        @Path("orderBy") orderBy: Int
    ): ApiResponse<List<Video>>

    @GET(ApiConstants.ENDPOINT_VIDEO_LIST)
    suspend fun getVideoList(
        @Path("typed") typed: Int,
        @Path("released") released: Int,
        @Path("orderBy") orderBy: Int
    ): ApiResponse<List<Video>>

    @GET(ApiConstants.ENDPOINT_VIDEO_LIST)
    suspend fun getVideoListWithPage(
        @Path("typed") typed: Int,
        @Path("released") released: Int,
        @Path("orderBy") orderBy: Int,
        @Query("page") page: Int
    ): ApiResponse<List<Video>>

    @GET(ApiConstants.ENDPOINT_FILTER_LIST)
    suspend fun getFilteredList(
        @Query("typed") typed: Int? = null,
        @Query("year") year: Int? = null,
        @Query("released") released: Int? = null,
        @Query("order_by") orderBy: Int? = null,
        @Query("page") page: Int = 1,
        @Query("cate") cate: String? = null,
        @Query("code") code: String? = null
    ): ApiResponse<FilterListResponse>

    @GET(ApiConstants.ENDPOINT_CATEGORIES)
    suspend fun getCategories(): ApiResponse<List<Category>>



    /**
     * 搜索视频
     * @param key 搜索关键词，为空时返回热门搜索
     * @return 搜索结果列表
     */
    @GET(ApiConstants.ENDPOINT_SEARCH)
    suspend fun searchVideos(
        @Query("key") key: String? = null
    ): ApiResponse<List<Video>>
}
