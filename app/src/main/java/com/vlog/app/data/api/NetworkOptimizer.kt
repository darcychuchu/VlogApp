package com.vlog.app.data.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

/**
 * 网络优化工具类，用于提高视频流的加载速度
 */
object NetworkOptimizer {
    
    private const val TAG = "NetworkOptimizer"
    private const val CACHE_SIZE = 50 * 1024 * 1024L // 50MB
    
    /**
     * 创建带有缓存的 OkHttpClient
     */
    fun createCachedOkHttpClient(context: Context): OkHttpClient {
        val cacheDir = File(context.cacheDir, "video_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        
        val cache = Cache(cacheDir, CACHE_SIZE)
        
        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor { chain ->
                var request = chain.request()
                
                // 根据网络状况设置缓存策略
                if (isNetworkAvailable(context)) {
                    // 有网络时，设置缓存超时时间为1小时
                    request = request.newBuilder()
                        .header("Cache-Control", "public, max-age=3600")
                        .build()
                    Log.d(TAG, "Network available, using network with cache")
                } else {
                    // 无网络时，设置缓存超时时间为7天
                    request = request.newBuilder()
                        .header("Cache-Control", "public, only-if-cached, max-stale=604800")
                        .build()
                    Log.d(TAG, "No network, using cache only")
                }
                
                chain.proceed(request)
            }
            .build()
    }
    
    /**
     * 检查网络是否可用
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    /**
     * 获取当前网络类型
     */
    fun getNetworkType(context: Context): NetworkType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.NONE
        }
    }
    
    /**
     * 根据网络类型获取最佳视频质量
     */
    fun getBestVideoQuality(context: Context): VideoQuality {
        return when (getNetworkType(context)) {
            NetworkType.WIFI, NetworkType.ETHERNET -> VideoQuality.HIGH
            NetworkType.CELLULAR -> VideoQuality.MEDIUM
            NetworkType.NONE -> VideoQuality.LOW
        }
    }
    
    /**
     * 网络类型枚举
     */
    enum class NetworkType {
        WIFI, CELLULAR, ETHERNET, NONE
    }
    
    /**
     * 视频质量枚举
     */
    enum class VideoQuality {
        LOW, MEDIUM, HIGH
    }
}
