package com.mishr.habittracker.domain.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResultTest {

    @Test
    fun `AppResult Success correctly triggers onSuccess and not onError`() {
        val result: AppResult<String> = AppResult.Success("Data")
        var successTriggered = false
        var errorTriggered = false

        result.onSuccess {
            successTriggered = true
            assertEquals("Data", it)
        }.onError {
            errorTriggered = true
        }

        assertTrue(result.isSuccess())
        assertFalse(result.isError())
        assertTrue(successTriggered)
        assertFalse(errorTriggered)
    }

    @Test
    fun `AppResult Error correctly triggers onError and not onSuccess`() {
        val error = AppError.NetworkError("Network failed")
        val result: AppResult<String> = AppResult.Error(error)
        var successTriggered = false
        var errorTriggered = false

        result.onSuccess {
            successTriggered = true
        }.onError {
            errorTriggered = true
            assertEquals(error, it)
        }

        assertTrue(result.isError())
        assertFalse(result.isSuccess())
        assertTrue(errorTriggered)
        assertFalse(successTriggered)
    }
}
