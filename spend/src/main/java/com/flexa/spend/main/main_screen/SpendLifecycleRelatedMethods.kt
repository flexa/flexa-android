package com.flexa.spend.main.main_screen

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@Composable
fun SpendLifecycleRelatedMethods(
    viewModel: SpendViewModel
) {

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.subscribeExchangeRates()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        viewModel.unsubscribeExchangeRates()
    }
}
