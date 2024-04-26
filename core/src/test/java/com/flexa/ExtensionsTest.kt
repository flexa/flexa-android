package com.flexa

import com.flexa.core.entity.AssetValue
import com.flexa.core.entity.AvailableAsset
import com.flexa.core.minutesBetween
import com.flexa.core.toDate
import com.flexa.core.zeroValue
import com.flexa.identity.toSha256
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Calendar.JUNE
import java.util.Calendar.MONDAY
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ExtensionsTest {


    @Test
    fun testSha256() {
        val data = "flexa"
        val res = data.toSha256()
        assertEquals("2ba9033a9b628b06c3e5ebbb82c9323d22ef9822e97163c51678b3a93329bd4c", res)
    }

    @Test
    fun `minutesBetween() returns correct value for future timestamp`() {
        val future = 1717075544L
        val past = Instant.ofEpochMilli(future * 1000L).minus(30, ChronoUnit.MINUTES)
        val minutesBetween = past.minutesBetween(future)
        assertEquals(30, minutesBetween)
    }

    @Test
    fun testDate() {
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
        val currentDate = Date(1717428271 * 1000L)
        val res = dateFormat.format(currentDate)
        assertEquals("Mon, 03 Jun 2024 18:24:31 EEST", res)
    }

    @Test
    fun stringToDate() {
        val dateString = "Mon, 03 Jun 2024 16:50:38 EEST"
        val date = dateString.toDate()
        val cal = Calendar.getInstance().apply { time = date }
        assertEquals(2024, cal.get(Calendar.YEAR))
        assertEquals(MONDAY, cal.get(Calendar.DAY_OF_WEEK))
        assertEquals(JUNE, cal.get(Calendar.MONTH))
        assertEquals(3, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(16, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(50, cal.get(Calendar.MINUTE))
        assertEquals(38, cal.get(Calendar.SECOND))
        assertEquals(TimeZone.getTimeZone("Europe/Uzhgorod"), cal.timeZone)
        println(date)
    }

    @Test
    fun `should return zero value`() {
        val asset = AvailableAsset(
            assetId = "",
            balance = "",
            value = AssetValue(asset = "", label = "$0.00 available", labelTitlecase = "")
        )
        assertTrue(asset.zeroValue())
    }

    @Test
    fun `should return non zero value`() {
        val asset = AvailableAsset(
            assetId = "",
            balance = "",
            value = AssetValue(asset = "", label = "$0.01 available", labelTitlecase = "")
        )
        assertFalse(asset.zeroValue())
    }

    @Test
    fun `should return zero value for null-value asset`() {
        val asset = AvailableAsset(
            assetId = "",
            balance = "",
        )
        assertTrue(asset.zeroValue())
    }

    @Test
    fun `should return zero value for any symbol`() {
        val asset = AvailableAsset(
            assetId = "",
            balance = "",
            value = AssetValue(asset = "", label = "&$%0.00 available", labelTitlecase = "")
        )
        assertTrue(asset.zeroValue())
    }

    @Test
    fun `should return non zero value for any symbol`() {
        val asset = AvailableAsset(
            assetId = "",
            balance = "",
            value = AssetValue(asset = "", label = "@#%0.01 available", labelTitlecase = "")
        )
        assertFalse(asset.zeroValue())
    }
}
