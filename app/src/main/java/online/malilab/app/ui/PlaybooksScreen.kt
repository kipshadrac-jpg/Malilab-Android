package online.malilab.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import online.malilab.app.data.PlaybookSummary
import online.malilab.app.data.Repo
import online.malilab.app.ui.theme.Mali

private sealed interface ListState {
    data object Loading : ListState
    data class Ready(val items: List<PlaybookSummary>, val offline: Boolean) : ListState
    data class Failed(val message: String) : ListState
}

@Composable
fun PlaybooksScreen(active: Boolean, onOpen: (String) -> Unit) {
    var attempt by remember { mutableIntStateOf(0) }
    var state by remember {
        mutableStateOf<ListState>(Repo.peekList()?.let { ListState.Ready(it, false) } ?: ListState.Loading)
    }
    val listState = rememberLazyListState()
    val progress = firstItemProgress(listState, 0.6f, 1f)

    LaunchedEffect(attempt) {
        if (state is ListState.Failed) state = ListState.Loading
        try {
            val loaded = Repo.playbooks()
            state = ListState.Ready(loaded.value, loaded.offline)
        } catch (e: Exception) {
            if (state !is ListState.Ready) {
                state = ListState.Failed("Could not reach MaliLab. Check your connection and try again.")
            }
        }
    }

    // The header and the phone's status bar are the same teal, so white icons read well.
    StatusBarIcons(darkIcons = true, active = active)

    Box(Modifier.fillMaxSize().background(Mali.Surface)) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item { IntroCard("Pick a situation. Follow the steps.", NavIcons.Checklist) }
            when (val s = state) {
                is ListState.Loading -> items(3) { SkeletonCard() }
                is ListState.Failed -> item {
                    MessageBlock(
                        title = "No connection",
                        body = s.message,
                        action = "Try again",
                        onAction = { attempt++ },
                    )
                }
                is ListState.Ready -> {
                    if (s.offline) item {
                        Text(
                            "Showing the copy saved on your phone.",
                            color = Mali.InkSoft,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                        )
                    }
                    if (s.items.isEmpty()) item {
                        MessageBlock(
                            title = "No playbooks yet",
                            body = "New playbooks appear here as soon as they are published.",
                            action = "Refresh",
                            onAction = { attempt++ },
                        )
                    }
                    items(s.items, key = { it.slug }) { p ->
                        PlaybookCard(p) { onOpen(p.slug) }
                    }
                }
            }
        }
    }
}

/** One playbook as a card: tag and title beside a small picture, a short summary, then Goal (opens a framed panel) and Open. */
@Composable
fun PlaybookCard(p: PlaybookSummary, onOpen: () -> Unit) {
    val accent = Mali.accentFor(p.slug)
    var showGoal by remember(p.slug) { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .clip(shape)
            .background(Mali.Surface)
            .border(1.dp, Mali.Sand, shape)
            .animateContentSize()
            .padding(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                if (p.stepCount > 0) {
                    Text(
                        "${p.stepCount} STEPS",
                        color = Mali.TealDeep, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.clip(RoundedCornerShape(7.dp)).background(Mali.TealTint).padding(horizontal = 9.dp, vertical = 5.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Text(p.title, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
            }
            Box(Modifier.size(width = 112.dp, height = 86.dp).clip(RoundedCornerShape(12.dp))) {
                CoverImage(p.coverUrl, p.coverAlt, accent, Modifier.fillMaxSize(), sprout = true)
            }
        }
        if (p.summary.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(p.summary, fontSize = 13.sp, lineHeight = 19.sp, color = Mali.InkSoft, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (p.goal.isNotBlank()) {
                PillButton(if (showGoal) "Hide goal" else "Goal", Mali.Ink, Mali.Sand, Modifier.weight(1f)) { showGoal = !showGoal }
            }
            PillButton("Open playbook", Mali.TealDeep, Mali.Teal, Modifier.weight(1.4f), onOpen)
        }
        if (showGoal && p.goal.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Mali.TealTint).padding(16.dp)) {
                Text("Goal", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Mali.TealDeep)
                Spacer(Modifier.height(6.dp))
                RichBlocks(p.goal, color = Mali.InkSoft, bulletColor = Mali.TealDeep)
            }
        }
    }
}

@Composable
private fun PillButton(label: String, textColor: Color, borderColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .border(1.5.dp, borderColor, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
}

/** The playbook's cover picture, or a coloured stand-in when it has none or cannot load. */
@Composable
fun CoverImage(url: String?, alt: String, accent: Color, modifier: Modifier, sprout: Boolean = false) {
    if (url == null) {
        if (sprout) {
            SproutArt(accent, modifier)
        } else {
            Box(
                modifier.background(Brush.linearGradient(listOf(accent, Mali.TealBottom))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(40.dp))
            }
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = alt,
            contentScale = ContentScale.Crop,
            placeholder = ColorPainter(Mali.TealTint),
            error = ColorPainter(Mali.TealTint),
            modifier = modifier,
        )
    }
}

/** A seedling growing out of two coins, on the playbook's own colour. Stands in for a missing cover. */
@Composable
fun SproutArt(accent: Color, modifier: Modifier) {
    val light = lerp(accent, Color.White, 0.25f)
    val dark = lerp(accent, Color.Black, 0.35f)
    Canvas(modifier) {
        drawRect(Brush.radialGradient(listOf(light, dark), center = Offset(size.width / 2f, size.height * 0.45f), radius = size.maxDimension * 0.75f))
        val k = minOf(size.width / 200f, size.height / 120f)
        val ox = (size.width - 200f * k) / 2f
        val oy = (size.height - 120f * k) / 2f
        fun x(v: Float) = ox + v * k
        fun y(v: Float) = oy + v * k
        val cream = Color(0xFFFFF8EF)
        val gold = Color(0xFFF6D23C)
        drawCircle(Color.White.copy(alpha = 0.14f), 44f * k, Offset(x(100f), y(58f)))
        drawOval(cream, Offset(x(66f), y(85f)), Size(68f * k, 14f * k))
        drawRoundRect(cream, Offset(x(97f), y(52f)), Size(6f * k, 40f * k), CornerRadius(3f * k))
        val left = Path().apply {
            moveTo(x(97f), y(58f))
            cubicTo(x(80f), y(58f), x(74f), y(44f), x(78f), y(36f))
            cubicTo(x(92f), y(36f), x(100f), y(44f), x(97f), y(58f))
            close()
        }
        drawPath(left, cream)
        val right = Path().apply {
            moveTo(x(103f), y(54f))
            cubicTo(x(103f), y(38f), x(113f), y(30f), x(127f), y(30f))
            cubicTo(x(131f), y(44f), x(121f), y(56f), x(103f), y(54f))
            close()
        }
        drawPath(right, gold)
        for (cx in listOf(84f, 116f)) {
            drawCircle(gold, 6f * k, Offset(x(cx), y(90f)))
            drawCircle(Color(0xFFE0A800), 6f * k, Offset(x(cx), y(90f)), style = Stroke(1.5f * k))
        }
    }
}

@Composable
fun SkeletonCard() {
    val pulse = rememberInfiniteTransition(label = "skeleton")
    val a by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha",
    )
    Column(Modifier.fillMaxWidth().background(Mali.Surface).alpha(a).padding(bottom = 22.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1200f / 630f).background(Mali.TealTint))
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth(0.8f).height(16.dp).clip(RoundedCornerShape(6.dp)).background(Mali.Sand))
            Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(Mali.Sand))
            Box(Modifier.fillMaxWidth(0.6f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(Mali.Sand))
        }
    }
}

@Composable
fun MessageBlock(title: String, body: String, action: String, onAction: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Mali.Surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = Mali.Ink)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = Mali.InkSoft)
        Spacer(Modifier.height(16.dp))
        Text(
            action,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Mali.Teal)
                .clickable(onClick = onAction)
                .padding(horizontal = 26.dp, vertical = 12.dp),
        )
    }
}
