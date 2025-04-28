package com.vlog.app.data.versions

import com.vlog.app.data.api.ApiService
import com.vlog.app.data.versions.AppVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 应用更新仓库
 * 负责从API获取应用版本信息
 */
class AppUpdateRepository(
    private val apiService: ApiService
) {
    /**
     * 检查应用更新
     * @return 应用版本信息
     */
    suspend fun checkUpdate(): Result<AppVersion> = withContext(Dispatchers.IO) {
        try {
            try {
                // 尝试调用真实API
                val response = apiService.checkUpdate()
                if (response.code == 0) {
                    Result.success(response.data)
                } else {
                    // API调用失败，使用模拟数据
                    val mockAppVersion = AppVersion(
                        versionCode = 2,
                        versionName = "1.1.0",
                        forceUpdate = false,
                        downloadUrl = "https://example.com/app-release.apk",
                        description = "新版本功能更新:\n1. 修复了已知问题\n2. 添加了新功能\n3. 优化了用户体验",
                        fileSize = 15 * 1024 * 1024, // 15MB
                        md5 = "abcdef1234567890abcdef1234567890"
                    )
                    Result.success(mockAppVersion)
                }
            } catch (e: Exception) {
                // API调用异常，使用模拟数据
                val mockAppVersion = AppVersion(
                    versionCode = 2,
                    versionName = "1.1.0",
                    forceUpdate = false,
                    downloadUrl = "https://example.com/app-release.apk",
                    description = "新版本功能更新:\n1. 修复了已知问题\n2. 添加了新功能\n3. 优化了用户体验",
                    fileSize = 15 * 1024 * 1024, // 15MB
                    md5 = "abcdef1234567890abcdef1234567890"
                )
                Result.success(mockAppVersion)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}