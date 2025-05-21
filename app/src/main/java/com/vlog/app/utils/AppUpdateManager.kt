package com.vlog.app.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileInputStream
import java.math.BigInteger
import java.security.MessageDigest
import androidx.core.net.toUri
import com.vlog.app.data.versions.AppVersion

/**
 * 应用更新管理器
 * 负责检查更新、下载APK和安装APK
 */
class AppUpdateManager(private val context: Context) {

    private var downloadId: Long = -1
    private var downloadReceiver: BroadcastReceiver? = null
    private var downloadCallback: ((Boolean, String?) -> Unit)? = null
    private var currentVersion: AppVersion? = null

    /**
     * 检查当前应用版本是否需要更新
     * @param appVersion 服务器返回的版本信息
     * @return 是否需要更新
     */
    fun needUpdate(appVersion: AppVersion): Boolean {
        // 当前版本号，实际应用中应使用BuildConfig.VERSION_CODE
        val currentVersionCode = 2 // 模拟当前版本号
        return appVersion.versionName != "1.0.5" && appVersion.versionCode != 2
    }

    /**
     * 检查下载URL是否有效
     * @param url 下载URL
     * @return 是否有效
     */
    private fun isValidUrl(url: String): Boolean {
        return try {
            // 检查URL格式是否正确
            val uri = url.toUri()
            if (uri.scheme.isNullOrEmpty() || uri.host.isNullOrEmpty()) {
                Log.e("AppUpdateManager", "Invalid URL format: $url")
                return false
            }

            // 检查URL是否以.apk结尾
            if (!url.lowercase().endsWith(".apk")) {
                Log.e("AppUpdateManager", "URL does not end with .apk: $url")
                return false
            }

            true
        } catch (e: Exception) {
            Log.e("AppUpdateManager", "Error validating URL: $url", e)
            false
        }
    }

    /**
     * 下载APK文件
     * @param appVersion 版本信息
     * @param callback 下载完成回调
     */
    fun downloadApk(appVersion: AppVersion, callback: (Boolean, String?) -> Unit) {
        currentVersion = appVersion
        downloadCallback = callback

        // 验证下载URL
        if (!isValidUrl(appVersion.downloadUrl)) {
            Log.e("AppUpdateManager", "Invalid download URL: ${appVersion.downloadUrl}")
            callback(false, "下载地址无效")
            return
        }

        // 检查是否已经下载过相同版本的APK
        val apkFile = getApkFile(appVersion.versionName)
        if (apkFile.exists()) {
            // 验证MD5
            val fileMd5 = calculateMD5(apkFile)
            if (fileMd5 == appVersion.md5) {
                // 文件已存在且MD5匹配，直接安装
                callback(true, apkFile.absolutePath)
                return
            } else {
                // MD5不匹配，删除旧文件
                apkFile.delete()
            }
        }

        try {
            // 创建下载请求
            val request = DownloadManager.Request(appVersion.downloadUrl.toUri())
                .setTitle("正在下载新版本 ${appVersion.versionName}")
                .setDescription("下载中...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(apkFile))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            // 获取下载管理器
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = downloadManager.enqueue(request)

            // 注册下载完成广播接收器
            registerDownloadReceiver()
        } catch (e: Exception) {
            Log.e("AppUpdateManager", "Error downloading APK", e)
            callback(false, e.message ?: "下载失败")
        }
    }

    /**
     * 注册下载完成广播接收器
     */
    private fun registerDownloadReceiver() {
        // 如果已经有接收器，先注销
        unregisterDownloadReceiver()

        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                try {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        val query = DownloadManager.Query().setFilterById(downloadId)
                        val cursor = downloadManager.query(query)

                        try {
                            if (cursor.moveToFirst()) {
                                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                                if (statusIndex != -1) {
                                    val status = cursor.getInt(statusIndex)

                                    when (status) {
                                        DownloadManager.STATUS_SUCCESSFUL -> {
                                            // 下载成功，验证MD5
                                            val apkFile = currentVersion?.let { getApkFile(it.versionName) }
                                            if (apkFile != null && apkFile.exists()) {
                                                val fileMd5 = calculateMD5(apkFile)
                                                if (fileMd5 == currentVersion?.md5) {
                                                    // MD5匹配，安装APK
                                                    downloadCallback?.invoke(true, apkFile.absolutePath)
                                                    // 自动安装APK
                                                    installApk(apkFile.absolutePath)
                                                } else {
                                                    // MD5不匹配，下载失败
                                                    apkFile.delete()
                                                    downloadCallback?.invoke(false, "文件校验失败，请重试")
                                                }
                                            } else {
                                                downloadCallback?.invoke(false, "下载文件不存在")
                                            }
                                        }
                                        DownloadManager.STATUS_FAILED -> {
                                            val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                                            val reason = if (reasonIndex != -1) cursor.getInt(reasonIndex) else -1
                                            downloadCallback?.invoke(false, "下载失败，错误码: $reason")
                                        }
                                        else -> {
                                            downloadCallback?.invoke(false, "下载状态未知: $status")
                                        }
                                    }
                                } else {
                                    downloadCallback?.invoke(false, "无法获取下载状态")
                                }
                            } else {
                                downloadCallback?.invoke(false, "无法获取下载信息")
                            }
                        } finally {
                            cursor.close()
                        }

                        // 注销广播接收器
                        unregisterDownloadReceiver()
                    }
                } catch (e: Exception) {
                    Log.e("AppUpdateManager", "Error in download receiver", e)
                    downloadCallback?.invoke(false, e.message ?: "下载过程中出错")
                    unregisterDownloadReceiver()
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (API 33) 及以上版本需要指定 RECEIVER_NOT_EXPORTED 标志
            context.registerReceiver(
                downloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            // 旧版本 Android
            ContextCompat.registerReceiver(
                context,
                downloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    /**
     * 注销下载完成广播接收器
     */
    private fun unregisterDownloadReceiver() {
        downloadReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                // 如果接收器没有注册，忽略异常
                Log.e("AppUpdateManager", "Error unregistering receiver", e)
            } finally {
                downloadReceiver = null
            }
        }
    }

    /**
     * 获取下载进度
     * @return 下载进度Flow
     */
    fun getDownloadProgress(): Flow<Int> = flow {
        if (downloadId == -1L) {
            emit(0)
            return@flow
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        var isDownloading = true

        while (isDownloading) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor: Cursor = downloadManager.query(query)

            if (cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                if (statusIndex != -1 && bytesDownloadedIndex != -1 && bytesTotalIndex != -1) {
                    val status = cursor.getInt(statusIndex)
                    val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
                    val bytesTotal = cursor.getLong(bytesTotalIndex)

                    when (status) {
                        DownloadManager.STATUS_RUNNING -> {
                            if (bytesTotal > 0) {
                                val progress = (bytesDownloaded * 100 / bytesTotal).toInt()
                                emit(progress)
                            }
                        }
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            emit(100)
                            isDownloading = false
                        }
                        DownloadManager.STATUS_FAILED -> {
                            emit(-1)
                            isDownloading = false
                        }
                    }
                }
            }
            cursor.close()

            // 暂停一下，避免过于频繁的查询
            kotlinx.coroutines.delay(500)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 安装APK
     * @param apkFilePath APK文件路径
     */
    fun installApk(apkFilePath: String) {
        val file = File(apkFilePath)
        if (!file.exists()) {
            Log.e("AppUpdateManager", "APK file not found: $apkFilePath")
            return
        }

        val intent = Intent(Intent.ACTION_VIEW)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        // Android 7.0及以上需要使用FileProvider
        val uri = FileProvider.getUriForFile(
            context,
            "com.vlog.app.fileprovider", // 实际应用中应使用"${BuildConfig.APPLICATION_ID}.fileprovider"
            file
        )
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        context.startActivity(intent)
    }

    /**
     * 获取APK文件
     * @param versionName 版本名称
     * @return APK文件
     */
    private fun getApkFile(versionName: String): File {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadDir, "wildlog_${versionName}.apk")
    }

    /**
     * 计算文件的MD5值
     * @param file 文件
     * @return MD5值
     */
    private fun calculateMD5(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        val inputStream = FileInputStream(file)
        val buffer = ByteArray(8192)
        var read: Int

        try {
            while (inputStream.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
        } finally {
            inputStream.close()
        }

        val md5sum = digest.digest()
        val bigInt = BigInteger(1, md5sum)
        var output = bigInt.toString(16)

        // 填充前导零
        while (output.length < 32) {
            output = "0$output"
        }

        return output
    }

    /**
     * 取消下载
     */
    fun cancelDownload() {
        if (downloadId != -1L) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.remove(downloadId)
            downloadId = -1L
            unregisterDownloadReceiver()
        }
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        unregisterDownloadReceiver()
        downloadCallback = null
        currentVersion = null
    }
}
