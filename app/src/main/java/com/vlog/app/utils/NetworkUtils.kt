package com.vlog.app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log

/**
 * 网络工具类
 */
object NetworkUtils {
    private const val TAG = "NetworkUtils"

    /**
     * 检查网络是否可用
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * 检查网络连接并记录日志
     */
    fun checkNetworkConnection(context: Context): Boolean {
        val isConnected = isNetworkAvailable(context)
        if (!isConnected) {
            Log.e(TAG, "Network is not available")
        } else {
            Log.d(TAG, "Network is available")
        }
        return isConnected
    }
}
