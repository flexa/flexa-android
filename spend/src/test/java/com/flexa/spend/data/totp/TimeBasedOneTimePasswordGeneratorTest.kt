package com.flexa.spend.data.totp

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.apache.commons.codec.binary.Base32
import org.junit.Test
import java.time.Instant
import java.util.concurrent.TimeUnit

class TimeBasedOneTimePasswordGeneratorTest {

    @Test
    fun testByRFC_sha1() {
        val config = TimeBasedOneTimePasswordConfig(
            30, TimeUnit.SECONDS, 8, HmacAlgorithm.SHA1
        )
        val secret = "12345678901234567890".toByteArray()
        val generator = TimeBasedOneTimePasswordGenerator(secret, config)
        var code = generator.generate(59 * 1000L)
        assertEquals("94287082", code)
        code = generator.generate(1111111111 * 1000L)
        assertEquals("14050471", code)
    }

    @Test
    fun testByRFC_sha256() {
        val config = TimeBasedOneTimePasswordConfig(
            30, TimeUnit.SECONDS, 8, HmacAlgorithm.SHA256
        )
        val secret = "12345678901234567890123456789012".toByteArray()
        val generator = TimeBasedOneTimePasswordGenerator(secret, config)
        val code = generator.generate(1111111109 * 1000L)
        assertEquals("68084774", code)
    }

    @Test
    fun testByRFC_sha512() {
        val config = TimeBasedOneTimePasswordConfig(
            30, TimeUnit.SECONDS, 8, HmacAlgorithm.SHA512
        )
        val secret =
            "1234567890123456789012345678901234567890123456789012345678901234".toByteArray()
        val generator = TimeBasedOneTimePasswordGenerator(secret, config)
        val code = generator.generate(1234567890 * 1000L)
        assertEquals("93441116", code)
    }

    @Test
    fun testExampleAssetCode() {
        val config = TimeBasedOneTimePasswordConfig(
            timeStep = 30,
            timeStepUnit = TimeUnit.SECONDS,
            codeDigits = 8,
            hmacAlgorithm = HmacAlgorithm.SHA512
        )
        val secret = "43C52N2YRHDNZFBGIWYO46YQN43QMVNE".toByteArray()
        val generator = TimeBasedOneTimePasswordGenerator(secret, config)
        val instant = Instant.ofEpochMilli(1234567890 * 1000L)
        println(instant)
        val code = generator.generate(timestamp = instant.toEpochMilli())
        assertEquals("51845424", code)
    }

    @Test
    fun testExampleAssetSecret() {
        val config = TimeBasedOneTimePasswordConfig(
            timeStep = 30,
            timeStepUnit = TimeUnit.SECONDS,
            codeDigits = 8,
            hmacAlgorithm = HmacAlgorithm.SHA512
        )
        val secret = "43C52N2YRHDNZFBGIWYO46YQN43QMVNE".toByteArray()
        val generator = TimeBasedOneTimePasswordGenerator(secret, config)
        val instant = Instant.ofEpochMilli(1234567890 * 1000L)
        println(instant)
        val code = generator.generate(timestamp = instant.toEpochMilli())
        assertEquals("51845424", code)
        assertTrue(generator.isValid(code, instant.plusSeconds(29).toEpochMilli()))
        assertFalse(generator.isValid(code, instant.plusSeconds(30).toEpochMilli()))
    }

    @Test
    fun testServerTOTP() {
        val config = TimeBasedOneTimePasswordConfig(
            timeStep = 30,
            timeStepUnit = TimeUnit.SECONDS,
            codeDigits = 8,
            hmacAlgorithm = HmacAlgorithm.SHA512
        )
        val secret = "43C52N2YRHDNZFBGIWYO46YQN43QMVNE".toByteArray()
        val generator = TimeBasedOneTimePasswordGenerator(secret, config)
        val instant = Instant.ofEpochMilli(1234567890 * 1000L)
        println(instant)
        val code = generator.generate(timestamp = instant.toEpochMilli())
        assertEquals("51845424", code)
        assertTrue(generator.isValid(code, instant.plusSeconds(29).toEpochMilli()))
        assertFalse(generator.isValid(code, instant.plusSeconds(30).toEpochMilli()))
    }

    @Test
    fun testByServerResults() {
        val config = TimeBasedOneTimePasswordConfig(
            30, TimeUnit.SECONDS, 6, HmacAlgorithm.SHA1
        )
        val secret = Base32().decode("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")
        val generator = TimeBasedOneTimePasswordGenerator(secret, config)
        val instant = Instant.ofEpochMilli(59 * 1000L)
        println(instant)
        val code = generator.generate(instant)
        assertEquals("287082", code)
    }
}