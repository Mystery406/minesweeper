package dev.hikari.minesweeper.game

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class MineLayoutGeneratorTest {
    @Test
    fun randomGeneratorProducesExactUniqueInBoundsLayout() {
        val config = BoardConfig.EXPERT
        val layout = RandomMineLayoutGenerator(Random(1234)).generate(config)

        assertEquals(config.mineCount, layout.size)
        assertEquals(config.mineCount, layout.toSet().size)
        assertEquals(config.mineCount, layout.count { it in 0 until config.cellCount })
    }
}
