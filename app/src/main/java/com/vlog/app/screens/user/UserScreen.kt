package com.vlog.app.screens.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.vlog.app.R
import com.vlog.app.utils.AppUpdateManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.vlog.app.screens.components.CommonTopBar
import android.util.Log
import androidx.compose.ui.graphics.vector.ImageVector
import com.vlog.app.data.api.ApiClient
import com.vlog.app.screens.components.UpdateDialog
import com.vlog.app.data.versions.AppUpdateRepository
import com.vlog.app.data.users.UserRepository
import com.vlog.app.data.versions.AppUpdatePreferences
import com.vlog.app.data.users.UserPreferences
import com.vlog.app.navigation.NavigationRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(
    navController: NavController
) {
    // 创建仓库和ViewModel
    val apiService = ApiClient.apiService
    val context = LocalContext.current

    // 创建应用更新仓库和ViewModel
    val appUpdateRepository = remember { AppUpdateRepository(apiService) }
    val appUpdatePreferences = remember { AppUpdatePreferences.getInstance(context) }
    val appUpdateViewModel = remember { AppUpdateViewModel(appUpdateRepository, appUpdatePreferences) }

    // 创建用户仓库和ViewModel
    val userPreferences = remember { UserPreferences.getInstance(context) }
    val userRepository = remember { UserRepository(apiService, context) }
    val userViewModel = remember { UserViewModel(userRepository, userPreferences) }
    // 获取当前UI状态
    val appUpdateUiState by appUpdateViewModel.uiState.collectAsState()
    val userUiState by userViewModel.uiState.collectAsState()

    // 创建AppUpdateManager
    val appUpdateManager = remember { AppUpdateManager(context) }

    // 检查更新 - 只在页面首次加载时检查
    LaunchedEffect(Unit) {
        // 获取上次提示时间
        val lastPromptTime = appUpdatePreferences.getLastPromptTime()
        val currentTime = System.currentTimeMillis()
        val timeSinceLastPrompt = currentTime - lastPromptTime

        Log.d("UserScreen", "Time since last update prompt: ${timeSinceLastPrompt / (60 * 60 * 1000)}h")

        // 检查更新
        appUpdateViewModel.checkUpdate(appUpdateManager)
    }

    // 显示更新对话框
    if (appUpdateUiState.showUpdateDialog && appUpdateUiState.appVersion != null) {
        UpdateDialog(
            appVersion = appUpdateUiState.appVersion!!,
            appUpdateManager = appUpdateManager,
            onDismiss = { appUpdateViewModel.postponeUpdate() },
            onConfirm = {
                try {
                    // 开始下载
                    appUpdateManager.downloadApk(appUpdateUiState.appVersion!!) { success, filePath ->
                        if (success && filePath != null) {
                            // 下载成功，安装APK
                            appUpdateManager.installApk(filePath)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("UserScreen", "Error downloading APK", e)
                }
            }
        )
    }

    // 显示登录表单
    if (userUiState.isLoginFormVisible) {
        LoginForm(
            username = userUiState.loginUsername,
            password = userUiState.loginPassword,
            isLoading = userUiState.isLoading,
            usernameError = userUiState.usernameError,
            passwordError = userUiState.passwordError,
            error = userUiState.error,
            onUsernameChange = userViewModel::updateLoginUsername,
            onPasswordChange = userViewModel::updateLoginPassword,
            onLogin = {
                Log.d("UserScreen", "Login button clicked")
                userViewModel.login()
            },
            onRegister = {
                Log.d("UserScreen", "Switch to register form")
                userViewModel.hideLoginForm()
                userViewModel.showRegisterForm()
            },
            onDismiss = {
                Log.d("UserScreen", "Login form dismissed")
                userViewModel.hideLoginForm()
            }
        )
    }

    // 显示注册表单
    if (userUiState.isRegisterFormVisible) {
        RegisterForm(
            username = userUiState.registerUsername,
            password = userUiState.registerPassword,
            nickname = userUiState.registerNickname,
            isLoading = userUiState.isLoading,
            usernameError = userUiState.usernameError,
            passwordError = userUiState.passwordError,
            nicknameError = userUiState.nicknameError,
            error = userUiState.error,
            onUsernameChange = userViewModel::updateRegisterUsername,
            onPasswordChange = userViewModel::updateRegisterPassword,
            onNicknameChange = userViewModel::updateRegisterNickname,
            onRegister = {
                Log.d("UserScreen", "Register button clicked")
                userViewModel.register()
            },
            onLogin = {
                Log.d("UserScreen", "Switch to login form")
                userViewModel.hideRegisterForm()
                userViewModel.showLoginForm()
            },
            onDismiss = {
                Log.d("UserScreen", "Register form dismissed")
                userViewModel.hideRegisterForm()
            }
        )
    }

    // 显示资料编辑表单
    if (userUiState.isProfileEditVisible) {
        val currentUser = userUiState.user
        ProfileEditForm(
            nickname = userUiState.editNickname,
            selectedAvatarUri = userUiState.selectedAvatarUri,
            currentAvatarUrl = currentUser?.avatar,
            isLoading = userUiState.isLoading,
            nicknameError = userUiState.nicknameError,
            error = userUiState.error,
            onNicknameChange = userViewModel::updateEditNickname,
            onAvatarSelected = userViewModel::updateSelectedAvatar,
            onSave = {
                Log.d("UserScreen", "Save profile button clicked")
                userViewModel.updateUserProfile()
            },
            onDismiss = {
                Log.d("UserScreen", "Profile edit form dismissed")
                userViewModel.hideProfileEdit()
            }
        )
    }

    Scaffold(
        topBar = {
            CommonTopBar(
                title = stringResource(R.string.user),
                navController = navController,
                currentRoute = NavigationRoutes.MainRoute.User.route,
                showSearchIcon = false
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 用户信息卡片
            UserProfileCard(
                user = userUiState.user,
                isLoggedIn = userUiState.isLoggedIn,
                onLoginClick = userViewModel::showLoginForm,
                onRegisterClick = userViewModel::showRegisterForm,
                onLogoutClick = userViewModel::logout,
                onEditProfileClick = userViewModel::showProfileEdit
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 功能项
            Text(
                text = "我的功能",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 观看历史
            FunctionItem(
                icon = Icons.Default.History,
                title = "观看历史",
                onClick = { navController.navigate("watch_history") }
            )

            HorizontalDivider()

            // 设置
            FunctionItem(
                icon = Icons.Default.Settings,
                title = "设置",
                onClick = { /* TODO */ }
            )

            HorizontalDivider()

            // 检查更新
            FunctionItem(
                icon = Icons.Default.Update,
                title = "检查更新",
                badge = if (appUpdateUiState.hasNewVersion) {
                    {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "NEW",
                                color = MaterialTheme.colorScheme.onError,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp
                            )
                        }
                    }
                } else null,
                onClick = {
                    // 清除之前的延迟设置
                    appUpdateViewModel.clearPostponeSettings()
                    // 手动检查更新
                    appUpdateViewModel.checkUpdate(appUpdateManager)
                    // 显示更新对话框
                    appUpdateViewModel.showUpdateDialog()

                    Log.d("UserScreen", "Manual update check initiated by user")
                }
            )

            HorizontalDivider()
        }
    }
}

/**
 * 用户信息卡片
 */
@Composable
fun UserInfoCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 用户头像
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 用户信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "未登录",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "点击登录/注册",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 箭头
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Login",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * 功能项
 */
@Composable
fun FunctionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Go",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}