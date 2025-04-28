package com.vlog.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vlog.app.data.categories.CategoryDao
import com.vlog.app.data.categories.CategoryEntity
import com.vlog.app.data.histories.search.SearchHistoryDao
import com.vlog.app.data.histories.search.SearchHistoryEntity
import com.vlog.app.data.videos.VideoDao
import com.vlog.app.data.videos.VideoEntity
import com.vlog.app.data.histories.watch.WatchHistory
import com.vlog.app.data.histories.watch.WatchHistoryDao

/**
 * 应用数据库
 */
@Database(entities = [WatchHistory::class, CategoryEntity::class, VideoEntity::class, SearchHistoryEntity::class], version = 6, exportSchema = false)
@TypeConverters(Converters::class)
abstract class LocalDatabase : RoomDatabase() {

    /**
     * 获取观看历史 DAO
     */
    abstract fun watchHistoryDao(): WatchHistoryDao

    /**
     * 获取分类 DAO
     */
    abstract fun categoryDao(): CategoryDao

    /**
     * 获取视频 DAO
     */
    abstract fun videoDao(): VideoDao

    /**
     * 获取搜索历史 DAO
     */
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: LocalDatabase? = null

        /**
         * 从版本 1 到版本 2 的迁移
         * 添加 gatherId, gatherName 和 playerUrl 字段
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加新列
                database.execSQL("ALTER TABLE watch_history ADD COLUMN gatherId TEXT")
                database.execSQL("ALTER TABLE watch_history ADD COLUMN gatherName TEXT")
                database.execSQL("ALTER TABLE watch_history ADD COLUMN playerUrl TEXT")
            }
        }

        /**
         * 从版本 2 到版本 3 的迁移
         * 添加 categories 表
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建 categories 表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `parentId` TEXT,
                        `isTyped` TEXT,
                        `description` TEXT,
                        `path` TEXT,
                        `createdBy` TEXT,
                        `modelId` TEXT,
                        `childrenIds` TEXT NOT NULL,
                        `lastUpdated` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """)
            }
        }

        /**
         * 从版本 3 到版本 4 的迁移
         * 添加 videos 表
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建 videos 表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `videos` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `cover` TEXT NOT NULL,
                        `score` TEXT NOT NULL,
                        `description` TEXT,
                        `director` TEXT,
                        `actors` TEXT,
                        `region` TEXT,
                        `language` TEXT,
                        `alias` TEXT,
                        `categoryId` TEXT,
                        `tags` TEXT,
                        `author` TEXT,
                        `playerUrl` TEXT,
                        `typeName` TEXT,
                        `coverUrl` TEXT,
                        `videoType` INTEGER NOT NULL,
                        `lastUpdated` INTEGER NOT NULL,
                        `pageSource` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """)
            }
        }

        /**
         * 从版本 4 到版本 5 的迁移
         * 添加 search_history 表
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建 search_history 表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `search_history` (
                        `query` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`query`)
                    )
                """)
            }
        }

        /**
         * 从版本 5 到版本 6 的迁移
         * 添加 attachmentId 字段到 videos 表
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加 attachmentId 字段到 videos 表
                database.execSQL("ALTER TABLE videos ADD COLUMN attachmentId TEXT")
            }
        }

        /**
         * 获取数据库实例
         */
        fun getInstance(context: Context): LocalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocalDatabase::class.java,
                    "wildlog_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6) // 添加迁移策略
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}