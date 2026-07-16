package dev.hikari.minesweeper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.hikari.minesweeper.game.BoardConfig
import dev.hikari.minesweeper.game.CustomConfigError
import dev.hikari.minesweeper.game.CustomConfigValidation
import dev.hikari.minesweeper.game.Difficulty
import minesweeper.shared.generated.resources.Res
import minesweeper.shared.generated.resources.action_cancel
import minesweeper.shared.generated.resources.action_close
import minesweeper.shared.generated.resources.action_start
import minesweeper.shared.generated.resources.best_no_time
import minesweeper.shared.generated.resources.best_seconds
import minesweeper.shared.generated.resources.best_times_title
import minesweeper.shared.generated.resources.custom_error_height
import minesweeper.shared.generated.resources.custom_error_mines
import minesweeper.shared.generated.resources.custom_error_width
import minesweeper.shared.generated.resources.custom_height
import minesweeper.shared.generated.resources.custom_height_hint
import minesweeper.shared.generated.resources.custom_mines
import minesweeper.shared.generated.resources.custom_mines_hint
import minesweeper.shared.generated.resources.custom_title
import minesweeper.shared.generated.resources.custom_width
import minesweeper.shared.generated.resources.custom_width_hint
import minesweeper.shared.generated.resources.difficulty_beginner
import minesweeper.shared.generated.resources.difficulty_custom
import minesweeper.shared.generated.resources.difficulty_expert
import minesweeper.shared.generated.resources.difficulty_intermediate
import minesweeper.shared.generated.resources.menu_reset_best_times
import org.jetbrains.compose.resources.stringResource

@Composable
fun CustomGameDialog(
    initialConfig: BoardConfig,
    onDismiss: () -> Unit,
    onStart: (BoardConfig) -> Unit,
) {
    var width by remember(initialConfig) { mutableStateOf(initialConfig.width.toString()) }
    var height by remember(initialConfig) { mutableStateOf(initialConfig.height.toString()) }
    var mines by remember(initialConfig) { mutableStateOf(initialConfig.mineCount.toString()) }
    var errors by remember { mutableStateOf(emptySet<CustomConfigError>()) }

    val parsedWidth = width.toIntOrNull() ?: 0
    val parsedHeight = height.toIntOrNull() ?: 0
    val maxMines = if (
        parsedWidth in BoardConfig.MIN_CUSTOM_WIDTH..BoardConfig.MAX_CUSTOM_WIDTH &&
        parsedHeight in BoardConfig.MIN_CUSTOM_HEIGHT..BoardConfig.MAX_CUSTOM_HEIGHT
    ) {
        BoardConfig.maxCustomMines(parsedWidth, parsedHeight)
    } else {
        BoardConfig.MAX_CUSTOM_MINES
    }

    ClassicDialog(onDismiss = onDismiss, testTag = "custom_dialog") {
        Text(
            text = stringResource(Res.string.custom_title),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.Black,
        )
        Spacer(Modifier.height(14.dp))
        NumericField(
            label = stringResource(Res.string.custom_width),
            hint = stringResource(Res.string.custom_width_hint),
            value = width,
            testTag = "custom_width",
            onValueChange = { width = it },
        )
        NumericField(
            label = stringResource(Res.string.custom_height),
            hint = stringResource(Res.string.custom_height_hint),
            value = height,
            testTag = "custom_height",
            onValueChange = { height = it },
        )
        NumericField(
            label = stringResource(Res.string.custom_mines),
            hint = stringResource(Res.string.custom_mines_hint, maxMines),
            value = mines,
            testTag = "custom_mines",
            onValueChange = { mines = it },
        )

        if (errors.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            if (CustomConfigError.Width in errors) ErrorText(stringResource(Res.string.custom_error_width))
            if (CustomConfigError.Height in errors) ErrorText(stringResource(Res.string.custom_error_height))
            if (CustomConfigError.Mines in errors) {
                ErrorText(stringResource(Res.string.custom_error_mines, maxMines))
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            ClassicButton(onClick = onDismiss, modifier = Modifier.testTag("custom_cancel")) {
                Text(stringResource(Res.string.action_cancel), color = Color.Black)
            }
            Spacer(Modifier.width(8.dp))
            ClassicButton(
                onClick = {
                    when (val validation = BoardConfig.validateCustom(
                        parsedWidth,
                        parsedHeight,
                        mines.toIntOrNull() ?: 0,
                    )) {
                        is CustomConfigValidation.Valid -> onStart(validation.config)
                        is CustomConfigValidation.Invalid -> errors = validation.errors
                    }
                },
                modifier = Modifier.testTag("custom_start"),
            ) {
                Text(stringResource(Res.string.action_start), color = Color.Black)
            }
        }
    }
}

@Composable
fun BestTimesDialog(
    bestTimes: Map<Difficulty, Int>,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    ClassicDialog(onDismiss = onDismiss, testTag = "best_times_dialog") {
        Text(
            text = stringResource(Res.string.best_times_title),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.Black,
        )
        Spacer(Modifier.height(14.dp))
        listOf(Difficulty.Beginner, Difficulty.Intermediate, Difficulty.Expert).forEach { difficulty ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(difficultyLabel(difficulty), color = Color.Black)
                Text(
                    text = bestTimes[difficulty]?.let { stringResource(Res.string.best_seconds, it) }
                        ?: stringResource(Res.string.best_no_time),
                    color = Color.Black,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            ClassicButton(
                onClick = onReset,
                modifier = Modifier.testTag("reset_best_times"),
            ) {
                Text(stringResource(Res.string.menu_reset_best_times), color = Color.Black)
            }
            Spacer(Modifier.width(8.dp))
            ClassicButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_close), color = Color.Black)
            }
        }
    }
}

@Composable
internal fun difficultyLabel(difficulty: Difficulty): String = when (difficulty) {
    Difficulty.Beginner -> stringResource(Res.string.difficulty_beginner)
    Difficulty.Intermediate -> stringResource(Res.string.difficulty_intermediate)
    Difficulty.Expert -> stringResource(Res.string.difficulty_expert)
    Difficulty.Custom -> stringResource(Res.string.difficulty_custom)
}

@Composable
private fun ClassicDialog(
    onDismiss: () -> Unit,
    testTag: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(min = 280.dp, max = 420.dp)
                .fillMaxWidth(0.92f)
                .testTag(testTag)
                .classicBevel(BevelStyle.Raised, ClassicMetrics.OuterBevel)
                .background(ClassicPalette.Panel)
                .padding(18.dp),
            content = content,
        )
    }
}

@Composable
private fun NumericField(
    label: String,
    hint: String,
    value: String,
    testTag: String,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.Black, modifier = Modifier.width(82.dp))
        Box(
            modifier = Modifier
                .width(88.dp)
                .height(32.dp)
                .classicBevel(BevelStyle.Sunken, 2.dp, Color.White)
                .padding(horizontal = 7.dp, vertical = 5.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = value,
                onValueChange = { candidate ->
                    onValueChange(candidate.filter(Char::isDigit).take(3))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag)
                    .semantics { contentDescription = label },
                textStyle = TextStyle(
                    color = Color.Black,
                    fontSize = 17.sp,
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(hint, color = ClassicPalette.Shadow, fontSize = 12.sp)
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(
        text = message,
        color = Color(0xFF800000),
        fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
    )
}
