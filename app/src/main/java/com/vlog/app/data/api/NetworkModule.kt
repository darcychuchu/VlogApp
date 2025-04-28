package com.vlog.app.data.api

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 网络模块
 * 负责提供网络请求相关的组件，包括OkHttpClient、Retrofit和API服务
 */
object NetworkModule {
    private const val TAG = "NetworkModule"
    private const val CACHE_SIZE = 20 * 1024 * 1024L // 20MB
    private const val TIMEOUT = 30L // 30秒

    // 应用版本，用于API请求
    private const val APP_VERSION = "1.0.4"

    // 设备ID，用于API请求
    private var deviceId: String = "unknown"

    /**
     * 初始化网络模块
     * @param context 应用上下文
     * @param androidId 设备ID
     */
    fun initialize(context: Context, androidId: String) {
        deviceId = androidId
        Log.d(TAG, "Network module initialized with device ID: $deviceId")
    }

    /**
     * 提供OkHttpClient实例
     * @param context 应用上下文，用于缓存目录
     * @return 配置好的OkHttpClient实例
     */
    fun provideOkHttpClient(context: Context? = null): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)

        // 添加日志拦截器
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        builder.addInterceptor(loggingInterceptor)

        // 添加认证拦截器
        builder.addInterceptor(provideAuthInterceptor())

        // 如果提供了上下文，添加缓存
        context?.let {
            val cacheDir = File(it.cacheDir, "http_cache")
            val cache = Cache(cacheDir, CACHE_SIZE)
            builder.cache(cache)
        }

        return builder.build()
    }

    /**
     * 提供认证拦截器
     * 为每个请求添加应用版本和设备ID
     */
    private fun provideAuthInterceptor(): Interceptor = Interceptor { chain ->
        val initialRequest = chain.request()
        val newUrl = initialRequest.url.newBuilder()
            .addQueryParameter("app_version", APP_VERSION)
            .addQueryParameter("android_id", deviceId)
            .build()
        val newRequest = initialRequest.newBuilder()
            .url(newUrl)
            .build()
        chain.proceed(newRequest)
    }

    /**
     * 提供Moshi实例，用于JSON解析
     */
    private fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    /**
     * 提供Retrofit实例
     * @param okHttpClient OkHttpClient实例
     * @param moshi Moshi实例
     * @return 配置好的Retrofit实例
     */
    private fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create()) // 支持字符串响应
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    /**
     * 提供API服务实例
     * 懒加载，确保只创建一次
     */
    val apiService: ApiService by lazy {
        val okHttpClient = provideOkHttpClient()
        val moshi = provideMoshi()
        val retrofit = provideRetrofit(okHttpClient, moshi)
        retrofit.create(ApiService::class.java)
    }
}
