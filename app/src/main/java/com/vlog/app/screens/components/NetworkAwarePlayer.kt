package com.vlog.app.screens.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.UnstableApi
import com.vlog.app.data.api.NetworkOptimizer

/**
 * 网络感知的视频播放器，根据网络状态自动调整视频质量
 */
@UnstableApi
@Composable
fun NetworkAwarePlayer(
    url: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentNetworkType by remember { mutableStateOf(NetworkOptimizer.getNetworkType(context)) }

    // 监听网络状态变化
    DisposableEffect(Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val newNetworkType = when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkOptimizer.NetworkType.WIFI
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkOptimizer.NetworkType.CELLULAR
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkOptimizer.NetworkType.ETHERNET
                    else -> NetworkOptimizer.NetworkType.NONE
                }

                if (newNetworkType != currentNetworkType) {
                    Log.d("NetworkAwarePlayer", "Network type changed from $currentNetworkType to $newNetworkType")
                    currentNetworkType = newNetworkType
                }
            }

            override fun onLost(network: Network) {
                Log.d("NetworkAwarePlayer", "Network lost")
                currentNetworkType = NetworkOptimizer.NetworkType.NONE
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        onDispose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    // 根据网络类型调整视频质量
    val videoQuality = when (currentNetworkType) {
        NetworkOptimizer.NetworkType.WIFI, NetworkOptimizer.NetworkType.ETHERNET -> {
            Log.d("NetworkAwarePlayer", "Using high quality for WiFi/Ethernet")
            VideoQuality.HIGH
        }
        NetworkOptimizer.NetworkType.CELLULAR -> {
            Log.d("NetworkAwarePlayer", "Using medium quality for cellular")
            VideoQuality.MEDIUM
        }
        NetworkOptimizer.NetworkType.NONE -> {
            Log.d("NetworkAwarePlayer", "Using low quality for no network")
            VideoQuality.LOW
        }
    }


}

/**
 * 视频质量枚举
 */
enum class VideoQuality {
    LOW, MEDIUM, HIGH
}
