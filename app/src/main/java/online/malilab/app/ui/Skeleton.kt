package online.malilab.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import online.malilab.app.ui.theme.Mali

/** A soft band of light that slides across a placeholder, so the person can see the screen is still filling in. */
@Composable
private fun shimmer(base: Color, light: Color): Brush {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val x by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "skeleton-x",
    )
    val start = x * 1800f - 700f
    return Brush.linearGradient(listOf(base, light, base), start = Offset(start, 0f), end = Offset(start + 700f, 120f))
}

/** One grey (or, on a coloured card, white) placeholder bar. [width] null means the full width. */
@Composable
fun SkelBar(height: Dp, modifier: Modifier = Modifier, width: Dp? = null, radius: Dp = 8.dp, onColour: Boolean = false) {
    val brush = if (onColour) shimmer(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.40f)) else shimmer(Mali.Sand.copy(alpha = 0.55f), Mali.Sand.copy(alpha = 0.95f))
    val size = if (width != null) modifier.width(width) else modifier.fillMaxWidth()
    Box(size.height(height).clip(RoundedCornerShape(radius)).background(brush))
}

@Composable
private fun SkelCircle(diameter: Dp) {
    Box(Modifier.size(diameter).clip(CircleShape).background(shimmer(Mali.Sand.copy(alpha = 0.55f), Mali.Sand.copy(alpha = 0.95f))))
}

@Composable
private fun SkelCard(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Mali.Surface).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

/** The portfolio while it is being fetched: the same shapes the finished screen has, filled with moving placeholders. */
@Composable
fun PortfolioSkeleton() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SkelCard {
            SkelBar(12.dp, width = 140.dp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                SkelCircle(130.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SkelBar(14.dp)
                    SkelBar(14.dp, width = 150.dp)
                    SkelBar(14.dp, width = 110.dp)
                }
            }
            SkelBar(10.dp)
            SkelBar(10.dp, width = 220.dp)
        }
        SkelCard {
            SkelBar(12.dp, width = 120.dp)
            SkelBar(26.dp, width = 190.dp)
            SkelBar(12.dp)
        }
        Spacer(Modifier.height(2.dp))
        SkelBar(12.dp, width = 90.dp)
        repeat(3) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Mali.Surface).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SkelCircle(44.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkelBar(14.dp, width = 160.dp)
                    SkelBar(11.dp, width = 110.dp)
                }
                SkelBar(16.dp, width = 70.dp)
            }
        }
    }
}
