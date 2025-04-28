package com.vlog.app.data.histories

//import android.content.Context
//import androidx.room.Database
//import androidx.room.Room
//import androidx.room.RoomDatabase
//import androidx.room.TypeConverters
//import com.vlog.app.data.histories.search.SearchHistoryDao
//import com.vlog.app.data.histories.search.SearchHistoryEntity
//import com.vlog.app.data.DateConverter
//import com.vlog.app.data.histories.watch.WatchHistoryEntity
//import com.vlog.app.data.histories.watch.WatchHistoryEntityDao
//
//@Database(
//    entities = [WatchHistoryEntity::class, SearchHistoryEntity::class],
//    version = 1,
//    exportSchema = false
//)
//@TypeConverters(DateConverter::class)
//abstract class HistoriesDatabase : RoomDatabase() {
//
//    abstract fun watchHistoryDao(): WatchHistoryEntityDao
//    abstract fun searchHistoryDao(): SearchHistoryDao
//
//    companion object {
//        private const val DATABASE_NAME = "wildlog_video_db"
//
//        @Volatile
//        private var INSTANCE: HistoriesDatabase? = null
//
//        fun getInstance(context: Context): HistoriesDatabase {
//            return INSTANCE ?: synchronized(this) {
//                val instance = Room.databaseBuilder(
//                    context.applicationContext,
//                    HistoriesDatabase::class.java,
//                    DATABASE_NAME
//                )
//                    .fallbackToDestructiveMigration()
//                    .build()
//                INSTANCE = instance
//                instance
//            }
//        }
//    }
//}