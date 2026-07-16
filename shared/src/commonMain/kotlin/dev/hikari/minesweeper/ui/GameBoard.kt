package dev.hikari.minesweeper.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.hikari.minesweeper.game.CellPosition
import dev.hikari.minesweeper.game.CellViewState
import dev.hikari.minesweeper.game.GamePhase
import dev.hikari.minesweeper.game.Mark
import dev.hikari.minesweeper.session.GameIntent
import dev.hikari.minesweeper.session.GameUiState
import minesweeper.shared.generated.resources.Res
import minesweeper.shared.generated.resources.board_description
import minesweeper.shared.generated.resources.board_scroll_description
import minesweeper.shared.generated.resources.cell_covered
import minesweeper.shared.generated.resources.cell_exploded_mine
import minesweeper.shared.generated.resources.cell_flagged
import minesweeper.shared.generated.resources.cell_mark_action
import minesweeper.shared.generated.resources.cell_mine
import minesweeper.shared.generated.resources.cell_question
import minesweeper.shared.generated.resources.cell_revealed_empty
import minesweeper.shared.generated.resources.cell_revealed_number
import minesweeper.shared.generated.resources.cell_wrong_flag
import org.jetbrains.compose.resources.stringResource

@Composable
fun GameBoard(
    state: GameUiState,
    onIntent: (GameIntent) -> Unit,
    onPressChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val boardDescription = stringResource(
        Res.string.board_description,
        state.config.width,
        state.config.height,
    )
    val scrollDescription = stringResource(Res.string.board_scroll_description)
    val markAction = stringResource(Res.string.cell_mark_action)
    val enabled = state.phase != GamePhase.Won && state.phase != GamePhase.Lost

    ClassicPanel(
        style = BevelStyle.Sunken,
        modifier = modifier
            .fillMaxSize()
            .testTag("board_frame"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(3.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val fittedWidth = maxWidth / state.config.width
            val fittedHeight = maxHeight / state.config.height
            val fitted = minOf(fittedWidth, fittedHeight)
            val cellSize = fitted.coerceIn(ClassicMetrics.MinCellSize, ClassicMetrics.MaxCellSize)
            val boardWidth = cellSize * state.config.width
            val boardHeight = cellSize * state.config.height
            val horizontalPadding = ((maxWidth - boardWidth) / 2).coerceAtLeast(0.dp)
            val verticalPadding = ((maxHeight - boardHeight) / 2).coerceAtLeast(0.dp)
            val scrollable = boardWidth > maxWidth || boardHeight > maxHeight
            val horizontalScroll = rememberScrollState()
            val verticalScroll = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("board_scroll")
                    .semantics {
                        contentDescription = boardDescription
                        if (scrollable) stateDescription = scrollDescription
                    }
                    .horizontalScroll(horizontalScroll)
                    .verticalScroll(verticalScroll),
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = horizontalPadding,
                        top = verticalPadding,
                        end = horizontalPadding,
                        bottom = verticalPadding,
                    ),
                ) {
                    for (row in 0 until state.config.height) {
                        Row {
                            for (column in 0 until state.config.width) {
                                val position = CellPosition(row, column)
                                val cellState = state.cellAt(position)
                                val primaryIntent = if (cellState is CellViewState.Revealed) {
                                    GameIntent.Chord(position)
                                } else {
                                    GameIntent.Reveal(position)
                                }
                                MineCell(
                                    state = cellState,
                                    size = cellSize,
                                    description = cellDescription(cellState, row + 1, column + 1),
                                    clickLabel = cellDescription(cellState, row + 1, column + 1),
                                    markLabel = markAction,
                                    testTagModifier = Modifier.testTag("cell_${row}_$column"),
                                    enabled = enabled,
                                    onPrimaryClick = { onIntent(primaryIntent) },
                                    onCycleMark = { onIntent(GameIntent.CycleMark(position)) },
                                    onChord = { onIntent(GameIntent.Chord(position)) },
                                    onPressChanged = onPressChanged,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun cellDescription(state: CellViewState, row: Int, column: Int): String = when (state) {
    is CellViewState.Covered -> when (state.mark) {
        Mark.None -> stringResource(Res.string.cell_covered, row, column)
        Mark.Flag -> stringResource(Res.string.cell_flagged, row, column)
        Mark.Question -> stringResource(Res.string.cell_question, row, column)
    }

    is CellViewState.Revealed -> if (state.adjacentMines == 0) {
        stringResource(Res.string.cell_revealed_empty, row, column)
    } else {
        stringResource(Res.string.cell_revealed_number, row, column, state.adjacentMines)
    }

    CellViewState.Mine -> stringResource(Res.string.cell_mine, row, column)
    CellViewState.ExplodedMine -> stringResource(Res.string.cell_exploded_mine, row, column)
    CellViewState.WrongFlag -> stringResource(Res.string.cell_wrong_flag, row, column)
}
