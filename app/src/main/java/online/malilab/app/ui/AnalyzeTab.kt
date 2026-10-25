package online.malilab.app.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import online.malilab.app.data.ApiError
import online.malilab.app.data.Block
import online.malilab.app.data.CalcApi
import online.malilab.app.data.Fmt
import online.malilab.app.data.InstrumentApi
import online.malilab.app.data.InstrumentEvidence
import online.malilab.app.ui.theme.Mali
import org.json.JSONArray
import org.json.JSONObject

private val ANALYSES = listOf(
    Triple("sensitivity", "What matters most", "Changes one assumption at a time, up and down, and ranks which ones move the result most."),
    Triple("stress", "Stress tests", "Shows how your plan holds up when returns fall, prices rise, savings stop or markets drop."),
)

/**
 * Two ways to question a savings plan, both from the website's calculation engine: which assumptions matter most, and what
 * happens when things go wrong. The steps and shocks are examples, not forecasts, and the results say so.
 */
@Composable
internal fun AnalyzeTab(effective: JSONObject, ready: Boolean, initial: String?, ask: (String, (String) -> List<Block>) -> Unit) {
    var kind by remember { mutableStateOf(initial ?: LabMemory.analysis) }
    var result by remember { mutableStateOf<JSONObject?>(null) }
    var loading by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    val inputsText = effective.toString()

    LaunchedEffect(inputsText, kind, ready) {
        if (!ready) { result = null; return@LaunchedEffect }
        delay(500)
        loading = true
        try {
            result = InstrumentApi.run(kind, effective)
            problem = null
        } catch (e: ApiError) {
            problem = if (e.code == "invalid_inputs") CalcApi.fieldErrors(e).values.firstOrNull() ?: e.message else e.message
            result = null
        }
        loading = false
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp, horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ANALYSES.forEach { (k, title, _) -> ActionChip(title, selected = kind == k) { kind = k; LabMemory.analysis = k; result = null } }
        }
        Text(ANALYSES.first { it.first == kind }.third, fontSize = 14.sp, lineHeight = 20.sp, color = Mali.InkSoft)

        val r = result
        if (!ready) {
            Text("Fill in the Set up tab first. The analysis uses the same assumptions.", fontSize = 15.sp, color = Mali.InkSoft)
            return@Column
        }
        if (r == null) {
            Text(
                if (loading) "Calculating…" else (problem ?: "Nothing to show yet."),
                fontSize = 15.sp, color = if (problem != null && !loading) Mali.Pink else Mali.InkSoft,
            )
            return@Column
        }

        val warnings = r.optJSONArray("warnings") ?: JSONArray()
        for (i in 0 until warnings.length()) {
            Text(
                warnings.getJSONObject(i).optString("message"), fontSize = 14.sp, lineHeight = 20.sp, color = Mali.Amber, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Mali.Amber.copy(alpha = 0.12f)).padding(14.dp),
            )
        }
        val head = r.getJSONObject("headline")
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Mali.Surface).padding(16.dp)) {
            Text(head.optString("label"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Mali.InkSoft)
            Text(head.optString("value"), fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
            Text(head.optString("caption"), fontSize = 14.sp, lineHeight = 20.sp, color = Mali.InkSoft)
        }
        val bars = r.optJSONArray("bars")
        if (bars != null && bars.length() > 0) TornadoBars(bars, r.optString("measure"), r.optString("base_value"))
        ChartSection(r, ask)
        TableSection(r, ask)

        val sentences = strs(r.optJSONArray("interpretation"))
        if (sentences.isNotEmpty()) {
            Card("What this shows", "Written from your numbers. You can edit it after inserting.") {
                sentences.forEach { Text(it, fontSize = 15.sp, lineHeight = 22.sp, color = Mali.Ink) }
                Text(
                    "+ Insert", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Teal,
                    modifier = Modifier.clickable { ask("Explanation") { m -> listOf(InstrumentEvidence.interpretation(r, m)) } },
                )
            }
        }
        InsertButton("Insert a full summary") {
            ask("Summary: key result, table, chart, explanation and assumptions") { mode ->
                buildList<Block> {
                    add(InstrumentEvidence.headline(r, mode))
                    add(InstrumentEvidence.table(r, mode))
                    if (r.optJSONObject("chart") != null) add(InstrumentEvidence.chart(r, mode))
                    add(InstrumentEvidence.interpretation(r, mode))
                    add(InstrumentEvidence.assumptions(r, mode))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** One pair of bars per assumption: what happens to the result if it is lower, and if it is higher. Arrows and words say which way, not colour alone. */
@Composable
private fun TornadoBars(bars: JSONArray, measure: String, base: String) {
    var biggest = 1.0
    for (i in 0 until bars.length()) {
        val b = bars.getJSONObject(i)
        biggest = Math.max(biggest, Math.max(Math.abs(b.optString("lower_delta").toDoubleOrNull() ?: 0.0), Math.abs(b.optString("higher_delta").toDoubleOrNull() ?: 0.0)))
    }
    Card(measure, "Base case " + Fmt.kes(base) + ". Longer bars mean a bigger change to the result.") {
        for (i in 0 until bars.length()) {
            val b = bars.getJSONObject(i)
            Column(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(b.optString("label"), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                BarLine("If " + b.optString("lower_value"), b.optString("lower_delta"), biggest)
                BarLine("If " + b.optString("higher_value"), b.optString("higher_delta"), biggest)
            }
        }
    }
}

@Composable
private fun BarLine(label: String, delta: String, biggest: Double) {
    val v = delta.toDoubleOrNull() ?: 0.0
    val fraction = (Math.abs(v) / biggest).toFloat().coerceIn(0.02f, 1f)
    val up = v >= 0
    Column {
        Box(Modifier.fillMaxWidth(fraction).height(12.dp).clip(RoundedCornerShape(6.dp)).background(if (up) Mali.Teal else Mali.Pink))
        Text(
            label + ": " + (if (up) "▲ " else "▼ ") + Fmt.kes(delta.removePrefix("-")) + (if (up) " more" else " less"),
            fontSize = 12.sp, lineHeight = 16.sp, color = Mali.InkSoft,
        )
    }
}
