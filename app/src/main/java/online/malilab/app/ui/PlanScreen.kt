package online.malilab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.malilab.app.data.COLLECTIONS
import online.malilab.app.data.NOTE_TEMPLATES
import online.malilab.app.data.NoteEntry
import online.malilab.app.data.NoteStore
import online.malilab.app.ui.theme.Mali

private const val FAVOURITES = "★ Favourites"

/** The Plan tab: every note the person has written, with search, collections and a button to start a new one. */
@Composable
fun PlanScreen(active: Boolean, onOpen: (String) -> Unit) {
    StatusBarIcons(darkIcons = false, active = active)
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("") }
    var picker by remember { mutableStateOf(false) }

    LaunchedEffect(active) { if (active) NoteStore.refresh() }

    val all = NoteStore.entries
    val q = query.trim().lowercase()
    val shown = all.filter { e ->
        (filter.isEmpty() || (filter == FAVOURITES && e.favourite) || e.collection == filter) &&
            (q.isEmpty() || e.title.lowercase().contains(q) || e.snippet.lowercase().contains(q) || e.tags.any { it.lowercase().contains(q) })
    }

    Box(Modifier.fillMaxSize().background(Mali.Cream)) {
        LazyColumn(Modifier.fillMaxSize()) {
            item { ScreenHeader("Plan", "Write it down. Test the numbers. Decide.") }
            item {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .background(Mali.Surface)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Search, null, tint = Mali.InkSoft)
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) Text("Search your notes", color = Mali.InkSoft.copy(alpha = 0.6f), fontSize = 16.sp)
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it.replace("\n", "") },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 16.sp, color = Mali.Ink),
                                cursorBrush = SolidColor(Mali.Teal),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionChip("All", selected = filter.isEmpty()) { filter = "" }
                        ActionChip(FAVOURITES, selected = filter == FAVOURITES, color = Mali.Amber) { filter = if (filter == FAVOURITES) "" else FAVOURITES }
                        COLLECTIONS.forEach { c ->
                            ActionChip(c, selected = filter == c, color = Mali.accentFor(c)) { filter = if (filter == c) "" else c }
                        }
                    }
                    if (!NoteStore.online) {
                        Spacer(Modifier.height(10.dp))
                        Text("Offline. Showing notes saved on this phone; changes are sent when you are back online.", fontSize = 13.sp, lineHeight = 18.sp, color = Mali.Amber)
                    }
                }
            }
            if (shown.isEmpty()) {
                item {
                    EmptyPlan(hasNotes = all.isNotEmpty(), onNew = { picker = true })
                }
            } else {
                items(shown, key = { it.uid }) { e ->
                    NoteCard(e) { onOpen(e.uid) }
                }
            }
            item { Spacer(Modifier.height(120.dp)) }
        }

        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(18.dp)
                .shadow(8.dp, RoundedCornerShape(50))
                .clip(RoundedCornerShape(50))
                .background(Brush.linearGradient(listOf(Mali.Teal, Mali.TealLight)))
                .clickable { picker = true }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Add, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("New note", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }

        if (picker) {
            TemplatePicker(
                onPick = { id ->
                    picker = false
                    val t = online.malilab.app.data.templateById(id)
                    onOpen(NoteStore.create(template = t.id, title = t.title, blocks = t.build(), collection = t.collection))
                },
                onClose = { picker = false },
            )
        }
    }
}

@Composable
private fun EmptyPlan(hasNotes: Boolean, onNew: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (hasNotes) "Nothing matches" else "Your plan starts here",
            fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (hasNotes) "Try a different search or collection."
            else "Write down what you want, test it with real numbers, and end with a plan you can follow.",
            fontSize = 16.sp, lineHeight = 23.sp, color = Mali.InkSoft,
        )
        if (!hasNotes) {
            Spacer(Modifier.height(18.dp))
            Text(
                "Start a note",
                color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Mali.Teal)
                    .clickable(onClick = onNew)
                    .padding(horizontal = 26.dp, vertical = 13.dp),
            )
        }
    }
}

@Composable
private fun NoteCard(e: NoteEntry, onClick: () -> Unit) {
    val accent = Mali.accentFor(e.collection.ifBlank { e.uid })
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Mali.Surface)
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.width(6.dp).height(110.dp).background(accent))
        Column(Modifier.weight(1f).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    e.title.ifBlank { "Untitled note" },
                    fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                if (e.favourite) Text("★", color = Mali.Amber, fontSize = 18.sp)
            }
            if (e.snippet.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(e.snippet, fontSize = 14.sp, lineHeight = 20.sp, color = Mali.InkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
            val bits = listOfNotNull(
                e.collection.ifBlank { null },
                NoteStore.ago(e.updatedAt).ifBlank { null },
                if (e.dirty) "Not sent yet" else null,
            )
            Text(bits.joinToString("  ·  "), fontSize = 12.sp, color = if (e.dirty) Mali.Amber else accent, fontWeight = FontWeight.Bold)
            if (e.tags.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(e.tags.joinToString("  ") { "#$it" }, fontSize = 12.sp, color = Mali.InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun TemplatePicker(onPick: (String) -> Unit, onClose: () -> Unit) {
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
                .padding(vertical = 20.dp),
        ) {
            Text("Start a note", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink, modifier = Modifier.padding(horizontal = 22.dp))
            Text(
                "Pick a shape to start from. You can change everything.",
                fontSize = 15.sp, color = Mali.InkSoft, modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            Column(Modifier.height(380.dp).verticalScroll(rememberScrollState())) {
                NOTE_TEMPLATES.forEach { t ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Mali.Surface)
                            .clickable { onPick(t.id) }
                            .padding(14.dp),
                    ) {
                        Text(t.name, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Mali.accentFor(t.id))
                        Text(t.blurb, fontSize = 14.sp, color = Mali.InkSoft)
                    }
                }
            }
        }
    }
}
