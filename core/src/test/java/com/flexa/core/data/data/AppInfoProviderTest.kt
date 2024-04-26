package com.flexa.core.data.data

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(sdk = [28])
@RunWith(RobolectricTestRunner::class)
class AppInfoProviderTest {

    val context = RuntimeEnvironment.getApplication()

    @Test
    fun testAppName() {
        val appName = AppInfoProvider.getAppName(application = context)
        println(appName)
    }
}