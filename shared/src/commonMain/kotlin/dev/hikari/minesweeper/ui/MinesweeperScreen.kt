package dev.hikari.minesweeper.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import dev.hikari.minesweeper.game.Difficulty
import dev.hikari.minesweeper.game.GamePhase
import dev.hikari.minesweeper.session.GameIntent
import dev.hikari.minesweeper.session.GameUiState
import dev.hikari.minesweeper.session.MinesweeperController
import kotlinx.coroutines.delay
import minesweeper.shared.generated.resources.Res
import minesweeper.shared.generated.resources.face_restart
import minesweeper.shared.generated.resources.menu_best_times
import minesweeper.shared.generated.resources.menu_game
import minesweeper.shared.generated.resources.menu_new_game
import minesweeper.shared.generated.resources.mine_counter_description
import minesweeper.shared.generated.resources.selected_difficulty
import minesweeper.shared.generated.resources.status_lost
import minesweeper.shared.generated.resources.status_ready
import minesweeper.shared.generated.resources.status_running
import minesweeper.shared.generated.resources.status_won
import minesweeper.shared.generated.resources.timer_description
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

private enum class OpenDialog {
    Custom,
    BestTimes,
}

@Composable
fun MinesweeperScreen(
    controller: MinesweeperController,
    modifier: Modifier = Modifier,
) {
    val state = controller.state
    var dialog by remember { mutableStateOf<OpenDialog?>(null) }
    var boardPressed by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    LaunchedEffect(state.phase) {
        if (state.phase == GamePhase.Running) {
            while (true) {
                delay(1.seconds)
                controller.dispatch(GameIntent.Tick)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClassicPalette.Panel)
            .safeContentPadding()
            .padding(6.dp)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.key == Key.F2 && event.type == KeyEventType.KeyUp) {
                    controller.dispatch(GameIntent.Restart)
                    true
                } else {
                    false
                }
            }
            .focusable()
            .classicBevel(BevelStyle.Raised, ClassicMetrics.OuterBevel)
            .padding(6.dp)
            .testTag("minesweeper_screen"),
    ) {
        ClassicMenuBar(
            state = state,
            onRestart = { controller.dispatch(GameIntent.Restart) },
            onSelectDifficulty = { controller.dispatch(GameIntent.SelectDifficulty(it)) },
            onOpenCustom = { dialog = OpenDialog.Custom },
            onOpenBestTimes = { dialog = OpenDialog.BestTimes },
        )
        Spacer(Modifier.height(6.dp))
        StatusPanel(
            state = state,
            boardPressed = boardPressed,
            onRestart = { controller.dispatch(GameIntent.Restart) },
        )
        Spacer(Modifier.height(6.dp))
        GameBoard(
            state = state,
            onIntent = controller::dispatch,
            onPressChanged = { boardPressed = it },
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }

    when (dialog) {
        OpenDialog.Custom -> CustomGameDialog(
            initialConfig = state.customConfig,
            onDismiss = { dialog = null },
            onStart = { config ->
                controller.dispatch(GameIntent.StartCustom(config))
                dialog = null
            },
        )

        OpenDialog.BestTimes -> BestTimesDialog(
            bestTimes = state.bestTimes,
            onReset = { controller.dispatch(GameIntent.ResetBestTimes) },
            onDismiss = { dialog = null },
        )

        null -> Unit
    }
}

@Composable
private fun StatusPanel(
    state: GameUiState,
    boardPressed: Boolean,
    onRestart: () -> Unit,
) {
    val faceStateDescription = when (state.phase) {
        GamePhase.Ready -> stringResource(Res.string.status_ready)
        GamePhase.Running -> stringResource(Res.string.status_running)
        GamePhase.Won -> stringResource(Res.string.status_won)
        GamePhase.Lost -> stringResource(Res.string.status_lost)
    }
    ClassicPanel(
        style = BevelStyle.Sunken,
        modifier = Modifier.fillMaxWidth().height(ClassicMetrics.StatusHeight)
            .testTag("status_panel"),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DigitalCounter(
                value = state.displayedMineCount,
                allowNegative = true,
                description = stringResource(
                    Res.string.mine_counter_description,
                    state.remainingMineCount
                ),
                modifier = Modifier.testTag("mine_counter"),
            )
            FaceButton(
                phase = state.phase,
                boardPressed = boardPressed,
                description = stringResource(Res.string.face_restart),
                stateDescriptionText = faceStateDescription,
                onRestart = onRestart,
                modifier = Modifier.testTag("face_button"),
            )
            DigitalCounter(
                value = state.displayedElapsedSeconds,
                allowNegative = false,
                description = stringResource(Res.string.timer_description, state.elapsedSeconds),
                modifier = Modifier.testTag("timer_counter"),
            )
        }
    }
}

@Composable
private fun ClassicMenuBar(
    state: GameUiState,
    onRestart: () -> Unit,
    onSelectDifficulty: (Difficulty) -> Unit,
    onOpenCustom: () -> Unit,
    onOpenBestTimes: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val menuOffset = with(LocalDensity.current) { 31.dp.roundToPx() }
    val currentDifficulty = difficultyLabel(state.selectedDifficulty)
    val status = when (state.phase) {
        GamePhase.Ready -> stringResource(Res.string.status_ready)
        GamePhase.Running -> stringResource(Res.string.status_running)
        GamePhase.Won -> stringResource(Res.string.status_won)
        GamePhase.Lost -> stringResource(Res.string.status_lost)
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            ClassicButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.height(30.dp).testTag("game_menu_button"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp),
            ) {
                Text(stringResource(Res.string.menu_game), color = Color.Black, fontSize = 14.sp)
            }
            if (expanded) {
                Popup(
                    alignment = Alignment.TopStart,
                    offset = IntOffset(0, menuOffset),
                    onDismissRequest = { expanded = false },
                    properties = PopupProperties(focusable = true),
                ) {
                    Column(
                        modifier = Modifier
                            .width(220.dp)
                            .testTag("game_menu")
                            .classicBevel(BevelStyle.Raised, ClassicMetrics.InnerBevel)
                            .background(ClassicPalette.Panel)
                            .padding(4.dp),
                    ) {
                        MenuItem(
                            stringResource(Res.string.menu_new_game),
                            testTag = "menu_new_game"
                        ) {
                            expanded = false
                            onRestart()
                        }
                        MenuSeparator()
                        listOf(
                            Difficulty.Beginner,
                            Difficulty.Intermediate,
                            Difficulty.Expert,
                        ).forEach { difficulty ->
                            MenuItem(
                                text = difficultyLabel(difficulty),
                                selected = state.selectedDifficulty == difficulty,
                                selectionItem = true,
                                testTag = "menu_difficulty_${difficulty.name}",
                            ) {
                                expanded = false
                                onSelectDifficulty(difficulty)
                            }
                        }
                        MenuItem(
                            text = difficultyLabel(Difficulty.Custom),
                            selected = state.selectedDifficulty == Difficulty.Custom,
                            selectionItem = true,
                            testTag = "menu_custom",
                        ) {
                            expanded = false
                            onOpenCustom()
                        }
                        MenuSeparator()
                        MenuItem(
                            stringResource(Res.string.menu_best_times),
                            testTag = "menu_best_times"
                        ) {
                            expanded = false
                            onOpenBestTimes()
                        }
                    }
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(Res.string.selected_difficulty, currentDifficulty),
            color = Color.Black,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Text(text = status, color = Color.Black, fontSize = 13.sp)
    }
}

@Composable
private fun MenuItem(
    text: String,
    selected: Boolean = false,
    selectionItem: Boolean = false,
    testTag: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .testTag(testTag)
            .semantics {
                if (selectionItem) this.selected = selected
                role = if (selectionItem) Role.RadioButton else Role.Button
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(12.dp)) {
            if (selected) {
                drawCircle(
                    Color.Black,
                    size.minDimension * 0.32f,
                    Offset(size.width / 2f, size.height / 2f)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.Black, fontSize = 14.sp)
    }
}

@Composable
private fun MenuSeparator() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(5.dp)
            .padding(vertical = 2.dp)
            .background(ClassicPalette.Shadow),
    )
}
