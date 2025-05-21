package com.vlog.app

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import android.util.Log
import coil.Coil
import coil.ImageLoader
import com.vlog.app.data.LocalDatabase
import com.vlog.app.data.api.NetworkModule
import com.vlog.app.data.categories.CategoryRepository
import com.vlog.app.data.comments.CommentRepository
import com.vlog.app.data.histories.search.SearchRepository
import com.vlog.app.data.histories.watch.WatchHistoryRepository
import com.vlog.app.data.images.ImageCacheDatabase
import com.vlog.app.data.images.ImageCacheRepository
import com.vlog.app.data.videos.VideoLocalRepository
import com.vlog.app.data.videos.VideoRepository
import com.vlog.app.utils.CachedImageInterceptor
import com.vlog.app.utils.ImageFileManager
import java.util.Locale

class VlogApp : Application() {
    // Repositories
    lateinit var videoRepository: VideoRepository
    lateinit var videoLocalRepository: VideoLocalRepository
    lateinit var commentRepository: CommentRepository
    lateinit var watchHistoryRepository: WatchHistoryRepository
    lateinit var imageCacheRepository: ImageCacheRepository
    lateinit var categoryRepository: CategoryRepository
    lateinit var searchRepository: SearchRepository

    // Database
    lateinit var database: LocalDatabase
    lateinit var imageCacheDatabase: ImageCacheDatabase

    // Image cache components
    lateinit var imageFileManager: ImageFileManager
    lateinit var imageLoader: ImageLoader

    @SuppressLint("HardwareIds")
    override fun onCreate() {
        super.onCreate()

        // Set default locale to Chinese
        setLocale(this)

        // 获取设备ID
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

        // 初始化网络模块
        NetworkModule.initialize(this, androidId)
        val apiService = NetworkModule.apiService
        val okHttpClient = NetworkModule.provideOkHttpClient(this) // 传入上下文以启用缓存

        // Initialize database
        database = LocalDatabase.getInstance(this)
        imageCacheDatabase = ImageCacheDatabase.getInstance(this)

        // Initialize image cache components
        imageFileManager = ImageFileManager(this)
        imageCacheRepository = ImageCacheRepository(
            context = this,
            imageCacheDao = imageCacheDatabase.imageCacheDao(),
            imageFileManager = imageFileManager,
            okHttpClient = okHttpClient
        )

        // Configure Coil with our custom interceptor
        val cachedImageInterceptor = CachedImageInterceptor(imageCacheRepository)
        imageLoader = ImageLoader.Builder(this)
            .components {
                add(cachedImageInterceptor)
            }
            .build()

        // Set as default ImageLoader
        Coil.setImageLoader(imageLoader)

        ////Log.d("VlogApp", "Image cache system initialized")

        // Initialize repositories
        videoRepository = VideoRepository(apiService)
        videoLocalRepository = VideoLocalRepository(applicationContext, apiService, database.videoDao())
        commentRepository = CommentRepository(apiService)
        watchHistoryRepository = WatchHistoryRepository(database.watchHistoryDao())
        categoryRepository = CategoryRepository(apiService, database.categoryDao())
        searchRepository = SearchRepository(apiService, database.searchHistoryDao())
    }

    private fun setLocale(context: Context) {
        val locale = Locale.CHINESE
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        // 使用 createConfigurationContext 替代已弃用的 updateConfiguration
        context.createConfigurationContext(config)

        // 为了向后兼容，仍然调用已弃用的方法，但添加抑制警告注解
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    override fun attachBaseContext(base: Context) {
        // Set locale before attachBaseContext
        val locale = Locale.CHINESE
        Locale.setDefault(locale)

        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)

        val context = base.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    companion object {
        private var instance: VlogApp? = null

        fun getInstance(): VlogApp {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }

    init {
        instance = this
    }
}
