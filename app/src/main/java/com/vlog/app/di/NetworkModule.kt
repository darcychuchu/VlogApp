package com.vlog.app.di

import android.annotation.SuppressLint
import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.vlog.app.di.Constants.API_BASE_URL
import com.vlog.app.di.Constants.APP_VERSION
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.LoggingEventListener
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File

object NetworkModule {

    lateinit var okHttpClient: OkHttpClient
    lateinit var moshi: Moshi
    lateinit var retrofit: Retrofit

    val provideAuthInterceptor =  Interceptor { chain: Interceptor.Chain ->
        val initialRequest = chain.request()
        val newUrl = initialRequest.url.newBuilder()
            .addQueryParameter("app_version", APP_VERSION)
            .addQueryParameter("android_id", RoomModule.androidID)
            .build()
        val newRequest = initialRequest.newBuilder()
            .url(newUrl)
            .build()
        chain.proceed(newRequest)
    }

    @SuppressLint("HardwareIds")
    fun provide(context: Context) {
        okHttpClient = OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, "http_cache"), (20 * 1024 * 1024).toLong()))
            .apply {eventListenerFactory(LoggingEventListener.Factory())}
            .addInterceptor(provideAuthInterceptor)
            .build()

        moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    }

//    val VlogApi by lazy {
//        retrofit.create(KamfordServiceApi::class.java)
//    }
}