package online.malilab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import online.malilab.app.data.COLLECTIONS
import online.malilab.app.data.NoteStore
import online.malilab.app.data.VersionInfo
import online.malilab.app.ui.theme.Mali

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetaDialog(
    collection: String,
    tags: List<String>,
    onApply: (String, List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var coll by remember { mutableStateOf(collection) }
    var list by remember { mutableStateOf(tags) }
    var draft by remember { mutableStateOf("") }

    fun addDraft() {
        val t = draft.trim().take(30)
        if (t.isNotEmpty() && list.none { it.equals(t, ignoreCase = true) } && list.size < 20) list = list + t
        draft = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Mali.Cream,
        title = { Text("Collection and tags", fontWeight = FontWeight.ExtraBold, color = Mali.Ink) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("COLLECTION", fontSize = 12.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Teal)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    COLLECTIONS.forEach { c ->
                        ActionChip(c, selected = coll == c) { coll = if (coll == c) "" else c }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("TAGS", fontSize = 12.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Teal)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    list.forEach { t -> ActionChip("$t  ×", color = Mali.Indigo) { list = list - t } }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it.replace("\n", "") },
                        placeholder = { Text("New tag") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Mali.Teal, unfocusedBorderColor = Mali.Sand, cursorColor = Mali.Teal,
                            focusedContainerColor = Mali.Surface, unfocusedContainerColor = Mali.Surface,
                        ),
                    )
                    ActionChip("Add") { addDraft() }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { addDraft(); onApply(coll, list) }) { Text("Done", color = Mali.TealDeep, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Mali.InkSoft) } },
    )
}

@Composable
fun HistoryDialog(uid: String, onRestore: (VersionInfo) -> Unit, onDismiss: () -> Unit) {
    var versions by remember { mutableStateOf<List<VersionInfo>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf<VersionInfo?>(null) }

    LaunchedEffect(uid) {
        try {
            versions = NoteStore.history(uid)
        } catch (e: ApiError) {
            error = if (e.status == 0) "Connect to the internet to see saved versions." else e.message
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Mali.Cream,
        title = { Text("Version history", fontWeight = FontWeight.ExtraBold, color = Mali.Ink) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                val list = versions
                when {
                    error != null -> Text(error.orEmpty(), color = Mali.Pink, fontSize = 15.sp)
                    list == null -> Text("Loading…", color = Mali.InkSoft)
                    list.isEmpty() -> Text("No saved versions yet. They appear as you work.", color = Mali.InkSoft)
                    else -> {
                        Text(
                            "Restoring keeps your current note too, as a version you can come back to.",
                            fontSize = 14.sp, lineHeight = 20.sp, color = Mali.InkSoft,
                        )
                        Spacer(Modifier.height(10.dp))
                        list.forEach { v ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Mali.Surface)
                                    .clickable { confirm = v }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        v.title.ifBlank { "Untitled note" },
                                        fontWeight = FontWeight.Bold, color = Mali.Ink, fontSize = 15.sp, maxLines = 1,
                                    )
                                    val extra = if (v.label.isNotBlank()) " · ${v.label}" else ""
                                    Text(
                                        "${NoteStore.ago(v.createdAt)} · ${v.blockCount} blocks$extra",
                                        fontSize = 13.sp, color = Mali.InkSoft,
                                    )
                                }
                                Text("Restore", color = Mali.Teal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = Mali.TealDeep, fontWeight = FontWeight.Bold) } },
    )

    confirm?.let { v ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = Mali.Cream,
            title = { Text("Restore this version?", fontWeight = FontWeight.ExtraBold, color = Mali.Ink) },
            text = { Text("The note will go back to how it was ${NoteStore.ago(v.createdAt).lowercase()}.", color = Mali.InkSoft) },
            confirmButton = {
                TextButton(onClick = { confirm = null; onRestore(v) }) { Text("Restore", color = Mali.Teal, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel", color = Mali.InkSoft) } },
        )
    }
}
