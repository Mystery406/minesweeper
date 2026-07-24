package dev.hikari.minesweeper

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import dev.hikari.minesweeper.session.createGamePreferences
import java.awt.Dimension
import minesweeper.desktopapp.generated.resources.Res
import minesweeper.desktopapp.generated.resources.app_icon
import org.jetbrains.compose.resources.painterResource

fun main() {
    val gamePreferences = createGamePreferences()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Minesweeper",
            icon = painterResource(Res.drawable.app_icon),
            state = rememberWindowState(width = 920.dp, height = 680.dp),
        ) {
            window.minimumSize = Dimension(360, 420)
            App(gamePreferences)
        }
    }
}
