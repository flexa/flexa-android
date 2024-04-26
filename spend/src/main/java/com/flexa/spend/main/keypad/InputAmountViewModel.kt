package com.flexa.spend.main.keypad

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flexa.core.shared.Brand
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val INPUT_DELAY = 1_000L

class InputAmountViewModel : ViewModel() {

    val formatter = Formatter()
    private val _inputState = MutableSharedFlow<InputState>()
    val amountBoundaries = _inputState.asSharedFlow()
    private var maxAmount = 0.0
    private var minAmount = 0.0
    private var amountVerifyJob: Job? = null
    private var stateJob: Job? = null

    init {
        listenInput()
    }

    fun clean() {
        viewModelScope.launch {
            amountVerifyJob?.cancel()
            stateJob?.cancel()
            _inputState.emit(InputState.Unspecified)
        }
    }

    override fun onCleared() {
        amountVerifyJob?.cancel()
        super.onCleared()
    }

    fun setMinMaxValue(brand: Brand) {
        maxAmount = brand.legacyFlexcodes?.firstOrNull()?.amount?.maximum?.toDoubleOrNull() ?: 0.0
        minAmount = brand.legacyFlexcodes?.firstOrNull()?.amount?.minimum?.toDoubleOrNull() ?: 0.0
    }

    fun getInputState(data: String): InputState {
        val amount = data.toDoubleOrNull() ?: 0.0
        return formatter.getInputState(
            minAmount = minAmount, maxAmount = maxAmount,
            amount = amount
        )
    }

    fun setInputState(inputState: InputState) {
        stateJob?.cancel()
        stateJob = viewModelScope.launch {
            if (isActive) _inputState.emit(inputState)
            if (inputState is InputState.Max) {
                delay(1000)
                if (isActive) _inputState.emit(InputState.Fine)
            }
        }
    }

    internal fun checkMinMaxValue(amount: Double) {
        Log.d(null, "checkMinMaxValue: >>> $amount")
        amountVerifyJob = viewModelScope.launch {
            when (val boundaries = formatter.getInputState(
                minAmount = minAmount, maxAmount = maxAmount,
                amount = amount
            )) {
                is InputState.Min -> {
                    delay(INPUT_DELAY)
                    if (isActive) {
                        Log.d(null, "checkMinMaxValue: >>> InputState.Min")
                        _inputState.emit(InputState.Min(System.currentTimeMillis()))
                    }
                }

                else -> _inputState.emit(boundaries)
            }
        }
    }

    private fun listenInput() {
        viewModelScope.launch {
            formatter.dataAsFlow
                .drop(1)
                .map { it?.toDoubleOrNull() ?: 0.0 }
                .onEach { amountVerifyJob?.cancel() }
                .collect { a -> checkMinMaxValue(a) }
        }
    }
}

sealed class InputState {
    class Min(timestamp: Long) : InputState()

    class Max(timestamp: Long) : InputState()

    data object Fine : InputState()
    data object Unspecified : InputState()
}

