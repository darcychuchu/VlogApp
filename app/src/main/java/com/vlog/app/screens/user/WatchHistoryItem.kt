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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.util.Log
import com.vlog.app.data.images.ImageType
import com.vlog.app.data.histories.watch.WatchHistory
import com.vlog.app.screens.components.CachedImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 观看历史列表项
 */
@Composable
fun WatchHistoryItem(
    watchHistory: WatchHistory,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面图
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                // 使用 CachedImage 组件加载海报图片，优先使用本地缓存
                if (!watchHistory.coverUrl.isNullOrEmpty()) {
                    // 确保 URL 是有效的
                    val validUrl = if (watchHistory.coverUrl.startsWith("http")) {
                        watchHistory.coverUrl
                    } else {
                        // 如果不是完整URL，添加基础URL
                        "http://192.168.43.175:8083/file/attachments/image/s/${watchHistory.coverUrl}"
                    }

                    Log.d("WatchHistoryItem", "Loading image from URL: $validUrl")

                    CachedImage(
                        url = validUrl,
                        contentDescription = watchHistory.title,
                        videoId = watchHistory.videoId,  // 关联视频ID
                        imageType = ImageType.POSTER,    // 指定图片类型为海报
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // 如果没有封面图，显示空白背景
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }

                // 半透明遮罩，增强播放图标的可见度
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )

                // 播放按钮
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,  // 使用白色图标增强对比度
                        modifier = Modifier.size(40.dp)
                    )
                }

                // 进度条
                if (watchHistory.duration > 0 && watchHistory.lastPlayedPosition > 0) {
                    LinearProgressIndicator(
                        progress = { (watchHistory.lastPlayedPosition.toFloat() / watchHistory.duration).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 视频信息
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
            ) {
                // 标题
                Text(
                    text = watchHistory.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 剧集信息
                if (!watchHistory.episodeTitle.isNullOrEmpty()) {
                    Text(
                        text = "正在观看: ${watchHistory.episodeTitle}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 服务商信息
                if (!watchHistory.gatherName.isNullOrEmpty()) {
                    Text(
                        text = "服务商: ${watchHistory.gatherName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 最后观看时间
                Text(
                    text = "上次观看: ${formatDate(watchHistory.lastWatchTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // 删除按钮
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * 格式化日期
 */
private fun formatDate(date: Date): String {
    val now = Date()
    val diff = now.time - date.time

    return when {
        diff < 60 * 60 * 1000 -> "刚刚"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}天前"
        else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
    }
}
