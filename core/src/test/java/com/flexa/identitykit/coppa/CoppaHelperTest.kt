package com.flexa.identity.coppa

import junit.framework.TestCase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@Config(sdk = [28])
@RunWith(RobolectricTestRunner::class)
class CoppaHelperTest {


    private val minimumAllowedAge = CoppaHelper.MINIMUM_ALLOWED_AGE
    private val utc = TimeZone.getTimeZone("UTC")
    private val currentYear = 2021
    private val currentDate = Calendar.getInstance().apply {
        set(Calendar.YEAR, currentYear)
        set(Calendar.MONTH, Calendar.FEBRUARY)
        set(Calendar.DAY_OF_MONTH, 17)
        set(Calendar.HOUR, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeZone = utc
    }.time

    private val olderOrEqual = Calendar.getInstance().apply {
        set(Calendar.YEAR, currentYear - minimumAllowedAge)
        set(Calendar.MONTH, Calendar.FEBRUARY)
        set(Calendar.DAY_OF_MONTH, 17)
        set(Calendar.HOUR, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeZone = utc
    }.time

    private val younger = Calendar.getInstance().apply {
        set(Calendar.YEAR, currentYear - minimumAllowedAge + 1)
        set(Calendar.MONTH, Calendar.FEBRUARY)
        set(Calendar.DAY_OF_MONTH, 17)
        set(Calendar.HOUR, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeZone = utc
    }.time

    @Test
    fun shouldCalculateDateDifference() {
        val start = Calendar.getInstance().apply {
            set(Calendar.YEAR, 1900)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            timeZone = utc
        }
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            timeZone = utc
        }

        while (cal != start) {
            cal.add(Calendar.DAY_OF_MONTH, -1)
            cal.set(Calendar.HOUR, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeZone = utc
            val date = cal.time
            val diff1 = CoppaHelper.getYearDiffJoda(date, currentDate)
            val diff2 = CoppaHelper.getYearDiff(date, currentDate)

            assertEquals(diff1, diff2)
        }
    }

    @Test
    fun shouldPassWithOldAndGreater() {
        val age = CoppaHelper.getYearDiff(olderOrEqual, currentDate)
        TestCase.assertTrue(age >= minimumAllowedAge)
    }

    @Test
    fun shouldFailsWithLessThenRequired() {
        val age = CoppaHelper.getYearDiff(younger, currentDate)
        TestCase.assertTrue(age < minimumAllowedAge)
    }

    @Test
    fun shouldReturnZeroOnNullDate() {
        TestCase.assertEquals(CoppaHelper.getUserAge(null), 0)
    }
}
