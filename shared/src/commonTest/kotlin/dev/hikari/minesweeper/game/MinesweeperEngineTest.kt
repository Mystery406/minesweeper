package dev.hikari.minesweeper.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MinesweeperEngineTest {
    @Test
    fun cluesUseCornerEdgeAndCenterNeighbors() {
        val engine = engine(width = 3, height = 3, mines = intArrayOf(0, 4))

        assertEquals(1, engine.clueAt(cell(0, 2)))
        assertEquals(2, engine.clueAt(cell(0, 1)))
        assertEquals(1, engine.clueAt(cell(1, 1)))
        assertEquals(1, engine.clueAt(cell(2, 2)))
    }

    @Test
    fun firstRevealRelocatesOnlyTheSelectedMine() {
        val engine = engine(width = 3, height = 3, mines = intArrayOf(0, 8))

        engine.reveal(cell(0, 0))

        assertFalse(engine.hasMineAt(cell(0, 0)))
        assertTrue(engine.hasMineAt(cell(0, 1)))
        assertTrue(engine.hasMineAt(cell(2, 2)))
        assertEquals(1, assertIs<CellViewState.Revealed>(engine.snapshot().cellAt(cell(0, 0))).adjacentMines)
    }

    @Test
    fun safeFirstRevealDoesNotChangeTheLayout() {
        val engine = engine(width = 3, height = 3, mines = intArrayOf(0, 8))

        engine.reveal(cell(1, 1))

        assertTrue(engine.hasMineAt(cell(0, 0)))
        assertTrue(engine.hasMineAt(cell(2, 2)))
    }

    @Test
    fun zeroExpansionRevealsConnectedAreaAndBorderClues() {
        val engine = engine(width = 4, height = 4, mines = intArrayOf(15))

        engine.reveal(cell(0, 0))

        assertEquals(GamePhase.Won, engine.phase)
        assertEquals(15, engine.revealedSafeCount)
        assertEquals(CellViewState.Covered(Mark.Flag), engine.snapshot().cellAt(cell(3, 3)))
    }

    @Test
    fun marksCycleAndOnlyFlagsChangeTheCounter() {
        val engine = engine(width = 3, height = 3, mines = intArrayOf(8))
        val position = cell(0, 0)

        engine.cycleMark(position)
        assertEquals(CellViewState.Covered(Mark.Flag), engine.snapshot().cellAt(position))
        assertEquals(0, engine.snapshot().remainingMineCount)

        engine.cycleMark(position)
        assertEquals(CellViewState.Covered(Mark.Question), engine.snapshot().cellAt(position))
        assertEquals(1, engine.snapshot().remainingMineCount)

        engine.cycleMark(position)
        assertEquals(CellViewState.Covered(Mark.None), engine.snapshot().cellAt(position))
    }

    @Test
    fun counterCanBecomeNegativeWhenTooManyFlagsArePlaced() {
        val engine = engine(width = 3, height = 3, mines = intArrayOf(8))
        engine.cycleMark(cell(0, 0))
        engine.cycleMark(cell(0, 1))

        assertEquals(-1, engine.snapshot().remainingMineCount)
    }

    @Test
    fun flagsBlockRevealAndQuestionsDoNot() {
        val engine = engine(width = 3, height = 3, mines = intArrayOf(8))
        val position = cell(0, 0)
        engine.cycleMark(position)

        assertFalse(engine.reveal(position).changed)
        assertEquals(GamePhase.Ready, engine.phase)

        engine.cycleMark(position)
        assertTrue(engine.reveal(position).changed)
        assertIs<CellViewState.Revealed>(engine.snapshot().cellAt(position))
    }

    @Test
    fun chordDoesNothingUntilFlagCountMatchesClue() {
        val engine = engine(width = 3, height = 3, mines = intArrayOf(0, 8))
        engine.reveal(cell(1, 1))
        engine.cycleMark(cell(0, 0))

        assertFalse(engine.chord(cell(1, 1)).changed)
        assertEquals(1, engine.revealedSafeCount)
    }

    @Test
    fun successfulChordRevealsNeighborsAndCanWin() {
        val engine = engine(width = 3, height = 3, mines = intArrayOf(0, 8))
        engine.reveal(cell(1, 1))
        engine.cycleMark(cell(0, 0))
        engine.cycleMark(cell(2, 2))

        assertTrue(engine.chord(cell(1, 1)).changed)
        assertEquals(GamePhase.Won, engine.phase)
        assertEquals(7, engine.revealedSafeCount)
    }

    @Test
    fun wrongFlagChordDetonatesAnUnflaggedMine() {
        val engine = engine(width = 3, height = 3, mines = intArrayOf(0, 8))
        engine.reveal(cell(1, 1))
        engine.cycleMark(cell(0, 0))
        engine.cycleMark(cell(0, 1))

        engine.chord(cell(1, 1))

        assertEquals(GamePhase.Lost, engine.phase)
        assertEquals(CellViewState.ExplodedMine, engine.snapshot().cellAt(cell(2, 2)))
        assertEquals(CellViewState.WrongFlag, engine.snapshot().cellAt(cell(0, 1)))
    }

    @Test
    fun lossProjectsMinesExplosionAndWrongFlags() {
        val engine = engine(width = 3, height = 3, mines = intArrayOf(0, 8))
        engine.reveal(cell(1, 1))
        engine.cycleMark(cell(0, 1))
        engine.cycleMark(cell(2, 2))
        engine.reveal(cell(0, 0))

        val snapshot = engine.snapshot()
        assertEquals(CellViewState.ExplodedMine, snapshot.cellAt(cell(0, 0)))
        assertEquals(CellViewState.WrongFlag, snapshot.cellAt(cell(0, 1)))
        assertEquals(CellViewState.Covered(Mark.Flag), snapshot.cellAt(cell(2, 2)))
    }

    @Test
    fun terminalActionsAreIgnoredAndRestartClearsTheBoardState() {
        val engine = engine(width = 3, height = 3, mines = intArrayOf(8))
        engine.reveal(cell(0, 0))
        assertEquals(GamePhase.Won, engine.phase)

        assertFalse(engine.cycleMark(cell(2, 2)).changed)
        assertFalse(engine.reveal(cell(2, 2)).changed)

        engine.restart()
        val restarted = engine.snapshot()
        assertEquals(GamePhase.Ready, restarted.phase)
        assertEquals(0, restarted.flagCount)
        assertEquals(0, restarted.revealedSafeCount)
        assertTrue(restarted.cells.all { it == CellViewState.Covered(Mark.None) })
    }

    private fun engine(width: Int, height: Int, mines: IntArray): MinesweeperEngine =
        MinesweeperEngine(
            config = BoardConfig(width, height, mines.size),
            layoutGenerator = MineLayoutGenerator { mines.copyOf() },
        )

    private fun cell(row: Int, column: Int) = CellPosition(row, column)
}
