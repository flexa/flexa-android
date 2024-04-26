package com.flexa.spend.pay

import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class CalculationsTest {

    @Test
    fun tmeCorrectionWhenLocalTimeFartherThanServer() {
        val serverTime = Instant.ofEpochMilli(1234567890 * 1000L) //2009-02-13 23:31:30
        val clientTime = Instant.from(serverTime).plusMillis(2500)
        val duration = Duration.between(serverTime, clientTime)
        val correctTime = clientTime.minusMillis(duration.toMillis())
        assertEquals(serverTime, correctTime)
    }

    @Test
    fun tmeCorrectionWhenServerTimeFartherThanLocal() {
        val clientTime = Instant.ofEpochMilli(1234567890 * 1000L) //2009-02-13 23:31:30
        val serverTime = Instant.from(clientTime).plusMillis(2500)
        val duration = Duration.between(serverTime, clientTime)
        val correctTime = clientTime.minusMillis(duration.toMillis())

        assertEquals(serverTime, correctTime)
    }
}
