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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import online.malilab.app.data.ApiError
import online.malilab.app.data.Block
import online.malilab.app.data.COMPARE_SPEC
import online.malilab.app.data.CalcStore
import online.malilab.app.data.INSTRUMENTS
import online.malilab.app.data.NoteStore
import online.malilab.app.data.SavedCalc
import online.malilab.app.ui.theme.Mali
import org.json.JSONObject
import java.util.UUID

private fun modelTitle(model: String): String = when (model) {
    "compound" -> "Savings & growth"
    COMPARE_SPEC.model -> COMPARE_SPEC.title
    else -> INSTRUMENTS.firstOrNull { it.model == model }?.title ?: "Calculation"
}

/**
 * The calculator's front page: every named calculation the person has saved. Tapping one opens the calculator on it;
 * the plus button starts a new one.
 */
@Composable
fun CalcHome(onClose: () -> Unit) {
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var fresh by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { CalcStore.load() }

    val id = editing
    if (id != null) {
        androidx.compose.runtime.key(id) {
            CalcEditor(id, fresh, onClose = { editing = null; fresh = false })
        }
        return
    }

    BackHandler(enabled = true) { onClose() }
    StatusBarIcons(darkIcons = false)
    var deleting by remember { mutableStateOf<SavedCalc?>(null) }
    val saved = CalcStore.items

    Box(Modifier.fillMaxSize().background(Mali.Cream)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().background(Mali.TealTop).statusBarsPadding().padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackCircle(onBack = onClose, tint = Color.White)
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Calculator", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("Your saved calculations. Tap one to change it.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                }
            }
            if (saved.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(70.dp))
                    Icon(NavIcons.Calculator, null, tint = Mali.InkSoft.copy(alpha = 0.5f), modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Nothing saved yet", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tap + to start a calculation. Give it a name and it is kept here, so you can come back and change it any time. Calculations without a name are not kept.",
                        fontSize = 15.sp, lineHeight = 21.sp, color = Mali.InkSoft,
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 16.dp, 16.dp, 120.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(saved, key = { it.id }) { c ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Mali.Surface)
                                .clickable { fresh = false; editing = c.id }.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Mali.TealTint), contentAlignment = Alignment.Center) {
                                Icon(NavIcons.Calculator, null, tint = Mali.TealDeep, modifier = Modifier.size(24.dp))
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(c.name.ifBlank { "Untitled calculation" }, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    modelTitle(c.model) + "  ·  " + java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(c.updatedAt)),
                                    fontSize = 13.sp, color = Mali.InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                "Delete", fontSize = 13.sp, color = Mali.Pink, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(RoundedCornerShape(50)).clickable { deleting = c }.padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        }
        PlusButton(
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(18.dp),
        ) {
            editing = UUID.randomUUID().toString()
            fresh = true
        }
    }

    deleting?.let { c ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = Mali.Cream,
            title = { Text("Delete this calculation?", fontWeight = FontWeight.ExtraBold, color = Mali.Ink) },
            text = { Text("\"" + c.name.ifBlank { "Untitled calculation" } + "\" will be removed from the calculator.", color = Mali.Ink) },
            confirmButton = { TextButton(onClick = { CalcStore.delete(c.id); deleting = null }) { Text("Delete", color = Mali.Pink, fontWeight = FontWeight.ExtraBold) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Keep it", color = Mali.InkSoft) } },
        )
    }
}

/** The rounded-square plus used for adding things: a new plan on the portfolio, a new calculation here. */
@Composable
fun PlusButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .size(60.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Mali.OceanTop, Mali.OceanBottom)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Add, "Add", tint = Color.White, modifier = Modifier.size(30.dp))
    }
}

/** One calculation open in the full calculator, with its name at the top. Changes are kept as they are made. */
@Composable
private fun CalcEditor(id: String, fresh: Boolean, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val start = remember(id) { if (fresh) null else CalcStore.get(id) }
    var cur by remember(id) { mutableStateOf(start ?: SavedCalc(id, "", "compound", JSONObject(), null, System.currentTimeMillis())) }
    var name by remember(id) { mutableStateOf(start?.name.orEmpty()) }

    // A new calculation starts empty; one that is reopened starts from what was saved.
    val seed = remember(id) {
        if (start == null || start.inputs.length() == 0) {
            online.malilab.app.ui.LabMemory.inputs = null
            online.malilab.app.ui.LabMemory.solve = null
            online.malilab.app.ui.LabMemory.instruments.clear()
            online.malilab.app.ui.LabMemory.model = start?.model ?: "compound"
            null
        } else CalcSeed(start.inputs, start.solve, start.model)
    }

    fun keep(c: SavedCalc) {
        cur = c
        // Only a calculation with a name is kept; take the name away and it is removed from the list.
        if (c.name.isNotBlank()) CalcStore.upsert(c.copy(updatedAt = System.currentTimeMillis())) else CalcStore.delete(c.id)
    }

    CalculatorHub(
        seed = seed,
        onInsert = { blocks -> addToCalcNote(scope, cur, blocks) { keep(cur.copy(noteUid = it)) } },
        onClose = onClose,
        name = name,
        onName = { n -> name = n; keep(cur.copy(name = n)) },
        onState = { m, i, s ->
            if (m != cur.model || i.toString() != cur.inputs.toString() || s != cur.solve) keep(cur.copy(model = m, inputs = i, solve = s))
        },
    )
}

// Anything inserted from a saved calculation goes into one note kept for it, made the first time.
private fun addToCalcNote(scope: kotlinx.coroutines.CoroutineScope, c: SavedCalc, blocks: List<Block>, remember: (String) -> Unit) {
    scope.launch {
        val existing = c.noteUid?.let {
            try {
                NoteStore.open(it)
            } catch (e: ApiError) {
                null
            }
        }
        if (existing != null) {
            NoteStore.update(existing.copy(blocks = existing.blocks + blocks))
            NoteStore.pushLater(existing.uid)
        } else {
            val uid = NoteStore.create(title = ("Calculator: " + c.name.ifBlank { "Untitled calculation" }).take(200), blocks = blocks)
            NoteStore.pushLater(uid)
            remember(uid)
        }
    }
}
