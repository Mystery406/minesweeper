package dev.hikari.minesweeper.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class ClassicPrimitivesTest {
    @Test
    fun digitalValuesAreAlwaysThreeCharactersAndClampVisually() {
        assertEquals("000", formatDigitalValue(0, allowNegative = true))
        assertEquals("-01", formatDigitalValue(-1, allowNegative = true))
        assertEquals("-99", formatDigitalValue(-150, allowNegative = true))
        assertEquals("999", formatDigitalValue(2_000, allowNegative = true))
        assertEquals("000", formatDigitalValue(-1, allowNegative = false))
    }
}
