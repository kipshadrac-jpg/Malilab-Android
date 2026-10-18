package online.malilab.app.ui

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
import androidx.compose.ui.platform.LocalDensity
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
    val density = LocalDensity.current
    val progress = scrollProgress(listState, with(density) { 40.dp.toPx() }, with(density) { 110.dp.toPx() })

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
    StatusBarIcons(darkIcons = false, active = active)

    Box(Modifier.fillMaxSize().background(Color.White)) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item { ScreenHeader("Playbooks", "Pick a situation. Follow the steps.") }
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
        FadingBar(
            progress = progress,
            background = Mali.TealTop,
            title = "Playbooks",
            titleColor = Color.White,
            onBack = null,
        )
    }
}

/** Cover picture edge to edge, then the title and description in full, then a coloured "Learn more". */
@Composable
fun PlaybookCard(p: PlaybookSummary, onOpen: () -> Unit) {
    val accent = Mali.accentFor(p.slug)
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onOpen)
            .padding(bottom = 22.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1200f / 630f)) {
            CoverImage(p.coverUrl, p.coverAlt, accent, Modifier.fillMaxSize())
            if (p.stepCount > 0) {
                Text(
                    text = "${p.stepCount} steps",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent)
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                )
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
            Text(
                p.title,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Mali.Ink,
            )
            if (p.summary.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(p.summary, fontSize = 13.sp, lineHeight = 19.sp, color = Mali.InkSoft)
            }
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Learn more", color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Spacer(Modifier.size(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
            }
        }
    }
}

/** The playbook's cover picture, or a coloured stand-in when it has none or cannot load. */
@Composable
fun CoverImage(url: String?, alt: String, accent: Color, modifier: Modifier) {
    if (url == null) {
        Box(
            modifier.background(Brush.linearGradient(listOf(accent, Mali.TealBottom))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(40.dp))
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

@Composable
fun SkeletonCard() {
    val pulse = rememberInfiniteTransition(label = "skeleton")
    val a by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha",
    )
    Column(Modifier.fillMaxWidth().background(Color.White).alpha(a).padding(bottom = 22.dp)) {
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
            .background(Color.White)
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
