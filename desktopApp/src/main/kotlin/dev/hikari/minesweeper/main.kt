package dev.hikari.minesweeper

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import java.awt.Dimension

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Minesweeper",
        state = rememberWindowState(width = 920.dp, height = 680.dp),
    ) {
        window.minimumSize = Dimension(360, 420)
        App()
    }
}
