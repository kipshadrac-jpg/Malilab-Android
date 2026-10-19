package online.malilab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import online.malilab.app.data.ApiError
import online.malilab.app.data.Block
import online.malilab.app.data.Fmt
import online.malilab.app.data.NoteStore
import online.malilab.app.ui.theme.Mali
import org.json.JSONArray
import org.json.JSONObject

private fun tagFor(type: String) = when (type) {
    "metric" -> "KEY NUMBER"
    "chart" -> "CHART"
    "table" -> "TABLE"
    "scenarios" -> "WHAT-IFS"
    "assumptions" -> "ASSUMPTIONS"
    "methodology" -> "METHOD"
    "interpretation" -> "EXPLANATION"
    "source" -> "SOURCE"
    else -> type.uppercase()
}

private fun strings(a: JSONArray?): List<String> = (0 until (a?.length() ?: 0)).map { a!!.optString(it) }

private fun edited(block: Block, change: (JSONObject) -> Unit): Block {
    val raw = JSONObject((block.raw ?: JSONObject()).toString())
    change(raw)
    return block.copy(raw = raw)
}

/**
 * A calculator result inside a note. A snapshot shows when it was captured and never changes; a live block remembers its
 * assumptions and can be refreshed. Either kind can be reopened in the calculator.
 */
@Composable
fun EvidenceBlock(block: Block, focused: Boolean, onReplace: (Block) -> Unit, onOpenCalc: (Block) -> Unit) {
    val raw = block.raw ?: return
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    val calc = raw.optJSONObject("calc")
    val live = raw.optString("mode") == "live"
    val canRefresh = live && calc?.optJSONObject("inputs") != null &&
        !(raw.optString("type") == "interpretation" && !raw.optBoolean("generated", true))
    val type = raw.optString("type")

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tagFor(type), fontSize = 11.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Indigo, modifier = Modifier.weight(1f))
            if (type != "source" && type != "interpretation") {
                Text(
                    if (live) "LIVE" else "SNAPSHOT",
                    fontSize = 10.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (live) Mali.Green else Mali.InkSoft,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background((if (live) Mali.Green else Mali.InkSoft).copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        val title = raw.optString("title")
        if (title.isNotBlank() && type != "metric") {
            Spacer(Modifier.height(4.dp))
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
        }
        Spacer(Modifier.height(8.dp))

        when (type) {
            "metric" -> {
                Text(raw.optString("label"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Mali.InkSoft)
                Text(raw.optString("value"), fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                if (raw.optString("caption").isNotBlank()) Text(raw.optString("caption"), fontSize = 13.sp, lineHeight = 18.sp, color = Mali.InkSoft)
            }
            "chart" -> {
                val xs = strings(raw.optJSONArray("x")).map { it.toDoubleOrNull() ?: 0.0 }
                val arr = raw.optJSONArray("series") ?: JSONArray()
                val names = (0 until arr.length()).map { arr.getJSONObject(it).optString("name") }
                val values = (0 until arr.length()).map { i -> strings(arr.getJSONObject(i).optJSONArray("values")).map { it.toDoubleOrNull() ?: 0.0 } }
                val xName = raw.optString("x_label", "Year").ifBlank { "Year" }
                LineChart(
                    xs, chartSeries(names, values), height = 190.dp, xTitle = xName,
                    readoutLabel = { x -> xName + " " + (if (x % 1.0 == 0.0) x.toInt().toString() else String.format(java.util.Locale.US, "%.1f", x)) },
                )
            }
            "table" -> TableBlock(raw)
            "scenarios" -> {
                val arr = raw.optJSONArray("scenarios") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val s = arr.getJSONObject(i)
                    val delta = s.optString("delta").toDoubleOrNull() ?: 0.0
                    Column(Modifier.padding(vertical = 5.dp)) {
                        Text(s.optString("name"), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                        Text("Final balance " + Fmt.kes(s.optString("final_balance")), fontSize = 14.sp, color = Mali.Ink)
                        if (i > 0) {
                            val word = if (delta > 0) "▲ " + Fmt.kes(s.optString("delta")) + " more than the base case"
                            else if (delta < 0) "▼ " + Fmt.kes(s.optString("delta").removePrefix("-")) + " less than the base case" else "Same as the base case"
                            Text(word, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (delta > 0) Mali.Green else if (delta < 0) Mali.Pink else Mali.InkSoft)
                            val ch = s.optJSONArray("changes") ?: JSONArray()
                            for (j in 0 until ch.length()) {
                                val c = ch.getJSONObject(j)
                                Text(c.optString("label") + ": " + c.optString("from") + " → " + c.optString("to"), fontSize = 12.sp, color = Mali.InkSoft)
                            }
                        } else {
                            Text("Starting point", fontSize = 12.sp, color = Mali.InkSoft)
                        }
                    }
                }
            }
            "assumptions" -> {
                val arr = raw.optJSONArray("items") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val a = arr.getJSONObject(i)
                    Column(Modifier.padding(vertical = 3.dp)) {
                        Text(a.optString("label"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Mali.InkSoft)
                        val kind = when (a.optString("kind")) { "user-entered" -> "you entered"; "assumption" -> "an assumption"; "sourced" -> "from a source"; else -> a.optString("kind") }
                        Text(a.optString("value") + "  ·  " + kind, fontSize = 14.sp, color = Mali.Ink)
                        if (a.optString("source").isNotBlank()) Text(a.optString("source"), fontSize = 11.sp, lineHeight = 15.sp, color = Mali.InkSoft)
                    }
                }
            }
            "methodology" -> {
                var open by remember { mutableStateOf(false) }
                Text(if (open) "Hide the method" else "Show how this was calculated", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Mali.Teal, modifier = Modifier.clickable { open = !open })
                if (open) strings(raw.optJSONArray("lines")).forEach { Text("• $it", fontSize = 13.sp, lineHeight = 19.sp, color = Mali.InkSoft, modifier = Modifier.padding(top = 4.dp)) }
            }
            "interpretation" -> {
                val text = strings(raw.optJSONArray("sentences")).joinToString("\n\n")
                Text(
                    if (raw.optBoolean("generated", true)) "Written from your numbers. Edit it to say what you conclude." else "Edited by you.",
                    fontSize = 12.sp, color = Mali.InkSoft,
                )
                Spacer(Modifier.height(6.dp))
                StudioField(
                    value = text,
                    onValue = { t ->
                        onReplace(edited(block) { r ->
                            r.put("sentences", JSONArray(t.split("\n\n")))
                            r.put("generated", false)
                        })
                    },
                    style = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, color = Mali.Ink),
                    placeholder = "Write what this shows…",
                    blockId = block.id, pendingFocus = null, onFocusConsumed = {}, onFocus = {},
                )
            }
            "source" -> {
                val style = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, color = Mali.Ink)
                StudioField(
                    value = raw.optString("text"),
                    onValue = { t -> onReplace(edited(block) { it.put("text", t) }) },
                    style = style, placeholder = "Where did this information come from?",
                    blockId = block.id, pendingFocus = null, onFocusConsumed = {}, onFocus = {},
                )
                Spacer(Modifier.height(6.dp))
                StudioField(
                    value = raw.optString("url"),
                    onValue = { t -> onReplace(edited(block) { it.put("url", t.replace("\n", "").trim()) }) },
                    style = style.copy(color = Mali.Indigo, fontSize = 14.sp), placeholder = "Link (optional)",
                    blockId = block.id + "-url", pendingFocus = null, onFocusConsumed = {}, onFocus = {},
                )
            }
            else -> Text("Unsupported block", color = Mali.InkSoft)
        }

        if (type != "source" && calc != null) {
            Spacer(Modifier.height(10.dp))
            val stamp = if (live) calc.optString("calculated_at") else raw.optString("captured_at").ifBlank { calc.optString("calculated_at") }
            val verb = if (live) "Live · calculated " else "Snapshot · captured "
            Text(
                verb + NoteStore.ago(stamp) + " · model " + calc.optString("model") + " " + calc.optString("version"),
                fontSize = 11.sp, lineHeight = 15.sp, color = Mali.InkSoft,
            )
        }
        problem?.let { Text(it, fontSize = 12.sp, color = Mali.Pink, modifier = Modifier.padding(top = 4.dp)) }
        if (focused && calc?.optJSONObject("inputs") != null) {
            Row(Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionChip("Open in calculator", color = Mali.Indigo) { onOpenCalc(block) }
                if (canRefresh) {
                    ActionChip(if (busy) "Refreshing…" else "Refresh", color = Mali.Green) {
                        if (!busy) {
                            busy = true
                            problem = null
                            scope.launch {
                                try {
                                    onReplace(online.malilab.app.data.Evidence.refresh(block))
                                } catch (e: ApiError) {
                                    problem = if (e.status == 0) "Connect to the internet to refresh." else e.message
                                }
                                busy = false
                            }
                        }
                    }
                }
                if (live) ActionChip("Freeze as snapshot") {
                    onReplace(edited(block) { r -> r.put("mode", "snapshot"); r.put("captured_at", NoteStore.nowIso()) })
                }
            }
        }
    }
}

@Composable
private fun TableBlock(raw: JSONObject) {
    val cols = raw.optJSONArray("columns") ?: JSONArray()
    val rows = raw.optJSONArray("rows") ?: JSONArray()
    var all by remember { mutableStateOf(false) }
    val limit = if (all) rows.length() else Math.min(rows.length(), 10)
    val cellW = 104.dp
    Column(Modifier.horizontalScroll(rememberScrollState())) {
        Row(Modifier.background(Mali.TealTint).padding(vertical = 6.dp)) {
            for (c in 0 until cols.length()) Text(cols.getJSONObject(c).optString("label"), Modifier.width(cellW).padding(horizontal = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Mali.TealDeep)
        }
        for (r in 0 until limit) {
            val row = rows.getJSONArray(r)
            Row(Modifier.background(if (r % 2 == 0) Color.White else Mali.Cream).padding(vertical = 5.dp)) {
                for (c in 0 until row.length()) Text(row.optString(c), Modifier.width(cellW).padding(horizontal = 4.dp), fontSize = 12.sp, color = Mali.Ink)
            }
        }
    }
    if (rows.length() > 10) {
        Text(
            if (all) "Show fewer rows" else "Show all ${rows.length()} rows",
            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Mali.Teal,
            modifier = Modifier.padding(top = 6.dp).clickable { all = !all },
        )
    }
}
