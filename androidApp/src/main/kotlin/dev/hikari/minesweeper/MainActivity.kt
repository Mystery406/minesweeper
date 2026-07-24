package dev.hikari.minesweeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.hikari.minesweeper.session.createGamePreferences

class MainActivity : ComponentActivity() {
    private val gamePreferences by lazy { createGamePreferences(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(gamePreferences)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    AppPreview()
}
