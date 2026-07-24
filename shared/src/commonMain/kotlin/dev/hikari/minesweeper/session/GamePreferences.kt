package dev.hikari.minesweeper.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.hikari.minesweeper.game.BoardConfig
import dev.hikari.minesweeper.game.CustomConfigValidation
import dev.hikari.minesweeper.game.Difficulty
import kotlinx.coroutines.flow.first

data class SavedGamePreferences(
    val selectedDifficulty: Difficulty = Difficulty.Beginner,
    val customConfig: BoardConfig = BoardConfig.DEFAULT_CUSTOM,
    val bestTimes: Map<Difficulty, Int> = emptyMap(),
)

interface GamePreferences {
    suspend fun load(): SavedGamePreferences
    suspend fun saveSelectedDifficulty(value: Difficulty)
    suspend fun saveCustomGame(value: BoardConfig)
    suspend fun saveBestTime(difficulty: Difficulty, seconds: Int)
    suspend fun resetBestTimes()
}

internal class DataStoreGamePreferences(
    private val dataStore: DataStore<Preferences>,
) : GamePreferences {
    override suspend fun load(): SavedGamePreferences {
        val preferences = dataStore.data.first()
        val selectedDifficulty = preferences[Keys.SELECTED_DIFFICULTY]
            ?.let { stored -> Difficulty.entries.firstOrNull { it.name == stored } }
            ?: Difficulty.Beginner

        val storedCustom = BoardConfig.validateCustom(
            width = preferences[Keys.CUSTOM_WIDTH] ?: BoardConfig.DEFAULT_CUSTOM.width,
            height = preferences[Keys.CUSTOM_HEIGHT] ?: BoardConfig.DEFAULT_CUSTOM.height,
            mineCount = preferences[Keys.CUSTOM_MINES] ?: BoardConfig.DEFAULT_CUSTOM.mineCount,
        )
        val customConfig = (storedCustom as? CustomConfigValidation.Valid)?.config
            ?: BoardConfig.DEFAULT_CUSTOM

        val bestTimes = buildMap {
            PRESET_DIFFICULTIES.forEach { difficulty ->
                preferences[Keys.bestTime(difficulty)]
                    ?.takeIf { it >= 0 }
                    ?.let { put(difficulty, it) }
            }
        }
        return SavedGamePreferences(selectedDifficulty, customConfig, bestTimes)
    }

    override suspend fun saveSelectedDifficulty(value: Difficulty) {
        dataStore.edit { preferences ->
            preferences[Keys.SELECTED_DIFFICULTY] = value.name
        }
    }

    override suspend fun saveCustomGame(value: BoardConfig) {
        val validation = BoardConfig.validateCustom(value.width, value.height, value.mineCount)
        require(validation is CustomConfigValidation.Valid) { "Only valid Custom settings can be saved." }
        dataStore.edit { preferences ->
            preferences[Keys.SELECTED_DIFFICULTY] = Difficulty.Custom.name
            preferences[Keys.CUSTOM_WIDTH] = value.width
            preferences[Keys.CUSTOM_HEIGHT] = value.height
            preferences[Keys.CUSTOM_MINES] = value.mineCount
        }
    }

    override suspend fun saveBestTime(difficulty: Difficulty, seconds: Int) {
        require(difficulty in PRESET_DIFFICULTIES) { "Custom games do not have best times." }
        require(seconds >= 0) { "Best time cannot be negative." }
        dataStore.edit { preferences ->
            preferences[Keys.bestTime(difficulty)] = seconds
        }
    }

    override suspend fun resetBestTimes() {
        dataStore.edit { preferences ->
            PRESET_DIFFICULTIES.forEach { difficulty ->
                val key = Keys.bestTime(difficulty)
                if (preferences[key] != null) {
                    preferences.remove(key)
                }
            }
        }
    }

    private object Keys {
        private const val PREFIX = "minesweeper.v1."
        val SELECTED_DIFFICULTY = stringPreferencesKey("${PREFIX}selectedDifficulty")
        val CUSTOM_WIDTH = intPreferencesKey("${PREFIX}custom.width")
        val CUSTOM_HEIGHT = intPreferencesKey("${PREFIX}custom.height")
        val CUSTOM_MINES = intPreferencesKey("${PREFIX}custom.mines")
        fun bestTime(difficulty: Difficulty) = intPreferencesKey("${PREFIX}best.${difficulty.name}")
    }
}

val PRESET_DIFFICULTIES = listOf(
    Difficulty.Beginner,
    Difficulty.Intermediate,
    Difficulty.Expert,
)
