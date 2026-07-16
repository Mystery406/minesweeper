package dev.hikari.minesweeper.session

import com.russhwolf.settings.Settings
import dev.hikari.minesweeper.game.BoardConfig
import dev.hikari.minesweeper.game.CustomConfigValidation
import dev.hikari.minesweeper.game.Difficulty

data class SavedGamePreferences(
    val selectedDifficulty: Difficulty = Difficulty.Beginner,
    val customConfig: BoardConfig = BoardConfig.DEFAULT_CUSTOM,
    val bestTimes: Map<Difficulty, Int> = emptyMap(),
)

interface GamePreferences {
    fun load(): SavedGamePreferences
    fun saveSelectedDifficulty(value: Difficulty)
    fun saveCustomConfig(value: BoardConfig)
    fun saveBestTime(difficulty: Difficulty, seconds: Int)
    fun resetBestTimes()
}

class SettingsGamePreferences(
    private val settings: Settings = Settings(),
) : GamePreferences {
    override fun load(): SavedGamePreferences {
        val selectedDifficulty = settings.getStringOrNull(Keys.SELECTED_DIFFICULTY)
            ?.let { stored -> Difficulty.entries.firstOrNull { it.name == stored } }
            ?: Difficulty.Beginner

        val storedCustom = BoardConfig.validateCustom(
            width = settings.getInt(Keys.CUSTOM_WIDTH, BoardConfig.DEFAULT_CUSTOM.width),
            height = settings.getInt(Keys.CUSTOM_HEIGHT, BoardConfig.DEFAULT_CUSTOM.height),
            mineCount = settings.getInt(Keys.CUSTOM_MINES, BoardConfig.DEFAULT_CUSTOM.mineCount),
        )
        val customConfig = (storedCustom as? CustomConfigValidation.Valid)?.config
            ?: BoardConfig.DEFAULT_CUSTOM

        val bestTimes = buildMap {
            PRESET_DIFFICULTIES.forEach { difficulty ->
                settings.getIntOrNull(Keys.bestTime(difficulty))
                    ?.takeIf { it >= 0 }
                    ?.let { put(difficulty, it) }
            }
        }
        return SavedGamePreferences(selectedDifficulty, customConfig, bestTimes)
    }

    override fun saveSelectedDifficulty(value: Difficulty) {
        settings.putString(Keys.SELECTED_DIFFICULTY, value.name)
    }

    override fun saveCustomConfig(value: BoardConfig) {
        val validation = BoardConfig.validateCustom(value.width, value.height, value.mineCount)
        require(validation is CustomConfigValidation.Valid) { "Only valid Custom settings can be saved." }
        settings.putInt(Keys.CUSTOM_WIDTH, value.width)
        settings.putInt(Keys.CUSTOM_HEIGHT, value.height)
        settings.putInt(Keys.CUSTOM_MINES, value.mineCount)
    }

    override fun saveBestTime(difficulty: Difficulty, seconds: Int) {
        require(difficulty in PRESET_DIFFICULTIES) { "Custom games do not have best times." }
        require(seconds >= 0) { "Best time cannot be negative." }
        settings.putInt(Keys.bestTime(difficulty), seconds)
    }

    override fun resetBestTimes() {
        PRESET_DIFFICULTIES.forEach { settings.remove(Keys.bestTime(it)) }
    }

    private object Keys {
        private const val PREFIX = "minesweeper.v1."
        const val SELECTED_DIFFICULTY = "${PREFIX}selectedDifficulty"
        const val CUSTOM_WIDTH = "${PREFIX}custom.width"
        const val CUSTOM_HEIGHT = "${PREFIX}custom.height"
        const val CUSTOM_MINES = "${PREFIX}custom.mines"
        fun bestTime(difficulty: Difficulty) = "${PREFIX}best.${difficulty.name}"
    }
}

val PRESET_DIFFICULTIES = listOf(
    Difficulty.Beginner,
    Difficulty.Intermediate,
    Difficulty.Expert,
)
