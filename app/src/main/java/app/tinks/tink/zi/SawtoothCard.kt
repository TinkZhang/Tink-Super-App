package app.tinks.tink.zi

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Composable
fun SawtoothCard(
    modifier: Modifier = Modifier,
    toothWidthDp: Float = 20f,
    toothHeightDp: Float = 10f,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    val shape = SawtoothShape(
        toothWidth = toothWidthDp,
        toothHeight = toothHeightDp
    )

    Surface(
        modifier = modifier,
        shape = shape,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.padding(contentPadding)
        ) {
            content()
        }
    }
}

class SawtoothShape(
    private val toothWidth: Float,
    private val toothHeight: Float
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height - toothHeight)

            var x = size.width
            var up = true

            while (x > 0f) {
                x -= toothWidth
                lineTo(
                    x.coerceAtLeast(0f),
                    if (up) size.height else size.height - toothHeight
                )
                up = !up
            }

            lineTo(0f, 0f)
            close()
        }

        return Outline.Generic(path)
    }
}