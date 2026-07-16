package dev.hikari.minesweeper.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hikari.minesweeper.game.CellViewState
import dev.hikari.minesweeper.game.GamePhase
import dev.hikari.minesweeper.game.Mark
import kotlin.math.min

fun Modifier.classicBevel(
    style: BevelStyle,
    width: Dp = ClassicMetrics.InnerBevel,
    background: Color = ClassicPalette.Panel,
): Modifier = drawWithContent {
    drawRect(background)
    drawContent()

    val edge = width.toPx().coerceAtLeast(1f)
    val outerTop = if (style == BevelStyle.Raised) ClassicPalette.Highlight else ClassicPalette.DarkEdge
    val innerTop = if (style == BevelStyle.Raised) ClassicPalette.LightEdge else ClassicPalette.Shadow
    val outerBottom = if (style == BevelStyle.Raised) ClassicPalette.DarkEdge else ClassicPalette.Highlight
    val innerBottom = if (style == BevelStyle.Raised) ClassicPalette.Shadow else ClassicPalette.LightEdge

    drawLine(outerTop, Offset(edge / 2, edge / 2), Offset(size.width - edge / 2, edge / 2), edge)
    drawLine(outerTop, Offset(edge / 2, edge / 2), Offset(edge / 2, size.height - edge / 2), edge)
    drawLine(outerBottom, Offset(edge / 2, size.height - edge / 2), Offset(size.width - edge / 2, size.height - edge / 2), edge)
    drawLine(outerBottom, Offset(size.width - edge / 2, edge / 2), Offset(size.width - edge / 2, size.height - edge / 2), edge)

    val inset = edge * 1.5f
    drawLine(innerTop, Offset(inset, inset), Offset(size.width - inset, inset), edge)
    drawLine(innerTop, Offset(inset, inset), Offset(inset, size.height - inset), edge)
    drawLine(innerBottom, Offset(inset, size.height - inset), Offset(size.width - inset, size.height - inset), edge)
    drawLine(innerBottom, Offset(size.width - inset, inset), Offset(size.width - inset, size.height - inset), edge)
}

@Composable
fun ClassicPanel(
    style: BevelStyle,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(ClassicMetrics.OuterBevel + 2.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .classicBevel(style, ClassicMetrics.OuterBevel)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun ClassicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 30.dp)
            .classicBevel(if (pressed) BevelStyle.Sunken else BevelStyle.Raised)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun DigitalCounter(
    value: Int,
    allowNegative: Boolean,
    description: String,
    modifier: Modifier = Modifier,
) {
    val displayed = formatDigitalValue(value, allowNegative)
    Canvas(
        modifier = modifier
            .size(ClassicMetrics.CounterWidth, ClassicMetrics.CounterHeight)
            .classicBevel(BevelStyle.Sunken, 2.dp, ClassicPalette.CounterBackground)
            .padding(4.dp)
            .semantics { contentDescription = description },
    ) {
        val characterWidth = size.width / 3f
        val top = size.height * 0.14f
        val middle = size.height * 0.5f
        val bottom = size.height * 0.86f
        val segmentWidth = min(characterWidth * 0.13f, size.height * 0.09f).coerceAtLeast(1f)

        displayed.forEachIndexed { index, character ->
            val left = index * characterWidth + characterWidth * 0.2f
            val right = (index + 1) * characterWidth - characterWidth * 0.2f
            val active = activeSegments(character)
            for (segment in 0..6) {
                drawSegment(
                    segment = segment,
                    left = left,
                    right = right,
                    top = top,
                    middle = middle,
                    bottom = bottom,
                    width = segmentWidth,
                    color = if (segment in active) ClassicPalette.CounterOn else ClassicPalette.CounterOff,
                )
            }
        }
    }
}

internal fun formatDigitalValue(value: Int, allowNegative: Boolean): String {
    val clamped = if (allowNegative) value.coerceIn(-99, 999) else value.coerceIn(0, 999)
    return if (clamped < 0) {
        "-" + (-clamped).toString().padStart(2, '0')
    } else {
        clamped.toString().padStart(3, '0')
    }
}

@Composable
fun FaceButton(
    phase: GamePhase,
    boardPressed: Boolean,
    description: String,
    stateDescriptionText: String,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val buttonPressed by interactionSource.collectIsPressedAsState()
    val expression = when {
        phase == GamePhase.Won -> FaceExpression.Won
        phase == GamePhase.Lost -> FaceExpression.Lost
        boardPressed -> FaceExpression.Surprised
        else -> FaceExpression.Normal
    }
    Box(
        modifier = modifier
            .size(ClassicMetrics.FaceSize)
            .classicBevel(if (buttonPressed) BevelStyle.Sunken else BevelStyle.Raised)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onRestart,
            )
            .semantics {
                contentDescription = description
                stateDescription = stateDescriptionText
                role = androidx.compose.ui.semantics.Role.Button
            }
            .padding(5.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = min(size.width, size.height) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(ClassicPalette.DarkEdge, radius, center)
            drawCircle(ClassicPalette.Face, radius - 1.5f, center)
            drawFace(expression)
        }
    }
}

@Composable
fun MineCell(
    state: CellViewState,
    size: Dp,
    description: String,
    clickLabel: String,
    markLabel: String,
    testTagModifier: Modifier,
    enabled: Boolean,
    onPrimaryClick: () -> Unit,
    onCycleMark: () -> Unit,
    onChord: () -> Unit,
    onPressChanged: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = testTagModifier
            .size(size)
            .semantics { contentDescription = description }
            .minesweeperCellInput(
                interactionSource = interactionSource,
                enabled = enabled,
                clickLabel = clickLabel,
                markLabel = markLabel,
                onPrimaryClick = onPrimaryClick,
                onCycleMark = onCycleMark,
                onChord = onChord,
                onPressChanged = onPressChanged,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            when (state) {
                is CellViewState.Covered -> {
                    if (pressed && state.mark != Mark.Flag) {
                        drawRevealedCellBackground()
                    } else {
                        drawCoveredCellBackground()
                    }
                    when (state.mark) {
                        Mark.Flag -> drawFlag()
                        Mark.None, Mark.Question -> Unit
                    }
                }

                is CellViewState.Revealed -> drawRevealedCellBackground()
                CellViewState.Mine -> {
                    drawRevealedCellBackground()
                    drawMine()
                }

                CellViewState.ExplodedMine -> {
                    drawRect(ClassicPalette.Exploded)
                    drawCellGrid()
                    drawMine()
                }

                CellViewState.WrongFlag -> {
                    drawRevealedCellBackground()
                    drawMine()
                    drawWrongFlagCross()
                }
            }
        }

        when (state) {
            is CellViewState.Covered -> if (state.mark == Mark.Question) {
                Text(
                    text = "?",
                    color = ClassicPalette.Question,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                )
            }

            is CellViewState.Revealed -> if (state.adjacentMines > 0) {
                Text(
                    text = state.adjacentMines.toString(),
                    color = ClassicPalette.clue(state.adjacentMines),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                )
            }

            else -> Unit
        }
    }
}

private fun DrawScope.drawSegment(
    segment: Int,
    left: Float,
    right: Float,
    top: Float,
    middle: Float,
    bottom: Float,
    width: Float,
    color: Color,
) {
    val start: Offset
    val end: Offset
    when (segment) {
        0 -> {
            start = Offset(left, top)
            end = Offset(right, top)
        }

        1 -> {
            start = Offset(right, top)
            end = Offset(right, middle)
        }

        2 -> {
            start = Offset(right, middle)
            end = Offset(right, bottom)
        }

        3 -> {
            start = Offset(left, bottom)
            end = Offset(right, bottom)
        }

        4 -> {
            start = Offset(left, middle)
            end = Offset(left, bottom)
        }

        5 -> {
            start = Offset(left, top)
            end = Offset(left, middle)
        }

        else -> {
            start = Offset(left, middle)
            end = Offset(right, middle)
        }
    }
    drawLine(color, start, end, strokeWidth = width, cap = StrokeCap.Square)
}

private fun activeSegments(character: Char): Set<Int> = when (character) {
    '0' -> setOf(0, 1, 2, 3, 4, 5)
    '1' -> setOf(1, 2)
    '2' -> setOf(0, 1, 6, 4, 3)
    '3' -> setOf(0, 1, 6, 2, 3)
    '4' -> setOf(5, 6, 1, 2)
    '5' -> setOf(0, 5, 6, 2, 3)
    '6' -> setOf(0, 5, 6, 4, 2, 3)
    '7' -> setOf(0, 1, 2)
    '8' -> setOf(0, 1, 2, 3, 4, 5, 6)
    '9' -> setOf(0, 1, 2, 3, 5, 6)
    '-' -> setOf(6)
    else -> emptySet()
}

private enum class FaceExpression {
    Normal,
    Surprised,
    Won,
    Lost,
}

private fun DrawScope.drawFace(expression: FaceExpression) {
    val stroke = (size.minDimension * 0.08f).coerceAtLeast(1f)
    val leftEye = Offset(size.width * 0.34f, size.height * 0.37f)
    val rightEye = Offset(size.width * 0.66f, size.height * 0.37f)
    when (expression) {
        FaceExpression.Won -> {
            drawRect(Color.Black, Offset(size.width * 0.2f, size.height * 0.29f), Size(size.width * 0.28f, size.height * 0.18f))
            drawRect(Color.Black, Offset(size.width * 0.52f, size.height * 0.29f), Size(size.width * 0.28f, size.height * 0.18f))
            drawLine(Color.Black, Offset(size.width * 0.46f, size.height * 0.34f), Offset(size.width * 0.54f, size.height * 0.34f), stroke)
        }

        FaceExpression.Lost -> {
            listOf(leftEye, rightEye).forEach { eye ->
                drawLine(Color.Black, eye - Offset(stroke, stroke), eye + Offset(stroke, stroke), stroke)
                drawLine(Color.Black, eye + Offset(-stroke, stroke), eye + Offset(stroke, -stroke), stroke)
            }
        }

        FaceExpression.Normal, FaceExpression.Surprised -> {
            drawCircle(Color.Black, stroke, leftEye)
            drawCircle(Color.Black, stroke, rightEye)
        }
    }

    when (expression) {
        FaceExpression.Surprised -> drawCircle(
            Color.Black,
            radius = size.minDimension * 0.11f,
            center = Offset(size.width * 0.5f, size.height * 0.68f),
            style = Stroke(stroke),
        )

        FaceExpression.Lost -> {
            val path = Path().apply {
                moveTo(size.width * 0.32f, size.height * 0.76f)
                quadraticTo(size.width * 0.5f, size.height * 0.58f, size.width * 0.68f, size.height * 0.76f)
            }
            drawPath(path, Color.Black, style = Stroke(stroke))
        }

        FaceExpression.Normal, FaceExpression.Won -> {
            val path = Path().apply {
                moveTo(size.width * 0.3f, size.height * 0.62f)
                quadraticTo(size.width * 0.5f, size.height * 0.82f, size.width * 0.7f, size.height * 0.62f)
            }
            drawPath(path, Color.Black, style = Stroke(stroke))
        }
    }
}

private fun DrawScope.drawCoveredCellBackground() {
    drawRect(ClassicPalette.Panel)
    val edge = (size.minDimension * 0.11f).coerceAtLeast(2f)
    drawLine(ClassicPalette.Highlight, Offset(edge / 2, edge / 2), Offset(size.width - edge / 2, edge / 2), edge)
    drawLine(ClassicPalette.Highlight, Offset(edge / 2, edge / 2), Offset(edge / 2, size.height - edge / 2), edge)
    drawLine(ClassicPalette.Shadow, Offset(edge / 2, size.height - edge / 2), Offset(size.width - edge / 2, size.height - edge / 2), edge)
    drawLine(ClassicPalette.Shadow, Offset(size.width - edge / 2, edge / 2), Offset(size.width - edge / 2, size.height - edge / 2), edge)
}

private fun DrawScope.drawRevealedCellBackground() {
    drawRect(ClassicPalette.Panel)
    drawCellGrid()
}

private fun DrawScope.drawCellGrid() {
    drawRect(ClassicPalette.Shadow, style = Stroke(width = 1f))
}

private fun DrawScope.drawFlag() {
    val poleX = size.width * 0.54f
    drawLine(Color.Black, Offset(poleX, size.height * 0.25f), Offset(poleX, size.height * 0.72f), size.minDimension * 0.08f)
    val flag = Path().apply {
        moveTo(poleX, size.height * 0.27f)
        lineTo(size.width * 0.25f, size.height * 0.42f)
        lineTo(poleX, size.height * 0.54f)
        close()
    }
    drawPath(flag, ClassicPalette.Flag)
    drawRect(Color.Black, Offset(size.width * 0.28f, size.height * 0.72f), Size(size.width * 0.46f, size.height * 0.09f))
}

private fun DrawScope.drawMine() {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension * 0.22f
    val stroke = (size.minDimension * 0.07f).coerceAtLeast(1f)
    drawLine(Color.Black, Offset(center.x - radius * 1.65f, center.y), Offset(center.x + radius * 1.65f, center.y), stroke)
    drawLine(Color.Black, Offset(center.x, center.y - radius * 1.65f), Offset(center.x, center.y + radius * 1.65f), stroke)
    drawLine(Color.Black, Offset(center.x - radius * 1.15f, center.y - radius * 1.15f), Offset(center.x + radius * 1.15f, center.y + radius * 1.15f), stroke)
    drawLine(Color.Black, Offset(center.x + radius * 1.15f, center.y - radius * 1.15f), Offset(center.x - radius * 1.15f, center.y + radius * 1.15f), stroke)
    drawCircle(Color.Black, radius, center)
    drawCircle(Color.White, radius * 0.25f, Offset(center.x - radius * 0.35f, center.y - radius * 0.35f))
}

private fun DrawScope.drawWrongFlagCross() {
    val width = (size.minDimension * 0.1f).coerceAtLeast(2f)
    drawLine(Color.Red, Offset(size.width * 0.18f, size.height * 0.18f), Offset(size.width * 0.82f, size.height * 0.82f), width)
    drawLine(Color.Red, Offset(size.width * 0.82f, size.height * 0.18f), Offset(size.width * 0.18f, size.height * 0.82f), width)
}
