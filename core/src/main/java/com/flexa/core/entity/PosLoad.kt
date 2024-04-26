package com.flexa.core.entity

sealed class PosLoad

data class LoadClaimed(val transactionToken: String) : PosLoad()

data class LoadCompleted(
    val transactionId: String,
    val amount: Int,
    val currency: String,
) : PosLoad()
