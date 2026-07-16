package dev.hikari.minesweeper.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.hikari.minesweeper.game.BoardConfig
import dev.hikari.minesweeper.game.CellPosition
import dev.hikari.minesweeper.game.CellViewState
import dev.hikari.minesweeper.game.Difficulty
import dev.hikari.minesweeper.game.GamePhase
import dev.hikari.minesweeper.game.Mark
import dev.hikari.minesweeper.game.MineLayoutGenerator
import dev.hikari.minesweeper.session.GameClock
import dev.hikari.minesweeper.session.GameIntent
import dev.hikari.minesweeper.session.GamePreferences
import dev.hikari.minesweeper.session.MinesweeperController
import dev.hikari.minesweeper.session.SavedGamePreferences
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

@OptIn(ExperimentalTestApi::class)
class MinesweeperUiTest {
    @Test
    fun initialBoardRevealAndFaceRestartAreConnected() = runComposeUiTest {
        val controller = controller()
        setContent {
            Box(Modifier.size(380.dp, 620.dp)) {
                MinesweeperScreen(controller)
            }
        }

        onNodeWithTag("status_panel").fetchSemanticsNode()
        val counterNode = onNodeWithTag("mine_counter").fetchSemanticsNode()
        onNodeWithTag("timer_counter").fetchSemanticsNode()
        assertTrue(counterNode.config[SemanticsProperties.ContentDescription].isNotEmpty())
        onNodeWithTag("cell_1_1").performClick()
        waitForIdle()
        assertEquals(GamePhase.Running, controller.state.phase)

        onNodeWithTag("face_button").performClick()
        waitForIdle()
        assertEquals(GamePhase.Ready, controller.state.phase)
        assertEquals(0, controller.state.elapsedSeconds)
    }

    @Test
    fun longClickMarksCellAndUpdatesCounter() = runComposeUiTest {
        val controller = controller()
        setContent { MinesweeperScreen(controller) }

        onNodeWithTag("cell_2_2").performSemanticsAction(SemanticsActions.OnLongClick)
        waitForIdle()

        assertEquals(Mark.Flag, (controller.state.cellAt(CellPosition(2, 2)) as dev.hikari.minesweeper.game.CellViewState.Covered).mark)
        assertEquals(9, controller.state.remainingMineCount)
        assertTrue(
            onNodeWithTag("mine_counter").fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription]
                .isNotEmpty(),
        )
    }

    @Test
    fun tapOnRevealedClueChordsAfterLongPressFlags() = runComposeUiTest {
        val controller = controller()
        setContent { MinesweeperScreen(controller) }

        listOf("cell_0_0", "cell_0_1", "cell_0_2", "cell_1_0").forEach { tag ->
            onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnLongClick)
        }
        onNodeWithTag("cell_1_1").performClick()
        waitForIdle()
        assertEquals(4, (controller.state.cellAt(CellPosition(1, 1)) as CellViewState.Revealed).adjacentMines)

        onNodeWithTag("cell_1_1").performClick()
        waitForIdle()
        assertTrue(controller.state.cellAt(CellPosition(1, 2)) is CellViewState.Revealed)
    }

    @Test
    fun secondaryMouseButtonCyclesMark() = runComposeUiTest {
        val controller = controller()
        setContent { MinesweeperScreen(controller) }

        onNodeWithTag("cell_2_2").performMouseInput {
            updatePointerTo(center)
            press(MouseButton.Secondary)
            advanceEventTime(60)
            release(MouseButton.Secondary)
        }
        waitForIdle()

        assertEquals(
            Mark.Flag,
            (controller.state.cellAt(CellPosition(2, 2)) as CellViewState.Covered).mark,
        )
    }

    @Test
    fun middleMouseButtonChordsARevealedClue() = runComposeUiTest {
        val controller = controller()
        setContent { MinesweeperScreen(controller) }
        prepareChord(controller)
        waitForIdle()

        onNodeWithTag("cell_1_1").performMouseInput {
            updatePointerTo(center)
            press(MouseButton.Tertiary)
            advanceEventTime(60)
            release(MouseButton.Tertiary)
        }
        waitForIdle()

        assertTrue(controller.state.cellAt(CellPosition(1, 2)) is CellViewState.Revealed)
    }

    @Test
    fun simultaneousPrimaryAndSecondaryButtonsChordARevealedClue() = runComposeUiTest {
        val controller = controller()
        setContent { MinesweeperScreen(controller) }
        prepareChord(controller)
        waitForIdle()

        onNodeWithTag("cell_1_1").performMouseInput {
            updatePointerTo(center)
            press(MouseButton.Primary)
            press(MouseButton.Secondary)
            advanceEventTime(60)
            release(MouseButton.Secondary)
            release(MouseButton.Primary)
        }
        waitForIdle()

        assertTrue(controller.state.cellAt(CellPosition(1, 2)) is CellViewState.Revealed)
    }

    @Test
    fun menuOpensCustomAndBestTimesDialogs() = runComposeUiTest {
        val controller = controller()
        setContent { MinesweeperScreen(controller) }

        onNodeWithTag("game_menu_button").performClick()
        onNodeWithTag("menu_custom").performClick()
        onNodeWithTag("custom_dialog").fetchSemanticsNode()
        onNodeWithTag("custom_width").fetchSemanticsNode()

        onNodeWithTag("custom_cancel").performClick()
        onNodeWithTag("game_menu_button").performClick()
        onNodeWithTag("menu_best_times").performClick()
        onNodeWithTag("best_times_dialog").fetchSemanticsNode()
        onNodeWithTag("reset_best_times").fetchSemanticsNode()
    }

    @Test
    fun expertBoardKeepsStatusOutsideScrollableBoardInSmallViewport() = runComposeUiTest {
        val controller = controller()
        controller.dispatch(GameIntent.SelectDifficulty(Difficulty.Expert))
        setContent {
            Box(Modifier.size(360.dp, 520.dp)) {
                MinesweeperScreen(controller)
            }
        }

        onNodeWithTag("status_panel").fetchSemanticsNode()
        onNodeWithTag("board_scroll").fetchSemanticsNode()
        onNodeWithTag("cell_15_29").fetchSemanticsNode()
        assertEquals(BoardConfig.EXPERT, controller.state.config)
    }

    @Test
    fun faceSemanticsTrackWinAndLossStates() = runComposeUiTest {
        val controller = controller()
        setContent { MinesweeperScreen(controller) }
        val readyDescription = onNodeWithTag("face_button").fetchSemanticsNode()
            .config[SemanticsProperties.StateDescription]

        controller.dispatch(GameIntent.Reveal(CellPosition(1, 1)))
        for (index in 10 until BoardConfig.BEGINNER.cellCount) {
            if (controller.state.phase != GamePhase.Won) {
                controller.dispatch(
                    GameIntent.Reveal(CellPosition(index / BoardConfig.BEGINNER.width, index % BoardConfig.BEGINNER.width)),
                )
            }
        }
        waitForIdle()
        assertEquals(GamePhase.Won, controller.state.phase)
        val wonDescription = onNodeWithTag("face_button").fetchSemanticsNode()
            .config[SemanticsProperties.StateDescription]
        assertNotEquals(readyDescription, wonDescription)

        controller.dispatch(GameIntent.Restart)
        controller.dispatch(GameIntent.Reveal(CellPosition(1, 1)))
        controller.dispatch(GameIntent.Reveal(CellPosition(0, 0)))
        waitForIdle()
        assertEquals(GamePhase.Lost, controller.state.phase)
        val lostDescription = onNodeWithTag("face_button").fetchSemanticsNode()
            .config[SemanticsProperties.StateDescription]
        assertNotEquals(wonDescription, lostDescription)
    }

    @Test
    fun desktopRendererContainsClassicPaletteAndProducesVisualArtifact() = runComposeUiTest {
        val controller = controller()
        setContent {
            Box(Modifier.size(920.dp, 680.dp)) {
                MinesweeperScreen(controller)
            }
        }
        waitForIdle()

        val rendered = onNodeWithTag("minesweeper_screen").captureToImage()
        val colors = rendered.toPixelMap().buffer.toSet()
        listOf(
            ClassicPalette.Panel,
            ClassicPalette.Highlight,
            ClassicPalette.CounterBackground,
            ClassicPalette.CounterOn,
            ClassicPalette.Face,
        ).forEach { expected ->
            assertTrue(expected.toArgb() in colors, "Desktop render is missing classic color $expected")
        }

        val report = File("build/reports/compose-ui/minesweeper-desktop.png")
        report.parentFile.mkdirs()
        val encoded = Image.makeFromBitmap(rendered.asSkiaBitmap())
            .encodeToData(EncodedImageFormat.PNG)
        requireNotNull(encoded) { "Desktop screenshot could not be encoded." }
        report.writeBytes(encoded.bytes)
    }

    private fun controller(): MinesweeperController = MinesweeperController(
        preferences = MemoryPreferences(),
        clock = GameClock { 0L },
        layoutGenerator = MineLayoutGenerator { config -> IntArray(config.mineCount) { it } },
    )

    private fun prepareChord(controller: MinesweeperController) {
        listOf(
            CellPosition(0, 0),
            CellPosition(0, 1),
            CellPosition(0, 2),
            CellPosition(1, 0),
        ).forEach { controller.dispatch(GameIntent.CycleMark(it)) }
        controller.dispatch(GameIntent.Reveal(CellPosition(1, 1)))
    }

    private class MemoryPreferences : GamePreferences {
        override fun load() = SavedGamePreferences()
        override fun saveSelectedDifficulty(value: Difficulty) = Unit
        override fun saveCustomConfig(value: BoardConfig) = Unit
        override fun saveBestTime(difficulty: Difficulty, seconds: Int) = Unit
        override fun resetBestTimes() = Unit
    }
}
