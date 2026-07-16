package dev.hikari.minesweeper.session

import dev.hikari.minesweeper.game.BoardConfig
import dev.hikari.minesweeper.game.CellPosition
import dev.hikari.minesweeper.game.Difficulty
import dev.hikari.minesweeper.game.GamePhase
import dev.hikari.minesweeper.game.MineLayoutGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class MinesweeperControllerTest {
    @Test
    fun markingDoesNotStartTimerAndTicksUseWholeSeconds() {
        val fixture = fixture()
        val position = CellPosition(1, 1)
        fixture.controller.dispatch(GameIntent.CycleMark(position))
        fixture.clock.now = 5_000
        fixture.controller.dispatch(GameIntent.Tick)

        assertEquals(GamePhase.Ready, fixture.controller.state.phase)
        assertEquals(0, fixture.controller.state.elapsedSeconds)

        fixture.controller.dispatch(GameIntent.CycleMark(position))
        fixture.controller.dispatch(GameIntent.Reveal(position))
        fixture.clock.now = 7_499
        fixture.controller.dispatch(GameIntent.Tick)
        assertEquals(2, fixture.controller.state.elapsedSeconds)
    }

    @Test
    fun timerStopsAtLossAndDisplayClampsAt999() {
        val fixture = fixture()
        fixture.controller.dispatch(GameIntent.Reveal(CellPosition(1, 1)))
        fixture.clock.now = 1_234_000
        fixture.controller.dispatch(GameIntent.Tick)
        assertEquals(1_234, fixture.controller.state.elapsedSeconds)
        assertEquals(999, fixture.controller.state.displayedElapsedSeconds)

        fixture.controller.dispatch(GameIntent.Reveal(CellPosition(0, 0)))
        assertEquals(GamePhase.Lost, fixture.controller.state.phase)
        fixture.clock.now = 2_000_000
        fixture.controller.dispatch(GameIntent.Tick)
        assertEquals(1_234, fixture.controller.state.elapsedSeconds)
    }

    @Test
    fun mineDisplayClampsAtNegative99() {
        val fixture = fixture()
        repeat(81) { index ->
            fixture.controller.dispatch(GameIntent.CycleMark(CellPosition(index / 9, index % 9)))
        }
        assertEquals(-71, fixture.controller.state.remainingMineCount)
        assertEquals(-71, fixture.controller.state.displayedMineCount)

        val expertFixture = fixture(
            saved = SavedGamePreferences(selectedDifficulty = Difficulty.Expert),
        )
        repeat(250) { index ->
            expertFixture.controller.dispatch(GameIntent.CycleMark(CellPosition(index / 30, index % 30)))
        }
        assertEquals(-151, expertFixture.controller.state.remainingMineCount)
        assertEquals(-99, expertFixture.controller.state.displayedMineCount)
    }

    @Test
    fun winningPresetSavesOnlyAFasterRecord() {
        val preferences = FakePreferences(
            SavedGamePreferences(bestTimes = mapOf(Difficulty.Beginner to 15)),
        )
        val fixture = fixture(preferences = preferences)

        winCurrentGame(fixture, seconds = 12)

        assertEquals(GamePhase.Won, fixture.controller.state.phase)
        assertEquals(12, fixture.controller.state.bestTimes[Difficulty.Beginner])
        assertEquals(listOf(Difficulty.Beginner to 12), preferences.savedBestTimes)
    }

    @Test
    fun slowerPresetTimeAndCustomWinDoNotWriteRecords() {
        val presetPreferences = FakePreferences(
            SavedGamePreferences(bestTimes = mapOf(Difficulty.Beginner to 8)),
        )
        val preset = fixture(preferences = presetPreferences)
        winCurrentGame(preset, seconds = 12)
        assertEquals(8, preset.controller.state.bestTimes[Difficulty.Beginner])
        assertEquals(emptyList(), presetPreferences.savedBestTimes)

        val customPreferences = FakePreferences()
        val custom = fixture(preferences = customPreferences)
        custom.controller.dispatch(GameIntent.StartCustom(BoardConfig(9, 9, 10)))
        winCurrentGame(custom, seconds = 5)
        assertEquals(GamePhase.Won, custom.controller.state.phase)
        assertEquals(emptyList(), customPreferences.savedBestTimes)
    }

    @Test
    fun selectionCustomAndResetPersistAtTheirBoundaries() {
        val preferences = FakePreferences()
        val fixture = fixture(preferences = preferences)
        fixture.controller.dispatch(GameIntent.SelectDifficulty(Difficulty.Expert))
        assertEquals(BoardConfig.EXPERT, fixture.controller.state.config)
        assertEquals(Difficulty.Expert, preferences.selectedDifficulty)

        val custom = BoardConfig(12, 10, 20)
        fixture.controller.dispatch(GameIntent.StartCustom(custom))
        assertEquals(custom, fixture.controller.state.config)
        assertEquals(custom, preferences.customConfig)
        assertEquals(Difficulty.Custom, preferences.selectedDifficulty)

        fixture.controller.dispatch(GameIntent.ResetBestTimes)
        assertEquals(1, preferences.resetCount)
        assertEquals(emptyMap(), fixture.controller.state.bestTimes)
    }

    @Test
    fun restartCreatesFreshReadySession() {
        val fixture = fixture()
        fixture.controller.dispatch(GameIntent.Reveal(CellPosition(1, 1)))
        fixture.clock.now = 2_000
        fixture.controller.dispatch(GameIntent.Tick)

        fixture.controller.dispatch(GameIntent.Restart)

        assertEquals(GamePhase.Ready, fixture.controller.state.phase)
        assertEquals(0, fixture.controller.state.elapsedSeconds)
        assertEquals(0, fixture.controller.state.flagCount)
    }

    @Test
    fun restoredSelectionAndCustomConfigurationCreateTheInitialBoard() {
        val custom = BoardConfig(11, 13, 25)
        val fixture = fixture(
            saved = SavedGamePreferences(
                selectedDifficulty = Difficulty.Custom,
                customConfig = custom,
            ),
        )

        assertEquals(Difficulty.Custom, fixture.controller.state.selectedDifficulty)
        assertEquals(custom, fixture.controller.state.config)
        assertNull(fixture.controller.state.bestTimes[Difficulty.Beginner])
    }

    private fun winCurrentGame(fixture: Fixture, seconds: Int) {
        val config = fixture.controller.state.config
        val mines = fixture.mineIndices(config)
        val firstSafe = (0 until config.cellCount).first { it !in mines }
        fixture.controller.dispatch(GameIntent.Reveal(position(firstSafe, config)))
        fixture.clock.now += seconds * 1_000L
        for (index in 0 until config.cellCount) {
            if (index !in mines && fixture.controller.state.phase != GamePhase.Won) {
                fixture.controller.dispatch(GameIntent.Reveal(position(index, config)))
            }
        }
    }

    private fun fixture(
        saved: SavedGamePreferences = SavedGamePreferences(),
        preferences: FakePreferences = FakePreferences(saved),
    ): Fixture {
        val clock = FakeClock()
        val mineIndices: (BoardConfig) -> Set<Int> = { config ->
            (0 until config.mineCount).toSet()
        }
        val controller = MinesweeperController(
            preferences = preferences,
            clock = clock,
            layoutGenerator = MineLayoutGenerator { config ->
                mineIndices(config).toIntArray()
            },
        )
        return Fixture(controller, clock, mineIndices)
    }

    private fun position(index: Int, config: BoardConfig) =
        CellPosition(index / config.width, index % config.width)

    private data class Fixture(
        val controller: MinesweeperController,
        val clock: FakeClock,
        val mineIndices: (BoardConfig) -> Set<Int>,
    )

    private class FakeClock(var now: Long = 0) : GameClock {
        override fun nowMillis(): Long = now
    }

    private class FakePreferences(
        private var saved: SavedGamePreferences = SavedGamePreferences(),
    ) : GamePreferences {
        var selectedDifficulty: Difficulty? = null
        var customConfig: BoardConfig? = null
        val savedBestTimes = mutableListOf<Pair<Difficulty, Int>>()
        var resetCount = 0

        override fun load(): SavedGamePreferences = saved

        override fun saveSelectedDifficulty(value: Difficulty) {
            selectedDifficulty = value
        }

        override fun saveCustomConfig(value: BoardConfig) {
            customConfig = value
        }

        override fun saveBestTime(difficulty: Difficulty, seconds: Int) {
            savedBestTimes += difficulty to seconds
        }

        override fun resetBestTimes() {
            resetCount += 1
        }
    }
}
