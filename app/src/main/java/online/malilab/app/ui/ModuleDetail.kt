package online.malilab.app.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.malilab.app.data.Auth
import online.malilab.app.data.Billing
import online.malilab.app.data.ModuleDetail
import online.malilab.app.data.Repo
import online.malilab.app.ui.theme.Mali

private sealed interface ModState {
    data object Loading : ModState
    data class Ready(val detail: ModuleDetail, val offline: Boolean) : ModState
    data object Failed : ModState
}

@Composable
fun ModuleDetailScreen(slug: String, active: Boolean, onBack: () -> Unit, onOpenLesson: (String) -> Unit, onOpenPractice: (String) -> Unit = {}, onUnlock: (String) -> Unit = {}) {
    var attempt by remember { mutableIntStateOf(0) }
    var state by remember(slug) { mutableStateOf<ModState>(ModState.Loading) }
    val accent = Mali.accentFor(slug)
    val owned = Billing.owns(slug)
    val done = Auth.progress.lessons
    val listState = rememberLazyListState()
    val progress = firstItemProgress(listState, 0.85f, 1f)

    BackHandler(enabled = active) { onBack() }
    StatusBarIcons(darkIcons = true, active = active)

    LaunchedEffect(slug) { Auth.loadProgress() }

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
                                if (Billing.status.paywall) Chip(if (owned) "Yours" else "KES ${Billing.status.priceModule}", if (owned) Mali.Green else Mali.Amber)
                                Chip(if (m.lessonCount == 1) "1 lesson" else "${m.lessonCount} lessons", Mali.InkSoft)
                                if (s.detail.practice.isNotEmpty()) {
                                    Chip(if (s.detail.practice.size == 1) "1 practice" else "${s.detail.practice.size} practice", Mali.InkSoft)
                                }
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
                            val total = s.detail.lessons.size
                            val doneCount = s.detail.lessons.count { it.slug in done }
                            val practiceOpen = s.detail.practice.any { it.classification == "required" && it.slug !in Auth.progress.practice }
                            val finished = total > 0 && doneCount == total && !practiceOpen
                            val nextLesson = s.detail.lessons.firstOrNull { it.slug !in done } ?: s.detail.lessons.firstOrNull()
                            if (total > 0) {
                                Spacer(Modifier.height(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        if (finished) "Module complete"
                                        else if (doneCount == total && practiceOpen) "Lessons done. Finish the practice to complete this module."
                                        else "$doneCount of $total lessons done",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (finished) Mali.Green else Mali.InkSoft,
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(Mali.Sand.copy(alpha = 0.6f))) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth(doneCount.toFloat() / total)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(if (finished) Mali.Green else accent),
                                    )
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                            Text(
                                when {
                                    !owned -> "Unlock this module · KES ${Billing.status.priceModule}"
                                    finished -> "Review lessons"
                                    doneCount > 0 -> "Continue"
                                    else -> "Start Lesson"
                                },
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(50))
                                    .background(accent)
                                    .clickable(enabled = nextLesson != null || !owned) {
                                        if (!owned) onUnlock(m.title) else nextLesson?.let { onOpenLesson(it.slug) }
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
                        val lessonDone = l.slug in done
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { if (owned) onOpenLesson(l.slug) else onUnlock(m.title) }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (lessonDone) Mali.Green else accent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (lessonDone) "✓" else "${l.n}",
                                    color = if (lessonDone) Color.White else accent,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                )
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
                    if (s.detail.practice.isNotEmpty()) {
                        item {
                            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 6.dp)) {
                                Text("Practice", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                                Spacer(Modifier.height(2.dp))
                                Text("Put it to work with your own numbers. Your answers stay private.", fontSize = 13.sp, color = Mali.InkSoft)
                            }
                        }
                        items(s.detail.practice.size) { i ->
                            val p = s.detail.practice[i]
                            val practiceDone = p.slug in Auth.progress.practice
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { if (owned) onOpenPractice(p.slug) else onUnlock(m.title) }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Box(
                                    Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (practiceDone) Mali.Green else Mali.Amber.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        if (practiceDone) "✓" else "✎",
                                        color = if (practiceDone) Color.White else Mali.Amber,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        when (p.classification) {
                                            "optional" -> "PRACTICE · OPTIONAL"
                                            "recommended" -> "PRACTICE · RECOMMENDED"
                                            else -> "PRACTICE"
                                        },
                                        fontSize = 11.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold, color = Mali.Amber,
                                    )
                                    Text(p.title, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                                    if (practiceDone) {
                                        Spacer(Modifier.height(2.dp))
                                        Text("Done. Tap to review or try again.", fontSize = 13.sp, color = Mali.InkSoft)
                                    }
                                }
                            }
                            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(1.dp).background(Mali.Sand.copy(alpha = 0.6f)))
                        }
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
                BackFab(onBack)
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
