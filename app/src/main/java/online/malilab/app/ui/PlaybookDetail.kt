package online.malilab.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.malilab.app.data.PlaybookDetail
import online.malilab.app.data.Repo
import online.malilab.app.ui.theme.Mali

private sealed interface DetailState {
    data object Loading : DetailState
    data class Ready(val detail: PlaybookDetail, val offline: Boolean) : DetailState
    data object Failed : DetailState
}

@Composable
fun PlaybookDetailScreen(slug: String, accentIndex: Int, onBack: () -> Unit) {
    var attempt by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<DetailState>(DetailState.Loading) }
    var notesOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (notesOpen) notesOpen = false else onBack()
    }

    LaunchedEffect(slug, attempt) {
        state = DetailState.Loading
        state = try {
            val loaded = Repo.playbook(slug)
            DetailState.Ready(loaded.value, loaded.offline)
        } catch (e: Exception) {
            DetailState.Failed
        }
    }

    Box(Modifier.fillMaxSize().background(Mali.Cream)) {
        when (val s = state) {
            is DetailState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Opening the playbook…", color = Mali.InkSoft, fontSize = 16.sp)
            }
            is DetailState.Failed -> Column(Modifier.fillMaxSize().statusBarsPadding()) {
                BackButton(onBack)
                MessageBlock(
                    title = "Could not open this playbook",
                    body = "Check your connection and try again.",
                    action = "Try again",
                    onAction = { attempt++ },
                )
            }
            is DetailState.Ready -> {
                DetailContent(s.detail, s.offline, accentIndex, onBack)
                PlanFab(
                    onClick = { notesOpen = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(18.dp),
                )
            }
        }
        StatusBarScrim()
        if (notesOpen) NotesSheet(slug) { notesOpen = false }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    Box(
        Modifier
            .padding(12.dp)
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.92f))
            .clickable(onClick = onBack)
            .semantics { contentDescription = "Back" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Mali.Ink)
    }
}

@Composable
private fun DetailContent(d: PlaybookDetail, offline: Boolean, accentIndex: Int, onBack: () -> Unit) {
    val s = d.summary
    val accent = Mali.accents[accentIndex % Mali.accents.size]
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Box(Modifier.fillMaxWidth()) {
                CoverImage(s.coverUrl, s.coverAlt, accent, Modifier.fillMaxWidth().aspectRatio(1200f / 630f))
                Box(Modifier.statusBarsPadding()) { BackButton(onBack) }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
                Text(
                    s.title,
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Mali.Ink,
                )
                if (s.summary.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(s.summary, fontSize = 17.sp, lineHeight = 28.sp, color = Mali.InkSoft)
                }
                if (offline) {
                    Spacer(Modifier.height(8.dp))
                    Text("Showing the copy saved on your phone.", fontSize = 13.sp, color = Mali.InkSoft)
                }
            }
        }
        if (s.goal.isNotBlank()) item {
            AccentSection(label = "GOAL", color = Mali.Teal, background = Color.White) {
                RichBlocks(s.goal, color = Mali.Ink, bulletColor = Mali.Teal)
            }
        }
        if (d.needs.isNotEmpty()) item { NeedsSection(d.needs) }
        if (d.steps.isNotEmpty()) {
            item {
                Row(
                    Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("STEPS", fontSize = 15.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Indigo)
                    Text(
                        "${d.steps.size} to follow",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Mali.InkSoft,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Mali.Sand.copy(alpha = 0.6f))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
            }
            items(d.steps.size) { i -> StepCard(d.steps[i], Mali.accents[i % Mali.accents.size]) }
        }
        if (d.outcome.isNotBlank()) item {
            Column(Modifier.fillMaxWidth().background(Mali.Night).padding(horizontal = 20.dp, vertical = 22.dp)) {
                Text("WHERE THIS LEAVES YOU", fontSize = 14.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD9B3))
                Spacer(Modifier.height(12.dp))
                RichBlocks(d.outcome, color = Color(0xFFF2F5F3), bulletColor = Color(0xFFFFD9B3))
            }
        }
        if (d.modules.isNotEmpty()) {
            item {
                Text(
                    "Go deeper: recommended modules",
                    style = MaterialTheme.typography.titleLarge,
                    color = Mali.Ink,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp),
                )
            }
            items(d.modules.size) { i ->
                val m = d.modules[i]
                val c = Mali.accents[i % Mali.accents.size]
                Row(Modifier.fillMaxWidth().background(Color.White).height(IntrinsicSize.Min)) {
                    Box(Modifier.width(8.dp).fillMaxHeight().background(c))
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                        Text(m.title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = c)
                        if (m.rationale.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(m.rationale, fontSize = 14.sp, lineHeight = 21.sp, color = Mali.InkSoft)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(110.dp)) }
    }
}

@Composable
private fun AccentSection(label: String, color: Color, background: Color, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().background(background).height(IntrinsicSize.Min)) {
        Box(Modifier.width(8.dp).fillMaxHeight().background(color))
        Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            Text(label, fontSize = 14.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun NeedsSection(needs: List<String>) {
    Column(Modifier.fillMaxWidth().background(Color(0xFFFFF8E8)).padding(horizontal = 18.dp, vertical = 18.dp)) {
        Text("WHAT YOU NEED", fontSize = 14.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Amber)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            needs.forEachIndexed { i, need ->
                val c = Mali.accents[i % Mali.accents.size]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(c), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                    Text(inlineText(need), fontSize = 16.sp, lineHeight = 24.sp, color = Mali.Ink)
                }
            }
        }
    }
}

@Composable
private fun StepCard(step: online.malilab.app.data.Step, accent: Color) {
    Column(Modifier.fillMaxWidth().background(Color.White)) {
        Box(Modifier.fillMaxWidth().height(5.dp).background(accent))
        Row(Modifier.padding(start = 16.dp, end = 18.dp, top = 16.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
                Text("${step.n}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(step.title, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold, color = accent)
                if (step.optional) {
                    Text(
                        "OPTIONAL",
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold,
                        color = Mali.Ink,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(accent.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                RichBlocks(step.text, color = Mali.Ink, bulletColor = accent)
            }
        }
    }
}

/** The "My plan" button: a pen inside a softly glowing circle. For now it opens a place to take notes. */
@Composable
fun PlanFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val glow = rememberInfiniteTransition(label = "glow")
    val ring by glow.animateFloat(
        initialValue = 1f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(tween(1900, easing = LinearEasing), RepeatMode.Restart),
        label = "ring",
    )
    val ringAlpha = 0.5f * (1.9f - ring) / 0.9f
    Box(modifier.size(84.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(56.dp).scale(ring).alpha(ringAlpha).background(Mali.Teal, CircleShape))
        Box(Modifier.size(70.dp).background(Mali.Teal.copy(alpha = 0.22f), CircleShape))
        Box(
            Modifier
                .size(56.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Mali.Teal, Mali.TealLight)))
                .clickable(onClick = onClick)
                .semantics { contentDescription = "My plan: take notes" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun NotesSheet(slug: String, onClose: () -> Unit) {
    var text by remember { mutableStateOf(Repo.notes(slug)) }
    var saved by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClose),
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Mali.Cream)
                .clickable(enabled = false, onClick = {})
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 22.dp, vertical = 22.dp),
        ) {
            Text("My plan", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
            Spacer(Modifier.height(4.dp))
            Text(
                "Take notes as you read. Soon you will turn them into your own plan.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Mali.InkSoft,
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; saved = false },
                placeholder = { Text("What stood out? What will you try this week?") },
                minLines = 5,
                maxLines = 10,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Mali.Teal,
                    unfocusedBorderColor = Mali.Sand,
                    cursorColor = Mali.Teal,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                ),
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (saved) "Saved" else "Save notes",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(Mali.Teal, Mali.TealLight)))
                        .clickable {
                            Repo.saveNotes(slug, text)
                            saved = true
                        }
                        .padding(horizontal = 28.dp, vertical = 13.dp),
                )
                Text(
                    "Close",
                    color = Mali.TealDeep,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 20.dp, vertical = 13.dp),
                )
            }
        }
    }
}
