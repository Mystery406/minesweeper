package dev.hikari.minesweeper

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.hikari.minesweeper.session.createGamePreferences

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val gamePreferences = createGamePreferences()
    ComposeViewport {
        App(gamePreferences)
    }
}
