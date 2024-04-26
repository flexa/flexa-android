package com.flexa.core.data.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


@Config(sdk = [28])
@RunWith(RobolectricTestRunner::class)
class PikSeeProviderTest {

    @Test
    fun testCodeVerifier() {
        val codeVerifier = PikSeeProvider.getCodeVerifier()
        println("codeVerifier: $codeVerifier")
        assertNotNull(codeVerifier)
        assertTrue(codeVerifier.isNotBlank())
    }

    @Test
    fun testCodeChallenge() {
        val codeVerifier = PikSeeProvider.getCodeVerifier()
        val codeChallenge = PikSeeProvider.getCodeChallenge(codeVerifier)
        println("codeChallenge: $codeChallenge")
        assertNotNull(codeChallenge)
        assertTrue(codeChallenge.isNotBlank())
        assertEquals(43, codeChallenge.length)
    }
}