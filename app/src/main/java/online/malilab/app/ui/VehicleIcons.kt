package online.malilab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import online.malilab.app.data.Vehicle

/** One outline icon for each place people put their money. Drawn with strokes, so the badge colour comes from the tint. */
object VehicleIcons {
    private fun draw(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(block).build()

    private fun ImageVector.Builder.stroke(width: Float = 1.7f, body: PathBuilder.() -> Unit) {
        path(
            fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = body,
        )
    }

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy)
        arcTo(r, r, 0f, true, true, cx + r, cy)
        arcTo(r, r, 0f, true, true, cx - r, cy)
        close()
    }

    private fun PathBuilder.box(l: Float, t: Float, r: Float, b: Float, c: Float) {
        moveTo(l + c, t); horizontalLineTo(r - c)
        arcTo(c, c, 0f, false, true, r, t + c); verticalLineTo(b - c)
        arcTo(c, c, 0f, false, true, r - c, b); horizontalLineTo(l + c)
        arcTo(c, c, 0f, false, true, l, b - c); verticalLineTo(t + c)
        arcTo(c, c, 0f, false, true, l + c, t); close()
    }

    /** Treasury bonds: a certificate with a ribbon seal. */
    private val Bond by lazy {
        draw("Bond") {
            stroke { box(4f, 3.5f, 20f, 16f, 2f) }
            stroke { moveTo(7.5f, 7.5f); horizontalLineTo(16.5f); moveTo(7.5f, 10.8f); horizontalLineTo(12f) }
            stroke { circle(16f, 15.5f, 3f) }
            stroke { moveTo(14.4f, 18.2f); lineTo(13.6f, 21f); lineTo(16f, 19.8f); lineTo(18.4f, 21f); lineTo(17.6f, 18.2f) }
        }
    }

    /** SACCOs: three people together. */
    private val Sacco by lazy {
        draw("Sacco") {
            stroke { circle(12f, 8f, 2.7f) }
            stroke { moveTo(6.8f, 19f); arcTo(5.2f, 5f, 0f, false, true, 17.2f, 19f) }
            stroke { circle(5.2f, 10f, 2f) }
            stroke { moveTo(2.4f, 17f); arcTo(3.4f, 3.6f, 0f, false, true, 6.6f, 13.6f) }
            stroke { circle(18.8f, 10f, 2f) }
            stroke { moveTo(21.6f, 17f); arcTo(3.4f, 3.6f, 0f, false, false, 17.4f, 13.6f) }
        }
    }

    /** Money market funds: a stack of coins. */
    private val Mmf by lazy {
        draw("Mmf") {
            stroke {
                moveTo(5f, 6.5f); arcTo(7f, 2.6f, 0f, false, true, 19f, 6.5f); arcTo(7f, 2.6f, 0f, false, true, 5f, 6.5f); close()
                moveTo(5f, 6.5f); verticalLineTo(17.5f)
                moveTo(19f, 6.5f); verticalLineTo(17.5f)
                moveTo(5f, 10.2f); arcTo(7f, 2.6f, 0f, false, false, 19f, 10.2f)
                moveTo(5f, 13.9f); arcTo(7f, 2.6f, 0f, false, false, 19f, 13.9f)
                moveTo(5f, 17.5f); arcTo(7f, 2.6f, 0f, false, false, 19f, 17.5f)
            }
        }
    }

    /** Pension: an umbrella, for the years you are covered. */
    private val Pension by lazy {
        draw("Pension") {
            stroke {
                moveTo(3f, 12f); arcTo(9f, 8f, 0f, false, true, 21f, 12f)
                moveTo(3f, 12f); arcTo(3f, 2.6f, 0f, false, true, 9f, 12f)
                arcTo(3f, 2.6f, 0f, false, true, 15f, 12f)
                arcTo(3f, 2.6f, 0f, false, true, 21f, 12f)
                moveTo(12f, 4f); verticalLineTo(3f)
                moveTo(12f, 12f); verticalLineTo(18.5f)
                arcTo(2f, 2f, 0f, false, true, 8f, 18.5f)
            }
        }
    }

    /** NSE stocks: a line chart climbing, with an arrow head. */
    private val Equities by lazy {
        draw("Equities") {
            stroke { moveTo(4f, 4f); verticalLineTo(20f); horizontalLineTo(20.5f) }
            stroke { moveTo(7.5f, 15.5f); lineTo(11f, 11.5f); lineTo(14f, 14f); lineTo(19.5f, 7.5f) }
            stroke { moveTo(15.2f, 7.4f); horizontalLineTo(19.6f); verticalLineTo(11.8f) }
        }
    }

    /** Real estate and land: a house. */
    private val RealEstate by lazy {
        draw("RealEstate") {
            stroke { moveTo(3f, 11f); lineTo(12f, 3.6f); lineTo(21f, 11f) }
            stroke { moveTo(5.5f, 9.4f); verticalLineTo(19.5f); horizontalLineTo(18.5f); verticalLineTo(9.4f) }
            stroke { moveTo(9.8f, 19.5f); verticalLineTo(14f); horizontalLineTo(14.2f); verticalLineTo(19.5f) }
        }
    }

    /** Chamas: two arrows chasing each other round a coin, the way a chama's money goes round. */
    private val Chama by lazy {
        draw("Chama") {
            stroke { circle(12f, 12f, 2.2f) }
            stroke { moveTo(5.6f, 9.5f); arcTo(7f, 7f, 0f, false, true, 18.6f, 8.6f); moveTo(15.2f, 8.2f); lineTo(18.8f, 8.6f); lineTo(18.4f, 5f) }
            stroke { moveTo(18.4f, 14.5f); arcTo(7f, 7f, 0f, false, true, 5.4f, 15.4f); moveTo(8.8f, 15.8f); lineTo(5.2f, 15.4f); lineTo(5.6f, 19f) }
        }
    }

    /** Insurance: a shield with a tick. */
    private val Insurance by lazy {
        draw("Insurance") {
            stroke { moveTo(12f, 3f); lineTo(19.5f, 6f); verticalLineTo(11.5f); curveTo(19.5f, 16f, 16.5f, 19.2f, 12f, 21f); curveTo(7.5f, 19.2f, 4.5f, 16f, 4.5f, 11.5f); verticalLineTo(6f); close() }
            stroke { moveTo(8.8f, 11.8f); lineTo(11.2f, 14.2f); lineTo(15.4f, 9.4f) }
        }
    }

    /** Mobile money investing: a phone with a coin on its screen. */
    private val Mobile by lazy {
        draw("Mobile") {
            stroke { box(6.5f, 2.5f, 17.5f, 21.5f, 2.2f) }
            stroke { moveTo(10.5f, 19f); horizontalLineTo(13.5f) }
            stroke { circle(12f, 10.8f, 3.2f) }
            stroke { moveTo(12f, 9.2f); verticalLineTo(12.4f); moveTo(10.6f, 10.8f); horizontalLineTo(13.4f) }
        }
    }

    /** Unit trusts: a pie chart with one slice pulled out. */
    private val Cis by lazy {
        draw("Cis") {
            stroke { moveTo(11f, 5f); arcTo(8f, 8f, 0f, true, false, 19f, 13f); horizontalLineTo(11f); close() }
            stroke { moveTo(14f, 3.2f); arcTo(7f, 7f, 0f, false, true, 20.8f, 10f); horizontalLineTo(14f); close() }
        }
    }

    /** Banks: a bank with columns. */
    private val Bank by lazy {
        draw("Bank") {
            stroke { moveTo(3f, 9f); lineTo(12f, 3.5f); lineTo(21f, 9f); close() }
            stroke { moveTo(6.5f, 11.5f); verticalLineTo(17f); moveTo(10.2f, 11.5f); verticalLineTo(17f); moveTo(13.8f, 11.5f); verticalLineTo(17f); moveTo(17.5f, 11.5f); verticalLineTo(17f) }
            stroke { moveTo(4f, 20f); horizontalLineTo(20f) }
        }
    }

    /** Business: a briefcase. */
    private val Business by lazy {
        draw("Business") {
            stroke { box(3f, 7.5f, 21f, 19.5f, 2.2f) }
            stroke { moveTo(8.8f, 7.5f); verticalLineTo(5.8f); arcTo(1.3f, 1.3f, 0f, false, true, 10.1f, 4.5f); horizontalLineTo(13.9f); arcTo(1.3f, 1.3f, 0f, false, true, 15.2f, 5.8f); verticalLineTo(7.5f) }
            stroke { moveTo(3f, 13f); horizontalLineTo(21f) }
            stroke { moveTo(10.6f, 13f); verticalLineTo(14.8f); horizontalLineTo(13.4f); verticalLineTo(13f) }
        }
    }

    fun of(key: String): ImageVector = when (key) {
        "tbonds" -> Bond
        "sacco" -> Sacco
        "mmf" -> Mmf
        "pension" -> Pension
        "equities" -> Equities
        "realestate" -> RealEstate
        "chama" -> Chama
        "insurance" -> Insurance
        "mobile" -> Mobile
        "cis" -> Cis
        "banks" -> Bank
        "business" -> Business
        else -> Mmf
    }
}

/** A rounded square holding the place's icon: colour-filled with a white icon, or softly tinted with a coloured one. */
@Composable
fun VehicleBadge(vehicle: Vehicle, size: Dp = 42.dp, filled: Boolean = false) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(if (filled) vehicle.color else vehicle.color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(VehicleIcons.of(vehicle.key), contentDescription = null, tint = if (filled) Color.White else vehicle.color, modifier = Modifier.size(size * 0.58f))
    }
}
