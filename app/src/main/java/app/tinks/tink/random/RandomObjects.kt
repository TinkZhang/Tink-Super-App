package app.tinks.tink.random

import android.graphics.Paint
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun RandomObjects(settings: RandomSettings, result: RandomResult, progress: Float = 0f, rolling: Boolean = false, rotation: Float = 0f, modifier: Modifier = Modifier) {
    when (settings.type) {
        RandomType.Dice -> DiceObjects(result.dice, progress, rolling, modifier)
        RandomType.Wheel -> WheelObject(settings.displaySections(), rotation, modifier)
        RandomType.Coin -> CoinObject(result.heads, progress, rolling, modifier)
    }
}

@Composable
private fun DiceObjects(values: List<Int>, progress: Float, rolling: Boolean, modifier: Modifier) {
    val face = Brush.linearGradient(listOf(Color(0xFFFFFEF8), Color(0xFFE7E2D5)))
    val pip = Color(0xFF263630)
    val shadow = Color.Black.copy(alpha = .24f)
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val columns = if (values.size > 4) 3 else 2
        val rows = (values.size + columns - 1) / columns
        val dieSize = minOf(104.dp, (maxWidth - 22.dp * (columns - 1)) / columns,
            (maxHeight - 22.dp * (rows - 1)) / rows)
        Column(verticalArrangement = Arrangement.spacedBy(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            values.chunked(columns).forEachIndexed { row, dice ->
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    dice.forEachIndexed { column, value ->
                        val index = row * columns + column
                        val shown = if (rolling) ((progress * 24).toInt() + index * 2) % 6 + 1 else value
                        Canvas(Modifier.size(dieSize).graphicsLayer {
                            rotationZ = if (rolling) sin(progress * PI * 10 + index).toFloat() * 28 else if (index % 2 == 0) -9f else 12f
                            translationY = if (rolling) -sin(progress * PI * 8).toFloat() * 18.dp.toPx() else 0f
                        }.testTag("random_die_$index").semantics { contentDescription = "${chineseNumber(value)}点" }) {
                            val edge = size.minDimension * .9f
                            val offset = (size.width - edge) / 2
                            drawRoundRect(shadow, Offset(offset, offset + 9.dp.toPx()), Size(edge, edge), CornerRadius(edge * .22f))
                            drawRoundRect(Color(0xFFC0B9A8), Offset(offset, offset + 5.dp.toPx()), Size(edge, edge), CornerRadius(edge * .2f))
                            drawRoundRect(face, Offset(offset, offset), Size(edge, edge), CornerRadius(edge * .2f))
                            drawRoundRect(Color.White.copy(alpha = .75f), Offset(offset + 1.dp.toPx(), offset + 1.dp.toPx()), Size(edge - 2.dp.toPx(), edge - 2.dp.toPx()), CornerRadius(edge * .19f), style = Stroke(1.dp.toPx()))
                            val dots = when (shown) {
                                1 -> listOf(4); 2 -> listOf(0, 8); 3 -> listOf(0, 4, 8)
                                4 -> listOf(0, 2, 6, 8); 5 -> listOf(0, 2, 4, 6, 8); else -> listOf(0, 2, 3, 5, 6, 8)
                            }
                            dots.forEach { d ->
                                val point = Offset(offset + edge * (.26f + d % 3 * .24f), offset + edge * (.26f + d / 3 * .24f))
                                drawCircle(Color.White, edge * .082f, point + Offset(0f, 1.dp.toPx()))
                                drawCircle(if (shown == 1 || shown == 4) Color(0xFFB3483F) else pip, edge * .075f, point)
                                drawCircle(Color.Black.copy(alpha = .16f), edge * .05f, point - Offset(0f, edge * .012f))
                            }
                        }
                    }
                }
            }
        }
    }
}

internal val wheelColors = listOf(
    0xFFDFB67D, 0xFF91B7AF, 0xFFD7A29C, 0xFFA7ADD3,
    0xFFC5CD94, 0xFF86BBD0, 0xFFE4B897, 0xFFB89AC5,
    0xFF8EC6AC, 0xFFE0CC83, 0xFF9AAAC0, 0xFFC593A9,
).map { Color(it) }

@Composable
private fun WheelObject(sections: List<WheelSection>, rotation: Float, modifier: Modifier) {
    val ink = Color(0xFF283532)
    val centerColor = Color(0xFFFFF7E8)
    val fontSize = with(LocalDensity.current) { 13.sp.toPx() }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth(.94f).aspectRatio(1f).testTag("random_wheel")) {
            val radius = size.minDimension / 2 - 12.dp.toPx()
            val total = sections.sumOf { it.weight }
            var start = -90f + rotation
            drawCircle(Color(0xFF57472E).copy(alpha = .18f), radius + 7.dp.toPx(), center + Offset(0f, 5.dp.toPx()))
            drawCircle(Color(0xFFFAF4E8), radius + 6.dp.toPx())
            drawCircle(Color(0xFFC7AD7B), radius + 6.dp.toPx(), style = Stroke(1.dp.toPx()))
            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = ink.toArgb(); textSize = fontSize; textAlign = Paint.Align.CENTER }
            sections.forEachIndexed { index, section ->
                val sweep = section.weight.toFloat() / total * 360f
                drawArc(wheelColors[index % wheelColors.size], start, sweep, true, center - Offset(radius, radius), Size(radius * 2, radius * 2))
                val boundary = start * PI / 180
                drawLine(centerColor, center, center + Offset(cos(boundary).toFloat(), sin(boundary).toFloat()) * radius, 1.5.dp.toPx())
                // Keep labels upright while the wheel turns. Tiny sectors remain selectable by their pointer position.
                if (sweep >= 10f) {
                    val angle = (start + sweep / 2) * PI / 180
                    val maxText = minOf(radius * .8f, radius * .65f * sweep * PI.toFloat() / 180)
                    val label = TextUtils.ellipsize(section.content, textPaint, maxText, TextUtils.TruncateAt.END).toString()
                    drawContext.canvas.nativeCanvas.drawText(label, center.x + cos(angle).toFloat() * radius * .65f, center.y + sin(angle).toFloat() * radius * .65f - (textPaint.ascent() + textPaint.descent()) / 2, textPaint)
                }
                start += sweep
            }
            drawCircle(centerColor, 17.dp.toPx())
            drawCircle(Color(0xFFC7AD7B), 17.dp.toPx(), style = Stroke(2.dp.toPx()))
            drawCircle(ink, 5.dp.toPx())
            val pointer = Path().apply {
                moveTo(center.x - 10.dp.toPx(), center.y - radius - 8.dp.toPx())
                lineTo(center.x + 10.dp.toPx(), center.y - radius - 8.dp.toPx())
                lineTo(center.x, center.y - radius + 14.dp.toPx()); close()
            }
            drawPath(pointer, ink)
            drawCircle(Color(0xFFC7AD7B), 3.dp.toPx(), Offset(center.x, center.y - radius - 3.dp.toPx()))
        }
    }
}

@Composable
private fun CoinObject(heads: Boolean, progress: Float, rolling: Boolean, modifier: Modifier) {
    val fill = MaterialTheme.colorScheme.tertiaryContainer
    val ink = MaterialTheme.colorScheme.onTertiaryContainer
    val sizeText = with(LocalDensity.current) { 54.sp.toPx() }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(172.dp).graphicsLayer {
            rotationX = if (rolling) progress * 1440f else 0f
            translationY = if (rolling) -sin(progress * PI).toFloat() * 50.dp.toPx() else 0f
            cameraDistance = 14 * density
        }.testTag("random_coin").semantics { contentDescription = if (heads) "正面" else "反面" }) {
            val radius = size.minDimension / 2 - 5.dp.toPx()
            drawCircle(fill, radius)
            drawCircle(ink.copy(alpha = .45f), radius, style = Stroke(6.dp.toPx()))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink.toArgb(); textSize = sizeText; textAlign = Paint.Align.CENTER }
            val face = if (rolling && (progress * 8).toInt() % 2 != 0) !heads else heads
            drawContext.canvas.nativeCanvas.drawText(if (face) "正" else "反", center.x, center.y - (paint.ascent() + paint.descent()) / 2, paint)
        }
    }
}
