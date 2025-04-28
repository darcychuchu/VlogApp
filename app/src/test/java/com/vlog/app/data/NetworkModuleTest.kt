package com.vlog.app.data

import com.vlog.app.data.api.ApiConstants
import com.vlog.app.data.api.NetworkModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NetworkModuleTest {

    @Test
    fun testApiConstants() {
        // 验证API常量
        assertEquals("https://66log.com/api/json/v1/", ApiConstants.BASE_URL)
        assertEquals("https://66log.com", ApiConstants.VLOG_APP)
    }

    @Test
    fun testNetworkModuleInitialization() {
        // 验证NetworkModule的懒加载属性
        val apiService = NetworkModule.apiService
        assertNotNull(apiService)
    }
}
