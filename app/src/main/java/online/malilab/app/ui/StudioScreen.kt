package online.malilab.app.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import online.malilab.app.data.ApiError
import online.malilab.app.data.Block
import online.malilab.app.data.ANALYSIS_MODELS
import online.malilab.app.data.INSTRUMENT_MODELS
import online.malilab.app.data.Evidence
import online.malilab.app.data.Note
import online.malilab.app.data.NoteStore
import online.malilab.app.data.PushResult
import online.malilab.app.ui.theme.Mali

private enum class SaveState(val label: String, val color: Color) {
    Saving("Saving…", Color.White),
    Saved("Saved", Color(0xFFB8F0D0)),
    Offline("Offline changes", Color(0xFFFFD9A0)),
    Issue("Sync issue", Color(0xFFFFB3C8)),
}

private class StudioMemory {
    var job: Job? = null
    var lastSnapshot = 0L
    var pending = false
    var deleted = false
}

private typealias Snapshot = Pair<String, List<Block>>

/**
 * The full-screen workspace for one note. Writing is saved on the phone at once and sent to the account a moment later,
 * so nothing is lost when the connection drops.
 */
@Composable
fun StudioScreen(uid: String, onClose: () -> Unit, onSwitch: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val memory = remember(uid) { StudioMemory() }
    val undo = remember(uid) { mutableStateListOf<Snapshot>() }
    val redo = remember(uid) { mutableStateListOf<Snapshot>() }

    var doc by remember(uid) { mutableStateOf<Note?>(null) }
    var loadFailed by remember(uid) { mutableStateOf(false) }
    var status by remember(uid) { mutableStateOf(SaveState.Saved) }
    var focusedId by remember(uid) { mutableStateOf<String?>(null) }
    var pendingFocus by remember(uid) { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var showMeta by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var labOpen by remember(uid) { mutableStateOf(false) }
    var labSeed by remember(uid) { mutableStateOf<CalcSeed?>(null) }
    var conflictCopy by remember(uid) { mutableStateOf<String?>(null) }
    var message by remember(uid) { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    var dragId by remember(uid) { mutableStateOf<String?>(null) }
    var dragDy by remember(uid) { mutableFloatStateOf(0f) }

    StatusBarIcons(darkIcons = false)

    // ---- saving ---------------------------------------------------------------------------------------------
    fun flushLocal() {
        val d = doc ?: return
        if (memory.pending && !memory.deleted) {
            NoteStore.update(d)
            memory.pending = false
        }
    }

    fun applyResult(r: PushResult) {
        when (r) {
            PushResult.Synced, PushResult.Idle -> status = SaveState.Saved
            PushResult.Offline -> status = SaveState.Offline
            is PushResult.Failed -> { status = SaveState.Issue; message = r.message }
            is PushResult.Conflict -> { status = SaveState.Issue; conflictCopy = r.copyUid }
        }
    }

    fun scheduleSave() {
        status = SaveState.Saving
        memory.pending = true
        memory.job?.cancel()
        memory.job = scope.launch {
            delay(500)
            flushLocal()
            delay(700)
            applyResult(NoteStore.push(uid))
        }
    }

    fun commit(next: Note, structural: Boolean) {
        val cur = doc ?: return
        val now = System.currentTimeMillis()
        if (structural || now - memory.lastSnapshot > 1500) {
            undo.add(cur.title to cur.blocks)
            if (undo.size > 60) undo.removeAt(0)
            redo.clear()
            memory.lastSnapshot = now
        }
        doc = next
        scheduleSave()
    }

    fun setBlocks(list: List<Block>, structural: Boolean) {
        val d = doc ?: return
        commit(d.copy(blocks = list), structural)
    }

    LaunchedEffect(uid) {
        val n = NoteStore.open(uid)
        if (n == null) {
            loadFailed = true
        } else {
            doc = n
            if (n.title.isBlank() && n.blocks.all { it.type == "paragraph" && it.text.isBlank() }) pendingFocus = "title"
            if (n.dirty) {
                status = SaveState.Saving
                applyResult(NoteStore.push(uid))
            }
        }
    }

    DisposableEffect(uid) {
        onDispose {
            memory.job?.cancel()
            flushLocal()
            if (!memory.deleted) NoteStore.pushLater(uid)
        }
    }

    // ---- editing --------------------------------------------------------------------------------------------
    fun addBlock(type: String) {
        val d = doc ?: return
        val fresh = when (type) {
            "heading" -> Block(type = "heading", level = 2)
            "list" -> Block(type = "list", items = listOf(""))
            "quote" -> Block(type = "quote")
            "callout" -> Block(type = "callout", tone = "tip")
            "divider" -> Block(type = "divider")
            "source" -> Evidence.source()
            else -> Block()
        }
        val idx = d.blocks.indexOfFirst { it.id == focusedId }
        val list = d.blocks.toMutableList()
        val current = list.getOrNull(idx)
        if (current != null && current.type == "paragraph" && current.text.isBlank() && type != "paragraph") {
            list[idx] = fresh
        } else {
            list.add(if (idx >= 0) idx + 1 else list.size, fresh)
        }
        setBlocks(list, true)
        focusedId = fresh.id
        pendingFocus = if (type == "divider") null else fresh.id
    }

    /** Puts calculator results into the note after the block being edited, or at the end, and leaves room to keep writing. */
    fun insertBlocks(blocks: List<Block>) {
        val d = doc ?: return
        if (blocks.isEmpty()) return
        val idx = d.blocks.indexOfFirst { it.id == focusedId }
        val list = d.blocks.toMutableList()
        val at = if (idx >= 0) idx + 1 else list.size
        list.addAll(at, blocks)
        if (at + blocks.size >= list.size) list.add(Block())
        setBlocks(list, true)
        focusedId = blocks.last().id
    }

    fun openCalculator(block: Block?) {
        val calc = block?.raw?.optJSONObject("calc")
        val inputs = calc?.optJSONObject("inputs")
        labSeed = if (inputs != null) {
            val model = calc.optString("model")
            CalcSeed(
                inputs, calc.optString("solve").ifBlank { null },
                if (model in INSTRUMENT_MODELS) model else "compound",
                if (model in ANALYSIS_MODELS) model else null,
            )
        } else null
        labOpen = true
    }

    fun move(id: String, delta: Int) {
        val d = doc ?: return
        val i = d.blocks.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in d.blocks.indices) return
        val list = d.blocks.toMutableList()
        val b = list.removeAt(i)
        list.add(j, b)
        setBlocks(list, true)
    }

    // ---- drag to reorder ------------------------------------------------------------------------------------
    fun startDrag(id: String) {
        val cur = doc ?: return
        undo.add(cur.title to cur.blocks)
        if (undo.size > 60) undo.removeAt(0)
        redo.clear()
        memory.lastSnapshot = Long.MAX_VALUE / 2
        dragId = id
        dragDy = 0f
        focusedId = id
    }

    fun endDrag() {
        dragId = null
        dragDy = 0f
        memory.lastSnapshot = System.currentTimeMillis()
        val cur = doc
        val before = undo.lastOrNull()
        if (cur != null && before != null && before.second.map { it.id } == cur.blocks.map { it.id }) undo.removeAt(undo.lastIndex)
    }

    /** Swaps the dragged block with the one it has been dragged over, keeping it under the finger. */
    fun swapStep() {
        val id = dragId ?: return
        val d = doc ?: return
        val info = listState.layoutInfo.visibleItemsInfo
        val cur = info.firstOrNull { it.key == id } ?: return
        val center = cur.offset + dragDy + cur.size / 2f
        val target = info.firstOrNull { it.key != id && it.key != "title" && it.key != "end" && center >= it.offset && center <= it.offset + it.size } ?: return
        val from = d.blocks.indexOfFirst { it.id == id }
        val to = d.blocks.indexOfFirst { it.id == target.key }
        if (from < 0 || to < 0 || from == to) return
        val shift = if (to > from) (target.offset + target.size) - (cur.offset + cur.size) else target.offset - cur.offset
        val list = d.blocks.toMutableList()
        list.add(to, list.removeAt(from))
        commit(d.copy(blocks = list), false)
        dragDy -= shift
    }

    LaunchedEffect(dragId) {
        if (dragId == null) return@LaunchedEffect
        while (dragId != null) {
            val li = listState.layoutInfo
            val cur = li.visibleItemsInfo.firstOrNull { it.key == dragId }
            if (cur != null) {
                val center = cur.offset + dragDy + cur.size / 2f
                val amount = if (center < li.viewportStartOffset + 120f) -18f else if (center > li.viewportEndOffset - 120f) 18f else 0f
                if (amount != 0f) dragDy += listState.scrollBy(amount)
            }
            swapStep()
            delay(16)
        }
    }

    fun duplicate(id: String) {
        val d = doc ?: return
        val i = d.blocks.indexOfFirst { it.id == id }
        if (i < 0) return
        val copy = d.blocks[i].copy(id = UUID.randomUUID().toString())
        val list = d.blocks.toMutableList().apply { add(i + 1, copy) }
        setBlocks(list, true)
        focusedId = copy.id
    }

    fun remove(id: String) {
        val d = doc ?: return
        val list = d.blocks.filter { it.id != id }.ifEmpty { listOf(Block()) }
        setBlocks(list, true)
        focusedId = null
    }

    fun tapEnd() {
        val d = doc ?: return
        val last = d.blocks.lastOrNull()
        if (last != null && last.type == "paragraph") {
            focusedId = last.id
            pendingFocus = last.id
        } else {
            focusedId = last?.id
            addBlock("paragraph")
        }
    }

    fun undoNow() {
        val prev = undo.lastOrNull() ?: return
        val cur = doc ?: return
        undo.removeAt(undo.lastIndex)
        redo.add(cur.title to cur.blocks)
        doc = cur.copy(title = prev.first, blocks = prev.second)
        scheduleSave()
    }

    fun redoNow() {
        val next = redo.lastOrNull() ?: return
        val cur = doc ?: return
        redo.removeAt(redo.lastIndex)
        undo.add(cur.title to cur.blocks)
        doc = cur.copy(title = next.first, blocks = next.second)
        scheduleSave()
    }

    fun close() {
        memory.job?.cancel()
        flushLocal()
        onClose()
    }

    BackHandler(enabled = true) { close() }

    // ---- screen ---------------------------------------------------------------------------------------------
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(Mali.Cream).imePadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Mali.TealTop)
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackCircle(onBack = { close() })
            Spacer(Modifier.width(10.dp))
            Text(
                status.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = status.color,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.16f))
                    .clickable(enabled = status == SaveState.Issue || status == SaveState.Offline) {
                        status = SaveState.Saving
                        scope.launch { flushLocal(); applyResult(NoteStore.push(uid)) }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
            Spacer(Modifier.weight(1f))
            BarText("↶", "Undo", enabled = undo.isNotEmpty()) { undoNow() }
            BarText("↷", "Redo", enabled = redo.isNotEmpty()) { redoNow() }
            val fav = doc?.favourite == true
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(enabled = doc != null) { doc?.let { commit(it.copy(favourite = !it.favourite), true) } }
                    .semantics { contentDescription = if (fav) "Remove from favourites" else "Add to favourites" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Star, null, tint = if (fav) Color(0xFFFFD36B) else Color.White.copy(alpha = 0.55f))
            }
            Box {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { menuOpen = true }
                        .semantics { contentDescription = "More" },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.MoreVert, null, tint = Color.White) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = Color.White) {
                    MenuRow("Collection and tags") { menuOpen = false; showMeta = true }
                    MenuRow("Version history") { menuOpen = false; memory.job?.cancel(); flushLocal(); showHistory = true }
                    MenuRow("Duplicate note") {
                        menuOpen = false
                        memory.job?.cancel()
                        flushLocal()
                        NoteStore.duplicate(uid)?.let { onSwitch(it) }
                    }
                    MenuRow("Share as Markdown") {
                        menuOpen = false
                        memory.job?.cancel()
                        flushLocal()
                        scope.launch {
                            try {
                                val md = NoteStore.export(uid)
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, doc?.title.orEmpty())
                                    putExtra(Intent.EXTRA_TEXT, md)
                                }
                                context.startActivity(Intent.createChooser(send, "Share note"))
                            } catch (e: ApiError) {
                                message = if (e.status == 0) "Connect to the internet to export this note." else e.message
                            }
                        }
                    }
                    MenuRow("Save as PDF") {
                        menuOpen = false
                        memory.job?.cancel()
                        flushLocal()
                        val d = doc
                        if (d != null) scope.launch {
                            try {
                                val file = withContext(Dispatchers.Default) { NotePdf.build(context, d) }
                                NotePdf.share(context, file, d.title)
                            } catch (e: Exception) {
                                message = "Could not make the PDF: " + (e.message ?: "unknown problem")
                            }
                        }
                    }
                    MenuRow("Delete note", color = Mali.Pink) { menuOpen = false; showDelete = true }
                }
            }
        }

        message?.let { text ->
            Text(
                "$text   (tap to hide)",
                fontSize = 14.sp,
                color = Mali.Amber,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Mali.Amber.copy(alpha = 0.12f))
                    .clickable { message = null }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }

        val d = doc
        when {
            loadFailed -> Column(Modifier.weight(1f).fillMaxWidth().padding(32.dp), verticalArrangement = Arrangement.Center) {
                Text("This note could not be opened", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                Spacer(Modifier.height(6.dp))
                Text("It may have been deleted on another device.", color = Mali.InkSoft)
            }
            d == null -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Opening your note…", color = Mali.InkSoft)
            }
            else -> {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState) {
                    item(key = "title") {
                        StudioField(
                            value = d.title,
                            onValue = { commit(d.copy(title = it.replace("\n", " ").take(200)), false) },
                            style = TextStyle(fontSize = 30.sp, lineHeight = 37.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink),
                            placeholder = "Title",
                            blockId = "title",
                            pendingFocus = pendingFocus,
                            onFocusConsumed = { pendingFocus = null },
                            onFocus = { focusedId = null },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                        )
                    }
                    items(d.blocks, key = { it.id }) { b ->
                        val i = d.blocks.indexOfFirst { it.id == b.id }
                        NoteBlockView(
                            block = b,
                            focused = focusedId == b.id,
                            pendingFocus = pendingFocus,
                            onFocusConsumed = { pendingFocus = null },
                            onFocus = { focusedId = b.id },
                            onEdit = { nb -> setBlocks(d.blocks.map { if (it.id == nb.id) nb else it }, false) },
                            onRestyle = { nb -> setBlocks(d.blocks.map { if (it.id == nb.id) nb else it }, true) },
                            onMoveUp = if (i > 0) ({ move(b.id, -1) }) else null,
                            onMoveDown = if (i < d.blocks.lastIndex) ({ move(b.id, 1) }) else null,
                            onDuplicate = { duplicate(b.id) },
                            onDelete = { remove(b.id) },
                            evidence = { blk, isFocused, replace ->
                                EvidenceBlock(blk, isFocused, replace, onOpenCalc = { openCalculator(it) })
                            },
                            modifier = if (dragId == b.id) Modifier.zIndex(1f).graphicsLayer { translationY = dragDy } else Modifier,
                            dragging = dragId == b.id,
                            onDragStart = { startDrag(b.id) },
                            onDragBy = { dy -> dragDy += dy },
                            onDragEnd = { endDrag() },
                        )
                    }
                    item(key = "end") {
                        Box(Modifier.fillMaxWidth().height(220.dp).clickable { tapEnd() })
                    }
                }
                StudioToolbar(onAdd = { if (it == "calc") openCalculator(null) else addBlock(it) })
            }
        }
    }
    if (labOpen) {
        CalculatorHub(
            seed = labSeed,
            onInsert = { insertBlocks(it) },
            onClose = { labOpen = false },
        )
    }
    }

    // ---- dialogs --------------------------------------------------------------------------------------------
    if (showMeta) {
        doc?.let { d ->
            MetaDialog(
                collection = d.collection,
                tags = d.tags,
                onApply = { c, t -> showMeta = false; commit(d.copy(collection = c, tags = t), true) },
                onDismiss = { showMeta = false },
            )
        }
    }
    if (showHistory) {
        HistoryDialog(
            uid = uid,
            onRestore = { v ->
                showHistory = false
                scope.launch {
                    try {
                        val restored = NoteStore.restore(uid, v.number)
                        doc = restored
                        undo.clear()
                        redo.clear()
                        focusedId = null
                        status = SaveState.Saved
                    } catch (e: ApiError) {
                        message = e.message
                    }
                }
            },
            onDismiss = { showHistory = false },
        )
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            containerColor = Mali.Cream,
            title = { Text("Delete this note?", fontWeight = FontWeight.ExtraBold, color = Mali.Ink) },
            text = { Text("It will be removed from all your devices.", color = Mali.InkSoft) },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    memory.deleted = true
                    memory.job?.cancel()
                    scope.launch {
                        NoteStore.delete(uid)
                        onClose()
                    }
                }) { Text("Delete", color = Mali.Pink, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Keep", color = Mali.InkSoft) } },
        )
    }
    conflictCopy?.let { copyUid ->
        AlertDialog(
            onDismissRequest = {},
            containerColor = Mali.Cream,
            title = { Text("Changed on another device", fontWeight = FontWeight.ExtraBold, color = Mali.Ink) },
            text = {
                Text(
                    "This note was edited somewhere else while you were writing. Nothing was lost: your edits are saved as a separate note called “(my edits)”.",
                    color = Mali.InkSoft, lineHeight = 22.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { conflictCopy = null; memory.deleted = false; onSwitch(copyUid) }) {
                    Text("Open my edits", color = Mali.Teal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    conflictCopy = null
                    NoteStore.peek(uid)?.let { doc = it; undo.clear(); redo.clear() }
                    status = SaveState.Saved
                }) { Text("Read the newer note", color = Mali.InkSoft) }
            },
        )
    }
}

@Composable
private fun BarText(glyph: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, fontSize = 22.sp, color = Color.White.copy(alpha = if (enabled) 1f else 0.35f))
    }
}

@Composable
private fun MenuRow(label: String, color: Color = Mali.Ink, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(label, color = color, fontWeight = FontWeight.SemiBold) }, onClick = onClick)
}

/** The row of "add a block" buttons that stays above the keyboard. */
@Composable
private fun StudioToolbar(onAdd: (String) -> Unit) {
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(50))
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionChip("Calculator", color = Mali.Teal, selected = true) { onAdd("calc") }
            ActionChip("Text") { onAdd("paragraph") }
            ActionChip("Heading") { onAdd("heading") }
            ActionChip("List") { onAdd("list") }
            ActionChip("Callout", color = Mali.Indigo) { onAdd("callout") }
            ActionChip("Quote", color = Mali.Purple) { onAdd("quote") }
            ActionChip("Source", color = Mali.Amber) { onAdd("source") }
            ActionChip("Line", color = Mali.InkSoft) { onAdd("divider") }
        }
    }
}

/** Shown for calculator blocks until they have their own display. Nothing in them is lost. */
@Composable
fun EvidenceFallback(block: Block) {
    val title = block.raw?.optString("title").orEmpty().ifBlank { block.type.replaceFirstChar { it.uppercase() } }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Mali.Indigo.copy(alpha = 0.08f))
            .padding(14.dp),
    ) {
        Text("CALCULATION · ${block.type.uppercase()}", fontSize = 12.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Indigo)
        Spacer(Modifier.height(4.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Mali.Ink)
    }
}
