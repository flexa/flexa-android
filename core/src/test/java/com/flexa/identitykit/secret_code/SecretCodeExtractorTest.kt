package com.flexa.identity.secret_code

import org.junit.Assert.assertEquals
import org.junit.Test

class SecretCodeExtractorTest {

    @Test
    fun shouldExtractCode() {
        val data = "fj b fkj12457 3234ffgdhnbghj"
        val result = SecretCodeExtractor(data, 6).code
        assertEquals("124573", result)
    }

    @Test
    fun shouldNotThrowException() {
        val data = "fj"
        val result = SecretCodeExtractor(data, 6).code
        assertEquals(null, result)
    }

    @Test
    fun shouldNotAllowShortCodes() {
        val data = "fj123"
        val result = SecretCodeExtractor(data, 6).code
        assertEquals(null, result)
    }
}
