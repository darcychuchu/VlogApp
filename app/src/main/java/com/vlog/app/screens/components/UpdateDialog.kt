package com.vlog.app.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vlog.app.data.versions.AppVersion
import com.vlog.app.utils.AppUpdateManager
import java.text.DecimalFormat

/**
 * 更新对话框
 */
@Composable
fun UpdateDialog(
    appVersion: AppVersion,
    appUpdateManager: AppUpdateManager,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    // 错误状态
    var errorMessage by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = {
        // 如果是强制更新，不允许关闭对话框
        if (!appVersion.forceUpdate) {
            onDismiss()
        }
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 标题
                Text(
                    text = "发现新版本 ${appVersion.versionName}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 文件大小
                Text(
                    text = "文件大小: ${formatFileSize(appVersion.fileSize)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 更新内容
                Text(
                    text = "更新内容:",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 更新内容详情
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = appVersion.description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 下载进度
                val progress by appUpdateManager.getDownloadProgress().collectAsState(initial = 0)
                if (progress > 0 && progress < 100) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "下载进度: $progress%",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // 显示错误信息
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 如果不是强制更新，显示"稍后再说"按钮
                    if (!appVersion.forceUpdate) {
                        OutlinedButton(
                            onClick = {
                                // 调用postponeUpdate而不是hideUpdateDialog
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "24小时后提示")
                        }

                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    Button(
                        onClick = {
                            if (progress == 100) {
                                // 已下载完成，立即安装
                                onConfirm()
                            } else {
                                // 开始下载，关闭对话框
                                errorMessage = null // 清除错误信息
                                try {
                                    // 如果不是强制更新，关闭对话框
                                    if (!appVersion.forceUpdate) {
                                        onDismiss()
                                    }
                                    onConfirm()
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "下载时出错"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = progress == 0 || progress == 100 || progress == -1 // 在未开始下载、下载完成或下载失败时可点击
                    ) {
                        Text(
                            text = when {
                                progress == 100 -> "立即安装"
                                progress > 0 && progress < 100 -> "下载中..."
                                progress == -1 -> "重试"
                                else -> "立即更新"
                            }
                        )
                    }
                }

                // 如果是强制更新，显示提示
                if (appVersion.forceUpdate) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "当前版本过低，请立即更新",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * 格式化文件大小
 */
private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"

    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()

    return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}
