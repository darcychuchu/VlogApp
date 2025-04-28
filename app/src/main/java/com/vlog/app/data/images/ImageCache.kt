package com.vlog.app.data.images

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vlog.app.data.images.ImageType

/**
 * 图片缓存实体类
 */
@Entity(tableName = "image_cache")
data class ImageCache(
    @PrimaryKey val url: String,
    val localPath: String,
    val videoId: String? = null,     // 关联的视频ID
    val imageType: String = ImageType.OTHER.name,  // 图片类型
    val downloadTime: Long = System.currentTimeMillis(),
    val lastModified: String? = null,
    val size: Long = 0,
    val lastAccessTime: Long = System.currentTimeMillis(),
    val description: String? = null  // 图片描述，可用于搜索或分类
)