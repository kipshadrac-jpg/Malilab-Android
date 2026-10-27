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

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy)
        arcTo(r, r, 0f, true, true, cx + r, cy)
        arcTo(r, r, 0f, true, true, cx - r, cy)
        close()
    }

    /** Playbooks: a clipboard with a numbered checklist. */
    val Checklist: ImageVector by lazy {
        line("Checklist") {
            stroke {
                moveTo(9f, 4.6f); horizontalLineTo(6.6f)
                arcTo(1.8f, 1.8f, 0f, false, false, 4.8f, 6.4f); verticalLineTo(19.6f)
                arcTo(1.8f, 1.8f, 0f, false, false, 6.6f, 21.4f); horizontalLineTo(17.4f)
                arcTo(1.8f, 1.8f, 0f, false, false, 19.2f, 19.6f); verticalLineTo(6.4f)
                arcTo(1.8f, 1.8f, 0f, false, false, 17.4f, 4.6f); horizontalLineTo(15f)
            }
            stroke {
                moveTo(8.8f, 6.4f); verticalLineTo(3.6f); horizontalLineTo(15.2f); verticalLineTo(6.4f); close()
            }
            for (y in listOf(10.6f, 14.4f, 18.2f)) {
                stroke(1.5f) { circle(8.4f, y, 1.15f) }
                stroke(1.5f) { moveTo(11.6f, y); horizontalLineTo(16.6f) }
            }
        }
    }

    val Person: ImageVector by lazy {
        line("Person") {
            stroke(1.8f) { circle(12f, 8f, 3.6f) }
            stroke(1.8f) {
                moveTo(4.8f, 20.2f)
                curveTo(4.8f, 15.6f, 8f, 14.2f, 12f, 14.2f)
                curveTo(16f, 14.2f, 19.2f, 15.6f, 19.2f, 20.2f)
            }
        }
    }

    val Bell: ImageVector by lazy {
        line("Bell") {
            stroke(1.7f) {
                moveTo(6f, 16.5f); verticalLineTo(11f)
                arcTo(6f, 6f, 0f, false, true, 18f, 11f); verticalLineTo(16.5f)
                lineTo(19.6f, 18.4f); horizontalLineTo(4.4f); close()
            }
            stroke(1.7f) { moveTo(12f, 3.2f); verticalLineTo(5f) }
            stroke(1.7f) { moveTo(9.8f, 20.6f); arcTo(2.2f, 2.2f, 0f, false, false, 14.2f, 20.6f) }
        }
    }

    val Chevron: ImageVector by lazy {
        line("Chevron") { stroke(1.9f) { moveTo(9.4f, 5.8f); lineTo(15.6f, 12f); lineTo(9.4f, 18.2f) } }
    }

    val Receipt: ImageVector by lazy {
        line("Receipt") {
            stroke { moveTo(6f, 3.4f); horizontalLineTo(18f); verticalLineTo(20.6f); lineTo(15.5f, 19f); lineTo(13f, 20.6f); lineTo(10.5f, 19f); lineTo(8f, 20.6f); lineTo(6f, 19f); close() }
            stroke(1.5f) { moveTo(9.2f, 8.4f); horizontalLineTo(14.8f); moveTo(9.2f, 12.2f); horizontalLineTo(14.8f) }
        }
    }

    val CheckCircle: ImageVector by lazy {
        line("CheckCircle") {
            stroke { circle(12f, 12f, 8.2f) }
            stroke(1.8f) { moveTo(8.2f, 12.3f); lineTo(11f, 15f); lineTo(16f, 9.4f) }
        }
    }

    val Sliders: ImageVector by lazy {
        line("Sliders") {
            stroke(1.6f) { moveTo(4.5f, 7f); horizontalLineTo(19.5f); moveTo(4.5f, 12f); horizontalLineTo(19.5f); moveTo(4.5f, 17f); horizontalLineTo(19.5f) }
            stroke(1.6f) { circle(9f, 7f, 2f) }
            stroke(1.6f) { circle(15f, 12f, 2f) }
            stroke(1.6f) { circle(9f, 17f, 2f) }
        }
    }

    val Info: ImageVector by lazy {
        line("Info") {
            stroke { circle(12f, 12f, 8.2f) }
            stroke(1.8f) { moveTo(12f, 11f); verticalLineTo(16f); moveTo(12f, 8.2f); verticalLineTo(8.3f) }
        }
    }

    val Logout: ImageVector by lazy {
        line("Logout") {
            stroke { moveTo(10f, 4.6f); horizontalLineTo(6.6f); arcTo(1.6f, 1.6f, 0f, false, false, 5f, 6.2f); verticalLineTo(17.8f); arcTo(1.6f, 1.6f, 0f, false, false, 6.6f, 19.4f); horizontalLineTo(10f) }
            stroke { moveTo(14f, 8f); lineTo(18f, 12f); lineTo(14f, 16f); moveTo(18f, 12f); horizontalLineTo(9f) }
        }
    }
}
