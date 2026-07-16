package dev.hikari.minesweeper.game

import kotlin.random.Random

fun interface MineLayoutGenerator {
    fun generate(config: BoardConfig): IntArray
}

class RandomMineLayoutGenerator(
    private val random: Random = Random.Default,
) : MineLayoutGenerator {
    override fun generate(config: BoardConfig): IntArray {
        val candidates = IntArray(config.cellCount) { it }
        for (index in 0 until config.mineCount) {
            val swapIndex = random.nextInt(index, candidates.size)
            val value = candidates[index]
            candidates[index] = candidates[swapIndex]
            candidates[swapIndex] = value
        }
        return candidates.copyOf(config.mineCount)
    }
}

internal class BoardGeometry(
    private val config: BoardConfig,
) {
    fun indexOf(position: CellPosition): Int {
        require(position.row in 0 until config.height) { "Row is outside the board." }
        require(position.column in 0 until config.width) { "Column is outside the board." }
        return position.row * config.width + position.column
    }

    fun positionOf(index: Int): CellPosition {
        require(index in 0 until config.cellCount) { "Cell index is outside the board." }
        return CellPosition(row = index / config.width, column = index % config.width)
    }

    fun forEachNeighbor(index: Int, action: (Int) -> Unit) {
        val position = positionOf(index)
        val firstRow = maxOf(0, position.row - 1)
        val lastRow = minOf(config.height - 1, position.row + 1)
        val firstColumn = maxOf(0, position.column - 1)
        val lastColumn = minOf(config.width - 1, position.column + 1)

        for (row in firstRow..lastRow) {
            for (column in firstColumn..lastColumn) {
                if (row != position.row || column != position.column) {
                    action(row * config.width + column)
                }
            }
        }
    }
}
