package dev.hikari.minesweeper.session

import kotlin.time.TimeSource

fun interface GameClock {
    fun nowMillis(): Long
}

class MonotonicGameClock : GameClock {
    private val origin = TimeSource.Monotonic.markNow()

    override fun nowMillis(): Long = origin.elapsedNow().inWholeMilliseconds
}
