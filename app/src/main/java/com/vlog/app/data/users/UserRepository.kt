package com.vlog.app.data.users

import android.content.Context
import android.net.Uri
import android.util.Log
import com.vlog.app.data.api.ApiService
import com.vlog.app.data.users.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * 用户仓库
 * 负责处理用户相关的数据操作
 */
class UserRepository(
    private val apiService: ApiService,
    private val context: Context
) {

    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 是否存在
     */
    suspend fun checkUserExists(username: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d("UserRepository", "Checking if user exists: $username")
            val response = apiService.checkUserExists(username)
            Log.d("UserRepository", "Check user exists response: $response")

            if (response.code == 0) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error checking if user exists", e)
            Result.failure(e)
        }
    }

    /**
     * 检查昵称是否存在
     * @param nickname 昵称
     * @return 是否存在
     */
    suspend fun checkNicknameExists(nickname: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                Log.d("UserRepository", "Checking if nickname exists: $nickname")
                val response = apiService.checkNicknameExists(nickname)
                Log.d("UserRepository", "Check nickname exists response: $response")

                if (response.code == 0) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "Unknown error"))
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "Error checking if nickname exists", e)
                Result.failure(e)
            }
        }

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 用户信息
     */
    suspend fun login(username: String, password: String): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                Log.d("UserRepository", "Logging in user: $username")
                val response = apiService.login(username, password)
                Log.d("UserRepository", "Login response: $response")

                if (response.code == 0) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "用户名或密码错误"))
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "Error logging in", e)
                Result.failure(e)
            }
        }

    /**
     * 用户注册
     * @param username 用户名
     * @param password 密码
     * @param nickname 昵称
     * @return 用户信息
     */
    suspend fun register(username: String, password: String, nickname: String): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                Log.d("UserRepository", "Registering user: $username, nickname: $nickname")
                val response = apiService.register(username, password, nickname)
                Log.d("UserRepository", "Register response: $response")

                if (response.code == 0) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "注册失败"))
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "Error registering user", e)
                Result.failure(e)
            }
        }

    /**
     * 获取用户信息
     * @param username 用户名
     * @param token 访问令牌
     * @return 用户信息
     */
    suspend fun getUserInfo(username: String, token: String): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                Log.d("UserRepository", "Getting user info: $username")
                val response = apiService.getUserInfo(username, token)
                Log.d("UserRepository", "Get user info response: $response")

                if (response.code == 0) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "获取用户信息失败"))
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "Error getting user info", e)
                Result.failure(e)
            }
        }

    /**
     * 更新用户信息
     * @param username 用户名
     * @param token 访问令牌
     * @param nickname 新昵称（可选）
     * @param avatarUri 新头像文件URI（可选）
     * @return 更新后的用户信息
     */
    suspend fun updateUserInfo(username: String, token: String, nickname: String?, avatarUri: Uri?): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(
                    "UserRepository",
                    "Updating user info: $username, nickname: $nickname, avatarUri: $avatarUri"
                )

                // 准备头像文件（如果有）
                val avatarPart = avatarUri?.let { uri ->
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val file = File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
                    inputStream?.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("avatar_file", file.name, requestBody)
                }

                val response = apiService.updateUserInfo(username, token, nickname, avatarPart)
                Log.d("UserRepository", "Update user info response: $response")

                if (response.code == 0) {
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "更新用户信息失败"))
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "Error updating user info", e)
                Result.failure(e)
            }
        }
}