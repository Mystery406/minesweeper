package dev.hikari.minesweeper.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.hikari.minesweeper.game.BoardConfig
import dev.hikari.minesweeper.game.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class GamePreferencesTest {
    @Test
    fun missingAndInvalidValuesFallBackSafely() = runTest {
        val dataStore = InMemoryPreferencesDataStore(
            mutablePreferencesOf(
                stringPreferencesKey("minesweeper.v1.selectedDifficulty") to "Unknown",
                intPreferencesKey("minesweeper.v1.custom.width") to 2,
                intPreferencesKey("minesweeper.v1.custom.height") to 99,
                intPreferencesKey("minesweeper.v1.custom.mines") to 900,
                intPreferencesKey("minesweeper.v1.best.Beginner") to -1,
            ),
        )

        val saved = DataStoreGamePreferences(dataStore).load()

        assertEquals(Difficulty.Beginner, saved.selectedDifficulty)
        assertEquals(BoardConfig.DEFAULT_CUSTOM, saved.customConfig)
        assertFalse(saved.bestTimes.containsKey(Difficulty.Beginner))
    }

    @Test
    fun validValuesAndZeroSecondRecordRoundTrip() = runTest {
        val preferences = DataStoreGamePreferences(InMemoryPreferencesDataStore())
        val custom = BoardConfig(30, 24, 668)

        preferences.saveCustomGame(custom)
        preferences.saveBestTime(Difficulty.Beginner, 0)

        val saved = preferences.load()
        assertEquals(Difficulty.Custom, saved.selectedDifficulty)
        assertEquals(custom, saved.customConfig)
        assertEquals(0, saved.bestTimes[Difficulty.Beginner])
    }

    @Test
    fun resettingRecordsPreservesSelectionAndCustomValues() = runTest {
        val preferences = DataStoreGamePreferences(InMemoryPreferencesDataStore())
        val custom = BoardConfig(12, 12, 20)
        preferences.saveCustomGame(custom)
        preferences.saveBestTime(Difficulty.Beginner, 12)
        preferences.saveBestTime(Difficulty.Intermediate, 34)

        preferences.resetBestTimes()

        val saved = preferences.load()
        assertEquals(Difficulty.Custom, saved.selectedDifficulty)
        assertEquals(custom, saved.customConfig)
        assertNull(saved.bestTimes[Difficulty.Beginner])
        assertNull(saved.bestTimes[Difficulty.Intermediate])
    }

    private class InMemoryPreferencesDataStore(
        initialPreferences: Preferences = mutablePreferencesOf(),
    ) : DataStore<Preferences> {
        private val state = MutableStateFlow(initialPreferences)
        private val mutex = Mutex()

        override val data: Flow<Preferences> = state

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences = mutex.withLock {
            transform(state.value).also { updated -> state.value = updated }
        }
    }
}
