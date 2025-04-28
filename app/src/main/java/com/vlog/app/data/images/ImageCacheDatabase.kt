package com.vlog.app.data.images

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 图片缓存数据库
 */
@Database(entities = [ImageCache::class], version = 1, exportSchema = false)
abstract class ImageCacheDatabase : RoomDatabase() {

    abstract fun imageCacheDao(): ImageCacheDao

    companion object {
        @Volatile
        private var INSTANCE: ImageCacheDatabase? = null

        fun getInstance(context: Context): ImageCacheDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ImageCacheDatabase::class.java,
                    "image_cache_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}