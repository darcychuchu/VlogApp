package com.vlog.app.data.versions

data class AppVersion(
    val versionCode: Int,          // 版本号
    val versionName: String,       // 版本名称
    val forceUpdate: Boolean,      // 是否强制更新
    val downloadUrl: String,       // APK下载地址
    val description: String,       // 更新说明
    val fileSize: Long,            // 文件大小（字节）
    val md5: String                // APK文件MD5值，用于校验
)