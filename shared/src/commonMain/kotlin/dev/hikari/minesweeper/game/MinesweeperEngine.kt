package dev.hikari.minesweeper.game

class MinesweeperEngine(
    val config: BoardConfig,
    private val layoutGenerator: MineLayoutGenerator = RandomMineLayoutGenerator(),
) {
    private val geometry = BoardGeometry(config)
    private val mines = BooleanArray(config.cellCount)
    private val adjacentMines = IntArray(config.cellCount)
    private val revealed = BooleanArray(config.cellCount)
    private val marks = Array(config.cellCount) { Mark.None }

    var phase: GamePhase = GamePhase.Ready
        private set
    var flagCount: Int = 0
        private set
    var revealedSafeCount: Int = 0
        private set
    private var detonatedIndex: Int = NO_CELL
    private var lossCheckpoint: EngineCheckpoint? = null

    init {
        installNewLayout()
    }

    fun reveal(position: CellPosition): EngineActionResult {
        val phaseBefore = phase
        if (phase in TERMINAL_PHASES) return unchanged(phaseBefore)

        val index = geometry.indexOf(position)
        if (revealed[index] || marks[index] == Mark.Flag) return unchanged(phaseBefore)

        if (phase == GamePhase.Ready) {
            relocateFirstMineIfNeeded(index)
            phase = GamePhase.Running
        }

        if (mines[index]) {
            lossCheckpoint = createCheckpoint()
            lose(index)
        } else {
            revealSafeRegion(index)
            finishWinIfComplete()
        }
        return EngineActionResult(phaseBefore, phase, changed = true)
    }

    fun cycleMark(position: CellPosition): EngineActionResult {
        val phaseBefore = phase
        if (phase in TERMINAL_PHASES) return unchanged(phaseBefore)

        val index = geometry.indexOf(position)
        if (revealed[index]) return unchanged(phaseBefore)

        marks[index] = when (marks[index]) {
            Mark.None -> {
                flagCount += 1
                Mark.Flag
            }

            Mark.Flag -> {
                flagCount -= 1
                Mark.Question
            }

            Mark.Question -> Mark.None
        }
        return EngineActionResult(phaseBefore, phase, changed = true)
    }

    fun chord(position: CellPosition): EngineActionResult {
        val phaseBefore = phase
        if (phase != GamePhase.Running) return unchanged(phaseBefore)

        val index = geometry.indexOf(position)
        if (!revealed[index]) return unchanged(phaseBefore)

        var adjacentFlags = 0
        val candidates = mutableListOf<Int>()
        geometry.forEachNeighbor(index) { neighbor ->
            if (marks[neighbor] == Mark.Flag) {
                adjacentFlags += 1
            } else if (!revealed[neighbor]) {
                candidates += neighbor
            }
        }
        if (adjacentFlags != adjacentMines[index] || candidates.isEmpty()) {
            return unchanged(phaseBefore)
        }

        val mine = candidates.firstOrNull { mines[it] }
        if (mine != null) {
            lossCheckpoint = createCheckpoint()
            lose(mine)
        } else {
            candidates.forEach(::revealSafeRegion)
            finishWinIfComplete()
        }
        return EngineActionResult(phaseBefore, phase, changed = true)
    }

    fun undoLoss(): EngineActionResult {
        val phaseBefore = phase
        val checkpoint = lossCheckpoint
        if (phase != GamePhase.Lost || checkpoint == null) return unchanged(phaseBefore)

        checkpoint.mines.copyInto(mines)
        checkpoint.adjacentMines.copyInto(adjacentMines)
        checkpoint.revealed.copyInto(revealed)
        checkpoint.marks.copyInto(marks)
        phase = checkpoint.phase
        flagCount = checkpoint.flagCount
        revealedSafeCount = checkpoint.revealedSafeCount
        detonatedIndex = checkpoint.detonatedIndex
        lossCheckpoint = null
        return EngineActionResult(phaseBefore, phase, changed = true)
    }

    fun restart(): EngineActionResult {
        val phaseBefore = phase
        mines.fill(false)
        adjacentMines.fill(0)
        revealed.fill(false)
        marks.fill(Mark.None)
        phase = GamePhase.Ready
        flagCount = 0
        revealedSafeCount = 0
        detonatedIndex = NO_CELL
        lossCheckpoint = null
        installNewLayout()
        return EngineActionResult(phaseBefore, phase, changed = true)
    }

    fun snapshot(): GameSnapshot = GameSnapshot(
        config = config,
        phase = phase,
        cells = List(config.cellCount, ::projectCell),
        flagCount = flagCount,
        remainingMineCount = config.mineCount - flagCount,
        revealedSafeCount = revealedSafeCount,
        canUndo = phase == GamePhase.Lost && lossCheckpoint != null,
    )

    internal fun hasMineAt(position: CellPosition): Boolean = mines[geometry.indexOf(position)]

    internal fun clueAt(position: CellPosition): Int = adjacentMines[geometry.indexOf(position)]

    private fun projectCell(index: Int): CellViewState {
        if (phase == GamePhase.Lost) {
            if (mines[index] && index == detonatedIndex) return CellViewState.ExplodedMine
            if (mines[index] && marks[index] == Mark.Flag) return CellViewState.Covered(Mark.Flag)
            if (mines[index]) return CellViewState.Mine
            if (marks[index] == Mark.Flag) return CellViewState.WrongFlag
        }
        if (revealed[index]) return CellViewState.Revealed(adjacentMines[index])
        return CellViewState.Covered(marks[index])
    }

    private fun installNewLayout() {
        val generated = layoutGenerator.generate(config)
        require(generated.size == config.mineCount) {
            "Mine layout must contain exactly ${config.mineCount} cells."
        }
        generated.forEach { index ->
            require(index in mines.indices) { "Mine layout contains an out-of-bounds cell." }
            require(!mines[index]) { "Mine layout contains a duplicate cell." }
            mines[index] = true
        }
        recomputeClues()
    }

    private fun relocateFirstMineIfNeeded(selectedIndex: Int) {
        if (!mines[selectedIndex]) return
        val destination = mines.indices.first { !mines[it] }
        mines[selectedIndex] = false
        mines[destination] = true
        recomputeClues()
    }

    private fun recomputeClues() {
        adjacentMines.fill(0)
        mines.indices.filter { mines[it] }.forEach { mine ->
            geometry.forEachNeighbor(mine) { adjacentMines[it] += 1 }
        }
    }

    private fun revealSafeRegion(startIndex: Int) {
        if (revealed[startIndex] || marks[startIndex] == Mark.Flag || mines[startIndex]) return

        val pending = ArrayDeque<Int>()
        pending.addLast(startIndex)
        while (pending.isNotEmpty()) {
            val index = pending.removeFirst()
            if (revealed[index] || marks[index] == Mark.Flag || mines[index]) continue

            revealed[index] = true
            marks[index] = Mark.None
            revealedSafeCount += 1

            if (adjacentMines[index] == 0) {
                geometry.forEachNeighbor(index) { neighbor ->
                    if (!revealed[neighbor] && marks[neighbor] != Mark.Flag && !mines[neighbor]) {
                        pending.addLast(neighbor)
                    }
                }
            }
        }
    }

    private fun finishWinIfComplete() {
        if (revealedSafeCount != config.cellCount - config.mineCount) return
        phase = GamePhase.Won
        mines.indices.forEach { index ->
            if (mines[index]) marks[index] = Mark.Flag
        }
        flagCount = config.mineCount
    }

    private fun lose(index: Int) {
        detonatedIndex = index
        phase = GamePhase.Lost
    }

    private fun createCheckpoint() = EngineCheckpoint(
        mines = mines.copyOf(),
        adjacentMines = adjacentMines.copyOf(),
        revealed = revealed.copyOf(),
        marks = marks.copyOf(),
        phase = phase,
        flagCount = flagCount,
        revealedSafeCount = revealedSafeCount,
        detonatedIndex = detonatedIndex,
    )

    private fun unchanged(phaseBefore: GamePhase) =
        EngineActionResult(phaseBefore, phase, changed = false)

    private companion object {
        const val NO_CELL = -1
        val TERMINAL_PHASES = setOf(GamePhase.Won, GamePhase.Lost)
    }

    private data class EngineCheckpoint(
        val mines: BooleanArray,
        val adjacentMines: IntArray,
        val revealed: BooleanArray,
        val marks: Array<Mark>,
        val phase: GamePhase,
        val flagCount: Int,
        val revealedSafeCount: Int,
        val detonatedIndex: Int,
    )
}
