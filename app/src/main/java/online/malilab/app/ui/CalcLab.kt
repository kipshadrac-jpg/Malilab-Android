package online.malilab.app.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import online.malilab.app.data.ApiError
import online.malilab.app.data.Block
import online.malilab.app.data.CalcApi
import online.malilab.app.data.CalcForm
import online.malilab.app.data.CalcOutcome
import online.malilab.app.data.COMPOUNDINGS
import online.malilab.app.data.Evidence
import online.malilab.app.data.FREQUENCIES
import online.malilab.app.data.Fmt
import online.malilab.app.data.Goal
import online.malilab.app.data.WITHDRAWAL_MODES
import online.malilab.app.ui.theme.Mali
import org.json.JSONArray
import org.json.JSONObject

/** A saved calculation to reopen, taken from a block in a note. */
class CalcSeed(val inputs: JSONObject, val solve: String?, val model: String = "compound", val analysis: String? = null)

/** Remembers the last calculation while the app is open, so reopening the Lab picks up where the person left off. */
object LabMemory {
    var model: String = "compound"
    var analysis: String = "sensitivity"
    val instruments = HashMap<String, JSONObject>()
    var inputs: JSONObject? = null
    var solve: String? = null
}

private class InsertRequest(val title: String, val build: (String) -> List<Block>)

private val TABS = listOf("Set up", "Summary", "Graph", "Table", "Compare", "Analyze")

/**
 * The calculator, full screen. The numbers always come from the website's tested calculation engine; this screen only
 * collects assumptions, shows the results in a graph and tables, and inserts chosen results into the note as blocks.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalculatorLab(seed: CalcSeed?, onInsert: (List<Block>) -> Unit, onClose: () -> Unit, header: @Composable () -> Unit = {}) {
    val context = LocalContext.current
    val form = remember {
        CalcForm().also { f ->
            val s = seed
            val last = LabMemory.inputs
            if (s != null) f.load(s.inputs, s.solve) else if (last != null) f.load(last, LabMemory.solve)
        }
    }
    var tab by remember { mutableIntStateOf(if (seed?.analysis != null) 5 else if (seed != null) 1 else 0) }
    var outcome by remember { mutableStateOf<CalcOutcome?>(null) }
    var loading by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var netError by remember { mutableStateOf<String?>(null) }
    var insertAsk by remember { mutableStateOf<InsertRequest?>(null) }
    var insertMode by remember { mutableStateOf("snapshot") }
    var withAssumptions by remember { mutableStateOf(true) }
    var toast by remember { mutableStateOf<String?>(null) }

    val inputs = form.toInputs()
    val solve = form.solveKind()
    val key = inputs.toString() + "|" + solve

    // Calculate a moment after the person stops typing.
    LaunchedEffect(key) {
        delay(700)
        loading = true
        try {
            outcome = CalcApi.run(inputs, solve)
            errors = emptyMap()
            netError = null
            LabMemory.inputs = inputs
            LabMemory.solve = solve
        } catch (e: ApiError) {
            if (e.code == "invalid_inputs") {
                errors = CalcApi.fieldErrors(e)
                netError = null
            } else {
                netError = e.message
            }
            outcome = null
        }
        loading = false
    }
    LaunchedEffect(toast) {
        if (toast != null) { delay(2400); toast = null }
    }

    BackHandler(enabled = true) { onClose() }
    StatusBarIcons(darkIcons = false)

    val result = outcome?.result
    val effective = CalcApi.effective(inputs, outcome?.solution)
    val calc = if (result != null) Evidence.calcInfo(result, inputs, solve) else null
    fun ask(title: String, build: (String) -> List<Block>) { insertAsk = InsertRequest(title, build) }

    Column(Modifier.fillMaxSize().background(Mali.Cream).imePadding()) {
        // ---- top bar
        Row(
            Modifier.fillMaxWidth().background(Mali.TealTop).statusBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackCircle(onBack = onClose, tint = Color.White)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Calculator Lab", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(
                    if (loading) "Calculating…" else "Results update as you type",
                    fontSize = 12.sp, color = Mali.TealDeep,
                )
            }
            Text(
                "Reset",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable { form.reset(); outcome = null; errors = emptyMap() }.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        header()
        Row(
            Modifier.fillMaxWidth().background(Mali.Surface).horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TABS.forEachIndexed { i, t -> ActionChip(t, selected = tab == i) { tab = i } }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                0 -> SetUpTab(form, errors, netError)
                1 -> SummaryTab(outcome, errors, netError, loading, inputs, ::ask, calc, form)
                2 -> GraphTab(outcome, inputs, calc, ::ask)
                3 -> TableTab(outcome, inputs, solve, calc, ::ask)
                4 -> CompareTab(outcome, effective, ::ask, calc)
                else -> AnalyzeTab(effective, outcome != null, seed?.analysis, ::ask)
            }
            toast?.let {
                Text(
                    it, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(18.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Mali.TealFixed)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
        Box(Modifier.navigationBarsPadding())
    }

    insertAsk?.let { req ->
        AlertDialog(
            onDismissRequest = { insertAsk = null },
            containerColor = Mali.Cream,
            title = { Text("Insert into your note", fontWeight = FontWeight.ExtraBold, color = Mali.Ink) },
            text = {
                Column {
                    Text(req.title, fontWeight = FontWeight.Bold, color = Mali.Ink, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    ModeRow("Snapshot", "Frozen as it is now. Stays exactly the same in your note, with the time it was captured.", insertMode == "snapshot") { insertMode = "snapshot" }
                    ModeRow("Live", "Remembers your assumptions. You can refresh it later, or open it again in the calculator.", insertMode == "live") { insertMode = "live" }
                    Row(Modifier.fillMaxWidth().clickable { withAssumptions = !withAssumptions }.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(withAssumptions, { withAssumptions = it }, colors = CheckboxDefaults.colors(checkedColor = Mali.Teal))
                        Text("Also add the assumptions behind it", color = Mali.Ink, fontSize = 15.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val blocks = req.build(insertMode).toMutableList()
                    val r = result
                    val c = calc
                    if (withAssumptions && r != null && c != null && blocks.none { it.type == "assumptions" }) {
                        blocks.add(Evidence.assumptions(r, insertMode, c))
                    }
                    insertAsk = null
                    onInsert(blocks)
                    toast = "Added to your note"
                }) { Text("Insert", color = Mali.Teal, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { insertAsk = null }) { Text("Cancel", color = Mali.InkSoft) } },
        )
    }
}

@Composable
internal fun ModeRow(title: String, blurb: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, if (selected) Mali.Teal else Mali.Sand, RoundedCornerShape(12.dp))
            .background(if (selected) Mali.TealTint else Mali.Surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(title, fontWeight = FontWeight.ExtraBold, color = if (selected) Mali.TealDeep else Mali.Ink, fontSize = 16.sp)
        Text(blurb, fontSize = 13.sp, lineHeight = 18.sp, color = Mali.InkSoft)
    }
}

// ================================================================================================ small building blocks
@Composable
internal fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Mali.Teal, unfocusedBorderColor = Mali.Sand, cursorColor = Mali.Teal,
    focusedContainerColor = Mali.Surface, unfocusedContainerColor = Mali.Surface, errorContainerColor = Mali.Surface,
    errorBorderColor = Mali.Pink,
)

@Composable
private fun NumField(
    form: CalcForm,
    key: String,
    label: String,
    errors: Map<String, String>,
    errKey: String = key,
    hint: String = "",
    money: Boolean = false,
    signed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val v = form.get(key)
    OutlinedTextField(
        value = v,
        onValueChange = { raw -> form.set(key, raw.filter { it.isDigit() || it == '.' || it == ',' || (signed && it == '-') }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (signed) KeyboardType.Text else KeyboardType.Decimal),
        isError = errors[errKey] != null,
        supportingText = {
            val e = errors[errKey]
            if (e != null) Text(e, color = Mali.Pink)
            else if (money && v.isNotBlank()) Text(Fmt.kes(v.replace(",", "")))
            else if (hint.isNotBlank()) Text(hint)
        },
        modifier = modifier.fillMaxWidth(),
        colors = fieldColors(),
    )
}

@Composable
internal fun Section(title: String, note: String = "", content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Mali.Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title.uppercase(), fontSize = 12.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Teal)
        if (note.isNotBlank()) Text(note, fontSize = 13.sp, lineHeight = 19.sp, color = Mali.InkSoft)
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Choice(form: CalcForm, key: String, label: String, options: List<Pair<String, String>>) {
    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Mali.InkSoft)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, shown) -> ActionChip(shown, selected = form.get(key) == value) { form.set(key, value) } }
    }
}

@Composable
internal fun SwitchRow(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Mali.Ink)
        Switch(on, onChange, colors = SwitchDefaults.colors(checkedTrackColor = Mali.Teal))
    }
}

// ================================================================================================ set up
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetUpTab(form: CalcForm, errors: Map<String, String>, netError: String?) {
    // Field names the website uses for withdrawals differ from the ones kept in the form.
    val e = errors.mapKeys { (k, _) ->
        when (k) {
            "withdrawal.mode" -> "w_mode"; "withdrawal.amount" -> "w_amount"; "withdrawal.percent" -> "w_percent"
            "withdrawal.frequency" -> "w_frequency"; "withdrawal.start_year" -> "w_start"; "withdrawal.end_year" -> "w_end"
            else -> k
        }
    }
    val goal = form.goal
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 40.dp)) {
        Section("What do you want to find out?") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Goal.values().forEach { g ->
                    ActionChip(g.title, selected = goal == g) {
                        form.goal = g
                        if (g.needsWithdrawals) {
                            if (form.get("w_mode") == "none" || (g == Goal.MaxWithdrawal && form.get("w_mode").startsWith("pct"))) form.set("w_mode", "fixed")
                        }
                        if (g.needsTarget && form.get("target").isBlank()) form.set("target", "")
                    }
                }
            }
            Text(goal.blurb, fontSize = 14.sp, color = Mali.InkSoft)
            Text(
                "Example numbers are filled in. Change them to match your situation. Returns, inflation and tax rates are assumptions you choose, not promises.",
                fontSize = 12.sp, lineHeight = 17.sp, color = Mali.InkSoft,
            )
        }
        if (netError != null) {
            Text(netError, color = Mali.Pink, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        } else if (errors.isNotEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(14.dp)).background(Mali.Pink.copy(alpha = 0.09f)).padding(14.dp),
            ) {
                Text("Please check", fontWeight = FontWeight.ExtraBold, color = Mali.Pink, fontSize = 14.sp)
                errors.values.forEach { Text("• $it", fontSize = 14.sp, color = Mali.Ink) }
            }
        }

        if (goal != Goal.Need) {
            Section("Where you start") {
                NumField(form, "initial", "Starting amount (KES)", e, money = true, hint = "Leave empty if you are starting from nothing.")
            }
        }

        Section("Saving", if (goal == Goal.Target) "We will work out how much to save each period." else "") {
            if (goal != Goal.Target) NumField(form, "contribution", "Amount you add (KES)", e, money = true)
            Choice(form, "contribution_frequency", "How often", FREQUENCIES)
            Choice(form, "contribution_timing", "When in the period", listOf("start" to "Start", "end" to "End"))
            NumField(form, "annual_increase", "Raise the amount each year by (%)", e, hint = "For example 5 if your savings grow with your pay.")
        }

        Section("Growth and time", "The return is your assumption. Real investments can earn less, or lose money.") {
            NumField(form, "annual_return", "Expected yearly return (%)", e, hint = "An assumption you choose.")
            Choice(form, "rate_type", "Rate type", listOf("nominal" to "Nominal", "effective" to "Effective yearly"))
            if (form.get("rate_type") == "nominal") Choice(form, "compounding", "Compounded", COMPOUNDINGS)
            NumField(form, "years", if (goal == Goal.Time) "Look ahead up to (years)" else "For how many years", e)
        }

        if (goal.needsTarget || (goal != Goal.Need && goal != Goal.MaxWithdrawal)) {
            Section("Target", if (goal.needsTarget) "" else "Optional. We will tell you if and when you reach it.") {
                NumField(form, "target", if (goal.needsTarget) "Target balance (KES)" else "Target balance, optional (KES)", e, money = true)
            }
        }

        Section("Costs and inflation", "Switch on what applies. Each one changes the result, and the note will say so.") {
            SwitchRow("Fees", form.flags["include_fees"] == true) { form.flags["include_fees"] = it }
            if (form.flags["include_fees"] == true) NumField(form, "annual_fee", "Yearly fee (% of the balance)", e)
            SwitchRow("Tax on growth", form.flags["include_tax"] == true) { form.flags["include_tax"] = it }
            if (form.flags["include_tax"] == true) NumField(form, "tax_on_growth", "Tax on growth (%)", e, hint = "You enter the rate. It is not checked against current tax rules.")
            SwitchRow("Show in today's money (inflation)", form.flags["include_inflation"] == true) { form.flags["include_inflation"] = it }
            if (form.flags["include_inflation"] == true) NumField(form, "inflation", "Expected yearly inflation (%)", e, hint = "An assumption you choose.")
        }

        if (goal.needsWithdrawals) {
            WithdrawalSection(form, e, goal)
        } else {
            Section("Withdrawals") {
                SwitchRow("Take money out", form.withdrawalsOpen) { form.withdrawalsOpen = it; if (it && form.get("w_mode") == "none") form.set("w_mode", "fixed") }
                if (form.withdrawalsOpen) WithdrawalFields(form, e, goal)
            }
        }

        Section("Pauses and one-off amounts", "Skip saving for a while, or add or take out a lump sum in a given year.") {
            Text("Pauses in saving", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Mali.InkSoft)
            form.pauses.forEachIndexed { i, p ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(p.first, { form.pauses[i] = it.filter { c -> c.isDigit() || c == '.' } to p.second }, label = { Text("From year") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), colors = fieldColors())
                    OutlinedTextField(p.second, { form.pauses[i] = p.first to it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("To year") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), colors = fieldColors())
                    ActionChip("×", color = Mali.Pink) { form.pauses.removeAt(i) }
                }
            }
            ActionChip("+ Add a pause") { form.pauses.add("" to "") }
            Spacer(Modifier.height(6.dp))
            Text("One-off amounts (use a minus sign to take money out)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Mali.InkSoft)
            form.lumps.forEachIndexed { i, p ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(p.first, { form.lumps[i] = it.filter { c -> c.isDigit() || c == '.' } to p.second }, label = { Text("In year") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), colors = fieldColors())
                    OutlinedTextField(p.second, { form.lumps[i] = p.first to it.filter { c -> c.isDigit() || c == '.' || c == '-' || c == ',' } }, label = { Text("Amount (KES)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), modifier = Modifier.weight(1.4f), colors = fieldColors())
                    ActionChip("×", color = Mali.Pink) { form.lumps.removeAt(i) }
                }
            }
            ActionChip("+ Add a one-off amount") { form.lumps.add("" to "") }
        }

        Section("More options") {
            OutlinedTextField(
                form.get("start_date"), { form.set("start_date", it.filter { c -> c.isDigit() || c == '-' }.take(10)) },
                label = { Text("Start date, optional (YYYY-MM-DD)") }, singleLine = true, isError = e["start_date"] != null,
                supportingText = { e["start_date"]?.let { m -> Text(m, color = Mali.Pink) } ?: Text("Adds calendar dates to tables and charts.") },
                modifier = Modifier.fillMaxWidth(), colors = fieldColors(),
            )
            NumField(form, "contribution_start_year", "Start adding in year", e, hint = "0 means from the start.")
            NumField(form, "contribution_end_year", "Stop adding after year", e, hint = "Leave empty to keep going.")
        }
    }
}

@Composable
private fun WithdrawalSection(form: CalcForm, e: Map<String, String>, goal: Goal) {
    Section(
        "Withdrawals",
        when (goal) {
            Goal.MaxWithdrawal -> "We will find the largest steady amount the money can pay."
            Goal.Need -> "Tell us what you want to take out. We will find the savings needed to pay it."
            else -> "Tell us how you will take money out, and when."
        },
    ) { WithdrawalFields(form, e, goal) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WithdrawalFields(form: CalcForm, e: Map<String, String>, goal: Goal) {
    val solving = goal == Goal.MaxWithdrawal || goal == Goal.Need
    val modes = if (solving) WITHDRAWAL_MODES.filter { it.first == "fixed" || it.first == "inflation_adjusted" } else WITHDRAWAL_MODES.filter { it.first != "none" }
    Choice(form, "w_mode", "How", modes)
    val mode = form.get("w_mode")
    if (mode == "fixed" || mode == "inflation_adjusted") {
        if (goal != Goal.MaxWithdrawal) NumField(form, "w_amount", if (mode == "inflation_adjusted") "Amount in today's money (KES)" else "Amount each time (KES)", e, money = true)
    }
    if (mode == "pct_start" || mode == "pct_current") NumField(form, "w_percent", "Percent taken out each year", e)
    Choice(form, "w_frequency", "How often", FREQUENCIES)
    NumField(form, "w_start", "Start taking out in year", e, hint = "0 means from the start.")
    NumField(form, "w_end", "Stop after year", e, hint = "Leave empty to continue to the end.")
}

// ================================================================================================ summary
@Composable
private fun SummaryTab(
    outcome: CalcOutcome?,
    errors: Map<String, String>,
    netError: String?,
    loading: Boolean,
    inputs: JSONObject,
    ask: (String, (String) -> List<Block>) -> Unit,
    calc: JSONObject?,
    form: CalcForm,
) {
    if (outcome == null || calc == null) {
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
        return
    }
    val result = outcome.result
    val s = result.getJSONObject("summary")
    val solution = outcome.solution
    val solve = form.solveKind()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp, horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val warnings = result.optJSONArray("warnings") ?: JSONArray()
        for (i in 0 until warnings.length()) {
            Text(
                warnings.getJSONObject(i).optString("message"), fontSize = 14.sp, lineHeight = 20.sp, color = Mali.Amber, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Mali.Amber.copy(alpha = 0.12f)).padding(14.dp),
            )
        }
        outcome.noSolution?.let {
            Text(
                it, fontSize = 15.sp, lineHeight = 21.sp, color = Mali.Pink, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Mali.Pink.copy(alpha = 0.09f)).padding(14.dp),
            )
        }
        if (solution != null) {
            val block = Evidence.solutionMetric(solution, result, "snapshot", calc)
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Mali.Teal, Mali.TealLight))).padding(18.dp),
            ) {
                Text(block.raw?.optString("label").orEmpty().uppercase(), fontSize = 12.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.ExtraBold, color = Color.White.copy(alpha = 0.85f))
                Text(block.raw?.optString("value").orEmpty(), fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(solution.optString("explanation"), fontSize = 14.sp, lineHeight = 20.sp, color = Color.White.copy(alpha = 0.92f))
                Spacer(Modifier.height(10.dp))
                Text(
                    "Insert into note", color = Mali.TealFixed, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Mali.Surface)
                        .clickable { ask(block.raw?.optString("label").orEmpty()) { m -> listOf(Evidence.solutionMetric(solution, result, m, Evidence.calcInfo(result, inputs, solve))) } }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }

        // the one-tap summary
        Text(
            "Insert a full summary into my note",
            color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Mali.Indigo)
                .clickable {
                    ask("Key numbers, chart and explanation") { m ->
                        val c = Evidence.calcInfo(result, inputs, solve)
                        buildList {
                            if (solution != null) add(Evidence.solutionMetric(solution, result, m, c))
                            add(Evidence.metric(result, "final_balance", m, c))
                            add(Evidence.metric(result, "total_contributed", m, c))
                            add(Evidence.chart(result, listOf("closing", "total_contributed"), m, c))
                            add(Evidence.interpretation(result, m, c))
                        }
                    }
                }
                .padding(vertical = 14.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        // key numbers
        val show = buildList {
            add("final_balance")
            if (inputs.optBoolean("include_inflation")) add("final_real_balance")
            add("total_contributed"); add("net_growth")
            if (s.optString("total_tax").toDoubleOrNull() ?: 0.0 > 0) add("total_tax")
            if (s.optString("total_fees").toDoubleOrNull() ?: 0.0 > 0) add("total_fees")
            if (s.optString("total_withdrawn").toDoubleOrNull() ?: 0.0 > 0) add("total_withdrawn")
            add("growth_share"); add("effective_annual_rate")
        }
        show.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { field ->
                    val (label, value) = Evidence.metricValue(result, field)
                    MetricCard(label, value, Modifier.weight(1f)) {
                        ask(label) { m -> listOf(Evidence.metric(result, field, m, Evidence.calcInfo(result, inputs, solve))) }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        val tm = s.optInt("target_month", -1).takeIf { !s.isNull("target_month") && it > 0 }
        val dm = s.optInt("depletion_month", -1).takeIf { !s.isNull("depletion_month") && it > 0 }
        if (tm != null) InfoLine("Target reached after " + Fmt.months(tm), Mali.Green)
        if (dm != null) InfoLine("Money runs out after " + Fmt.months(dm), Mali.Pink)

        // in words
        val sentences = result.optJSONArray("interpretation") ?: JSONArray()
        Card("In words", "Written from the numbers above. Once in your note you can edit it to say what you conclude.") {
            for (i in 0 until sentences.length()) Text(sentences.getString(i), fontSize = 15.sp, lineHeight = 22.sp, color = Mali.Ink)
            ActionChip("Insert explanation") { ask("Explanation in words") { m -> listOf(Evidence.interpretation(result, m, Evidence.calcInfo(result, inputs, solve))) } }
        }
        val assumptions = result.optJSONArray("assumptions") ?: JSONArray()
        Card("Assumptions behind these numbers", "") {
            for (i in 0 until assumptions.length()) {
                val a = assumptions.getJSONObject(i)
                Column {
                    Text(a.optString("label"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Mali.InkSoft)
                    Text(a.optString("value") + "  ·  " + kindWords(a.optString("kind")), fontSize = 15.sp, color = Mali.Ink)
                }
            }
            ActionChip("Insert assumptions") { ask("Assumptions") { m -> listOf(Evidence.assumptions(result, m, Evidence.calcInfo(result, inputs, solve))) } }
        }
        val method = result.optJSONArray("methodology") ?: JSONArray()
        Card("How this was calculated", "") {
            for (i in 0 until method.length()) Text("• " + method.getString(i), fontSize = 14.sp, lineHeight = 20.sp, color = Mali.InkSoft)
            ActionChip("Insert method") { ask("How this was calculated") { m -> listOf(Evidence.methodology(result, m, Evidence.calcInfo(result, inputs, solve))) } }
        }
        Text(
            "These figures are illustrations based on the assumptions you entered. They are not forecasts, guarantees or financial advice. " +
                "Model " + result.optString("model") + " " + result.optString("version") + ".",
            fontSize = 12.sp, lineHeight = 17.sp, color = Mali.InkSoft, modifier = Modifier.padding(bottom = 30.dp),
        )
    }
}

private fun kindWords(kind: String) = when (kind) {
    "user-entered" -> "you entered"
    "assumption" -> "an assumption"
    "sourced" -> "from a source"
    else -> kind
}

@Composable
internal fun MetricCard(label: String, value: String, modifier: Modifier, onInsert: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Mali.Surface).padding(14.dp)) {
        Text(label, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, color = Mali.InkSoft)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
        Spacer(Modifier.height(8.dp))
        Text("+ Insert", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Teal, modifier = Modifier.clickable(onClick = onInsert))
    }
}

@Composable
internal fun InfoLine(text: String, color: Color) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.10f)).padding(14.dp))
}

@Composable
internal fun Card(title: String, note: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Mali.Surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), fontSize = 12.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Teal)
        if (note.isNotBlank()) Text(note, fontSize = 13.sp, lineHeight = 18.sp, color = Mali.InkSoft)
        content()
    }
}

// ================================================================================================ graph
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GraphTab(outcome: CalcOutcome?, inputs: JSONObject, calc: JSONObject?, ask: (String, (String) -> List<Block>) -> Unit) {
    val result = outcome?.result
    if (result == null || calc == null) { EmptyResults(); return }
    val picked = remember { mutableStateListOf("closing", "total_contributed") }
    val pts = result.getJSONArray("series")
    val withInflation = inputs.optBoolean("include_inflation")
    val withdrawals = (result.getJSONObject("summary").optString("total_withdrawn").toDoubleOrNull() ?: 0.0) > 0
    val options = Evidence.SERIES.filter { (k, _, _) -> (k != "real_closing" || withInflation) && (k != "total_withdrawn" || withdrawals) }
    val keys = options.map { it.first }.filter { it in picked }.ifEmpty { listOf("closing") }
    val xs = (0 until pts.length()).map { pts.getJSONObject(it).optInt("month") / 12.0 }
    val names = keys.map { k -> options.first { it.first == k }.second }
    val values = keys.map { k -> (0 until pts.length()).map { pts.getJSONObject(it).optString(k, "0").toDoubleOrNull() ?: 0.0 } }
    val marks = buildList {
        inputs.optString("target").replace(",", "").toDoubleOrNull()?.let { add(ChartMark(y = it, label = "Target")) }
        val s = result.getJSONObject("summary")
        if (!s.isNull("depletion_month")) add(ChartMark(x = s.optInt("depletion_month") / 12.0, label = "Runs out"))
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Mali.Surface).padding(14.dp)) {
            LineChart(xs, chartSeries(names, values), marks = marks)
        }
        Text("Show", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Mali.InkSoft)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (k, name, _) ->
                ActionChip(name, selected = k in picked) { if (k in picked) { if (picked.size > 1) picked.remove(k) } else picked.add(k) }
            }
        }
        Text(
            "Insert this chart",
            color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Mali.Indigo)
                .clickable { ask("Chart: " + names.joinToString(", ")) { m -> listOf(Evidence.chart(result, keys, m, Evidence.calcInfo(result, inputs, calc.optString("solve").ifBlank { null }))) } }
                .padding(vertical = 14.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Text(
            "The chart shows the same numbers as the Table tab. Lines differ by dash pattern and point shape as well as colour.",
            fontSize = 12.sp, lineHeight = 17.sp, color = Mali.InkSoft,
        )
    }
}

@Composable
internal fun EmptyResults() {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No results yet", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
        Spacer(Modifier.height(6.dp))
        Text("Complete the Set up tab first.", color = Mali.InkSoft)
    }
}

// ================================================================================================ table
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TableTab(outcome: CalcOutcome?, inputs: JSONObject, solve: String?, calc: JSONObject?, ask: (String, (String) -> List<Block>) -> Unit) {
    val context = LocalContext.current
    val base = outcome?.result
    if (base == null || calc == null) { EmptyResults(); return }
    var granularity by remember { mutableStateOf("yearly") }
    var detail by remember { mutableStateOf<JSONObject?>(null) }
    var detailError by remember { mutableStateOf<String?>(null) }
    val cols = remember { mutableStateListOf("year", "opening", "contributions", "growth", "closing") }
    var excluded by remember { mutableStateOf<Set<Int>>(emptySet()) }

    LaunchedEffect(granularity, base) {
        detailError = null
        if (granularity != "yearly") {
            detail = null
            try {
                detail = CalcApi.run(inputs, solve, detail = true).result
            } catch (e: ApiError) {
                detailError = e.message
            }
        }
        excluded = emptySet()
    }

    val source = if (granularity == "yearly") base else detail
    // headers, rows and the index of each row in the full list
    val header: List<String>
    val rows = ArrayList<Pair<Int, List<String>>>()
    if (source == null) {
        header = emptyList()
    } else if (granularity == "yearly") {
        val annual = source.getJSONArray("annual")
        val shownCols = Evidence.TABLE_COLUMNS.filter { it.first in cols }
        header = shownCols.map { it.second }
        for (i in 0 until annual.length()) {
            val r = annual.getJSONObject(i)
            rows.add(i to shownCols.map { (k, _) -> if (k == "year") r.optString("year") else Fmt.money(r.optString(k)) })
        }
    } else {
        header = listOf("Month", "Balance", "In today's money", "Put in so far", "Withdrawn so far")
        val monthly = source.optJSONArray("monthly") ?: JSONArray()
        for (i in 0 until monthly.length()) {
            val r = monthly.getJSONObject(i)
            if (granularity == "quarterly" && r.optInt("month") % 3 != 0) continue
            rows.add(i to listOf(r.optString("month"), Fmt.money(r.optString("balance")), Fmt.money(r.optString("real_balance")), Fmt.money(r.optString("contributions")), Fmt.money(r.optString("withdrawn"))))
        }
    }
    val chosen = rows.filter { it.first !in excluded }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("yearly" to "Yearly", "quarterly" to "Quarterly", "monthly" to "Monthly").forEach { (k, l) ->
                    ActionChip(l, selected = granularity == k) { granularity = k }
                }
            }
            if (granularity == "yearly") {
                Text("Columns", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Mali.InkSoft)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Evidence.TABLE_COLUMNS.forEach { (k, l) ->
                        if (k != "year") ActionChip(l, selected = k in cols) { if (k in cols) cols.remove(k) else cols.add(k) }
                    }
                }
            }
            detailError?.let { Text(it, color = Mali.Pink, fontSize = 13.sp) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ActionChip("Insert ${chosen.size} rows", color = Mali.Indigo) {
                    ask("Table: " + chosen.size + " rows, " + granularity) { m ->
                        val c = Evidence.calcInfo(base, inputs, solve)
                        listOf(Evidence.table(source ?: base, Evidence.TABLE_COLUMNS.map { it.first }.filter { it == "year" || it in cols }, chosen.map { it.first }, granularity, m, c))
                    }
                }
                ActionChip("Share as CSV") {
                    val csv = (listOf(header) + chosen.map { it.second }).joinToString("\n") { r -> r.joinToString(",") { "\"" + it.replace(",", "") + "\"" } }
                    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, "MaliLab table.csv"); putExtra(Intent.EXTRA_TEXT, csv) }
                    context.startActivity(Intent.createChooser(send, "Share table"))
                }
                ActionChip(if (excluded.isEmpty()) "Clear all" else "Select all") { excluded = if (excluded.isEmpty()) rows.map { it.first }.toSet() else emptySet() }
            }
        }
        if (source == null) {
            Text(if (detailError == null) "Loading…" else "", Modifier.padding(16.dp), color = Mali.InkSoft)
        } else {
            val cellW = 118.dp
            Box(Modifier.weight(1f).fillMaxWidth().horizontalScroll(rememberScrollState())) {
                LazyColumn(Modifier.width(cellW * header.size + 52.dp).fillMaxHeight()) {
                    item {
                        Row(Modifier.background(Mali.TealTint).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.width(52.dp))
                            header.forEach { Text(it, Modifier.width(cellW), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Mali.TealDeep) }
                        }
                    }
                    itemsIndexed(rows) { n, (idx, cells) ->
                        val on = idx !in excluded
                        Row(Modifier.background(if (n % 2 == 0) Mali.Surface else Mali.Cream).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(on, { excluded = if (it) excluded - idx else excluded + idx }, modifier = Modifier.width(52.dp), colors = CheckboxDefaults.colors(checkedColor = Mali.Teal))
                            cells.forEach { Text(it, Modifier.width(cellW), fontSize = 13.sp, color = if (on) Mali.Ink else Mali.InkSoft.copy(alpha = 0.5f)) }
                        }
                    }
                }
            }
        }
    }
}

// ================================================================================================ compare
private class Preset(val name: String, val make: (JSONObject) -> JSONObject?)

private fun num(o: JSONObject, k: String): Double? = o.optString(k).replace(",", "").toDoubleOrNull()

private fun fmtNum(v: Double): String = String.format(java.util.Locale.US, "%.4f", v).trimEnd('0').trimEnd('.')

private val PRESETS = listOf(
    Preset("Return 2 points lower") { b -> num(b, "annual_return")?.let { JSONObject().put("annual_return", fmtNum(it - 2)) } },
    Preset("Return 2 points higher") { b -> num(b, "annual_return")?.let { JSONObject().put("annual_return", fmtNum(it + 2)) } },
    Preset("Save 20% more") { b -> num(b, "contribution")?.takeIf { it > 0 }?.let { JSONObject().put("contribution", fmtNum(it * 1.2)) } },
    Preset("Save 20% less") { b -> num(b, "contribution")?.takeIf { it > 0 }?.let { JSONObject().put("contribution", fmtNum(it * 0.8)) } },
    Preset("5 more years") { b -> num(b, "years")?.let { JSONObject().put("years", fmtNum(it + 5)) } },
    Preset("5 fewer years") { b -> num(b, "years")?.takeIf { it > 5 }?.let { JSONObject().put("years", fmtNum(it - 5)) } },
    Preset("Inflation 3 points higher") { b -> JSONObject().put("include_inflation", true).put("inflation", fmtNum((num(b, "inflation") ?: 0.0) + 3)) },
    Preset("Start with 50% more") { b -> num(b, "initial")?.takeIf { it > 0 }?.let { JSONObject().put("initial", fmtNum(it * 1.5)) } },
)

private val CUSTOM_FIELDS = listOf(
    "annual_return" to "Return %", "contribution" to "Saving", "years" to "Years", "inflation" to "Inflation %",
    "initial" to "Starting amount", "annual_fee" to "Fee %", "tax_on_growth" to "Tax %",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompareTab(outcome: CalcOutcome?, effective: JSONObject, ask: (String, (String) -> List<Block>) -> Unit, calc: JSONObject?) {
    val base = outcome?.result
    if (base == null || calc == null) { EmptyResults(); return }
    val scenarios = remember { mutableStateListOf<Pair<String, JSONObject>>() }
    var cmp by remember { mutableStateOf<JSONObject?>(null) }
    var cmpError by remember { mutableStateOf<String?>(null) }
    var field by remember { mutableStateOf("annual_return") }
    var value by remember { mutableStateOf("") }
    val effKey = effective.toString() + scenarios.joinToString { it.first + it.second }

    LaunchedEffect(effKey) {
        cmpError = null
        if (scenarios.isEmpty()) { cmp = null; return@LaunchedEffect }
        delay(500)
        try {
            cmp = CalcApi.compare(effective, scenarios.toList())
        } catch (e: ApiError) {
            cmp = null
            cmpError = if (e.code == "invalid_inputs") (CalcApi.fieldErrors(e).values.firstOrNull() ?: e.message) else e.message
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Change one assumption at a time to see how much it matters. These are what-ifs to learn from, not predictions.",
            fontSize = 14.sp, lineHeight = 20.sp, color = Mali.InkSoft,
        )
        Card("Quick what-ifs", "Each one is an assumption, not a forecast.") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PRESETS.forEach { p ->
                    val overrides = p.make(effective)
                    if (overrides != null && scenarios.size < 5 && scenarios.none { it.first == p.name }) {
                        ActionChip("+ " + p.name) { scenarios.add(p.name to overrides) }
                    }
                }
            }
        }
        Card("Your own what-if", "Pick what to change and enter the new value.") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CUSTOM_FIELDS.forEach { (k, l) -> ActionChip(l, selected = field == k) { field = k } }
            }
            OutlinedTextField(
                value, { value = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("New value") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), colors = fieldColors(),
            )
            ActionChip("Add what-if", color = Mali.Indigo) {
                if (value.isNotBlank() && scenarios.size < 5) {
                    val label = CUSTOM_FIELDS.first { it.first == field }.second
                    val o = JSONObject().put(field, value)
                    if (field == "inflation") o.put("include_inflation", true)
                    if (field == "annual_fee") o.put("include_fees", true)
                    if (field == "tax_on_growth") o.put("include_tax", true)
                    scenarios.add("$label: $value" to o)
                    value = ""
                }
            }
            if (scenarios.size >= 5) Text("You can compare up to 5 what-ifs.", fontSize = 12.sp, color = Mali.InkSoft)
        }
        cmpError?.let { Text(it, color = Mali.Pink, fontSize = 14.sp) }

        val c = cmp
        if (scenarios.isEmpty()) {
            Text("Add a what-if above to compare it with your base case.", fontSize = 14.sp, color = Mali.InkSoft)
        } else if (c != null) {
            val rows = c.getJSONArray("scenarios")
            for (i in 0 until rows.length()) {
                val r = rows.getJSONObject(i)
                val delta = r.optString("delta_final_balance")
                val d = delta.toDoubleOrNull() ?: 0.0
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Mali.Surface).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(r.optString("name"), Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                        if (i > 0) ActionChip("Remove", color = Mali.Pink) { scenarios.removeAt(i - 1) }
                    }
                    val s = r.getJSONObject("result").getJSONObject("summary")
                    Text("Final balance " + Fmt.kes(s.optString("final_balance")), fontSize = 15.sp, color = Mali.Ink)
                    if (i > 0) {
                        val word = if (d > 0) "▲ " + Fmt.kes(delta) + " more than the base case" else if (d < 0) "▼ " + Fmt.kes(delta.removePrefix("-")) + " less than the base case" else "Same as the base case"
                        Text(word, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (d > 0) Mali.Green else if (d < 0) Mali.Pink else Mali.InkSoft)
                        val ch = r.optJSONArray("changes") ?: JSONArray()
                        for (j in 0 until ch.length()) {
                            val x = ch.getJSONObject(j)
                            if (x.optString("from") != x.optString("to") && !x.optString("field").startsWith("include_")) Text(x.optString("label") + ": " + x.optString("from") + " → " + x.optString("to"), fontSize = 13.sp, color = Mali.InkSoft)
                        }
                    } else {
                        Text("Your Set up, with no changes.", fontSize = 13.sp, color = Mali.InkSoft)
                    }
                }
            }
            // overlay chart
            val first = rows.getJSONObject(0).getJSONObject("result").getJSONArray("series")
            val xs = (0 until first.length()).map { first.getJSONObject(it).optInt("month") / 12.0 }
            val names = (0 until rows.length()).map { rows.getJSONObject(it).optString("name") }
            val values = (0 until rows.length()).map { r ->
                val pts = rows.getJSONObject(r).getJSONObject("result").getJSONArray("series")
                (0 until pts.length()).map { pts.getJSONObject(it).optString("closing", "0").toDoubleOrNull() ?: 0.0 }
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Mali.Surface).padding(14.dp)) {
                Text("BALANCE UNDER EACH WHAT-IF", fontSize = 12.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Teal)
                Spacer(Modifier.height(8.dp))
                LineChart(xs, chartSeries(names, values))
            }
            val cc = Evidence.calcInfo(c.getJSONArray("scenarios").getJSONObject(0).getJSONObject("result"), effective, null, scenarios.toList())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionChip("Insert comparison table", color = Mali.Indigo) { ask("Comparison of what-ifs") { m -> listOf(Evidence.scenarios(c, m, cc)) } }
                ActionChip("Insert comparison chart", color = Mali.Indigo) { ask("Comparison chart") { m -> listOf(Evidence.compareChart(c, m, cc)) } }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
