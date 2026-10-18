package online.malilab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.malilab.app.data.ModuleSummary
import online.malilab.app.data.Repo
import online.malilab.app.ui.theme.Mali

private sealed interface LearnState {
    data object Loading : LearnState
    data class Ready(val items: List<ModuleSummary>, val offline: Boolean) : LearnState
    data object Failed : LearnState
}

@Composable
fun LearnScreen(active: Boolean, onOpen: (String) -> Unit) {
    var attempt by remember { mutableIntStateOf(0) }
    var state by remember {
        mutableStateOf<LearnState>(Repo.peekModules()?.let { LearnState.Ready(it, false) } ?: LearnState.Loading)
    }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val progress = scrollProgress(listState, with(density) { 40.dp.toPx() }, with(density) { 110.dp.toPx() })

    LaunchedEffect(attempt) {
        if (state is LearnState.Failed) state = LearnState.Loading
        try {
            val loaded = Repo.modules()
            state = LearnState.Ready(loaded.value, loaded.offline)
        } catch (e: Exception) {
            if (state !is LearnState.Ready) state = LearnState.Failed
        }
    }

    StatusBarIcons(darkIcons = false, active = active)

    Box(Modifier.fillMaxSize().background(Color.White)) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item { ScreenHeader("Learn", "Each module is a book of lessons.") }
            when (val s = state) {
                is LearnState.Loading -> item {
                    Text("Opening the library…", color = Mali.InkSoft, fontSize = 15.sp, modifier = Modifier.padding(20.dp))
                }
                is LearnState.Failed -> item {
                    MessageBlock(
                        title = "No connection",
                        body = "Could not reach MaliLab. Check your connection and try again.",
                        action = "Try again",
                        onAction = { attempt++ },
                    )
                }
                is LearnState.Ready -> {
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
                            title = "No modules yet",
                            body = "Modules appear here as soon as they are published.",
                            action = "Refresh",
                            onAction = { attempt++ },
                        )
                    }
                    items(s.items, key = { it.slug }) { m -> ModuleRow(m) { onOpen(m.slug) } }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
        FadingBar(progress = progress, background = Mali.TealTop, title = "Learn", titleColor = Color.White, onBack = null)
    }
}

@Composable
private fun ModuleRow(m: ModuleSummary, onOpen: () -> Unit) {
    val accent = Mali.accentFor(m.slug)
    Column(Modifier.fillMaxWidth().background(Color.White).clickable(onClick = onOpen)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BookCover(title = m.title, accent = accent, width = 104.dp)
            Column(Modifier.weight(1f)) {
                Text(m.title, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                if (m.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(m.description, fontSize = 13.sp, lineHeight = 19.sp, color = Mali.InkSoft)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (m.lessonCount == 1) "1 lesson" else "${m.lessonCount} lessons",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accent.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Mali.Sand.copy(alpha = 0.6f)))
    }
}
