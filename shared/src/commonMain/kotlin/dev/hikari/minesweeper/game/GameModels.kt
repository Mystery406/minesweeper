package dev.hikari.minesweeper.game

data class CellPosition(
    val row: Int,
    val column: Int,
)

enum class GamePhase {
    Ready,
    Running,
    Won,
    Lost,
}

enum class Mark {
    None,
    Flag,
    Question,
}

sealed interface CellViewState {
    data class Covered(val mark: Mark) : CellViewState
    data class Revealed(val adjacentMines: Int) : CellViewState
    data object Mine : CellViewState
    data object ExplodedMine : CellViewState
    data object WrongFlag : CellViewState
}

data class GameSnapshot(
    val config: BoardConfig,
    val phase: GamePhase,
    val cells: List<CellViewState>,
    val flagCount: Int,
    val remainingMineCount: Int,
    val revealedSafeCount: Int,
) {
    fun cellAt(position: CellPosition): CellViewState {
        require(position.row in 0 until config.height && position.column in 0 until config.width)
        return cells[position.row * config.width + position.column]
    }
}

data class EngineActionResult(
    val phaseBefore: GamePhase,
    val phaseAfter: GamePhase,
    val changed: Boolean,
) {
    val started: Boolean
        get() = phaseBefore == GamePhase.Ready && phaseAfter != GamePhase.Ready

    val finished: Boolean
        get() = phaseBefore !in TERMINAL_PHASES && phaseAfter in TERMINAL_PHASES

    private companion object {
        val TERMINAL_PHASES = setOf(GamePhase.Won, GamePhase.Lost)
    }
}
