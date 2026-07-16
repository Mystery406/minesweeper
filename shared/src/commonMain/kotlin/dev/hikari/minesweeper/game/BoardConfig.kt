package dev.hikari.minesweeper.game

data class BoardConfig(
    val width: Int,
    val height: Int,
    val mineCount: Int,
) {
    init {
        require(width > 0) { "Board width must be positive." }
        require(height > 0) { "Board height must be positive." }
        val area = width.toLong() * height
        require(area <= Int.MAX_VALUE) { "Board is too large." }
        require(mineCount in 1 until area.toInt()) {
            "Mine count must leave at least one safe cell."
        }
    }

    val cellCount: Int = width * height

    companion object {
        val BEGINNER = BoardConfig(width = 9, height = 9, mineCount = 10)
        val INTERMEDIATE = BoardConfig(width = 16, height = 16, mineCount = 40)
        val EXPERT = BoardConfig(width = 30, height = 16, mineCount = 99)
        val DEFAULT_CUSTOM = INTERMEDIATE

        const val MIN_CUSTOM_WIDTH = 9
        const val MAX_CUSTOM_WIDTH = 30
        const val MIN_CUSTOM_HEIGHT = 9
        const val MAX_CUSTOM_HEIGHT = 24
        const val MIN_CUSTOM_MINES = 10
        const val MAX_CUSTOM_MINES = 668

        fun validateCustom(width: Int, height: Int, mineCount: Int): CustomConfigValidation {
            val errors = buildSet {
                if (width !in MIN_CUSTOM_WIDTH..MAX_CUSTOM_WIDTH) add(CustomConfigError.Width)
                if (height !in MIN_CUSTOM_HEIGHT..MAX_CUSTOM_HEIGHT) add(CustomConfigError.Height)

                val maxMines = maxCustomMines(width, height)
                if (mineCount !in MIN_CUSTOM_MINES..maxMines) add(CustomConfigError.Mines)
            }
            return if (errors.isEmpty()) {
                CustomConfigValidation.Valid(BoardConfig(width, height, mineCount))
            } else {
                CustomConfigValidation.Invalid(errors)
            }
        }

        fun maxCustomMines(width: Int, height: Int): Int {
            val cells = width.toLong().coerceAtLeast(0) * height.toLong().coerceAtLeast(0)
            return minOf(MAX_CUSTOM_MINES.toLong(), (cells - 1).coerceAtLeast(0)).toInt()
        }
    }
}

enum class Difficulty {
    Beginner,
    Intermediate,
    Expert,
    Custom,
}

fun Difficulty.boardConfig(customConfig: BoardConfig = BoardConfig.DEFAULT_CUSTOM): BoardConfig =
    when (this) {
        Difficulty.Beginner -> BoardConfig.BEGINNER
        Difficulty.Intermediate -> BoardConfig.INTERMEDIATE
        Difficulty.Expert -> BoardConfig.EXPERT
        Difficulty.Custom -> customConfig
    }

enum class CustomConfigError {
    Width,
    Height,
    Mines,
}

sealed interface CustomConfigValidation {
    data class Valid(val config: BoardConfig) : CustomConfigValidation
    data class Invalid(val errors: Set<CustomConfigError>) : CustomConfigValidation
}
