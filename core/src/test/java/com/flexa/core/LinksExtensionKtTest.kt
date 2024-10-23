package com.flexa.core

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [28])
@RunWith(RobolectricTestRunner::class)
class LinksExtensionKtTest {

    @Test
    fun testUriSegments() {
        val data = "https://com.example.link/verify/auth/hello"
        val res = data.getPathSegments()
        var index = 0
        assertEquals("verify", res[index++])
        assertEquals("auth", res[index++])
        assertEquals("hello", res[index])
    }
}