package com.flexa.spend

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@ExperimentalCoroutinesApi
class CoroutinesTestRule : TestWatcher() {

    @OptIn(DelicateCoroutinesApi::class)
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(mainThreadSurrogate)
    }

    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
        mainThreadSurrogate.close()
    }

}
