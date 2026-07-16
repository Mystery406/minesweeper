package dev.hikari.minesweeper.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.minesweeperCellInput(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    clickLabel: String,
    markLabel: String,
    onPrimaryClick: () -> Unit,
    onCycleMark: () -> Unit,
    onChord: () -> Unit,
    onPressChanged: (Boolean) -> Unit,
): Modifier = this
    .pointerInput(enabled, onCycleMark, onChord, onPressChanged) {
        if (!enabled) return@pointerInput
        try {
            awaitPointerEventScope {
                var secondaryHandled = false
                var chordHandled = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val buttons = event.buttons

                    if (event.type == PointerEventType.Press && buttons.isPrimaryPressed) {
                        onPressChanged(true)
                    }

                    val isChord = buttons.isTertiaryPressed ||
                        (buttons.isPrimaryPressed && buttons.isSecondaryPressed)
                    when {
                        isChord && !chordHandled -> {
                            chordHandled = true
                            secondaryHandled = true
                            onChord()
                            event.changes.forEach { it.consume() }
                        }

                        buttons.isSecondaryPressed && !secondaryHandled -> {
                            secondaryHandled = true
                            onCycleMark()
                            event.changes.forEach { it.consume() }
                        }
                    }

                    if (!buttons.isPrimaryPressed) onPressChanged(false)
                    if (!buttons.isSecondaryPressed) secondaryHandled = false
                    if (!buttons.isPrimaryPressed && !buttons.isSecondaryPressed && !buttons.isTertiaryPressed) {
                        chordHandled = false
                    }
                }
            }
        } finally {
            onPressChanged(false)
        }
    }
    .combinedClickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        role = Role.Button,
        onClickLabel = clickLabel,
        onLongClickLabel = markLabel,
        onLongClick = onCycleMark,
        onClick = onPrimaryClick,
    )
