package dev.hikari.minesweeper

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.hikari.minesweeper.session.MinesweeperController
import dev.hikari.minesweeper.session.SettingsGamePreferences
import dev.hikari.minesweeper.ui.MinesweeperScreen

@Composable
fun App() {
    val preferences = remember { SettingsGamePreferences() }
    val controller = remember(preferences) { MinesweeperController(preferences) }

    MaterialTheme {
        MinesweeperScreen(controller)
    }
}
