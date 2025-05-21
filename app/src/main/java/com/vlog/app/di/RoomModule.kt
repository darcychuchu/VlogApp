package com.vlog.app.di

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.room.Room
import com.vlog.app.data.LocalDatabase


object RoomModule {
    lateinit var localDatabase: LocalDatabase
        private set

    lateinit var androidID: String
        private set

//    val userStore by lazy {
//        UsersStore(
//            usersDao = localDatabase.usersDao()
//        )
//    }


    @SuppressLint("HardwareIds")
    fun provide(context: Context) {
        localDatabase = Room.databaseBuilder(context, LocalDatabase::class.java, "movie-room.db")
            .build()
        androidID = "${Build.BRAND}${Build.PRODUCT}-MovieApp-${Constants.APP_VERSION}"

    }
}