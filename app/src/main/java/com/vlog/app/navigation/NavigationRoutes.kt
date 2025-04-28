package com.vlog.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import com.vlog.app.R

/**
 * 导航路由
 * 定义应用中的所有导航路由
 */
object NavigationRoutes {
    // 主导航路由
    sealed class MainRoute(val route: String, val resourceId: Int, val icon: ImageVector) {
        object Home : MainRoute("home", R.string.home, Icons.Default.Home)
        object Filter : MainRoute("filter", R.string.filter, Icons.AutoMirrored.Filled.List)
        object Shorts : MainRoute("shorts", R.string.shorts, Icons.Default.PlayArrow)
        object User : MainRoute("user", R.string.user, Icons.Default.Person)
    }

    // 全屏导航路由
    sealed class FullScreenRoute(val route: String) {
        object VideoDetail : FullScreenRoute("video/{videoId}") {
            fun createRoute(videoId: String) = "video/$videoId"
        }
        object VideoPlayer : FullScreenRoute("player/{videoId}?url={url}&title={title}&position={position}") {
            fun createRoute(videoId: String, url: String, title: String, position: Long = 0) =
                "player/$videoId?url=${url.encodeUrl()}&title=${title.encodeUrl()}&position=$position"
        }
    }

    // URL编码
    private fun String.encodeUrl(): String {
        return java.net.URLEncoder.encode(this, "UTF-8")
    }

    // 其他导航路由
    sealed class OtherRoute(val route: String) {
        object Search : OtherRoute("search")
        object WatchHistory : OtherRoute("watch_history")
    }

    // 底部导航项
    val bottomNavItems = listOf(
        MainRoute.Home,
        MainRoute.Filter,
        MainRoute.Shorts,
        MainRoute.User
    )

    // 判断路由是否为全屏路由
    fun isFullScreenRoute(route: String?): Boolean {
        if (route == null) return false
        return route.startsWith("video/") || route.startsWith("player/")
    }
}
