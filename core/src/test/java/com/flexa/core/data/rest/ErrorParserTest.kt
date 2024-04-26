package com.flexa.core.data.rest

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [28])
@RunWith(RobolectricTestRunner::class)
class ErrorParserTest {

    @Test
    fun testErrorParsing() {
        val raw = "{\n" +
                "                   \"code\": \"500\",\n" +
                "                   \"message\": \"Internal\",\n" +
                "                   \"status\": \"Internal\"\n" +
                "                 }"
        val exception = Throwable(message = raw)
        val res = ErrorParser.parseError(exception)

        assertEquals(500, res.code)
        assertEquals("Internal", res.message)
        assertEquals("Internal", res.status)
    }

    @Test
    fun testErrorParsingFail() {
        val raw = "{\n" +
                "                   \"code\": \"500\",\n" +
                "                   \"message\": \"Internal\",\n" +
                "                   \"status\": \"Internal\"\n" +
                "                 "
        val exception = Throwable(message = raw)
        val res = ErrorParser.parseError(exception)

        assertEquals(-1, res.code)
        assertEquals(null, res.message)
        assertEquals(null, res.status)
    }

}