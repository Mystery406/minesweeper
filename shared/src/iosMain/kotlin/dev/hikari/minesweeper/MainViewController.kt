package dev.hikari.minesweeper

import androidx.compose.ui.window.ComposeUIViewController
import dev.hikari.minesweeper.session.createGamePreferences

private val gamePreferences = createGamePreferences()

fun MainViewController() = ComposeUIViewController { App(gamePreferences) }
