package online.malilab.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.launch
import online.malilab.app.ui.theme.Mali

private data class OnboardingPage(
    val icon: ImageVector,
    val label: String,
    val word: String,
    val body: String,
)

private val pages = listOf(
    OnboardingPage(
        icon = Icons.Filled.Star,
        label = "Learn money",
        word = "Made clear",
        body = "Short, colourful lessons written for Kenya. Saccos, M-Pesa, budgeting and saving, " +
            "explained in plain words you can read in minutes.",
    ),
    OnboardingPage(
        icon = Icons.Filled.CheckCircle,
        label = "Follow a",
        word = "Playbook",
        body = "Step-by-step playbooks turn a good idea into action. One small card at a time, " +
            "with what you need and where it leaves you.",
    ),
    OnboardingPage(
        icon = Icons.Filled.DateRange,
        label = "Keep your",
        word = "Plan",
        body = "Save your notes, see how far you have come, and carry your money plan " +
            "in your pocket.",
    ),
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val current = pagerState.currentPage
    val last = current == pages.size - 1

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Mali.TealTop, Mali.TealBottom)))
    ) {
        Bubbles()
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassChip(text = "${current + 1}/${pages.size}", onClick = null)
                if (!last) GlassChip(text = "Skip", onClick = onFinished)
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { index ->
                PageHero(pages[index])
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp))
                    .background(Mali.Cream)
                    .navigationBarsPadding()
                    .padding(horizontal = 26.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Crossfade(targetState = current, label = "body") { p ->
                    Text(
                        text = pages[p].body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Mali.InkSoft,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.heightIn(min = 112.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pages.indices.forEach { i -> Dot(selected = i == current) }
                }
                Spacer(Modifier.height(26.dp))
                GradientButton(
                    text = if (last) "Get Started" else "Next",
                    onClick = {
                        if (last) onFinished()
                        else scope.launch { pagerState.animateScrollToPage(current + 1) }
                    },
                )
            }
        }
    }
}

@Composable
private fun PageHero(page: OnboardingPage) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(2300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale",
    )
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(230.dp).scale(pulseScale), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(230.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            )
            Box(
                Modifier
                    .size(184.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
            )
            Box(
                Modifier
                    .size(132.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.28f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(page.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(58.dp))
            }
        }
        Spacer(Modifier.height(30.dp))
        Text(
            text = page.label,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Mali.TealDeep.copy(alpha = 0.8f),
            letterSpacing = 1.sp,
        )
        Text(
            text = page.word,
            fontSize = 42.sp,
            lineHeight = 48.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Mali.TealDeep,
        )
    }
}

@Composable
private fun Bubbles() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawCircle(Color.White.copy(alpha = 0.10f), radius = 44.dp.toPx(), center = Offset(w * 0.14f, h * 0.20f))
        drawCircle(Color.White.copy(alpha = 0.14f), radius = 30.dp.toPx(), center = Offset(w * 0.78f, h * 0.17f))
        drawCircle(Color.White.copy(alpha = 0.12f), radius = 26.dp.toPx(), center = Offset(w * 0.84f, h * 0.40f))
        drawCircle(Color.White.copy(alpha = 0.16f), radius = 6.dp.toPx(), center = Offset(w * 0.62f, h * 0.10f))
        drawCircle(Color.White.copy(alpha = 0.16f), radius = 5.dp.toPx(), center = Offset(w * 0.70f, h * 0.30f))
        drawCircle(
            Color.White.copy(alpha = 0.28f),
            radius = 150.dp.toPx(),
            center = Offset(w * 1.0f, h * 0.05f),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

@Composable
private fun GlassChip(text: String, onClick: (() -> Unit)?) {
    val base = Modifier
        .clip(RoundedCornerShape(50))
        .background(Color.White.copy(alpha = 0.22f))
        .border(1.dp, Color.White.copy(alpha = 0.40f), RoundedCornerShape(50))
    Box(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun Dot(selected: Boolean) {
    val width by animateDpAsState(if (selected) 30.dp else 10.dp, label = "dotWidth")
    val color by animateColorAsState(if (selected) Mali.Teal else Mali.Sand, label = "dotColor")
    Box(
        Modifier
            .height(10.dp)
            .width(width)
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}

@Composable
private fun GradientButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(32.dp)
    Button(
        onClick = onClick,
        shape = shape,
        contentPadding = PaddingValues(horizontal = 22.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
        elevation = null,
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .background(Brush.horizontalGradient(listOf(Mali.Teal, Mali.TealLight)), shape),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
            }
        }
    }
}
