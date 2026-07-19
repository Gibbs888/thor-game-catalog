package com.gibbstech.thorgamecatalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenScraperResponseTest {
    @Test
    fun emptyErrorFieldIsSuccessful() {
        val body = """{"header":{"success":"true","error":""}}"""

        assertNull(screenScraperApiError(body))
    }

    @Test
    fun actualApiErrorIsReturned() {
        val body = """{"header":{"success":"false","error":"Invalid user"}}"""

        assertEquals("Invalid user", screenScraperApiError(body))
    }

    @Test
    fun responseWithoutJsonErrorIsNotRejected() {
        assertNull(screenScraperApiError("service available"))
    }
}
