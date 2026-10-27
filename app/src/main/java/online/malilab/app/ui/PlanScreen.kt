package online.malilab.app.ui

import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
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
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(active) { if (active) NoteStore.refresh() }

    val all = NoteStore.entries
    val q = query.trim().lowercase()
    val shown = all.filter { e ->
        (filter.isEmpty() || (filter == FAVOURITES && e.favourite) || e.collection == filter) &&
            (q.isEmpty() || e.title.lowercase().contains(q) || e.snippet.lowercase().contains(q) || e.tags.any { it.lowercase().contains(q) })
    }

    Box(Modifier.fillMaxSize().background(Mali.Cream)) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column {
                    ScreenHeader(
                        "Notes", "Write it down. Test the numbers. Decide.",
                        showProfile = false,
                        trailing = {
                            Box(
                                Modifier.size(44.dp).clip(CircleShape).clickable { menuOpen = !menuOpen }
                                    .semantics { contentDescription = if (menuOpen) "Close tags" else "Tags" },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (menuOpen) Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(30.dp))
                                else Icon(Icons.Filled.Menu, null, tint = Color.White, modifier = Modifier.size(30.dp))
                            }
                        },
                    )
                    if (menuOpen) {
                        Column(Modifier.fillMaxWidth().background(Color(0xFF0A4A43)).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            val entries = listOf("All", FAVOURITES) + COLLECTIONS
                            entries.forEach { name ->
                                val value = if (name == "All") "" else name
                                val on = filter == value
                                Text(
                                    name,
                                    fontSize = 17.sp,
                                    fontWeight = if (on) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = Color.White,
                                    modifier = Modifier
                                        .padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (on) Color.White.copy(alpha = 0.18f) else Color.Transparent)
                                        .clickable { filter = value; menuOpen = false }
                                        .padding(horizontal = 22.dp, vertical = 11.dp),
                                )
                            }
                        }
                    }
                }
            }
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
                    if (filter.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        ActionChip("$filter  ✕", selected = true, color = if (filter == FAVOURITES) Mali.Amber else Mali.accentFor(filter)) { filter = "" }
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
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(shape)
            .background(Mali.Surface)
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.40f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
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
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                e.collection.ifBlank { "Note" },
                fontSize = 12.sp, color = accent, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            val right = listOfNotNull(NoteStore.ago(e.updatedAt).ifBlank { null }, if (e.dirty) "Not sent yet" else null).joinToString("  ·  ")
            Text(right, fontSize = 12.sp, fontStyle = FontStyle.Italic, color = if (e.dirty) Mali.Amber else Mali.InkSoft)
        }
        if (e.tags.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(e.tags.joinToString("  ") { "#$it" }, fontSize = 12.sp, color = Mali.InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
