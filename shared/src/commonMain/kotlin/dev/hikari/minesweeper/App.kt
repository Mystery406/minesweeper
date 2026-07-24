package dev.hikari.minesweeper

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.hikari.minesweeper.game.BoardConfig
import dev.hikari.minesweeper.game.Difficulty
import dev.hikari.minesweeper.session.GamePreferences
import dev.hikari.minesweeper.session.MinesweeperController
import dev.hikari.minesweeper.session.SavedGamePreferences
import dev.hikari.minesweeper.ui.MinesweeperScreen
import kotlinx.coroutines.CancellationException

@Composable
fun App(preferences: GamePreferences) {
    AppContent(preferences = preferences, initialPreferences = null)
}

@Composable
fun AppPreview() {
    AppContent(
        preferences = PreviewGamePreferences,
        initialPreferences = SavedGamePreferences(),
    )
}

@Composable
private fun AppContent(
    preferences: GamePreferences,
    initialPreferences: SavedGamePreferences?,
) {
    val restoredPreferences = produceState(
        initialValue = initialPreferences,
        key1 = preferences,
    ) {
        if (value == null) {
            value = try {
                preferences.load()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                SavedGamePreferences()
            }
        }
    }.value
    val persistenceScope = rememberCoroutineScope()

    MaterialTheme {
        if (restoredPreferences == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            val controller = remember(preferences, restoredPreferences, persistenceScope) {
                MinesweeperController(
                    preferences = preferences,
                    initialPreferences = restoredPreferences,
                    persistenceScope = persistenceScope,
                )
            }
            MinesweeperScreen(controller)
        }
    }
}

private object PreviewGamePreferences : GamePreferences {
    override suspend fun load() = SavedGamePreferences()
    override suspend fun saveSelectedDifficulty(value: Difficulty) = Unit
    override suspend fun saveCustomGame(value: BoardConfig) = Unit
    override suspend fun saveBestTime(difficulty: Difficulty, seconds: Int) = Unit
    override suspend fun resetBestTimes() = Unit
}
