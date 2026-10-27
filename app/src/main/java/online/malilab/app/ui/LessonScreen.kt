package online.malilab.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import online.malilab.app.data.ApiError
import online.malilab.app.data.Auth
import online.malilab.app.data.LessonBlock
import online.malilab.app.data.LessonDetail
import online.malilab.app.data.ModuleDetail
import online.malilab.app.data.Repo
import online.malilab.app.ui.theme.Mali

private sealed interface LessonState {
    data object Loading : LessonState
    data class Ready(val lesson: LessonDetail, val offline: Boolean) : LessonState
    data class Failed(val message: String = "Check your connection and try again.", val locked: Boolean = false) : LessonState
}

/** Reads one lesson, ends with "Mark lesson complete", and celebrates when the last lesson of a module is done. */
@Composable
fun LessonScreen(
    moduleSlug: String,
    lessonSlug: String,
    active: Boolean,
    onBack: () -> Unit,
    onOpenLesson: (String) -> Unit,
    onPlan: (String) -> Unit,
) {
    var attempt by remember { mutableIntStateOf(0) }
    var state by remember(lessonSlug) { mutableStateOf<LessonState>(LessonState.Loading) }
    var module by remember(moduleSlug) { mutableStateOf<ModuleDetail?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var celebrate by remember(lessonSlug) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val accent = Mali.accentFor(moduleSlug)

    BackHandler(enabled = active) {
        when {
            celebrate -> celebrate = false
            else -> onBack()
        }
    }
    StatusBarIcons(darkIcons = !celebrate, active = active)

    LaunchedEffect(moduleSlug) {
        module = try {
            Repo.module(moduleSlug).value
        } catch (e: Exception) {
            null
        }
    }
    LaunchedEffect(lessonSlug, attempt) {
        state = LessonState.Loading
        error = null
        state = try {
            val loaded = Repo.lesson(lessonSlug)
            LessonState.Ready(loaded.value, loaded.offline)
        } catch (e: ApiError) {
            if (e.status == 402) LessonState.Failed(e.message ?: "Unlock this module to read its lessons.", true) else LessonState.Failed()
        } catch (e: Exception) {
            LessonState.Failed()
        }
    }

    val lessons = module?.lessons.orEmpty()
    val index = lessons.indexOfFirst { it.slug == lessonSlug }
    val next = if (index >= 0) lessons.getOrNull(index + 1) else null
    val isDone = lessonSlug in Auth.progress.lessons

    Box(Modifier.fillMaxSize().background(Mali.Surface)) {
        when (val s = state) {
            is LessonState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Opening the lesson…", color = Mali.InkSoft, fontSize = 16.sp)
            }
            is LessonState.Failed -> Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Box(Modifier.padding(12.dp)) { BackCircle(onBack) }
                MessageBlock(
                    title = if (s.locked) "This lesson is locked" else "Could not open this lesson",
                    body = s.message,
                    action = if (s.locked) "Go back" else "Try again",
                    onAction = { if (s.locked) onBack() else attempt++ },
                )
            }
            is LessonState.Ready -> {
                val readState = androidx.compose.foundation.lazy.rememberLazyListState()
                LazyColumn(Modifier.fillMaxSize(), state = readState) {
                    item {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(accent.copy(alpha = 0.10f))
                                .statusBarsPadding()
                                .padding(start = 20.dp, end = 20.dp, top = 68.dp, bottom = 22.dp),
                        ) {
                            val where = module?.summary?.title.orEmpty()
                            val label = if (index >= 0) "LESSON ${index + 1} OF ${lessons.size}" else "LESSON"
                            Text(label, fontSize = 12.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.ExtraBold, color = accent)
                            if (where.isNotBlank()) Text(where, fontSize = 13.sp, color = Mali.InkSoft)
                            Spacer(Modifier.height(10.dp))
                            Text(s.lesson.title, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                            if (s.lesson.coverUrl != null) {
                                Spacer(Modifier.height(12.dp))
                                Box(Modifier.fillMaxWidth().aspectRatio(1200f / 630f).clip(RoundedCornerShape(14.dp))) {
                                    CoverImage(s.lesson.coverUrl, s.lesson.coverAlt, accent, Modifier.fillMaxSize())
                                }
                            }
                            if (s.lesson.summary.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(s.lesson.summary, fontSize = 16.sp, lineHeight = 24.sp, color = Mali.InkSoft)
                            }
                            if (s.offline) {
                                Spacer(Modifier.height(8.dp))
                                Text("Showing the copy saved on your phone.", fontSize = 13.sp, color = Mali.InkSoft)
                            }
                        }
                    }
                    items(s.lesson.blocks.size) { i ->
                        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                            BlockView(s.lesson.blocks[i], headingColor = Mali.accents[(i / 2) % Mali.accents.size])
                        }
                    }
                    item {
                        CompletionArea(
                            done = isDone,
                            busy = busy,
                            error = error,
                            accent = accent,
                            hasNext = next != null,
                            onMark = {
                                scope.launch {
                                    busy = true
                                    error = null
                                    val wasComplete = Auth.progress.modules[moduleSlug]?.complete == true
                                    try {
                                        val updated = Auth.completeLesson(lessonSlug)
                                        if (!wasComplete && updated.modules[moduleSlug]?.complete == true) celebrate = true
                                    } catch (e: ApiError) {
                                        error = e.message
                                    }
                                    busy = false
                                }
                            },
                            onNext = { next?.let { onOpenLesson(it.slug) } },
                            onBackToModule = onBack,
                        )
                    }
                    item { Spacer(Modifier.height(120.dp)) }
                }
                StatusScrim(readState, accent.copy(alpha = 0.10f).compositeOver(Mali.Surface), Modifier.align(Alignment.TopStart))
                BackFab(onBack, readState)
                PlanFab(
                    onClick = { onPlan(s.lesson.title) },
                    modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(18.dp),
                )
            }
        }
        if (celebrate) {
            Congratulations(
                moduleTitle = module?.summary?.title.orEmpty(),
                lessonCount = lessons.size,
                accent = accent,
                onPlan = { celebrate = false; onPlan((state as? LessonState.Ready)?.lesson?.title.orEmpty()) },
                onBackToModule = onBack,
            )
        }
    }
}

@Composable
private fun CompletionArea(
    done: Boolean,
    busy: Boolean,
    error: String?,
    accent: Color,
    hasNext: Boolean,
    onMark: () -> Unit,
    onNext: () -> Unit,
    onBackToModule: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (done) {
            Text(
                "✓  Lesson complete",
                color = Mali.Green,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(Mali.Green.copy(alpha = 0.12f))
                    .padding(vertical = 15.dp),
            )
            Text(
                if (hasNext) "Next lesson  →" else "Back to the module",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(accent)
                    .clickable { if (hasNext) onNext() else onBackToModule() }
                    .padding(vertical = 15.dp),
            )
        } else {
            Text(
                if (busy) "Saving…" else "Mark lesson complete",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(if (busy) accent.copy(alpha = 0.5f) else accent)
                    .clickable(enabled = !busy, onClick = onMark)
                    .padding(vertical = 15.dp),
            )
        }
        if (error != null) {
            Text(
                error,
                fontSize = 14.sp,
                color = Mali.Pink,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Congratulations(moduleTitle: String, lessonCount: Int, accent: Color, onPlan: () -> Unit, onBackToModule: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(accent, Mali.Night)))
            .clickable(enabled = false, onClick = {}),
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.size(110.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(80.dp).clip(CircleShape).background(Mali.Surface), contentAlignment = Alignment.Center) {
                    Text("✓", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = accent)
                }
            }
            Spacer(Modifier.height(26.dp))
            Text("Module complete!", fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            val what = if (moduleTitle.isBlank()) "this module" else moduleTitle
            val count = if (lessonCount == 1) "the lesson" else "all $lessonCount lessons"
            Text(
                "You finished $count in $what. Well done.",
                fontSize = 17.sp,
                lineHeight = 25.sp,
                color = Color.White.copy(alpha = 0.92f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "While it is fresh, jot down what you will do with it. That is the start of your plan.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "Start my plan",
                color = accent,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Mali.Surface).clickable(onClick = onPlan).padding(vertical = 16.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Back to the module",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.16f))
                    .clickable(onClick = onBackToModule)
                    .padding(vertical = 16.dp),
            )
        }
    }
}

// ---------------------------------------------------------------- lesson blocks
@Composable
internal fun BlockView(b: LessonBlock, headingColor: Color) {
    when (b.type) {
        "heading" -> Text(
            inlineText(b.text),
            fontSize = if (b.level == 3) 19.sp else 23.sp,
            lineHeight = if (b.level == 3) 25.sp else 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = headingColor,
            modifier = Modifier.padding(top = 10.dp),
        )
        "paragraph" -> Text(inlineText(b.text), fontSize = 17.sp, lineHeight = 28.sp, color = Mali.Ink)
        "list" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            b.items.forEachIndexed { i, item ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (b.ordered) {
                        Text("${i + 1}.", fontSize = 17.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold, color = headingColor, modifier = Modifier.width(26.dp))
                    } else {
                        Box(Modifier.padding(top = 10.dp).size(8.dp).clip(CircleShape).background(headingColor))
                    }
                    Text(inlineText(item), fontSize = 17.sp, lineHeight = 26.sp, color = Mali.Ink, modifier = Modifier.weight(1f))
                }
            }
        }
        "callout" -> Callout(b)
        "table" -> TableBlock(b)
        else -> Unit
    }
}

@Composable
private fun Callout(b: LessonBlock) {
    val (label, color) = when (b.tone) {
        "tip" -> "TIP" to Mali.Green
        "warning" -> "WATCH OUT" to Mali.Amber
        "key" -> "KEY IDEA" to Mali.Indigo
        "example" -> "EXAMPLE" to Mali.Purple
        else -> "NOTE" to Mali.Teal
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(16.dp),
    ) {
        Text(label, fontSize = 12.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Spacer(Modifier.height(6.dp))
        Text(inlineText(b.text), fontSize = 16.sp, lineHeight = 24.sp, color = Mali.Ink)
    }
}

@Composable
private fun TableBlock(b: LessonBlock) {
    val columns = b.header.size.coerceAtLeast(1)
    val cellWidth = if (columns <= 2) 170.dp else 130.dp
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Mali.Cream)
            .horizontalScroll(rememberScrollState()),
    ) {
        Row(Modifier.background(Mali.TealTint)) {
            b.header.forEach { h ->
                Text(inlineText(h), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Mali.TealDeep, modifier = Modifier.width(cellWidth).padding(12.dp))
            }
        }
        b.rows.forEach { row ->
            Box(Modifier.width(cellWidth * columns).height(1.dp).background(Mali.Sand))
            Row {
                row.forEach { cell ->
                    Text(inlineText(cell), fontSize = 14.sp, lineHeight = 20.sp, color = Mali.Ink, modifier = Modifier.width(cellWidth).padding(12.dp))
                }
            }
        }
    }
}
