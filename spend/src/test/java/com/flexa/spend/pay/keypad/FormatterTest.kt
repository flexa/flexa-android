package com.flexa.spend.pay.keypad

import com.flexa.spend.CoroutinesTestRule
import com.flexa.spend.main.keypad.Backspace
import com.flexa.spend.main.keypad.Formatter
import com.flexa.spend.main.keypad.InputState
import com.flexa.spend.main.keypad.Point
import com.flexa.spend.main.keypad.Symbol
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FormatterTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    var coroutineTestRule = CoroutinesTestRule()
    private val formatter = Formatter(2)

    @Test
    fun backspace() {
        formatter.append(Symbol("1"))
        formatter.append(Symbol("5"))
        formatter.append(Point())
        formatter.append(Point())
        formatter.append(Symbol("4"))
        formatter.append(Symbol("7"))
        formatter.append(Backspace())
        assertEquals("15.4", formatter.getText())
        formatter.append(Backspace())
        assertEquals("15.", formatter.getText())
    }

    @Test
    fun `should prevent two points input`() {
        formatter.append(Point())
        formatter.append(Point())
        assertEquals("0.", formatter.getText())
    }

    @Test
    fun `should control fraction size`() {
        formatter.append(Point())
        formatter.append(Symbol("1"))
        formatter.append(Symbol("5"))
        formatter.append(Symbol("7"))
        assertEquals("0.15", formatter.getText())
    }

    @Test
    fun `should add two digits suffix`() {
        formatter.append(Point())
        assertEquals("00", formatter.getSuffix())
    }

    @Test
    fun `should add one digit suffix`() {
        formatter.append(Point())
        formatter.append(Symbol("3"))
        assertEquals("0", formatter.getSuffix())
    }

    @Test
    fun `determine min value`() {
        val state = formatter.getInputState(
            minAmount = 5.0,
            maxAmount = 100.0,
            data = "4")
        assertTrue(state is InputState.Min)
    }

    @Test
    fun `determine max value`() {
        val state = formatter.getInputState(
            minAmount = 5.0,
            maxAmount = 100.0,
            data = "101")
        assertTrue(state is InputState.Max)
    }

    @Test
    fun `determine fine value`() {
        val state = formatter.getInputState(
            minAmount = 5.0,
            maxAmount = 100.0,
            data = "100")
        assertEquals(InputState.Fine, state)
    }

    @Test
    fun `determine Unspecified value`() {
        val state = formatter.getInputState(
            minAmount = 5.0,
            maxAmount = 100.0,
            data = ".0")
        assertEquals(InputState.Unspecified, state)
    }

}
