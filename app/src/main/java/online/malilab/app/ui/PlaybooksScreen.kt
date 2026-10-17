package online.malilab.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import online.malilab.app.R
import online.malilab.app.data.PlaybookSummary
import online.malilab.app.data.Repo
import online.malilab.app.ui.theme.Mali

private sealed interface ListState {
    data object Loading : ListState
    data class Ready(val items: List<PlaybookSummary>, val offline: Boolean) : ListState
    data class Failed(val message: String) : ListState
}

@Composable
fun PlaybooksScreen(onOpen: (String, Int) -> Unit) {
    var attempt by remember { mutableIntStateOf(0) }
    var state by remember {
        mutableStateOf<ListState>(Repo.peekList()?.let { ListState.Ready(it, false) } ?: ListState.Loading)
    }

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

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { PlaybooksHeader() }
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
                            modifier = Modifier.padding(horizontal = 18.dp),
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
                    itemsIndexed(s.items, key = { _, p -> p.slug }) { index, p ->
                        PlaybookCard(p, index) { onOpen(p.slug, index) }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
        StatusBarScrim()
    }
}

@Composable
fun StatusBarScrim() {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        Modifier
            .fillMaxWidth()
            .height(top)
            .background(Mali.TealTop)
    )
}

@Composable
private fun PlaybooksHeader() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.verticalGradient(listOf(Mali.TealTop, Mali.TealBottom)))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.logo_mark), contentDescription = "MaliLab", modifier = Modifier.size(48.dp))
            }
            Column {
                Text("Playbooks", fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("Pick a situation. Follow the steps.", fontSize = 14.sp, color = Mali.TealDeep)
            }
        }
    }
}

@Composable
fun PlaybookCard(p: PlaybookSummary, index: Int, onOpen: () -> Unit) {
    val accent = Mali.accents[index % Mali.accents.size]
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onOpen),
    ) {
        Row(Modifier.fillMaxWidth().height(160.dp)) {
            Box(
                Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)),
            ) {
                CoverImage(p.coverUrl, p.coverAlt, accent, Modifier.fillMaxSize())
                if (p.stepCount > 0) {
                    Text(
                        text = "${p.stepCount} steps",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accent)
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
            ) {
                Text(
                    p.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 18.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Mali.Ink,
                )
                if (p.goal.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("GOAL", fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.ExtraBold, color = accent)
                    Text(
                        p.goal,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Mali.InkSoft,
                    )
                }
            }
        }
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            if (p.summary.isNotBlank()) {
                Text(
                    p.summary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Mali.Ink,
                )
            }
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("See more", color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Spacer(Modifier.size(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
        }
        Box(Modifier.fillMaxWidth().height(4.dp).background(accent.copy(alpha = 0.85f)))
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
private fun SkeletonCard() {
    val pulse = rememberInfiniteTransition(label = "skeleton")
    val a by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha",
    )
    Column(Modifier.fillMaxWidth().background(Color.White).alpha(a)) {
        Row(Modifier.height(160.dp)) {
            Box(Modifier.size(160.dp).clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)).background(Mali.TealTint))
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(6.dp)).background(Mali.Sand))
                Box(Modifier.fillMaxWidth(0.6f).height(18.dp).clip(RoundedCornerShape(6.dp)).background(Mali.Sand))
                Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(6.dp)).background(Mali.Sand))
            }
        }
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(6.dp)).background(Mali.Sand))
            Box(Modifier.fillMaxWidth(0.7f).height(14.dp).clip(RoundedCornerShape(6.dp)).background(Mali.Sand))
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
