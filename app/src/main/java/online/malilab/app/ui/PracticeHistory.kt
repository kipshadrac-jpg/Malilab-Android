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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.malilab.app.data.ApiError
import online.malilab.app.data.NoteStore
import online.malilab.app.data.PHistoryItem
import online.malilab.app.data.PracticeStore
import online.malilab.app.ui.theme.Mali

/** Everything the learner has started or finished in practice, newest first. */
@Composable
fun PracticeHistoryScreen(active: Boolean, onBack: () -> Unit, onOpen: (slug: String, attemptId: Int, submitted: Boolean) -> Unit) {
    var reload by remember { mutableIntStateOf(0) }
    var items by remember { mutableStateOf<List<PHistoryItem>?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = active) { onBack() }
    StatusBarIcons(darkIcons = true, active = active)

    LaunchedEffect(reload, active) {
        if (active) {
            failure = null
            try {
                items = PracticeStore.history()
            } catch (e: ApiError) {
                failure = e.message
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().background(Mali.TealTint).statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackCircle(onBack, circleAlpha = 1f)
            Column(Modifier.weight(1f)) {
                Text("MY PRACTICE", fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.ExtraBold, color = Mali.TealDeep)
                Text("Your answers", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
            }
        }
        val list = items
        when {
            failure != null && list == null -> MessageBlock(
                title = "Could not load your practice",
                body = failure ?: "",
                action = "Try again",
                onAction = { reload++ },
            )
            list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading…", color = Mali.InkSoft, fontSize = 16.sp)
            }
            list.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Nothing here yet. Open a module and try one of its practice activities.",
                    color = Mali.InkSoft, fontSize = 16.sp, lineHeight = 23.sp,
                )
            }
            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                list.forEach { a ->
                    val submitted = a.state == "submitted"
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (submitted) Mali.Green.copy(alpha = 0.07f) else Mali.Amber.copy(alpha = 0.08f))
                            .clickable { onOpen(a.slug, a.id, submitted) }
                            .padding(16.dp),
                    ) {
                        Text(
                            if (submitted) "SUBMITTED" else "NOT FINISHED",
                            fontSize = 11.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (submitted) Mali.Green else Mali.Amber,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(a.title, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                        Spacer(Modifier.height(2.dp))
                        val whenText = NoteStore.ago(if (submitted) a.submittedAt else a.startedAt)
                        Text(
                            (if (submitted) "Finished " else "Started ") + whenText.lowercase() + " · ${a.answered} answered",
                            fontSize = 13.sp, color = Mali.InkSoft,
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
