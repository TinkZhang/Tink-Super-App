package app.tinks.tink.random

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** Decorative surfaces have no semantics or gesture handlers. */
@Composable
internal fun RandomBackdrop(type: RandomType, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val shortest = size.minDimension
        when (type) {
            RandomType.Dice -> {
                drawRect(Brush.radialGradient(listOf(Color(0xFF28675C), Color(0xFF102E2C)),
                    center = center - Offset(shortest * .12f, shortest * .15f), radius = size.maxDimension * .8f))
                val step = 13.dp.toPx()
                for (x in 0..(size.width / step).toInt()) {
                    for (y in 0..(size.height / step).toInt()) {
                        val point = Offset(x * step + (y % 2) * step / 2, y * step)
                        drawLine(Color.White.copy(alpha = .035f), point, point + Offset(2.dp.toPx(), 1.dp.toPx()), .7.dp.toPx())
                    }
                }
                val inset = 14.dp.toPx()
                drawRoundRect(Color(0xFFD6C799).copy(alpha = .22f), Offset(inset, inset),
                    Size(size.width - inset * 2, size.height - inset * 2), CornerRadius(28.dp.toPx()), style = Stroke(1.dp.toPx()))
                drawCircle(Color.White.copy(alpha = .025f), shortest * .42f, style = Stroke(1.dp.toPx()))
            }
            RandomType.Wheel -> {
                drawRect(Brush.radialGradient(listOf(Color(0xFFFFFCF5), Color(0xFFE7DDCA)), radius = size.maxDimension * .85f))
                listOf(.42f, .49f, .68f).forEach { scale ->
                    drawCircle(Color(0xFFB9A886).copy(alpha = .22f), shortest * scale, style = Stroke(.8.dp.toPx()))
                }
                repeat(48) { index ->
                    val angle = index * Math.PI * 2 / 48
                    val radius = shortest * .45f
                    val point = center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * radius
                    drawCircle(Color(0xFFB6A07A).copy(alpha = .4f), if (index % 4 == 0) 1.5.dp.toPx() else .7.dp.toPx(), point)
                }
            }
            RandomType.Coin -> {
                drawRect(Brush.radialGradient(listOf(Color(0xFF354653), Color(0xFF151F29)), radius = size.maxDimension * .75f))
                drawCircle(Color(0xFFB6C6D1).copy(alpha = .1f), shortest * .4f, style = Stroke(.8.dp.toPx()))
                drawCircle(Color(0xFFB6C6D1).copy(alpha = .045f), shortest * .47f, style = Stroke(.8.dp.toPx()))
            }
        }
    }
}
