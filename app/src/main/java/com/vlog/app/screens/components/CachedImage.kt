package com.vlog.app.screens.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.vlog.app.data.images.ImageType

/**
 * 带缓存的图片组件
 *
 * @param url 图片URL
 * @param contentDescription 图片描述
 * @param videoId 关联的视频ID（可选）
 * @param imageType 图片类型（可选）
 * @param description 图片描述（可选）
 * @param modifier Modifier
 * @param contentScale 内容缩放方式
 */
@Composable
fun CachedImage(
    url: String,
    contentDescription: String? = null,
    videoId: String? = null,
    imageType: ImageType = ImageType.OTHER,
    description: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current

    // 构建图片请求，包含视频ID和图片类型参数
    val request = ImageRequest.Builder(context)
        .data(url)
        .apply {
            videoId?.let { addHeader("X-VideoId", it) }
            addHeader("X-ImageType", imageType.name)
            description?.let { addHeader("X-Description", it) }
        }
        .build()

    // 使用SubcomposeAsyncImage以便在加载时显示进度指示器
    SubcomposeAsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    )
}

/**
 * 视频海报图片组件
 */
@Composable
fun VideoPoster(
    url: String,
    videoId: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    CachedImage(
        url = url,
        contentDescription = contentDescription,
        videoId = videoId,
        imageType = ImageType.POSTER,
        description = "Poster for video $videoId",
        modifier = modifier
    )
}

/**
 * 视频缩略图组件
 */
@Composable
fun VideoThumbnail(
    url: String,
    videoId: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    CachedImage(
        url = url,
        contentDescription = contentDescription,
        videoId = videoId,
        imageType = ImageType.THUMBNAIL,
        description = "Thumbnail for video $videoId",
        modifier = modifier
    )
}

/**
 * 视频截图组件
 */
@Composable
fun VideoScreenshot(
    url: String,
    videoId: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    CachedImage(
        url = url,
        contentDescription = contentDescription,
        videoId = videoId,
        imageType = ImageType.SCREENSHOT,
        description = "Screenshot for video $videoId",
        modifier = modifier
    )
}

/**
 * 横幅广告组件
 */
@Composable
fun BannerImage(
    url: String,
    contentDescription: String? = null,
    videoId: String? = null,
    modifier: Modifier = Modifier
) {
    CachedImage(
        url = url,
        contentDescription = contentDescription,
        videoId = videoId,
        imageType = ImageType.BANNER,
        description = "Banner image",
        modifier = modifier
    )
}

/**
 * 活动图片组件
 */
@Composable
fun ActivityImage(
    url: String,
    contentDescription: String? = null,
    videoId: String? = null,
    modifier: Modifier = Modifier
) {
    CachedImage(
        url = url,
        contentDescription = contentDescription,
        videoId = videoId,
        imageType = ImageType.ACTIVITY,
        description = "Activity image",
        modifier = modifier
    )
}
