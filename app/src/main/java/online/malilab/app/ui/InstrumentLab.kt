package online.malilab.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import online.malilab.app.data.ApiError
import online.malilab.app.data.Block
import online.malilab.app.data.CalcApi
import online.malilab.app.data.FieldSpec
import online.malilab.app.data.Fmt
import online.malilab.app.data.INSTRUMENTS
import online.malilab.app.data.InstrumentApi
import online.malilab.app.data.InstrumentEvidence
import online.malilab.app.data.COMPARE_SPEC
import online.malilab.app.data.CompareForm
import online.malilab.app.data.InstrumentForm
import online.malilab.app.data.LabForm
import online.malilab.app.data.INSTRUMENT_MODELS
import online.malilab.app.data.instrumentSpec
import online.malilab.app.ui.theme.Mali
import org.json.JSONArray
import org.json.JSONObject

private val PICKER = listOf("compound" to "Savings & growth") + INSTRUMENTS.map { it.model to it.title } + listOf(COMPARE_SPEC.model to COMPARE_SPEC.title)
private val INSTRUMENT_TABS = listOf("Set up", "Results", "Graph", "Table", "Sources")

/** The calculator, with a row at the top to choose what is being calculated: savings, or one of the Kenyan instruments. */
@Composable
fun CalculatorHub(seed: CalcSeed?, onInsert: (List<Block>) -> Unit, onClose: () -> Unit) {
    var model by remember { mutableStateOf(seed?.model ?: LabMemory.model) }
    val picker: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().background(Mali.TealTint).horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PICKER.forEach { (m, title) -> ActionChip(title, selected = model == m) { model = m; LabMemory.model = m } }
        }
    }
    key(model) {
        if (model == "compound") {
            CalculatorLab(seed?.takeIf { it.model == "compound" }, onInsert, onClose, header = picker)
        } else {
            InstrumentLab(model, seed?.takeIf { it.model == model }?.inputs, onInsert, onClose, picker)
        }
    }
}

private class InstrumentInsert(val title: String, val build: (String) -> List<Block>)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InstrumentLab(model: String, seedInputs: JSONObject?, onInsert: (List<Block>) -> Unit, onClose: () -> Unit, header: @Composable () -> Unit) {
    val spec = instrumentSpec(model)
    val form: LabForm = remember(model) {
        val f: LabForm = if (model == "compare") CompareForm() else InstrumentForm(spec)
        val start = seedInputs ?: LabMemory.instruments[model]
        if (start != null) f.load(start)
        f
    }
    var tab by remember { mutableIntStateOf(if (seedInputs != null) 1 else 0) }
    var result by remember { mutableStateOf<JSONObject?>(null) }
    var loading by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var netError by remember { mutableStateOf<String?>(null) }
    var ask by remember { mutableStateOf<InstrumentInsert?>(null) }
    var insertMode by remember { mutableStateOf("snapshot") }
    var withAssumptions by remember { mutableStateOf(true) }
    var withSources by remember { mutableStateOf(true) }
    var toast by remember { mutableStateOf<String?>(null) }

    val inputs = form.toInputs()
    val keyText = inputs.toString()

    LaunchedEffect(keyText) {
        delay(700)
        loading = true
        try {
            result = InstrumentApi.run(model, inputs)
            errors = emptyMap()
            netError = null
            LabMemory.instruments[model] = inputs
        } catch (e: ApiError) {
            if (e.code == "invalid_inputs") {
                errors = CalcApi.fieldErrors(e)
                netError = null
            } else {
                netError = e.message
            }
            result = null
        }
        loading = false
    }
    LaunchedEffect(toast) {
        if (toast != null) { delay(2400); toast = null }
    }
    BackHandler(enabled = true) { onClose() }
    StatusBarIcons(darkIcons = false)

    fun request(title: String, build: (String) -> List<Block>) { ask = InstrumentInsert(title, build) }

    Column(Modifier.fillMaxSize().background(Mali.Cream).imePadding()) {
        Row(
            Modifier.fillMaxWidth().background(Mali.TealTop).statusBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackCircle(onBack = onClose)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(spec.title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(if (loading) "Calculating…" else "Results update as you type", fontSize = 12.sp, color = Mali.TealDeep)
            }
            Text(
                "Reset", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable { form.reset(); result = null; errors = emptyMap() }.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        header()
        Row(
            Modifier.fillMaxWidth().background(Color.White).horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            INSTRUMENT_TABS.forEachIndexed { i, t -> ActionChip(t, selected = tab == i) { tab = i } }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                0 -> when (form) {
                    is CompareForm -> CompareSetUp(form, errors, netError)
                    is InstrumentForm -> InstrumentSetUp(form, errors, netError)
                    else -> Unit
                }
                1 -> InstrumentResults(result, errors, netError, loading, ::request)
                2 -> InstrumentGraph(result, ::request)
                3 -> InstrumentTable(result, ::request)
                else -> InstrumentSources(result, ::request)
            }
            toast?.let {
                Text(
                    it, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(18.dp).clip(RoundedCornerShape(50)).background(Mali.TealDeep).padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
        Box(Modifier.navigationBarsPadding())
    }

    ask?.let { req ->
        AlertDialog(
            onDismissRequest = { ask = null },
            containerColor = Mali.Cream,
            title = { Text("Insert into your note", fontWeight = FontWeight.ExtraBold, color = Mali.Ink) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(req.title, fontWeight = FontWeight.Bold, color = Mali.Ink, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    ModeRow("Snapshot", "Frozen as it is now. Stays exactly the same in your note, with the time it was captured.", insertMode == "snapshot") { insertMode = "snapshot" }
                    ModeRow("Live", "Remembers your assumptions. You can refresh it later with the rules in force then, or open it again in the calculator.", insertMode == "live") { insertMode = "live" }
                    Row(Modifier.fillMaxWidth().clickable { withAssumptions = !withAssumptions }.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(withAssumptions, { withAssumptions = it }, colors = CheckboxDefaults.colors(checkedColor = Mali.Teal))
                        Text("Also add the assumptions behind it", color = Mali.Ink, fontSize = 15.sp)
                    }
                    Row(Modifier.fillMaxWidth().clickable { withSources = !withSources }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(withSources, { withSources = it }, colors = CheckboxDefaults.colors(checkedColor = Mali.Teal))
                        Text("Also add where the tax rules came from", color = Mali.Ink, fontSize = 15.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val blocks = req.build(insertMode).toMutableList()
                    val r = result
                    if (r != null) {
                        if (withAssumptions && blocks.none { it.type == "assumptions" }) blocks.add(InstrumentEvidence.assumptions(r, insertMode))
                        if (withSources && blocks.none { it.type == "source" }) blocks.addAll(InstrumentEvidence.sources(r))
                    }
                    ask = null
                    onInsert(blocks)
                    toast = "Added to your note"
                }) { Text("Insert", color = Mali.Teal, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { ask = null }) { Text("Cancel", color = Mali.InkSoft) } },
        )
    }
}

// ================================================================================================ set up
@Composable
private fun InstField(form: InstrumentForm, f: FieldSpec, errors: Map<String, String>) {
    val v = form.text[f.key].orEmpty()
    OutlinedTextField(
        value = v,
        onValueChange = { raw -> form.text[f.key] = raw.filter { it.isDigit() || it == '.' || it == ',' } },
        label = { Text(f.label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = errors[f.key] != null,
        supportingText = {
            val e = errors[f.key]
            if (e != null) Text(e, color = Mali.Pink)
            else if (f.kind == "money" && v.isNotBlank()) Text(Fmt.kes(v.replace(",", "")))
            else if (f.hint.isNotBlank()) Text(f.hint)
        },
        modifier = Modifier.fillMaxWidth(),
        colors = fieldColors(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InstChoice(form: InstrumentForm, f: FieldSpec) {
    Text(f.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Mali.InkSoft)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        f.options.forEach { (value, shown) -> ActionChip(shown, selected = form.text[f.key] == value) { form.text[f.key] = value } }
    }
    if (f.hint.isNotBlank()) Text(f.hint, fontSize = 12.sp, lineHeight = 17.sp, color = Mali.InkSoft)
}

@Composable
private fun InstFieldAny(form: InstrumentForm, f: FieldSpec, errors: Map<String, String>) {
    when (f.kind) {
        "choice" -> InstChoice(form, f)
        "switch" -> {
            SwitchRow(f.label, form.flags[f.key] == true) { form.flags[f.key] = it }
            if (f.hint.isNotBlank()) Text(f.hint, fontSize = 12.sp, lineHeight = 17.sp, color = Mali.InkSoft)
        }
        else -> InstField(form, f, errors)
    }
}

@Composable
private fun InstrumentSetUp(form: InstrumentForm, errors: Map<String, String>, netError: String?) {
    val spec = form.spec
    val advanced = spec.fields.filter { it.advanced }
    val usedAdvanced = advanced.any { (form.text[it.key].orEmpty() != it.default && form.text[it.key].orEmpty().isNotBlank()) || form.flags[it.key] == true } ||
        advanced.any { errors[it.key] != null }
    var more by remember(spec.model) { mutableStateOf(usedAdvanced) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
        Section(spec.title, spec.blurb) {
            netError?.let { Text(it, color = Mali.Pink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
            spec.fields.filter { !it.advanced }.forEach { InstFieldAny(form, it, errors) }
        }
        if (advanced.isNotEmpty()) {
            Section(
                "More options",
                "Tax settings and extras. The defaults use the rules shown in the Sources tab.",
            ) {
                Text(
                    if (more) "Hide" else "Show", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Teal,
                    modifier = Modifier.clickable { more = !more },
                )
                if (more) advanced.forEach { InstFieldAny(form, it, errors) }
            }
        }
        Text(
            "Rates and yields change. Enter the figure you expect, not a promise. Nothing here is advice or a guarantee.",
            fontSize = 12.sp, lineHeight = 17.sp, color = Mali.InkSoft, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}

// ================================================================================================ results
internal fun strs(a: JSONArray?): List<String> = (0 until (a?.length() ?: 0)).map { a!!.optString(it) }

@Composable
internal fun NothingYet(loading: Boolean, errors: Map<String, String>, netError: String?) {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (loading) "Calculating…" else if (netError != null) "Could not calculate" else "Nothing to show yet",
            fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            netError ?: if (errors.isNotEmpty()) "Go to Set up and check: " + errors.values.first() else "Fill in the Set up tab and the results appear here.",
            color = Mali.InkSoft, fontSize = 15.sp,
        )
    }
}

@Composable
internal fun InsertButton(text: String, onClick: () -> Unit) {
    Text(
        text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Mali.Indigo).clickable(onClick = onClick).padding(vertical = 14.dp),
    )
}

@Composable
private fun InstrumentResults(
    result: JSONObject?, errors: Map<String, String>, netError: String?, loading: Boolean,
    request: (String, (String) -> List<Block>) -> Unit,
) {
    if (result == null) { NothingYet(loading, errors, netError); return }
    val head = result.getJSONObject("headline")
    val metrics = result.optJSONArray("metrics") ?: JSONArray()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp, horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val warnings = result.optJSONArray("warnings") ?: JSONArray()
        for (i in 0 until warnings.length()) {
            Text(
                warnings.getJSONObject(i).optString("message"), fontSize = 14.sp, lineHeight = 20.sp, color = Mali.Amber, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Mali.Amber.copy(alpha = 0.12f)).padding(14.dp),
            )
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(16.dp)) {
            Text(head.optString("label"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Mali.InkSoft)
            Text(head.optString("value"), fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
            if (head.optString("caption").isNotBlank()) Text(head.optString("caption"), fontSize = 14.sp, color = Mali.InkSoft)
            Spacer(Modifier.height(8.dp))
            Text(
                "+ Insert", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Teal,
                modifier = Modifier.clickable { request("Key number: " + head.optString("label")) { m -> listOf(InstrumentEvidence.headline(result, m)) } },
            )
        }
        val list = (0 until metrics.length()).map { metrics.getJSONObject(it) }
        list.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { m ->
                    MetricCard(m.optString("label"), m.optString("value"), Modifier.weight(1f)) {
                        request("Key number: " + m.optString("label")) { mode -> listOf(InstrumentEvidence.metric(result, m.optString("key"), mode)) }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        val sentences = strs(result.optJSONArray("interpretation"))
        if (sentences.isNotEmpty()) {
            Card("What this shows", "Written from your numbers. You can edit it after inserting.") {
                sentences.forEach { Text(it, fontSize = 15.sp, lineHeight = 22.sp, color = Mali.Ink) }
                Text(
                    "+ Insert", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Teal,
                    modifier = Modifier.clickable { request("Explanation") { m -> listOf(InstrumentEvidence.interpretation(result, m)) } },
                )
            }
        }
        Card("How this was calculated", "") {
            strs(result.optJSONArray("methodology")).forEach { Text("• $it", fontSize = 13.sp, lineHeight = 19.sp, color = Mali.InkSoft) }
            Text(
                "+ Insert", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Teal,
                modifier = Modifier.clickable { request("Method") { m -> listOf(InstrumentEvidence.methodology(result, m)) } },
            )
        }
        InsertButton("Insert a full summary") {
            request("Summary: key number, explanation, assumptions and sources") { mode ->
                listOf(InstrumentEvidence.headline(result, mode), InstrumentEvidence.interpretation(result, mode), InstrumentEvidence.assumptions(result, mode)) + InstrumentEvidence.sources(result)
            }
        }
    }
}

@Composable
private fun InstrumentGraph(result: JSONObject?, request: (String, (String) -> List<Block>) -> Unit) {
    if (result == null || result.optJSONObject("chart") == null) { EmptyResults(); return }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { ChartSection(result, request) }
}

@Composable
internal fun ChartSection(result: JSONObject, request: (String, (String) -> List<Block>) -> Unit) {
    val c = result.optJSONObject("chart") ?: return
    val xs = strs(c.optJSONArray("x")).map { it.toDoubleOrNull() ?: 0.0 }
    val arr = c.optJSONArray("series") ?: JSONArray()
    val names = (0 until arr.length()).map { arr.getJSONObject(it).optString("name") }
    val values = (0 until arr.length()).map { i -> strs(arr.getJSONObject(i).optJSONArray("values")).map { it.toDoubleOrNull() ?: 0.0 } }
    val xName = c.optString("x_label", "Year").ifBlank { "Year" }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(c.optString("title"), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(14.dp)) {
            LineChart(
                xs, chartSeries(names, values), xTitle = xName,
                readoutLabel = { x -> xName + " " + (if (x % 1.0 == 0.0) x.toInt().toString() else String.format(java.util.Locale.US, "%.1f", x)) },
            )
        }
        InsertButton("Insert this chart") { request("Chart: " + c.optString("title")) { m -> listOf(InstrumentEvidence.chart(result, m)) } }
        Text(
            "The chart shows the same numbers as the table. Lines differ by dash pattern and point shape as well as colour.",
            fontSize = 12.sp, lineHeight = 17.sp, color = Mali.InkSoft,
        )
    }
}

@Composable
private fun InstrumentTable(result: JSONObject?, request: (String, (String) -> List<Block>) -> Unit) {
    if (result == null || result.optJSONObject("table") == null) { EmptyResults(); return }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { TableSection(result, request) }
}

@Composable
internal fun TableSection(result: JSONObject, request: (String, (String) -> List<Block>) -> Unit) {
    val t = result.optJSONObject("table") ?: return
    val cols = t.getJSONArray("columns")
    val rows = t.getJSONArray("rows")
    var all by remember(rows.length()) { mutableStateOf(false) }
    val limit = if (all) rows.length() else Math.min(rows.length(), 12)
    fun width(c: Int) = if (cols.optJSONObject(c)?.optString("key") in listOf("what", "factor", "scenario")) 176.dp else 112.dp
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(t.optString("title"), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).horizontalScroll(rememberScrollState())) {
            Row(Modifier.background(Mali.TealTint).padding(vertical = 8.dp)) {
                for (c in 0 until cols.length()) Text(cols.getJSONObject(c).optString("label"), Modifier.width(width(c)).padding(horizontal = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Mali.TealDeep)
            }
            for (r in 0 until limit) {
                val row = rows.getJSONArray(r)
                Row(Modifier.background(if (r % 2 == 0) Color.White else Mali.Cream).padding(vertical = 7.dp)) {
                    for (c in 0 until row.length()) {
                        val kind = cols.optJSONObject(c)?.optString("kind")
                        Text(if (kind == "money") Fmt.money(row.optString(c)) else row.optString(c), Modifier.width(width(c)).padding(horizontal = 6.dp), fontSize = 13.sp, color = Mali.Ink)
                    }
                }
            }
        }
        if (rows.length() > 12) {
            Text(
                if (all) "Show fewer rows" else "Show all ${rows.length()} rows", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Mali.Teal,
                modifier = Modifier.clickable { all = !all },
            )
        }
        InsertButton("Insert this table") { request("Table: " + t.optString("title")) { m -> listOf(InstrumentEvidence.table(result, m)) } }
    }
}

// ================================================================================================ sources
@Composable
private fun InstrumentSources(result: JSONObject?, request: (String, (String) -> List<Block>) -> Unit) {
    if (result == null) { EmptyResults(); return }
    val rules = result.optJSONArray("rules") ?: JSONArray()
    val items = result.optJSONArray("assumptions") ?: JSONArray()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp, horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card("Assumptions", "Which numbers you entered, which are guesses, and which come from a published source.") {
            for (i in 0 until items.length()) {
                val a = items.getJSONObject(i)
                val kind = when (a.optString("kind")) { "user-entered" -> "you entered"; "assumption" -> "an assumption"; "sourced" -> "from a source"; else -> a.optString("kind") }
                Column(Modifier.padding(vertical = 3.dp)) {
                    Text(a.optString("label"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Mali.InkSoft)
                    Text(a.optString("value") + "  ·  " + kind, fontSize = 14.sp, color = Mali.Ink)
                }
            }
            Text(
                "+ Insert", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Teal,
                modifier = Modifier.clickable { request("Assumptions") { m -> listOf(InstrumentEvidence.assumptions(result, m)) } },
            )
        }
        if (rules.length() == 0) {
            Card("Rules", "") { Text("This calculation does not depend on a tax rule.", fontSize = 14.sp, color = Mali.InkSoft) }
        }
        for (i in 0 until rules.length()) {
            val r = rules.getJSONObject(i)
            val unit = if (r.optString("id").contains("day_basis")) " days" else "%"
            Card(r.optString("label"), "") {
                Text(r.optString("rate") + unit, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                Text(r.optString("status"), fontSize = 14.sp, lineHeight = 20.sp, color = Mali.Ink)
                val from = r.optString("effective_from")
                Text(
                    (if (from.isNotBlank() && from != "null") "In force from $from. " else "The source does not state a start date. ") + "Read on " + r.optString("retrieved") + ".",
                    fontSize = 12.sp, lineHeight = 17.sp, color = Mali.InkSoft,
                )
                Text(r.optString("source"), fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold, color = Mali.InkSoft)
                Text(r.optString("url"), fontSize = 12.sp, lineHeight = 17.sp, color = Mali.Indigo)
                if (r.optString("caveat").isNotBlank()) Text("Note: " + r.optString("caveat"), fontSize = 12.sp, lineHeight = 17.sp, color = Mali.Amber, fontWeight = FontWeight.SemiBold)
            }
        }
        if (rules.length() > 0) InsertButton("Insert the sources into my note") { request("Sources for the rules used") { _ -> InstrumentEvidence.sources(result) } }
        Text("Rules change with each Finance Act. Check the source before you rely on a rate for a real decision.", fontSize = 12.sp, lineHeight = 17.sp, color = Mali.InkSoft)
    }
}
