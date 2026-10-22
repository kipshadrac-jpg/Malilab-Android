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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import online.malilab.app.data.ApiError
import online.malilab.app.data.Auth
import online.malilab.app.data.Block
import online.malilab.app.data.Fmt
import online.malilab.app.data.NoteStore
import online.malilab.app.data.PActivity
import online.malilab.app.data.PQuestion
import online.malilab.app.data.PracticeStore
import online.malilab.app.ui.theme.Mali

private sealed interface PState {
    data object Loading : PState
    data class Failed(val message: String) : PState
    data class Intro(val activity: PActivity) : PState
    data class Form(val activity: PActivity, val offline: Boolean) : PState
    data class Done(val activity: PActivity, val answers: Map<String, Any>, val submittedAt: String) : PState
}

/**
 * One practice activity: write your answers (they save as you go, even offline), submit when every required answer is in,
 * then keep them, copy them into My plan, or try again. [viewAttempt] opens one finished attempt to read.
 */
@Composable
fun PracticeScreen(
    slug: String,
    viewAttempt: Int,
    active: Boolean,
    onBack: () -> Unit,
    onOpenCalculator: (model: String, slug: String, title: String) -> Unit,
    onCopyToPlan: (title: String, blocks: List<Block>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var reload by remember { mutableIntStateOf(0) }
    var state by remember(slug, viewAttempt) { mutableStateOf<PState>(PState.Loading) }
    val answers = remember(slug) { mutableStateMapOf<String, Any>() }
    val errors = remember(slug) { mutableStateMapOf<String, String>() }
    var saveNote by remember { mutableStateOf("") }
    var tick by remember { mutableIntStateOf(0) }
    var submitting by remember { mutableStateOf(false) }
    var banner by remember { mutableStateOf<String?>(null) }
    var syncKey by remember { mutableIntStateOf(0) }

    BackHandler(enabled = active) { onBack() }
    StatusBarIcons(darkIcons = true, active = active)

    fun startForm(activity: PActivity, offline: Boolean) {
        val d = PracticeStore.draft(slug)
        answers.clear()
        answers.putAll(d.answers)
        errors.clear()
        banner = null
        saveNote = ""
        syncKey++
        state = PState.Form(activity, offline)
    }

    LaunchedEffect(slug, viewAttempt, reload) {
        state = PState.Loading
        try {
            if (viewAttempt > 0) {
                val o = Auth.api("GET", "/api/v1/practice/attempts/$viewAttempt/")
                state = PState.Done(PracticeStore.parseActivity(o), PracticeStore.answersFrom(o.optJSONObject("answers")), o.optString("submitted_at"))
            } else {
                val loaded = PracticeStore.activity(slug)
                val act = loaded.value
                val d = PracticeStore.draft(slug)
                val hasWork = d.answers.isNotEmpty() || act.openAttemptId != 0
                if (act.lastSubmittedId != 0 && !hasWork) state = PState.Intro(act) else startForm(act, loaded.offline)
            }
        } catch (e: ApiError) {
            state = PState.Failed(e.message ?: "Could not open this practice.")
        }
    }

    // When the form opens, join this phone's answers with the attempt open on the website.
    val formState = state as? PState.Form
    LaunchedEffect(formState) {
        if (formState != null) {
            try {
                val d = PracticeStore.begin(slug)
                answers.clear()
                answers.putAll(d.answers)
                syncKey++
                val r = PracticeStore.flush(slug)
                errors.putAll(r.errors)
                saveNote = if (r.offline) "Saved on this phone. Will send when you are online." else ""
            } catch (e: ApiError) {
                if (e.status == 0) saveNote = "Offline. Your answers are saved on this phone." else banner = e.message
            }
        }
    }

    // Send answers a moment after the person stops typing.
    LaunchedEffect(tick) {
        if (tick > 0 && state is PState.Form) {
            saveNote = "Saving…"
            delay(700)
            try {
                val r = PracticeStore.flush(slug)
                for (k in errors.keys.toList()) if (k !in r.errors) errors.remove(k)
                errors.putAll(r.errors)
                saveNote = if (r.offline) "Saved on this phone. Will send when you are online." else "All changes saved"
            } catch (e: ApiError) {
                saveNote = "Saved on this phone."
            }
        }
    }

    DisposableEffect(slug) {
        onDispose { PracticeStore.flushLater(slug) }
    }

    fun change(q: PQuestion, value: Any?) {
        if (value == null) answers.remove(q.id) else answers[q.id] = value
        PracticeStore.setAnswer(slug, q.id, value)
        errors.remove(q.id)
        banner = null
        tick++
    }

    fun submit(act: PActivity) {
        val missing = act.questions.filter { q ->
            if (!q.required) false else {
                val v = answers[q.id]
                v == null || (v is String && v.isBlank()) || (v is List<*> && v.isEmpty())
            }
        }
        if (missing.isNotEmpty()) {
            for (q in missing) errors[q.id] = "This answer is required."
            banner = if (missing.size == 1) "One required answer is missing." else "${missing.size} required answers are missing."
            return
        }
        submitting = true
        banner = null
        scope.launch {
            val snapshot = HashMap(answers)
            val out = PracticeStore.submit(slug)
            submitting = false
            if (out.ok) {
                state = PState.Done(act, snapshot, NoteStore.nowIso())
            } else {
                errors.putAll(out.errors)
                banner = out.message ?: "Could not submit. Please check your answers."
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Color.White).imePadding()) {
        Row(
            Modifier.fillMaxWidth().background(Mali.TealTint).statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackCircle(onBack, circleAlpha = 1f)
            Column(Modifier.weight(1f)) {
                Text("PRACTICE", fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.ExtraBold, color = Mali.TealDeep)
                val title = when (val s = state) {
                    is PState.Intro -> s.activity.title
                    is PState.Form -> s.activity.title
                    is PState.Done -> s.activity.title
                    else -> ""
                }
                if (title.isNotBlank()) Text(title, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink, maxLines = 2)
            }
        }
        when (val s = state) {
            is PState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Opening the practice…", color = Mali.InkSoft, fontSize = 16.sp)
            }
            is PState.Failed -> MessageBlock(
                title = "Could not open this practice",
                body = s.message,
                action = "Try again",
                onAction = { reload++ },
            )
            is PState.Intro -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                IntroBlock(s.activity)
                Text(
                    "Last finished: " + NoteStore.ago(s.activity.lastSubmittedAt).ifBlank { "earlier" } +
                        if (s.activity.submittedCount > 1) ". You have done it ${s.activity.submittedCount} times." else ".",
                    fontSize = 15.sp, lineHeight = 22.sp, color = Mali.Ink,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Mali.Green.copy(alpha = 0.08f)).padding(16.dp),
                )
                BigButton("View my answers", Mali.Teal) {
                    val o = s.activity
                    scope.launch {
                        state = PState.Loading
                        try {
                            val r = Auth.api("GET", "/api/v1/practice/attempts/${o.lastSubmittedId}/")
                            state = PState.Done(o, PracticeStore.answersFrom(r.optJSONObject("answers")), r.optString("submitted_at"))
                        } catch (e: ApiError) {
                            state = PState.Failed(e.message ?: "Could not open your answers.")
                        }
                    }
                }
                BigButton("Try again", Mali.Amber) { startForm(s.activity, false) }
            }
            is PState.Form -> {
                val act = s.activity
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    IntroBlock(act)
                    if (s.offline) PracticeNote("Showing the copy saved on your phone. You can answer now and submit when you are online.")
                    act.questions.forEachIndexed { i, q ->
                        QuestionCard(i + 1, q, answers[q.id], errors[q.id], syncKey) { change(q, it) }
                    }
                    if (act.tools.isNotEmpty()) {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Mali.TealTint).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("TOOLS", fontSize = 12.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.ExtraBold, color = Mali.TealDeep)
                            act.tools.forEach { t ->
                                ActionChip(t.label, color = Mali.TealDeep) { onOpenCalculator(t.model, act.slug, act.title) }
                            }
                            Text("Results from the calculator are saved in a note for this practice.", fontSize = 13.sp, color = Mali.InkSoft)
                        }
                    }
                    if (act.safety.isNotBlank()) PracticeNote(act.safety)
                    Spacer(Modifier.height(4.dp))
                }
                Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 10.dp).navigationBarsPadding()) {
                    val b = banner
                    if (b != null) Text(b, fontSize = 14.sp, color = Mali.Pink, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    if (saveNote.isNotBlank()) Text(saveNote, fontSize = 12.sp, color = Mali.InkSoft, modifier = Modifier.padding(bottom = 6.dp))
                    BigButton(if (submitting) "Submitting…" else "Submit my answers", Mali.Teal, enabled = !submitting) { submit(act) }
                }
            }
            is PState.Done -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Submitted ✓" + NoteStore.ago(s.submittedAt).let { if (it.isBlank()) "" else " · $it" },
                    fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Green,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Mali.Green.copy(alpha = 0.10f)).padding(14.dp),
                )
                s.activity.questions.forEach { q ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Mali.Cream).padding(14.dp)) {
                        Text(q.label, fontSize = 13.sp, lineHeight = 18.sp, color = Mali.InkSoft, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(PracticeStore.answerText(q, s.answers[q.id]), fontSize = 16.sp, lineHeight = 22.sp, color = Mali.Ink)
                    }
                }
                Spacer(Modifier.height(4.dp))
                BigButton("Copy my answers into My plan", Mali.Teal) {
                    onCopyToPlan("Practice: " + s.activity.title, answerBlocks(s.activity, s.answers, s.submittedAt))
                }
                BigButton("Try again", Mali.Amber) { startForm(s.activity, false) }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun answerBlocks(activity: PActivity, answers: Map<String, Any>, submittedAt: String): List<Block> {
    val out = ArrayList<Block>()
    out.add(Block(type = "heading", text = activity.title, level = 2))
    val when_ = NoteStore.ago(submittedAt)
    out.add(Block(text = "My answers" + if (when_.isBlank()) "" else " ($when_)"))
    activity.questions.forEach { q ->
        if (answers[q.id] != null) out.add(Block(text = "**${q.label}** " + PracticeStore.answerText(q, answers[q.id])))
    }
    return out
}

@Composable
private fun IntroBlock(a: PActivity) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (a.instructions.isNotBlank()) Text(a.instructions, fontSize = 15.sp, lineHeight = 23.sp, color = Mali.Ink)
        if (a.prompt.isNotBlank()) {
            Text(
                a.prompt, fontSize = 15.sp, lineHeight = 23.sp, color = Mali.Ink, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Mali.Cream).padding(16.dp),
            )
        }
    }
}

@Composable
private fun PracticeNote(text: String) {
    Text(
        text, fontSize = 13.sp, lineHeight = 19.sp, color = Mali.InkSoft,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Mali.Amber.copy(alpha = 0.08f)).padding(12.dp),
    )
}

@Composable
private fun BigButton(text: String, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text,
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(if (enabled) color else color.copy(alpha = 0.45f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
    )
}

@Composable
private fun QuestionCard(number: Int, q: PQuestion, value: Any?, error: String?, syncKey: Int, onChange: (Any?) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (error != null) Mali.Pink.copy(alpha = 0.06f) else Mali.Cream)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "$number. ${q.label}" + if (q.required) "" else " (optional)",
            fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink,
        )
        when (q.type) {
            "short_text", "long_text" -> {
                val text = (value as? String).orEmpty()
                val limit = if (q.maxLength > 0) q.maxLength else if (q.type == "short_text") 280 else 10_000
                OutlinedTextField(
                    value = text,
                    onValueChange = { t -> val c = t.take(limit); onChange(if (c.isEmpty()) null else c) },
                    singleLine = q.type == "short_text",
                    minLines = if (q.type == "long_text") 4 else 1,
                    isError = error != null,
                    supportingText = { Text("${text.length}/$limit") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
            }
            "integer" -> {
                var text by remember(q.id, syncKey) { mutableStateOf(value?.toString().orEmpty()) }
                OutlinedTextField(
                    value = text,
                    onValueChange = { raw ->
                        val t = raw.filterIndexed { i, c -> c.isDigit() || (i == 0 && c == '-') }.take(12)
                        text = t
                        onChange(t.toIntOrNull())
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = error != null,
                    supportingText = {
                        val range = listOfNotNull(q.min?.let { "from $it" }, q.max?.let { "up to $it" }).joinToString(" ")
                        if (range.isNotBlank()) Text("A whole number $range")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
            }
            "decimal_amount" -> {
                var text by remember(q.id, syncKey) { mutableStateOf(value?.toString().orEmpty()) }
                OutlinedTextField(
                    value = text,
                    onValueChange = { raw ->
                        val t = raw.filterIndexed { i, c -> c.isDigit() || c == '.' || (i == 0 && c == '-') }.take(18)
                        text = t
                        onChange(if (t.isBlank() || t == "-" || t == ".") null else t)
                    },
                    singleLine = true,
                    prefix = { Text("KSh ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = error != null,
                    supportingText = { if (text.isNotBlank() && error == null) Text(Fmt.kes(text)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
            }
            "single_choice" -> q.options.forEach { (id, label) ->
                ChoiceRow(label, selected = value == id, square = false) { onChange(id) }
            }
            "multiple_choice" -> {
                val picked = (value as? List<*>)?.map { it.toString() }.orEmpty()
                q.options.forEach { (id, label) ->
                    ChoiceRow(label, selected = id in picked, square = true) {
                        val next = if (id in picked) picked - id else picked + id
                        onChange(if (next.isEmpty()) null else next)
                    }
                }
            }
        }
        if (error != null) Text(error, fontSize = 13.sp, color = Mali.Pink, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, square: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Mali.Teal.copy(alpha = 0.12f) else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(if (square) RoundedCornerShape(6.dp) else CircleShape)
                .background(if (selected) Mali.Teal else Mali.Sand),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text(label, fontSize = 15.sp, lineHeight = 20.sp, color = Mali.Ink, modifier = Modifier.weight(1f))
    }
}
