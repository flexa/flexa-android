package com.flexa.core.shared

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ApiErrorHandler {

    var error by mutableStateOf<ApiError?>(null)
    val hasError by derivedStateOf { error != null }

    fun setError(e:  Throwable?) {
        error = ApiErrorAdapter.parseError(e)
    }

    fun setApiError(e: ApiError) {
        error = e
    }

    fun clearError() {
        error = null
    }
}
