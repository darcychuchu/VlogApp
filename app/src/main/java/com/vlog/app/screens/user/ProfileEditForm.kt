package com.vlog.app.screens.user

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.vlog.app.data.api.ApiConstants

/**
 * 用户资料编辑表单
 */
@Composable
fun ProfileEditForm(
    nickname: String,
    selectedAvatarUri: Uri?,
    currentAvatarUrl: String?,
    isLoading: Boolean,
    nicknameError: String?,
    error: String?,
    onNicknameChange: (String) -> Unit,
    onAvatarSelected: (Uri?) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    
    // 图片选择器
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onAvatarSelected(uri)
    }
    
    // 相机拍照
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        // 这里需要提前准备好一个Uri，传递给TakePicture
        // 由于实现复杂度，这里简化处理，只实现图库选择
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "编辑个人资料",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 头像选择
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedAvatarUri != null) {
                        // 显示选择的新头像
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(context)
                                    .data(selectedAvatarUri)
                                    .crossfade(true)
                                    .build()
                            ),
                            contentDescription = "选择的头像",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (!currentAvatarUrl.isNullOrEmpty()) {
                        // 显示当前头像
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(context)
                                    .data(ApiConstants.IMAGE_BASE_URL + currentAvatarUrl)
                                    .crossfade(true)
                                    .build()
                            ),
                            contentDescription = "当前头像",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // 显示默认头像
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "默认头像",
                                modifier = Modifier.size(50.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    // 相机图标提示
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "选择图片",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 昵称输入框
                OutlinedTextField(
                    value = nickname,
                    onValueChange = onNicknameChange,
                    label = { Text("昵称") },
                    isError = nicknameError != null,
                    supportingText = nicknameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { 
                            focusManager.clearFocus()
                        }
                    ),
                    singleLine = true,
                    enabled = !isLoading
                )
                
                // 错误信息
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("保存")
                        }
                    }
                }
            }
        }
    }
}
