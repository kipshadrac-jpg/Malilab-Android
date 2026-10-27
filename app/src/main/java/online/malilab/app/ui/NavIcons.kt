package online.malilab.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Outline icons for the bottom tabs. They are drawn with strokes, so the tab colour is applied by the Icon's tint. */
object NavIcons {
    private fun line(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(block).build()

    private fun ImageVector.Builder.stroke(width: Float = 1.7f, body: PathBuilder.() -> Unit) {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = body,
        )
    }

    /** Portfolio: a wallet with a card peeking out and a clasp. */
    val Wallet: ImageVector by lazy {
        line("Wallet") {
            stroke {
                // body
                moveTo(6f, 8.5f); horizontalLineTo(17f)
                arcTo(2f, 2f, 0f, false, true, 19f, 10.5f); verticalLineTo(17f)
                arcTo(2f, 2f, 0f, false, true, 17f, 19f); horizontalLineTo(6f)
                arcTo(2f, 2f, 0f, false, true, 4f, 17f); verticalLineTo(10.5f)
                arcTo(2f, 2f, 0f, false, true, 6f, 8.5f); close()
            }
            stroke {
                // the card behind it
                moveTo(4.4f, 9.6f); lineTo(15.6f, 5.6f)
                curveTo(16.6f, 5.3f, 17.5f, 5.8f, 17.6f, 6.8f); lineTo(17.8f, 8.5f)
            }
            stroke {
                // the clasp
                moveTo(15.6f, 12.4f); horizontalLineTo(20f)
                arcTo(1f, 1f, 0f, false, true, 21f, 13.4f); verticalLineTo(14.4f)
                arcTo(1f, 1f, 0f, false, true, 20f, 15.4f); horizontalLineTo(15.6f)
                arcTo(1.5f, 1.5f, 0f, false, true, 14.1f, 13.9f)
                arcTo(1.5f, 1.5f, 0f, false, true, 15.6f, 12.4f); close()
            }
        }
    }

    /** Discover: a compass. */
    val Compass: ImageVector by lazy {
        line("Compass") {
            stroke {
                // outer ring
                moveTo(12f, 5.2f)
                arcTo(8f, 8f, 0f, true, true, 12f, 21.2f)
                arcTo(8f, 8f, 0f, true, true, 12f, 5.2f); close()
            }
            stroke {
                // inner ring
                moveTo(12f, 7.4f)
                arcTo(5.8f, 5.8f, 0f, true, true, 12f, 19f)
                arcTo(5.8f, 5.8f, 0f, true, true, 12f, 7.4f); close()
            }
            stroke {
                // the top pointer
                moveTo(9.4f, 5.9f); lineTo(12f, 2f); lineTo(14.6f, 5.9f)
            }
            stroke {
                // needle
                moveTo(12f, 9.6f); lineTo(14.6f, 15.6f); lineTo(12f, 14.2f); lineTo(9.4f, 15.6f); close()
            }
            stroke(1.5f) {
                // ticks
                moveTo(12f, 7.8f); verticalLineTo(8.8f)
                moveTo(12f, 17.6f); verticalLineTo(18.6f)
                moveTo(6.4f, 13.2f); horizontalLineTo(7.4f)
                moveTo(16.6f, 13.2f); horizontalLineTo(17.6f)
            }
        }
    }

    /** Learn: an open book. */
    val Book: ImageVector by lazy {
        line("Book") {
            stroke {
                // the open pages
                moveTo(12f, 7.2f)
                curveTo(10.2f, 5.9f, 7.2f, 5.6f, 4.6f, 6.4f)
                verticalLineTo(17f)
                curveTo(7.2f, 16.3f, 10.2f, 16.6f, 12f, 18f)
                curveTo(13.8f, 16.6f, 16.8f, 16.3f, 19.4f, 17f)
                verticalLineTo(6.4f)
                curveTo(16.8f, 5.6f, 13.8f, 5.9f, 12f, 7.2f)
                verticalLineTo(18f)
            }
            stroke {
                // the cover edge underneath
                moveTo(3f, 8f); verticalLineTo(18.6f)
                curveTo(6.2f, 17.9f, 9.6f, 18.3f, 12f, 19.8f)
                curveTo(14.4f, 18.3f, 17.8f, 17.9f, 21f, 18.6f)
                verticalLineTo(8f)
            }
            stroke(1.4f) {
                // lines of text
                moveTo(6.6f, 9.2f); curveTo(7.8f, 8.9f, 9.2f, 9f, 10.2f, 9.5f)
                moveTo(6.6f, 11.6f); curveTo(7.8f, 11.3f, 9.2f, 11.4f, 10.2f, 11.9f)
                moveTo(6.6f, 14f); curveTo(7.8f, 13.7f, 9.2f, 13.8f, 10.2f, 14.3f)
                moveTo(13.8f, 9.5f); curveTo(14.8f, 9f, 16.2f, 8.9f, 17.4f, 9.2f)
                moveTo(13.8f, 11.9f); curveTo(14.8f, 11.4f, 16.2f, 11.3f, 17.4f, 11.6f)
                moveTo(13.8f, 14.3f); curveTo(14.8f, 13.8f, 16.2f, 13.7f, 17.4f, 14f)
            }
        }
    }
}
