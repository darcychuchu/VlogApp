package com.vlog.app.data.versions

import android.content.Context
import android.content.SharedPreferences

/**
 * 应用更新偏好设置
 * 用于存储应用更新相关的设置
 */
class AppUpdatePreferences(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME, Context.MODE_PRIVATE
    )

    /**
     * 保存上次更新提示时间
     */
    fun saveLastPromptTime(time: Long) {
        sharedPreferences.edit().putLong(KEY_LAST_PROMPT_TIME, time).apply()
    }

    /**
     * 获取上次更新提示时间
     */
    fun getLastPromptTime(): Long {
        return sharedPreferences.getLong(KEY_LAST_PROMPT_TIME, 0)
    }

    /**
     * 清除上次更新提示时间
     */
    fun clearLastPromptTime() {
        sharedPreferences.edit().remove(KEY_LAST_PROMPT_TIME).apply()
    }

    companion object {
        private const val PREF_NAME = "app_update_preferences"
        private const val KEY_LAST_PROMPT_TIME = "last_prompt_time"

        @Volatile
        private var INSTANCE: AppUpdatePreferences? = null

        fun getInstance(context: Context): AppUpdatePreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = AppUpdatePreferences(context)
                INSTANCE = instance
                instance
            }
        }
    }
}