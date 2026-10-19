package online.malilab.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

// ============================================================================================ number words
object Fmt {
    /** 573211.17 -> 573,211.17 */
    fun money(s: String?): String {
        if (s.isNullOrBlank()) return ""
        val neg = s.startsWith("-")
        val parts = s.removePrefix("-").split(".")
        val whole = parts[0]
        if (whole.isEmpty() || !whole.all { it.isDigit() }) return s
        val grouped = whole.reversed().chunked(3).joinToString(",").reversed()
        return (if (neg) "-" else "") + grouped + (if (parts.size > 1) "." + parts[1] else "")
    }

    fun kes(s: String?): String = if (s.isNullOrBlank()) "" else "KES " + money(s)

    /** 1250000 -> 1.25M, 45000 -> 45K. For chart axes. */
    fun compact(v: Double): String {
        val a = Math.abs(v)
        val sign = if (v < 0) "-" else ""
        fun trim(x: Double): String = String.format(java.util.Locale.US, "%.2f", x).trimEnd('0').trimEnd('.')
        return when {
            a >= 1_000_000_000 -> sign + trim(a / 1_000_000_000) + "B"
            a >= 1_000_000 -> sign + trim(a / 1_000_000) + "M"
            a >= 1_000 -> sign + trim(a / 1_000) + "K"
            else -> sign + trim(a)
        }
    }

    fun months(m: Int): String {
        val y = m / 12
        val r = m % 12
        return when {
            y == 0 -> "$r month" + if (r == 1) "" else "s"
            r == 0 -> "$y year" + if (y == 1) "" else "s"
            else -> "$y year${if (y == 1) "" else "s"} $r month${if (r == 1) "" else "s"}"
        }
    }

    fun pct(s: String?): String = if (s.isNullOrBlank()) "" else s.trimEnd('0').trimEnd('.') + "%"
}

// ============================================================================================ goals
enum class Goal(val key: String, val title: String, val blurb: String, val solve: String?) {
    Grow("grow", "Grow my savings", "See where regular saving could take you.", null),
    Target("target", "Reach a target", "How much to save each period.", "contribution_for_target"),
    Time("time", "How long will it take?", "Time needed to reach a target.", "years_to_target"),
    Income("income", "Live off my savings", "Will the money last?", null),
    Need("need", "How much do I need?", "Savings needed to fund withdrawals.", "capital_for_withdrawal"),
    MaxWithdrawal("maxwd", "How much can I take out?", "The largest steady withdrawal.", "max_withdrawal");

    val needsWithdrawals: Boolean get() = this == Income || this == Need || this == MaxWithdrawal
    val needsTarget: Boolean get() = this == Target || this == Time

    companion object {
        fun of(solve: String?, hasWithdrawals: Boolean): Goal = when (solve) {
            "contribution_for_target" -> Target
            "years_to_target" -> Time
            "capital_for_withdrawal" -> Need
            "max_withdrawal" -> MaxWithdrawal
            else -> if (hasWithdrawals) Income else Grow
        }
    }
}

val FREQUENCIES = listOf("monthly" to "Monthly", "quarterly" to "Quarterly", "semiannual" to "Half-yearly", "annually" to "Yearly")
val COMPOUNDINGS = listOf("daily" to "Daily", "monthly" to "Monthly", "quarterly" to "Quarterly", "semiannual" to "Half-yearly", "annual" to "Yearly")
val WITHDRAWAL_MODES = listOf(
    "none" to "None",
    "fixed" to "Fixed amount",
    "inflation_adjusted" to "Fixed, rising with inflation",
    "pct_start" to "% of starting balance",
    "pct_current" to "% of current balance",
)

// ============================================================================================ the form
/** Everything the person types into the calculator, kept as text so half-typed numbers are never rejected on the phone. */
class CalcForm {
    var goal by mutableStateOf(Goal.Grow)
    val text = mutableStateMapOf<String, String>()
    val flags = mutableStateMapOf<String, Boolean>()
    val pauses = mutableStateListOf<Pair<String, String>>()
    val lumps = mutableStateListOf<Pair<String, String>>()
    var withdrawalsOpen by mutableStateOf(false)

    init {
        reset()
    }

    fun reset() {
        text.clear(); flags.clear(); pauses.clear(); lumps.clear()
        goal = Goal.Grow
        withdrawalsOpen = false
        text["years"] = "10"
        text["annual_return"] = "10"
        text["rate_type"] = "nominal"
        text["compounding"] = "monthly"
        text["contribution_frequency"] = "monthly"
        text["contribution_timing"] = "end"
        text["w_mode"] = "none"
        text["w_frequency"] = "monthly"
        flags["include_fees"] = false
        flags["include_tax"] = false
        flags["include_inflation"] = false
    }

    fun get(key: String): String = text[key].orEmpty()
    fun set(key: String, value: String) { text[key] = value }

    private fun clean(v: String) = v.replace(",", "").replace(" ", "").trim()

    private fun effectiveMode(): String {
        val m = get("w_mode")
        return if (goal.needsWithdrawals && m == "none") "fixed" else if (!goal.needsWithdrawals && !withdrawalsOpen) "none" else m
    }

    /** The calculation inputs in the shape the website expects. Blank fields are left out so the website can name what is missing. */
    fun toInputs(): JSONObject {
        val o = JSONObject()
        for (k in listOf("initial", "years", "annual_return", "contribution", "annual_increase", "annual_fee", "tax_on_growth", "inflation", "target", "contribution_start_year", "contribution_end_year")) {
            val v = clean(get(k))
            if (v.isNotEmpty()) o.put(k, v)
        }
        for (k in listOf("rate_type", "compounding", "contribution_frequency", "contribution_timing")) {
            if (get(k).isNotBlank()) o.put(k, get(k))
        }
        for (k in listOf("include_fees", "include_tax", "include_inflation")) o.put(k, flags[k] == true)
        if (get("start_date").isNotBlank()) o.put("start_date", get("start_date").trim())
        val mode = effectiveMode()
        val w = JSONObject().put("mode", mode)
        if (mode != "none") {
            w.put("frequency", get("w_frequency").ifBlank { "monthly" })
            var amount = clean(get("w_amount"))
            if (goal == Goal.MaxWithdrawal && amount.isEmpty()) amount = "1"
            if (amount.isNotEmpty()) w.put("amount", amount)
            if (clean(get("w_percent")).isNotEmpty()) w.put("percent", clean(get("w_percent")))
            if (clean(get("w_start")).isNotEmpty()) w.put("start_year", clean(get("w_start")))
            if (clean(get("w_end")).isNotEmpty()) w.put("end_year", clean(get("w_end")))
        }
        o.put("withdrawal", w)
        val pa = JSONArray()
        pauses.filter { clean(it.first).isNotEmpty() && clean(it.second).isNotEmpty() }
            .forEach { pa.put(JSONObject().put("from_year", clean(it.first)).put("to_year", clean(it.second))) }
        o.put("pauses", pa)
        val la = JSONArray()
        lumps.filter { clean(it.first).isNotEmpty() && clean(it.second).isNotEmpty() }
            .forEach { la.put(JSONObject().put("year", clean(it.first)).put("amount", clean(it.second))) }
        o.put("lump_sums", la)
        return o
    }

    fun solveKind(): String? = goal.solve

    /** Fills the form from saved inputs, so a calculation inside a note can be opened and changed. */
    fun load(inputs: JSONObject, solve: String?) {
        reset()
        for (k in listOf("initial", "years", "annual_return", "contribution", "annual_increase", "annual_fee", "tax_on_growth", "inflation", "target", "contribution_start_year", "contribution_end_year", "rate_type", "compounding", "contribution_frequency", "contribution_timing", "start_date")) {
            if (inputs.has(k) && !inputs.isNull(k)) text[k] = inputs.optString(k)
        }
        for (k in listOf("include_fees", "include_tax", "include_inflation")) flags[k] = inputs.optBoolean(k, false)
        val w = inputs.optJSONObject("withdrawal")
        if (w != null) {
            text["w_mode"] = w.optString("mode", "none")
            text["w_frequency"] = w.optString("frequency", "monthly")
            for ((src, dst) in listOf("amount" to "w_amount", "percent" to "w_percent", "start_year" to "w_start", "end_year" to "w_end")) {
                if (w.has(src) && !w.isNull(src)) text[dst] = w.optString(src)
            }
            if (text["w_mode"] != "none") withdrawalsOpen = true
            if (text["w_amount"] == "0") text.remove("w_amount")
            if (text["w_percent"] == "0") text.remove("w_percent")
            if (text["w_start"] == "0") text.remove("w_start")
        }
        for (k in listOf("initial", "contribution", "annual_increase", "annual_fee", "tax_on_growth", "inflation", "contribution_start_year")) {
            if (text[k] == "0") text.remove(k)
        }
        inputs.optJSONArray("pauses")?.let { a ->
            for (i in 0 until a.length()) a.getJSONObject(i).let { pauses.add(it.optString("from_year") to it.optString("to_year")) }
        }
        inputs.optJSONArray("lump_sums")?.let { a ->
            for (i in 0 until a.length()) a.getJSONObject(i).let { lumps.add(it.optString("year") to it.optString("amount")) }
        }
        goal = Goal.of(solve, text["w_mode"] != "none" && text["w_mode"] != null)
    }
}

// ============================================================================================ talking to the website
class CalcOutcome(val result: JSONObject, val solution: JSONObject?, val noSolution: String?)

object CalcApi {
    suspend fun run(inputs: JSONObject, solve: String?, detail: Boolean = false): CalcOutcome {
        val body = JSONObject().put("inputs", inputs).put("detail", detail)
        if (solve != null) body.put("solve", JSONObject().put("kind", solve))
        val o = Auth.api("POST", "/api/v1/calc/compound/", body)
        return CalcOutcome(o.getJSONObject("result"), o.optJSONObject("solution"), if (o.has("no_solution")) o.optString("no_solution") else null)
    }

    suspend fun compare(inputs: JSONObject, scenarios: List<Pair<String, JSONObject>>): JSONObject {
        val arr = JSONArray()
        scenarios.forEach { (name, overrides) -> arr.put(JSONObject().put("name", name).put("overrides", overrides)) }
        return Auth.api("POST", "/api/v1/calc/compare/", JSONObject().put("inputs", inputs).put("scenarios", arr))
    }

    /** Messages for the fields the website could not accept, keyed by field name. */
    fun fieldErrors(e: ApiError): Map<String, String> {
        val arr = e.body.optJSONArray("fields") ?: return emptyMap()
        val map = LinkedHashMap<String, String>()
        for (i in 0 until arr.length()) {
            val f = arr.optJSONObject(i) ?: continue
            map.putIfAbsent(f.optString("field"), f.optString("message"))
        }
        return map
    }

    /** The inputs with the solved unknown filled in, so charts, tables and comparisons describe the answer itself. */
    fun effective(inputs: JSONObject, solution: JSONObject?): JSONObject {
        val o = JSONObject(inputs.toString())
        val value = solution?.optString("value").orEmpty()
        when (solution?.optString("kind")) {
            "contribution_for_target" -> o.put("contribution", value)
            "years_to_target" -> o.put("years", (solution.optInt("months") / 12.0).toString())
            "capital_for_withdrawal" -> o.put("initial", value)
            "max_withdrawal" -> (o.optJSONObject("withdrawal") ?: JSONObject().also { o.put("withdrawal", it) }).put("amount", value)
        }
        return o
    }
}

// ============================================================================================ evidence blocks
/** Turns calculator results into typed note blocks. Every block carries what it needs to be understood later. */
object Evidence {
    val METRICS = listOf(
        "final_balance" to "Final balance",
        "final_real_balance" to "Final balance in today's money",
        "total_contributed" to "Total put in",
        "net_growth" to "Growth after tax and fees",
        "total_growth" to "Growth before tax and fees",
        "total_tax" to "Tax on growth",
        "total_fees" to "Fees paid",
        "total_withdrawn" to "Total withdrawn",
        "growth_share" to "Growth as a share of the balance",
        "effective_annual_rate" to "Effective yearly rate",
    )

    val SERIES = listOf(
        Triple("closing", "Balance", "Balance"),
        Triple("total_contributed", "Money put in", "Put in"),
        Triple("total_growth", "Growth earned", "Growth"),
        Triple("real_closing", "Balance in today's money", "Today's money"),
        Triple("total_withdrawn", "Withdrawn so far", "Withdrawn"),
    )

    val TABLE_COLUMNS = listOf(
        "year" to "Year", "opening" to "Opening", "contributions" to "Put in", "growth" to "Growth", "tax" to "Tax",
        "fees" to "Fees", "withdrawals" to "Withdrawn", "closing" to "Closing", "real_closing" to "In today's money",
    )

    private fun stamp(): String = NoteStore.nowIso()

    fun calcInfo(result: JSONObject, inputs: JSONObject, solve: String?, scenarios: List<Pair<String, JSONObject>>? = null): JSONObject {
        val c = JSONObject()
            .put("model", result.optString("model"))
            .put("version", result.optString("version"))
            .put("calculated_at", result.optString("calculated_at", stamp()))
            .put("inputs", JSONObject(inputs.toString()))
        if (solve != null) c.put("solve", solve)
        if (scenarios != null) {
            val a = JSONArray()
            scenarios.forEach { (n, o) -> a.put(JSONObject().put("name", n).put("overrides", o)) }
            c.put("scenarios", a)
        }
        return c
    }

    private fun frame(type: String, mode: String, calc: JSONObject, title: String?): JSONObject {
        val o = JSONObject().put("type", type).put("mode", mode).put("calc", calc)
        if (mode == "snapshot") o.put("captured_at", stamp())
        if (title != null) o.put("title", title)
        return o
    }

    private fun block(o: JSONObject, id: String? = null): Block =
        Block(id = id ?: java.util.UUID.randomUUID().toString(), type = o.getString("type"), raw = o)

    fun metricValue(result: JSONObject, field: String): Pair<String, String> {
        val s = result.optJSONObject("summary") ?: JSONObject()
        val label = METRICS.firstOrNull { it.first == field }?.second ?: field
        val raw = s.optString(field)
        val shown = when (field) {
            "growth_share", "effective_annual_rate" -> Fmt.pct(raw)
            else -> Fmt.kes(raw)
        }
        return label to shown
    }

    fun metric(result: JSONObject, field: String, mode: String, calc: JSONObject, id: String? = null): Block {
        val (label, shown) = metricValue(result, field)
        val s = result.optJSONObject("summary") ?: JSONObject()
        val o = frame("metric", mode, calc, null)
            .put("label", label).put("value", shown).put("raw", s.optString(field)).put("field", field)
            .put("unit", if (field == "growth_share" || field == "effective_annual_rate") "%" else "KES")
        return block(o, id)
    }

    /** A headline for a solved answer, e.g. "Monthly saving needed". */
    fun solutionMetric(solution: JSONObject, result: JSONObject, mode: String, calc: JSONObject, id: String? = null): Block {
        val kind = solution.optString("kind")
        val label = when (kind) {
            "contribution_for_target" -> "Saving needed each period"
            "years_to_target" -> "Time to reach the target"
            "capital_for_withdrawal" -> "Starting amount needed"
            else -> "Largest steady withdrawal"
        }
        val shown = if (kind == "years_to_target") Fmt.months(solution.optInt("months")) else Fmt.kes(solution.optString("value"))
        val o = frame("metric", mode, calc, null)
            .put("label", label).put("value", shown).put("raw", solution.optString("value")).put("field", "solution")
            .put("unit", if (kind == "years_to_target") "months" else "KES").put("caption", solution.optString("explanation"))
        return block(o, id)
    }

    private fun yearLabel(month: Int): String {
        val y = month / 12.0
        return if (month % 12 == 0) (month / 12).toString() else String.format(java.util.Locale.US, "%.1f", y)
    }

    fun chart(result: JSONObject, keys: List<String>, mode: String, calc: JSONObject, id: String? = null): Block {
        val pts = result.getJSONArray("series")
        val xs = JSONArray()
        for (i in 0 until pts.length()) xs.put(yearLabel(pts.getJSONObject(i).optInt("month")))
        val sArr = JSONArray()
        for (k in keys) {
            val name = SERIES.firstOrNull { it.first == k }?.second ?: k
            val vals = JSONArray()
            for (i in 0 until pts.length()) vals.put(pts.getJSONObject(i).optString(k, "0"))
            sArr.put(JSONObject().put("key", k).put("name", name).put("values", vals))
        }
        val o = frame("chart", mode, calc, "How the money changes over time")
            .put("x_label", "Year").put("y_label", "Amount").put("unit", "KES").put("x", xs).put("series", sArr)
            .put("view", JSONObject().put("kind", "series").put("keys", JSONArray(keys)))
        return block(o, id)
    }

    /** [rows] are indexes into the yearly rows; [granularity] is "yearly", "quarterly" or "monthly". */
    fun table(result: JSONObject, columns: List<String>, rows: List<Int>?, granularity: String, mode: String, calc: JSONObject, id: String? = null): Block {
        val cols = JSONArray()
        val rowsOut = JSONArray()
        if (granularity == "yearly") {
            val annual = result.getJSONArray("annual")
            columns.forEach { k -> cols.put(JSONObject().put("key", k).put("label", TABLE_COLUMNS.firstOrNull { it.first == k }?.second ?: k).put("kind", if (k == "year") "text" else "money")) }
            for (i in 0 until annual.length()) {
                if (rows != null && i !in rows) continue
                val r = annual.getJSONObject(i)
                val row = JSONArray()
                columns.forEach { k -> row.put(if (k == "year") r.optString("year") else Fmt.money(r.optString(k))) }
                rowsOut.put(row)
            }
        } else {
            val labels = listOf("month" to "Month", "balance" to "Balance", "real_balance" to "In today's money", "contributions" to "Put in so far", "withdrawn" to "Withdrawn so far")
            labels.forEach { (k, l) -> cols.put(JSONObject().put("key", k).put("label", l).put("kind", if (k == "month") "text" else "money")) }
            val monthly = result.optJSONArray("monthly") ?: JSONArray()
            for (i in 0 until monthly.length()) {
                val r = monthly.getJSONObject(i)
                if (granularity == "quarterly" && r.optInt("month") % 3 != 0) continue
                if (rows != null && i !in rows) continue
                val row = JSONArray()
                labels.forEach { (k, _) -> row.put(if (k == "month") r.optString("month") else Fmt.money(r.optString(k))) }
                rowsOut.put(row)
            }
        }
        val o = frame("table", mode, calc, if (granularity == "yearly") "Year by year" else if (granularity == "quarterly") "Quarter by quarter" else "Month by month")
            .put("columns", cols).put("rows", rowsOut)
            .put("view", JSONObject().put("kind", "table").put("granularity", granularity).put("columns", JSONArray(columns)))
        if (rows != null) o.getJSONObject("view").put("rows", JSONArray(rows))
        return block(o, id)
    }

    fun assumptions(result: JSONObject, mode: String, calc: JSONObject, id: String? = null): Block {
        val items = JSONArray()
        val src = result.optJSONArray("assumptions") ?: JSONArray()
        for (i in 0 until src.length()) items.put(src.getJSONObject(i))
        return block(frame("assumptions", mode, calc, "Assumptions behind these numbers").put("items", items), id)
    }

    fun methodology(result: JSONObject, mode: String, calc: JSONObject, id: String? = null): Block {
        val lines = JSONArray()
        val src = result.optJSONArray("methodology") ?: JSONArray()
        for (i in 0 until src.length()) lines.put(src.getString(i))
        return block(frame("methodology", mode, calc, "How this was calculated").put("lines", lines), id)
    }

    fun interpretation(result: JSONObject, mode: String, calc: JSONObject, id: String? = null): Block {
        val sentences = JSONArray()
        val src = result.optJSONArray("interpretation") ?: JSONArray()
        for (i in 0 until src.length()) sentences.put(src.getString(i))
        return block(frame("interpretation", mode, calc, "What this shows").put("sentences", sentences).put("generated", true), id)
    }

    fun source(text: String = "", url: String = ""): Block =
        Block(type = "source", raw = JSONObject().put("type", "source").put("text", text).put("url", url))

    fun scenarios(cmp: JSONObject, mode: String, calc: JSONObject, id: String? = null): Block {
        val rows = cmp.getJSONArray("scenarios")
        val out = JSONArray()
        for (i in 0 until rows.length()) {
            val r = rows.getJSONObject(i)
            val res = r.getJSONObject("result")
            val s = res.getJSONObject("summary")
            val ch = JSONArray()
            val src = r.optJSONArray("changes") ?: JSONArray()
            for (j in 0 until src.length()) {
                val c = src.getJSONObject(j)
                if (c.optString("field").startsWith("include_")) continue
                ch.put(JSONObject().put("label", c.optString("label")).put("from", c.optString("from")).put("to", c.optString("to")))
            }
            out.put(
                JSONObject().put("name", r.optString("name")).put("changes", ch)
                    .put("final_balance", s.optString("final_balance")).put("final_real_balance", s.optString("final_real_balance"))
                    .put("total_contributed", s.optString("total_contributed")).put("delta", r.optString("delta_final_balance")),
            )
        }
        return block(frame("scenarios", mode, calc, "Comparing what-ifs").put("scenarios", out), id)
    }

    fun compareChart(cmp: JSONObject, mode: String, calc: JSONObject, id: String? = null): Block {
        val rows = cmp.getJSONArray("scenarios")
        val first = rows.getJSONObject(0).getJSONObject("result").getJSONArray("series")
        val xs = JSONArray()
        for (i in 0 until first.length()) xs.put(yearLabel(first.getJSONObject(i).optInt("month")))
        val sArr = JSONArray()
        for (r in 0 until rows.length()) {
            val row = rows.getJSONObject(r)
            val pts = row.getJSONObject("result").getJSONArray("series")
            val vals = JSONArray()
            for (i in 0 until pts.length()) vals.put(pts.getJSONObject(i).optString("closing", "0"))
            sArr.put(JSONObject().put("key", "s$r").put("name", row.optString("name")).put("values", vals))
        }
        val o = frame("chart", mode, calc, "Balance under each what-if")
            .put("x_label", "Year").put("y_label", "Balance").put("unit", "KES").put("x", xs).put("series", sArr)
            .put("view", JSONObject().put("kind", "compare"))
        return block(o, id)
    }

    /** Re-runs the saved calculation and rebuilds the block with fresh numbers. Throws ApiError when the website cannot be reached. */
    suspend fun refresh(b: Block): Block {
        val raw = b.raw ?: return b
        val calc = raw.optJSONObject("calc") ?: return b
        val inputs = calc.optJSONObject("inputs") ?: return b
        val view = raw.optJSONObject("view")
        val newCalcBase = { res: JSONObject, solve: String?, sc: List<Pair<String, JSONObject>>? -> calcInfo(res, inputs, solve, sc) }
        if (raw.optString("type") == "scenarios" || view?.optString("kind") == "compare") {
            val list = ArrayList<Pair<String, JSONObject>>()
            val arr = calc.optJSONArray("scenarios") ?: JSONArray()
            for (i in 0 until arr.length()) arr.getJSONObject(i).let { list.add(it.optString("name") to (it.optJSONObject("overrides") ?: JSONObject())) }
            val cmp = CalcApi.compare(inputs, list)
            val base = cmp.getJSONArray("scenarios").getJSONObject(0).getJSONObject("result")
            val c = newCalcBase(base, null, list)
            return if (raw.optString("type") == "scenarios") scenarios(cmp, "live", c, b.id) else compareChart(cmp, "live", c, b.id)
        }
        val solve = calc.optString("solve").ifBlank { null }
        val granularity = view?.optString("granularity").orEmpty().ifBlank { "yearly" }
        val out = CalcApi.run(inputs, solve, detail = raw.optString("type") == "table" && granularity != "yearly")
        val c = newCalcBase(out.result, solve, null)
        return when (raw.optString("type")) {
            "metric" -> if (raw.optString("field") == "solution" && out.solution != null) solutionMetric(out.solution, out.result, "live", c, b.id)
            else metric(out.result, raw.optString("field", "final_balance"), "live", c, b.id)
            "chart" -> {
                val keys = view?.optJSONArray("keys")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: listOf("closing")
                chart(out.result, keys, "live", c, b.id)
            }
            "table" -> {
                val cols = view?.optJSONArray("columns")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: TABLE_COLUMNS.map { it.first }
                val rows = view?.optJSONArray("rows")?.let { a -> (0 until a.length()).map { a.getInt(it) } }
                table(out.result, cols, rows, granularity, "live", c, b.id)
            }
            "assumptions" -> assumptions(out.result, "live", c, b.id)
            "methodology" -> methodology(out.result, "live", c, b.id)
            "interpretation" -> interpretation(out.result, "live", c, b.id)
            else -> b
        }
    }
}
