package online.malilab.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.malilab.app.data.ModuleDetail
import online.malilab.app.data.Repo
import online.malilab.app.ui.theme.Mali

private sealed interface ModState {
    data object Loading : ModState
    data class Ready(val detail: ModuleDetail, val offline: Boolean) : ModState
    data object Failed : ModState
}

@Composable
fun ModuleDetailScreen(slug: String, active: Boolean, onBack: () -> Unit) {
    var attempt by remember { mutableIntStateOf(0) }
    var state by remember(slug) { mutableStateOf<ModState>(ModState.Loading) }
    val accent = Mali.accentFor(slug)
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val progress = scrollProgress(listState, with(density) { 200.dp.toPx() }, with(density) { 300.dp.toPx() })

    BackHandler(enabled = active) { onBack() }
    StatusBarIcons(darkIcons = true, active = active)

    LaunchedEffect(slug, attempt) {
        state = ModState.Loading
        state = try {
            val loaded = Repo.module(slug)
            ModState.Ready(loaded.value, loaded.offline)
        } catch (e: Exception) {
            ModState.Failed
        }
    }

    Box(Modifier.fillMaxSize().background(Color.White)) {
        when (val s = state) {
            is ModState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Opening the module…", color = Mali.InkSoft, fontSize = 16.sp)
            }
            is ModState.Failed -> Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Box(Modifier.padding(12.dp)) { BackCircle(onBack) }
                MessageBlock(
                    title = "Could not open this module",
                    body = "Check your connection and try again.",
                    action = "Try again",
                    onAction = { attempt++ },
                )
            }
            is ModState.Ready -> {
                val m = s.detail.summary
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    item {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(accent.copy(alpha = 0.14f))
                                .statusBarsPadding()
                                .padding(top = 64.dp, bottom = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            BookCover(title = m.title, accent = accent, width = 190.dp, large = true)
                        }
                    }
                    item {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                            Text(m.title, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Chip("Module", accent)
                                Chip(if (m.lessonCount == 1) "1 lesson" else "${m.lessonCount} lessons", Mali.InkSoft)
                            }
                            if (s.offline) {
                                Spacer(Modifier.height(8.dp))
                                Text("Showing the copy saved on your phone.", fontSize = 13.sp, color = Mali.InkSoft)
                            }
                            if (m.description.isNotBlank()) {
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    m.description,
                                    fontSize = 15.sp,
                                    lineHeight = 23.sp,
                                    color = Mali.Ink,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Mali.Cream)
                                        .padding(16.dp),
                                )
                            }
                            Spacer(Modifier.height(18.dp))
                            Text(
                                "Start Lesson",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(50))
                                    .background(accent)
                                    .clickable {
                                        Toast.makeText(context, "Lessons open in the next update", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 15.dp),
                            )
                            Spacer(Modifier.height(24.dp))
                            Text("Lessons", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Finish every lesson to complete this module.",
                                fontSize = 13.sp,
                                color = Mali.InkSoft,
                            )
                        }
                    }
                    items(s.detail.lessons.size) { i ->
                        val l = s.detail.lessons[i]
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(
                                Modifier.size(38.dp).clip(CircleShape).background(accent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${l.n}", color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Lesson ${l.n}", fontSize = 11.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold, color = accent)
                                Text(l.title, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                                if (l.summary.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(l.summary, fontSize = 13.sp, lineHeight = 19.sp, color = Mali.InkSoft)
                                }
                            }
                        }
                        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(1.dp).background(Mali.Sand.copy(alpha = 0.6f)))
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
                FadingBar(progress = progress, background = Mali.Cream, title = m.title, titleColor = Mali.Ink, onBack = onBack)
            }
        }
    }
}

@Composable
private fun Chip(text: String, color: Color) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 11.dp, vertical = 4.dp),
    )
}
