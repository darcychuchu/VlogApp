package com.vlog.app.navigation

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vlog.app.screens.detail.VideoDetailScreen
import com.vlog.app.screens.filter.FilterScreen
import com.vlog.app.screens.home.HomeScreen
import com.vlog.app.screens.search.SearchScreen
import com.vlog.app.screens.shorts.ShortsScreen
import com.vlog.app.screens.user.UserScreen
import com.vlog.app.screens.user.WatchHistoryScreen

/**
 * 主导航图
 * 包含应用的所有导航路由
 */
@Composable
fun MainNavGraph(
    navController: NavHostController,
    startDestination: String = NavigationRoutes.MainRoute.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 主导航路由
        addMainRoutes(navController)

        // 全屏导航路由
        addFullScreenRoutes(navController)

        // 其他导航路由
        addOtherRoutes(navController)
    }
}

/**
 * 添加主导航路由
 */
private fun NavGraphBuilder.addMainRoutes(navController: NavHostController) {
    // 首页
    composable(NavigationRoutes.MainRoute.Home.route) {
        HomeScreen(navController)
    }

    // 筛选页面
    composable(
        route = "${NavigationRoutes.MainRoute.Filter.route}?typed={typed}",
        arguments = listOf(
            navArgument("typed") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val typed = backStackEntry.arguments?.getString("typed")
        FilterScreen(navController, typed = typed)
    }

    // 短视频页面
    composable(NavigationRoutes.MainRoute.Shorts.route) {
        ShortsScreen(navController)
    }

    // 用户页面
    composable(NavigationRoutes.MainRoute.User.route) {
        UserScreen(navController)
    }
}

/**
 * 添加全屏导航路由
 */
@OptIn(UnstableApi::class)
private fun NavGraphBuilder.addFullScreenRoutes(navController: NavHostController) {
    // 视频详情页面
    composable(
        route = NavigationRoutes.FullScreenRoute.VideoDetail.route,
        arguments = listOf(
            navArgument("videoId") {
                type = NavType.StringType
                nullable = false
            }
        )
    ) { backStackEntry ->
        val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
        // 设置为全屏导航
        NavigationManager.setNavigationType(NavigationType.FULLSCREEN)
        VideoDetailScreen(navController, videoId)
    }

    // 视频播放器页面
    composable(
        route = NavigationRoutes.FullScreenRoute.VideoPlayer.route,
        arguments = listOf(
            navArgument("videoId") {
                type = NavType.StringType
                nullable = false
            },
            navArgument("url") {
                type = NavType.StringType
                nullable = false
                defaultValue = ""
            },
            navArgument("title") {
                type = NavType.StringType
                nullable = true
                defaultValue = ""
            },
            navArgument("position") {
                type = NavType.LongType
                defaultValue = 0L
            }
        )
    ) { backStackEntry ->
        val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
        val url = backStackEntry.arguments?.getString("url") ?: ""
        val title = backStackEntry.arguments?.getString("title") ?: ""
        val position = backStackEntry.arguments?.getLong("position") ?: 0L

        // 设置为全屏导航
        NavigationManager.setNavigationType(NavigationType.FULLSCREEN)

        // 解码URL
        val decodedUrl = java.net.URLDecoder.decode(url, "UTF-8")
        val decodedTitle = java.net.URLDecoder.decode(title, "UTF-8")

    }
}

/**
 * 添加其他导航路由
 */
private fun NavGraphBuilder.addOtherRoutes(navController: NavHostController) {
    // 搜索页面
    composable(NavigationRoutes.OtherRoute.Search.route) {
        SearchScreen(navController)
    }

    // 观看历史页面
    composable(NavigationRoutes.OtherRoute.WatchHistory.route) {
        WatchHistoryScreen(navController)
    }
}
