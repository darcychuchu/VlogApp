package com.vlog.app.screens.shorts

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * ShortsViewModel 工厂类
 */
class ShortsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShortsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShortsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
