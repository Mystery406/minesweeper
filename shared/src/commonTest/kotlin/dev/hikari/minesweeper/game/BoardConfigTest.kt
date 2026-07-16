package dev.hikari.minesweeper.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BoardConfigTest {
    @Test
    fun classicPresetsHaveExactDimensions() {
        assertEquals(BoardConfig(9, 9, 10), Difficulty.Beginner.boardConfig())
        assertEquals(BoardConfig(16, 16, 40), Difficulty.Intermediate.boardConfig())
        assertEquals(BoardConfig(30, 16, 99), Difficulty.Expert.boardConfig())
    }

    @Test
    fun customBoundariesAreAccepted() {
        assertIs<CustomConfigValidation.Valid>(BoardConfig.validateCustom(9, 9, 10))
        assertIs<CustomConfigValidation.Valid>(BoardConfig.validateCustom(30, 24, 668))
    }

    @Test
    fun customErrorsAreReportedTogether() {
        val result = assertIs<CustomConfigValidation.Invalid>(
            BoardConfig.validateCustom(width = 8, height = 25, mineCount = 700),
        )
        assertEquals(
            setOf(CustomConfigError.Width, CustomConfigError.Height, CustomConfigError.Mines),
            result.errors,
        )
    }

    @Test
    fun customMineMaximumAlwaysLeavesASafeCell() {
        assertEquals(80, BoardConfig.maxCustomMines(9, 9))
        assertEquals(668, BoardConfig.maxCustomMines(30, 24))
        val result = BoardConfig.validateCustom(9, 9, 81)
        assertTrue(result is CustomConfigValidation.Invalid)
    }
}
