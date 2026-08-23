package dev.hikari.minesweeper.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role

@Composable
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
): Modifier {
    val currentOnCycleMark = rememberUpdatedState(onCycleMark)
    val currentOnChord = rememberUpdatedState(onChord)
    val currentOnPressChanged = rememberUpdatedState(onPressChanged)

    return this.pointerInput(enabled) {
        if (!enabled) return@pointerInput
        try {
            awaitPointerEventScope {
                var secondaryHandled = false
                var chordHandled = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val buttons = event.buttons
                    val isPress = event.type == PointerEventType.Press

                    if (isPress && buttons.isPrimaryPressed) {
                        currentOnPressChanged.value(true)
                    }

                    val isChord = buttons.isTertiaryPressed ||
                        (buttons.isPrimaryPressed && buttons.isSecondaryPressed)
                    when {
                        isPress && isChord && !chordHandled -> {
                            chordHandled = true
                            secondaryHandled = true
                            currentOnChord.value()
                            event.changes.forEach { it.consume() }
                        }

                        isPress && buttons.isSecondaryPressed && !secondaryHandled -> {
                            secondaryHandled = true
                            currentOnCycleMark.value()
                            event.changes.forEach { it.consume() }
                        }
                    }

                    if (!buttons.isPrimaryPressed) currentOnPressChanged.value(false)
                    if (!buttons.isSecondaryPressed) secondaryHandled = false
                    if (!buttons.isPrimaryPressed && !buttons.isSecondaryPressed && !buttons.isTertiaryPressed) {
                        chordHandled = false
                    }
                }
            }
        } finally {
            currentOnPressChanged.value(false)
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
}
