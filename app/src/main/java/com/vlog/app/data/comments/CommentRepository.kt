package com.vlog.app.data.comments

import com.vlog.app.data.api.ApiService
import com.vlog.app.data.comments.Comment
import com.vlog.app.data.comments.CommentRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CommentRepository(private val apiService: ApiService) {

    suspend fun getComments(videoId: String, typed: Int): List<Comment> {
        return withContext(Dispatchers.IO) {
            apiService.getComments(videoId, typed).data
        }
    }

    suspend fun postComment(videoId: String, typed: Int, content: String): Boolean {
        return withContext(Dispatchers.IO) {
            val request = CommentRequest(content)
            apiService.postComment(videoId, typed, request).data
        }
    }
}