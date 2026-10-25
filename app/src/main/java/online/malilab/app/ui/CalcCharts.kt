package online.malilab.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.malilab.app.data.Fmt
import online.malilab.app.ui.theme.Mali

/** One line on a chart. [style] picks a dash pattern and point shape, so lines never rely on colour alone. */
class ChartSeries(val name: String, val values: List<Double>, val color: Color, val style: Int)

/** A reference line: vertical at [x] (a year) or horizontal at [y] (an amount). */
class ChartMark(val x: Double? = null, val y: Double? = null, val label: String)

val ChartColors = listOf(Mali.Teal, Mali.Indigo, Mali.Pink, Mali.Amber, Mali.Purple, Mali.Green)

fun chartSeries(names: List<String>, values: List<List<Double>>): List<ChartSeries> =
    names.indices.map { ChartSeries(names[it], values[it], ChartColors[it % ChartColors.size], it) }

private fun dashFor(style: Int): PathEffect? = when (style % 4) {
    1 -> PathEffect.dashPathEffect(floatArrayOf(28f, 16f))
    2 -> PathEffect.dashPathEffect(floatArrayOf(6f, 14f))
    3 -> PathEffect.dashPathEffect(floatArrayOf(34f, 12f, 6f, 12f))
    else -> null
}

private fun niceStep(range: Double, ticks: Int): Double {
    if (range <= 0) return 1.0
    val raw = range / ticks
    val pow = Math.pow(10.0, Math.floor(Math.log10(raw)))
    val f = raw / pow
    val nice = when {
        f <= 1 -> 1.0
        f <= 2 -> 2.0
        f <= 5 -> 5.0
        else -> 10.0
    }
    return nice * pow
}

private fun DrawScope.drawPoint(style: Int, c: Offset, r: Float, color: Color) {
    when (style % 4) {
        0 -> drawCircle(color, r, c)
        1 -> drawRect(color, Offset(c.x - r, c.y - r), Size(2 * r, 2 * r))
        2 -> {
            val p = Path().apply { moveTo(c.x, c.y - r * 1.3f); lineTo(c.x + r * 1.3f, c.y); lineTo(c.x, c.y + r * 1.3f); lineTo(c.x - r * 1.3f, c.y); close() }
            drawPath(p, color)
        }
        else -> {
            val p = Path().apply { moveTo(c.x, c.y - r * 1.3f); lineTo(c.x + r * 1.2f, c.y + r); lineTo(c.x - r * 1.2f, c.y + r); close() }
            drawPath(p, color)
        }
    }
}

/**
 * A line chart drawn from the numbers. Touch or drag to read the values for a year; the readout is also shown as text.
 * Each line has its own dash pattern and point shape in addition to its colour.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LineChart(
    xs: List<Double>,
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    marks: List<ChartMark> = emptyList(),
    height: Dp = 230.dp,
    xTitle: String = "Year",
    readoutLabel: (Double) -> String = { x -> "Year " + (if (x % 1.0 == 0.0) x.toInt().toString() else String.format(java.util.Locale.US, "%.1f", x)) },
) {
    val measurer = rememberTextMeasurer()
    var selected by remember(xs, series.size) { mutableStateOf<Int?>(null) }
    val axisStyle = TextStyle(fontSize = 11.sp, color = Mali.InkSoft)
    val markStyle = TextStyle(fontSize = 11.sp, color = Mali.Ink, fontWeight = FontWeight.Bold)

    val all = series.flatMap { it.values }
    val describe = if (series.isEmpty() || xs.isEmpty()) "Empty chart"
    else "Line chart of " + series.joinToString(", ") { it.name } + " from year " + xs.first() + " to year " + xs.last() +
        ". Final values: " + series.joinToString("; ") { it.name + " " + Fmt.compact(it.values.lastOrNull() ?: 0.0) }

    Column(modifier) {
        if (xs.size >= 2 && all.isNotEmpty()) {
            val minV = Math.min(0.0, all.min())
            val maxRaw = all.max()
            val step = niceStep(maxRaw - minV, 4)
            val top = Math.ceil(maxRaw / step) * step
            val bottom = Math.floor(minV / step) * step
            val range = if (top - bottom <= 0) 1.0 else top - bottom
            val xMin = xs.first()
            val xMax = xs.last()
            val xRange = if (xMax - xMin <= 0) 1.0 else xMax - xMin

            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .semantics { contentDescription = describe }
                    .pointerInput(xs) {
                        detectTapGestures { pos ->
                            val left = 58.dp.toPx()
                            val w = size.width - left - 12.dp.toPx()
                            val frac = ((pos.x - left) / w).coerceIn(0f, 1f)
                            val target = xMin + frac * xRange
                            selected = xs.indices.minByOrNull { Math.abs(xs[it] - target) }
                        }
                    }
                    .pointerInput(xs) {
                        detectHorizontalDragGestures { change, _ ->
                            val left = 58.dp.toPx()
                            val w = size.width - left - 12.dp.toPx()
                            val frac = ((change.position.x - left) / w).coerceIn(0f, 1f)
                            val target = xMin + frac * xRange
                            selected = xs.indices.minByOrNull { Math.abs(xs[it] - target) }
                        }
                    },
            ) {
                val left = 58.dp.toPx()
                val right = 12.dp.toPx()
                val topPad = 10.dp.toPx()
                val bottomPad = 28.dp.toPx()
                val w = size.width - left - right
                val h = size.height - topPad - bottomPad
                fun px(x: Double) = left + ((x - xMin) / xRange * w).toFloat()
                fun py(y: Double) = topPad + (h - ((y - bottom) / range * h)).toFloat()

                // grid and y labels
                var t = bottom
                while (t <= top + step / 2) {
                    val y = py(t)
                    drawLine(Mali.Sand, Offset(left, y), Offset(left + w, y), strokeWidth = 1.dp.toPx())
                    val lay = measurer.measure(Fmt.compact(t), axisStyle)
                    drawText(lay, topLeft = Offset(left - 6.dp.toPx() - lay.size.width, y - lay.size.height / 2f))
                    t += step
                }
                // x labels
                val xTicks = Math.min(6, xs.size)
                for (i in 0 until xTicks) {
                    val xv = xMin + xRange * i / (xTicks - 1)
                    val label = if (xv % 1.0 == 0.0) xv.toInt().toString() else String.format(java.util.Locale.US, "%.1f", xv)
                    val lay = measurer.measure(label, axisStyle)
                    drawText(lay, topLeft = Offset(px(xv) - lay.size.width / 2f, topPad + h + 6.dp.toPx()))
                }
                // reference lines
                marks.forEach { m ->
                    val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    if (m.x != null && m.x in xMin..xMax) {
                        drawLine(Mali.Pink, Offset(px(m.x), topPad), Offset(px(m.x), topPad + h), strokeWidth = 2.dp.toPx(), pathEffect = dash)
                        val lay = measurer.measure(m.label, markStyle)
                        drawText(lay, topLeft = Offset(Math.min(px(m.x) + 4.dp.toPx(), left + w - lay.size.width), topPad + 2.dp.toPx()))
                    }
                    if (m.y != null && m.y in bottom..top) {
                        drawLine(Mali.Amber, Offset(left, py(m.y)), Offset(left + w, py(m.y)), strokeWidth = 2.dp.toPx(), pathEffect = dash)
                        val lay = measurer.measure(m.label, markStyle)
                        drawText(lay, topLeft = Offset(left + 4.dp.toPx(), py(m.y) - lay.size.height - 2.dp.toPx()))
                    }
                }
                // lines
                series.forEach { s ->
                    val path = Path()
                    s.values.forEachIndexed { i, v ->
                        val p = Offset(px(xs[i]), py(v))
                        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                    }
                    drawPath(path, s.color, style = Stroke(width = 3.dp.toPx(), pathEffect = dashFor(s.style)))
                    if (s.values.size <= 16) {
                        s.values.forEachIndexed { i, v -> drawPoint(s.style, Offset(px(xs[i]), py(v)), 4.dp.toPx(), s.color) }
                    }
                }
                // selection
                selected?.let { i ->
                    if (i in xs.indices) {
                        drawLine(Mali.Ink.copy(alpha = 0.5f), Offset(px(xs[i]), topPad), Offset(px(xs[i]), topPad + h), strokeWidth = 1.5.dp.toPx())
                        series.forEach { s -> if (i in s.values.indices) drawPoint(s.style, Offset(px(xs[i]), py(s.values[i])), 6.dp.toPx(), s.color) }
                    }
                }
            }
            Text(xTitle, fontSize = 11.sp, color = Mali.InkSoft, modifier = Modifier.padding(start = 58.dp))
        }
        // readout as plain text (also for screen readers)
        val i = selected
        if (i != null && i in xs.indices) {
            Spacer(Modifier.height(8.dp))
            Text(readoutLabel(xs[i]), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
            series.forEach { s ->
                if (i in s.values.indices) Text("${s.name}: ${Fmt.kes(String.format(java.util.Locale.US, "%.2f", s.values[i]))}", fontSize = 13.sp, color = Mali.InkSoft)
            }
        } else if (xs.size >= 2) {
            Text("Touch the chart to read the numbers for a year.", fontSize = 12.sp, color = Mali.InkSoft, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            series.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.width(34.dp).height(14.dp)) {
                        drawLine(s.color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 3.dp.toPx(), pathEffect = dashFor(s.style))
                        drawPoint(s.style, Offset(size.width / 2, size.height / 2), 4.dp.toPx(), s.color)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(s.name, fontSize = 12.sp, color = Mali.Ink)
                }
            }
        }
    }
}
