package com.vlog.app.screens.user

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.data.users.UserPreferences
import com.vlog.app.data.users.User
import com.vlog.app.data.users.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 用户ViewModel
 * 负责处理用户相关的业务逻辑
 */
class UserViewModel(
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    // UI状态
    data class UiState(
        val isLoading: Boolean = false,
        val user: User? = null,
        val isLoggedIn: Boolean = false,
        val error: String? = null,
        val isLoginFormVisible: Boolean = false,
        val isRegisterFormVisible: Boolean = false,
        val isProfileEditVisible: Boolean = false,
        val loginUsername: String = "",
        val loginPassword: String = "",
        val registerUsername: String = "",
        val registerPassword: String = "",
        val registerNickname: String = "",
        val editNickname: String = "",
        val selectedAvatarUri: Uri? = null,
        val usernameError: String? = null,
        val passwordError: String? = null,
        val nicknameError: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // 检查用户是否已登录
        checkLoginStatus()
    }

    /**
     * 检查用户登录状态
     */
    private fun checkLoginStatus() {
        val user = userPreferences.getUser()
        val isLoggedIn = userPreferences.isLoggedIn()

        _uiState.update {
            it.copy(
                user = user,
                isLoggedIn = isLoggedIn
            )
        }
    }

    /**
     * 显示登录表单
     */
    fun showLoginForm() {
        _uiState.update {
            it.copy(
                isLoginFormVisible = true,
                isRegisterFormVisible = false,
                error = null
            )
        }
    }

    /**
     * 隐藏登录表单
     */
    fun hideLoginForm() {
        _uiState.update {
            it.copy(
                isLoginFormVisible = false,
                loginUsername = "",
                loginPassword = "",
                error = null
            )
        }
    }

    /**
     * 显示注册表单
     */
    fun showRegisterForm() {
        _uiState.update {
            it.copy(
                isRegisterFormVisible = true,
                isLoginFormVisible = false,
                error = null
            )
        }
    }

    /**
     * 隐藏注册表单
     */
    fun hideRegisterForm() {
        _uiState.update {
            it.copy(
                isRegisterFormVisible = false,
                registerUsername = "",
                registerPassword = "",
                registerNickname = "",
                error = null
            )
        }
    }

    /**
     * 更新登录用户名
     */
    fun updateLoginUsername(username: String) {
        _uiState.update { it.copy(loginUsername = username, usernameError = null) }
    }

    /**
     * 更新登录密码
     */
    fun updateLoginPassword(password: String) {
        _uiState.update { it.copy(loginPassword = password, passwordError = null) }
    }

    /**
     * 更新注册用户名
     */
    fun updateRegisterUsername(username: String) {
        _uiState.update { it.copy(registerUsername = username, usernameError = null) }
    }

    /**
     * 更新注册密码
     */
    fun updateRegisterPassword(password: String) {
        _uiState.update { it.copy(registerPassword = password, passwordError = null) }
    }

    /**
     * 更新注册昵称
     */
    fun updateRegisterNickname(nickname: String) {
        _uiState.update { it.copy(registerNickname = nickname, nicknameError = null) }
    }

    /**
     * 登录
     */
    fun login() {
        val username = uiState.value.loginUsername
        val password = uiState.value.loginPassword

        Log.d("UserViewModel", "Login attempt with username: $username")

        // 验证输入
        if (username.isBlank()) {
            _uiState.update { it.copy(usernameError = "用户名不能为空") }
            Log.d("UserViewModel", "Login validation failed: username is blank")
            return
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(passwordError = "密码不能为空") }
            Log.d("UserViewModel", "Login validation failed: password is blank")
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        Log.d("UserViewModel", "Login validation passed, attempting login...")

        viewModelScope.launch {
            try {
                userRepository.login(username, password).fold(
                    onSuccess = { user ->
                        // 保存用户信息
                        Log.d("UserViewModel", "Login successful for user: ${user.name}")
                        userPreferences.saveUser(user)

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                user = user,
                                isLoggedIn = true,
                                isLoginFormVisible = false,
                                loginUsername = "",
                                loginPassword = ""
                            )
                        }
                    },
                    onFailure = { e ->
                        Log.e("UserViewModel", "Login error", e)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "登录失败"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("UserViewModel", "Login exception", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "登录失败"
                    )
                }
            }
        }
    }

    /**
     * 注册
     */
    fun register() {
        val username = uiState.value.registerUsername
        val password = uiState.value.registerPassword
        val nickname = uiState.value.registerNickname

        Log.d("UserViewModel", "Register attempt with username: $username, nickname: $nickname")

        // 验证输入
        if (username.isBlank()) {
            _uiState.update { it.copy(usernameError = "用户名不能为空") }
            Log.d("UserViewModel", "Register validation failed: username is blank")
            return
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(passwordError = "密码不能为空") }
            Log.d("UserViewModel", "Register validation failed: password is blank")
            return
        }
        if (nickname.isBlank()) {
            _uiState.update { it.copy(nicknameError = "昵称不能为空") }
            Log.d("UserViewModel", "Register validation failed: nickname is blank")
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        Log.d("UserViewModel", "Register validation passed, checking if username exists...")

        viewModelScope.launch {
            try {
                // 先检查用户名是否存在
                userRepository.checkUserExists(username).fold(
                    onSuccess = { exists ->
                        if (exists) {
                            Log.d("UserViewModel", "Username already exists: $username")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    usernameError = "用户名已存在"
                                )
                            }
                        } else {
                            // 用户名不存在，可以注册
                            Log.d("UserViewModel", "Username available, proceeding with registration")
                            registerUser(username, password, nickname)
                        }
                    },
                    onFailure = { e ->
                        Log.e("UserViewModel", "Check user exists error", e)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "检查用户名失败"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("UserViewModel", "Register exception", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "注册失败"
                    )
                }
            }
        }
    }

    /**
     * 注册用户
     */
    private suspend fun registerUser(username: String, password: String, nickname: String) {
        Log.d("UserViewModel", "Registering user: $username, nickname: $nickname")
        userRepository.register(username, password, nickname).fold(
            onSuccess = { user ->
                // 保存用户信息
                Log.d("UserViewModel", "Registration successful for user: ${user.name}")
                userPreferences.saveUser(user)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = user,
                        isLoggedIn = true,
                        isRegisterFormVisible = false,
                        registerUsername = "",
                        registerPassword = "",
                        registerNickname = ""
                    )
                }
            },
            onFailure = { e ->
                Log.e("UserViewModel", "Register error", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "注册失败"
                    )
                }
            }
        )
    }

    /**
     * 登出
     */
    fun logout() {
        userPreferences.clearUser()
        _uiState.update {
            it.copy(
                user = null,
                isLoggedIn = false
            )
        }
    }

    /**
     * 显示用户资料编辑对话框
     */
    fun showProfileEdit() {
        val currentUser = uiState.value.user
        _uiState.update {
            it.copy(
                isProfileEditVisible = true,
                editNickname = currentUser?.nickName ?: "",
                error = null
            )
        }
    }

    /**
     * 隐藏用户资料编辑对话框
     */
    fun hideProfileEdit() {
        _uiState.update {
            it.copy(
                isProfileEditVisible = false,
                editNickname = "",
                selectedAvatarUri = null,
                error = null,
                nicknameError = null
            )
        }
    }

    /**
     * 更新编辑昵称
     */
    fun updateEditNickname(nickname: String) {
        _uiState.update { it.copy(editNickname = nickname, nicknameError = null) }
    }

    /**
     * 更新选择的头像
     */
    fun updateSelectedAvatar(uri: Uri?) {
        _uiState.update { it.copy(selectedAvatarUri = uri) }
    }

    /**
     * 检查昵称是否存在
     */
    fun checkNicknameExists(nickname: String, onResult: (Boolean) -> Unit) {
        if (nickname.isBlank()) {
            _uiState.update { it.copy(nicknameError = "昵称不能为空") }
            return
        }

        // 如果昵称没有变化，不需要检查
        if (nickname == uiState.value.user?.nickName) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                userRepository.checkNicknameExists(nickname).fold(
                    onSuccess = { exists ->
                        _uiState.update { it.copy(isLoading = false) }
                        if (exists) {
                            _uiState.update { it.copy(nicknameError = "昵称已存在") }
                        }
                        onResult(exists)
                    },
                    onFailure = { e ->
                        Log.e("UserViewModel", "Check nickname exists error", e)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "检查昵称失败"
                            )
                        }
                        onResult(false)
                    }
                )
            } catch (e: Exception) {
                Log.e("UserViewModel", "Check nickname exists exception", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "检查昵称失败"
                    )
                }
                onResult(false)
            }
        }
    }

    /**
     * 更新用户信息
     */
    fun updateUserProfile() {
        val currentUser = uiState.value.user ?: return
        val nickname = uiState.value.editNickname
        val avatarUri = uiState.value.selectedAvatarUri

        // 验证昵称
        if (nickname.isBlank()) {
            _uiState.update { it.copy(nicknameError = "昵称不能为空") }
            return
        }

        // 如果昵称没有变化且没有选择新头像，不需要更新
        if (nickname == currentUser.nickName && avatarUri == null) {
            hideProfileEdit()
            return
        }

        // 先检查昵称是否存在
        checkNicknameExists(nickname) { exists ->
            if (!exists) {
                // 昵称不存在，可以更新
                performUpdateUserProfile(currentUser.name!!, currentUser.accessToken!!, nickname, avatarUri)
            }
        }
    }

    /**
     * 执行更新用户信息
     */
    private fun performUpdateUserProfile(username: String, token: String, nickname: String, avatarUri: Uri?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                userRepository.updateUserInfo(username, token, nickname, avatarUri).fold(
                    onSuccess = { updatedUser ->
                        // 保存更新后的用户信息
                        userPreferences.saveUser(updatedUser)

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                user = updatedUser,
                                isProfileEditVisible = false,
                                editNickname = "",
                                selectedAvatarUri = null
                            )
                        }
                    },
                    onFailure = { e ->
                        Log.e("UserViewModel", "Update user profile error", e)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "更新用户信息失败"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("UserViewModel", "Update user profile exception", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "更新用户信息失败"
                    )
                }
            }
        }
    }

    /**
     * 刷新用户信息
     */
    fun refreshUserInfo() {
        val currentUser = uiState.value.user ?: return
        val username = currentUser.name ?: return
        val token = currentUser.accessToken ?: return

        viewModelScope.launch {
            try {
                userRepository.getUserInfo(username, token).fold(
                    onSuccess = { user ->
                        // 保存更新后的用户信息
                        userPreferences.saveUser(user)

                        _uiState.update {
                            it.copy(user = user)
                        }
                    },
                    onFailure = { e ->
                        Log.e("UserViewModel", "Refresh user info error", e)
                    }
                )
            } catch (e: Exception) {
                Log.e("UserViewModel", "Refresh user info exception", e)
            }
        }
    }
}
