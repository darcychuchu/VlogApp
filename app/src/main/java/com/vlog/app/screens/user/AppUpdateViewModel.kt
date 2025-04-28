package com.vlog.app.screens.user

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.data.versions.AppUpdatePreferences
import com.vlog.app.data.versions.AppVersion
import com.vlog.app.data.versions.AppUpdateRepository
import com.vlog.app.utils.AppUpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 应用更新ViewModel
 */
class AppUpdateViewModel(
    private val appUpdateRepository: AppUpdateRepository,
    private val appUpdatePreferences: AppUpdatePreferences
) : ViewModel() {

    // UI状态
    data class UiState(
        val isLoading: Boolean = false,
        val appVersion: AppVersion? = null,
        val error: String? = null,
        val showUpdateDialog: Boolean = false,
        val needUpdate: Boolean = false,
        val hasNewVersion: Boolean = false,  // 标记是否有新版本，用于显示"new"标记
        val lastPromptTime: Long = 0  // 上次提示时间，用于实现24小时内不再提示
    )

    private val _uiState = MutableStateFlow(UiState(
        lastPromptTime = appUpdatePreferences.getLastPromptTime()
    ))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * 检查应用更新
     */
    fun checkUpdate(appUpdateManager: AppUpdateManager) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                appUpdateRepository.checkUpdate().fold(
                    onSuccess = { appVersion ->
                        val needUpdate = appUpdateManager.needUpdate(appVersion)
                        val currentTime = System.currentTimeMillis()
                        val lastPromptTime = _uiState.value.lastPromptTime

                        // 判断是否需要显示更新对话框
                        // 1. 需要更新
                        // 2. 上次提示时间距离现在超过24小时，或者是强制更新，或者是用户手动检查
                        val showDialog = needUpdate && (
                            appVersion.forceUpdate ||
                            (currentTime - lastPromptTime > 24 * 60 * 60 * 1000) ||
                            lastPromptTime == 0L  // 如果上次提示时间为0，表示用户手动检查
                        )

                        Log.d("AppUpdateViewModel", "Update check: needUpdate=$needUpdate, forceUpdate=${appVersion.forceUpdate}, "
                            + "timeSinceLastPrompt=${(currentTime - lastPromptTime) / (60 * 60 * 1000)}h, lastPromptTime=$lastPromptTime, showDialog=$showDialog")

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                appVersion = appVersion,
                                needUpdate = needUpdate,
                                hasNewVersion = needUpdate,  // 有新版本时显示"new"标记
                                showUpdateDialog = showDialog,
                                // 保持上次提示时间不变，除非用户明确选择延迟
                                lastPromptTime = it.lastPromptTime
                            )
                        }
                    },
                    onFailure = { e ->
                        Log.e("AppUpdateViewModel", "Error checking update", e)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "Unknown error"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("AppUpdateViewModel", "Exception checking update", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    /**
     * 显示更新对话框
     */
    fun showUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = true) }
    }

    /**
     * 清除延迟设置，用于用户主动点击“检查更新”时
     */
    fun clearPostponeSettings() {
        // 清除持久化存储中的上次提示时间
        appUpdatePreferences.clearLastPromptTime()

        // 更新UI状态
        _uiState.update {
            it.copy(lastPromptTime = 0)
        }

        Log.d("AppUpdateViewModel", "Postpone settings cleared. Will check for updates immediately.")
    }

    /**
     * 隐藏更新对话框
     */
    fun hideUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    /**
     * 延后更新，记录当前时间作为上次提示时间
     */
    fun postponeUpdate() {
        val currentTime = System.currentTimeMillis()

        // 保存到持久化存储
        appUpdatePreferences.saveLastPromptTime(currentTime)

        _uiState.update {
            it.copy(
                showUpdateDialog = false,
                lastPromptTime = currentTime
            )
        }

        Log.d("AppUpdateViewModel", "Update postponed for 24 hours. Next prompt after: ${currentTime + 24 * 60 * 60 * 1000}")
    }
}
