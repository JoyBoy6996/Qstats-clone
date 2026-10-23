package dev.pulsekit.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Simple line chart on a fixed y-range. Compose Canvas, redraws when values change. */
@Composable
fun LineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    yMin: Float = 0f,
    yMax: Float? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    height: Int = 120
) {
    val top = yMax ?: (values.maxOrNull()?.coerceAtLeast(1f) ?: 1f)
    val grid = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    Box(modifier.fillMaxWidth().height(height.dp)) {
        Canvas(Modifier.fillMaxWidth().height(height.dp)) {
            // three horizontal guide lines
            for (i in 0..2) {
                val y = size.height * i / 2f
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
            if (values.size < 2) return@Canvas
            val range = (top - yMin).coerceAtLeast(0.001f)
            val dx = size.width / (values.size - 1)
            val path = Path()
            values.forEachIndexed { i, v ->
                val x = i * dx
                val y = size.height * (1f - ((v - yMin) / range).coerceIn(0f, 1f))
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color, style = Stroke(width = 3f, cap = StrokeCap.Round))
        }
    }
}
