package dev.hikari.minesweeper.session

import com.russhwolf.settings.MapSettings
import dev.hikari.minesweeper.game.BoardConfig
import dev.hikari.minesweeper.game.Difficulty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class GamePreferencesTest {
    @Test
    fun missingAndInvalidValuesFallBackSafely() {
        val settings = MapSettings(
            "minesweeper.v1.selectedDifficulty" to "Unknown",
            "minesweeper.v1.custom.width" to 2,
            "minesweeper.v1.custom.height" to 99,
            "minesweeper.v1.custom.mines" to 900,
            "minesweeper.v1.best.Beginner" to -1,
        )

        val saved = SettingsGamePreferences(settings).load()

        assertEquals(Difficulty.Beginner, saved.selectedDifficulty)
        assertEquals(BoardConfig.DEFAULT_CUSTOM, saved.customConfig)
        assertFalse(saved.bestTimes.containsKey(Difficulty.Beginner))
    }

    @Test
    fun validValuesAndZeroSecondRecordRoundTrip() {
        val settings = MapSettings()
        val preferences = SettingsGamePreferences(settings)
        val custom = BoardConfig(30, 24, 668)

        preferences.saveSelectedDifficulty(Difficulty.Custom)
        preferences.saveCustomConfig(custom)
        preferences.saveBestTime(Difficulty.Beginner, 0)

        val saved = preferences.load()
        assertEquals(Difficulty.Custom, saved.selectedDifficulty)
        assertEquals(custom, saved.customConfig)
        assertEquals(0, saved.bestTimes[Difficulty.Beginner])
    }

    @Test
    fun resettingRecordsPreservesSelectionAndCustomValues() {
        val settings = MapSettings()
        val preferences = SettingsGamePreferences(settings)
        val custom = BoardConfig(12, 12, 20)
        preferences.saveSelectedDifficulty(Difficulty.Custom)
        preferences.saveCustomConfig(custom)
        preferences.saveBestTime(Difficulty.Beginner, 12)
        preferences.saveBestTime(Difficulty.Intermediate, 34)

        preferences.resetBestTimes()

        val saved = preferences.load()
        assertEquals(Difficulty.Custom, saved.selectedDifficulty)
        assertEquals(custom, saved.customConfig)
        assertNull(saved.bestTimes[Difficulty.Beginner])
        assertNull(saved.bestTimes[Difficulty.Intermediate])
    }
}
