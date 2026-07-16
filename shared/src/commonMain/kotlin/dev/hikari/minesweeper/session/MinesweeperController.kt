package dev.hikari.minesweeper.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.hikari.minesweeper.game.BoardConfig
import dev.hikari.minesweeper.game.CellPosition
import dev.hikari.minesweeper.game.CellViewState
import dev.hikari.minesweeper.game.CustomConfigValidation
import dev.hikari.minesweeper.game.Difficulty
import dev.hikari.minesweeper.game.EngineActionResult
import dev.hikari.minesweeper.game.GamePhase
import dev.hikari.minesweeper.game.MineLayoutGenerator
import dev.hikari.minesweeper.game.MinesweeperEngine
import dev.hikari.minesweeper.game.RandomMineLayoutGenerator
import dev.hikari.minesweeper.game.boardConfig

sealed interface GameIntent {
    data class Reveal(val position: CellPosition) : GameIntent
    data class CycleMark(val position: CellPosition) : GameIntent
    data class Chord(val position: CellPosition) : GameIntent
    data object Restart : GameIntent
    data class SelectDifficulty(val difficulty: Difficulty) : GameIntent
    data class StartCustom(val config: BoardConfig) : GameIntent
    data object Tick : GameIntent
    data object ResetBestTimes : GameIntent
}

data class GameUiState(
    val selectedDifficulty: Difficulty,
    val customConfig: BoardConfig,
    val config: BoardConfig,
    val phase: GamePhase,
    val cells: List<CellViewState>,
    val flagCount: Int,
    val remainingMineCount: Int,
    val elapsedSeconds: Int,
    val bestTimes: Map<Difficulty, Int>,
) {
    val displayedMineCount: Int
        get() = remainingMineCount.coerceIn(MIN_DISPLAYED_MINES, MAX_DISPLAY_VALUE)

    val displayedElapsedSeconds: Int
        get() = elapsedSeconds.coerceIn(0, MAX_DISPLAY_VALUE)

    fun cellAt(position: CellPosition): CellViewState {
        require(position.row in 0 until config.height && position.column in 0 until config.width)
        return cells[position.row * config.width + position.column]
    }

    private companion object {
        const val MIN_DISPLAYED_MINES = -99
        const val MAX_DISPLAY_VALUE = 999
    }
}

class MinesweeperController(
    private val preferences: GamePreferences,
    private val clock: GameClock = MonotonicGameClock(),
    private val layoutGenerator: MineLayoutGenerator = RandomMineLayoutGenerator(),
) {
    private val saved = preferences.load()
    private var selectedDifficulty = saved.selectedDifficulty
    private var customConfig = saved.customConfig
    private var bestTimes = saved.bestTimes.toMutableMap()
    private var engine = createEngine(selectedDifficulty.boardConfig(customConfig))
    private var startedAtMillis: Long? = null
    private var elapsedSeconds: Int = 0

    var state: GameUiState by mutableStateOf(createUiState())
        private set

    fun dispatch(intent: GameIntent) {
        when (intent) {
            is GameIntent.Reveal -> handleEngineAction(engine.reveal(intent.position))
            is GameIntent.CycleMark -> handleEngineAction(engine.cycleMark(intent.position))
            is GameIntent.Chord -> handleEngineAction(engine.chord(intent.position))
            GameIntent.Restart -> restart()
            is GameIntent.SelectDifficulty -> selectDifficulty(intent.difficulty)
            is GameIntent.StartCustom -> startCustom(intent.config)
            GameIntent.Tick -> tick()
            GameIntent.ResetBestTimes -> resetBestTimes()
        }
    }

    private fun handleEngineAction(result: EngineActionResult) {
        if (!result.changed) return

        val now = clock.nowMillis()
        if (result.started) {
            startedAtMillis = now
            elapsedSeconds = 0
        }
        if (result.finished) {
            updateElapsed(now)
            startedAtMillis = null
            if (result.phaseAfter == GamePhase.Won) updateBestTime()
        }
        publish()
    }

    private fun restart() {
        engine.restart()
        startedAtMillis = null
        elapsedSeconds = 0
        publish()
    }

    private fun selectDifficulty(difficulty: Difficulty) {
        selectedDifficulty = difficulty
        preferences.saveSelectedDifficulty(difficulty)
        engine = createEngine(difficulty.boardConfig(customConfig))
        startedAtMillis = null
        elapsedSeconds = 0
        publish()
    }

    private fun startCustom(config: BoardConfig) {
        val validation = BoardConfig.validateCustom(config.width, config.height, config.mineCount)
        if (validation !is CustomConfigValidation.Valid) return

        customConfig = validation.config
        selectedDifficulty = Difficulty.Custom
        preferences.saveCustomConfig(customConfig)
        preferences.saveSelectedDifficulty(selectedDifficulty)
        engine = createEngine(customConfig)
        startedAtMillis = null
        elapsedSeconds = 0
        publish()
    }

    private fun tick() {
        if (engine.phase != GamePhase.Running) return
        val previousElapsed = elapsedSeconds
        updateElapsed(clock.nowMillis())
        if (elapsedSeconds != previousElapsed) publish()
    }

    private fun resetBestTimes() {
        preferences.resetBestTimes()
        bestTimes.clear()
        publish()
    }

    private fun updateElapsed(nowMillis: Long) {
        val startedAt = startedAtMillis ?: return
        val wholeSeconds = ((nowMillis - startedAt).coerceAtLeast(0) / MILLIS_PER_SECOND)
            .coerceAtMost(Int.MAX_VALUE.toLong())
        elapsedSeconds = wholeSeconds.toInt()
    }

    private fun updateBestTime() {
        if (selectedDifficulty !in PRESET_DIFFICULTIES) return
        val previous = bestTimes[selectedDifficulty]
        if (previous == null || elapsedSeconds < previous) {
            bestTimes[selectedDifficulty] = elapsedSeconds
            preferences.saveBestTime(selectedDifficulty, elapsedSeconds)
        }
    }

    private fun createEngine(config: BoardConfig) = MinesweeperEngine(config, layoutGenerator)

    private fun publish() {
        state = createUiState()
    }

    private fun createUiState(): GameUiState {
        val snapshot = engine.snapshot()
        return GameUiState(
            selectedDifficulty = selectedDifficulty,
            customConfig = customConfig,
            config = snapshot.config,
            phase = snapshot.phase,
            cells = snapshot.cells,
            flagCount = snapshot.flagCount,
            remainingMineCount = snapshot.remainingMineCount,
            elapsedSeconds = elapsedSeconds,
            bestTimes = bestTimes.toMap(),
        )
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}
