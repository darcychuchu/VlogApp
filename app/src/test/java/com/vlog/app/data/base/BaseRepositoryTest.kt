package com.vlog.app.data.base

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@ExperimentalCoroutinesApi
class BaseRepositoryTest {

    // 创建一个测试用的BaseRepository实现
    private val repository = object : BaseRepository {}

    @Test
    fun `safeApiCall should return success result when api call succeeds`() = runTest {
        // 准备
        val expectedValue = "Success"

        // 执行
        val result = repository.safeApiCall { expectedValue }

        // 验证
        assertTrue(result.isSuccess)
        assertEquals(expectedValue, result.getOrNull())
    }

    @Test
    fun `safeApiCall should return failure result when api call fails`() = runTest {
        // 准备
        val expectedException = IOException("Network error")

        // 执行
        val result = repository.safeApiCall<String> { throw expectedException }

        // 验证
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkError.NetworkError)
    }

    @Test
    fun `getDataWithCache should emit loading state first`() = runTest {
        // 执行
        val flow = repository.getDataWithCache(
            localDataSource = { "Local data" },
            remoteDataSource = { "Remote data" },
            saveRemoteData = {},
            shouldFetch = { false }
        )

        // 获取第一个值
        val firstEmission = flow.first()

        // 验证
        assertTrue(firstEmission is Resource.Loading)
    }

    @Test
    fun `getDataWithCache should emit success with local data when shouldFetch is false`() = runTest {
        // 准备
        val localData = "Local data"

        // 执行
        val flow = repository.getDataWithCache(
            localDataSource = { localData },
            remoteDataSource = { "Remote data" },
            saveRemoteData = {},
            shouldFetch = { false }
        )

        // 跳过Loading状态，获取第二个值
        val secondEmission = flow.first { it !is Resource.Loading }

        // 验证
        assertTrue(secondEmission is Resource.Success)
        assertEquals(localData, secondEmission.data)
    }
}
