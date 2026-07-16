package dev.hikari.minesweeper.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object ClassicPalette {
    val Panel = Color(0xFFC0C0C0)
    val Highlight = Color(0xFFFFFFFF)
    val LightEdge = Color(0xFFDFDFDF)
    val Shadow = Color(0xFF808080)
    val DarkEdge = Color(0xFF000000)
    val CounterBackground = Color(0xFF050000)
    val CounterOn = Color(0xFFFF1010)
    val CounterOff = Color(0xFF310000)
    val Face = Color(0xFFFFFF00)
    val Exploded = Color(0xFFFF0000)
    val Flag = Color(0xFFFF0000)
    val Question = Color(0xFF000080)

    fun clue(number: Int): Color = when (number) {
        1 -> Color(0xFF0000FF)
        2 -> Color(0xFF008000)
        3 -> Color(0xFFFF0000)
        4 -> Color(0xFF000080)
        5 -> Color(0xFF800000)
        6 -> Color(0xFF008080)
        7 -> Color(0xFF000000)
        8 -> Color(0xFF808080)
        else -> Color.Transparent
    }
}

object ClassicMetrics {
    val MinCellSize = 28.dp
    val MaxCellSize = 36.dp
    val OuterBevel = 3.dp
    val InnerBevel = 2.dp
    val StatusHeight = 64.dp
    val FaceSize = 38.dp
    val CounterWidth = 62.dp
    val CounterHeight = 36.dp
}

enum class BevelStyle {
    Raised,
    Sunken,
}
