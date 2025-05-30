package com.vlog.app.screens

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import com.vlog.app.screens.components.CommonBottomBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.vlog.app.navigation.MainNavGraph
import com.vlog.app.navigation.NavigationManager
import com.vlog.app.navigation.NavigationRoutes
import com.vlog.app.navigation.NavigationType
import com.vlog.app.navigation.rememberNavigationState
import com.vlog.app.ui.theme.VlogAppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {

    // 应用状态
    private val _appState = MutableStateFlow(AppState())
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            //val windowSize = calculateWindowSizeClass(this)
            VlogAppTheme {
                NavigationApp()
            }
        }
    }
}

/**
 * 应用状态
 */
data class AppState(
    val isPlayerVisible: Boolean = false
)

/**
 * 应用主界面
 */
@Composable
fun NavigationApp() {
    // 创建导航状态
    val navigationState = rememberNavigationState()
    val navController = navigationState.navController

    // 获取当前导航类型
    val navigationType by NavigationManager.currentNavigationType.collectAsState()

    // 获取当前路由
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    // 检查当前路由是否是全屏路由
    val isFullScreenRoute = NavigationRoutes.isFullScreenRoute(currentRoute)

    // 如果是全屏路由，设置导航类型为全屏
    if (isFullScreenRoute && navigationType != NavigationType.FULLSCREEN) {
        NavigationManager.setNavigationType(NavigationType.FULLSCREEN)
    }

    // 根据导航类型显示不同的界面
    when (navigationType) {
        NavigationType.MAIN -> {
            // 主导航界面，包含底部导航栏
            MainLayout(
                navController = navController,
                currentDestination = currentDestination
            )
        }
        NavigationType.FULLSCREEN -> {
            // 全屏导航界面，不包含底部导航栏
            Box(modifier = Modifier.fillMaxSize()) {
                MainNavGraph(navController = navController)
            }
        }
        NavigationType.OVERLAY -> {
            // 覆盖导航界面，显示在当前页面上方
            Box(modifier = Modifier.fillMaxSize()) {
                MainNavGraph(navController = navController)
                // 视频播放器覆盖层已移除
            }
        }
    }

    // 如果当前路由不是全屏路由，但导航类型是FULLSCREEN，重置为主导航
    LaunchedEffect(currentRoute) {
        if (!isFullScreenRoute && navigationType == NavigationType.FULLSCREEN) {
            NavigationManager.resetToMainNavigation()
        }
    }
}

/**
 * 主布局
 * 包含底部导航栏和主内容
 */
@Composable
fun MainLayout(
    navController: NavHostController,
    currentDestination: NavDestination?
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            CommonBottomBar(
                navController = navController,
                currentDestination = currentDestination
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
            MainNavGraph(navController = navController)
        }
    }
}


