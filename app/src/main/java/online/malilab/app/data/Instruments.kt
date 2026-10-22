package online.malilab.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

// ============================================================================================ what the person can pick
/** One input on an instrument form. kind: money, number, choice or switch. */
class FieldSpec(
    val key: String,
    val label: String,
    val kind: String = "number",
    val options: List<Pair<String, String>> = emptyList(),
    val hint: String = "",
    val default: String = "",
    val advanced: Boolean = false,
)

class InstrumentSpec(val model: String, val title: String, val blurb: String, val fields: List<FieldSpec>)

val INSTRUMENT_MODELS = listOf("tbill", "tbond", "mmf", "sacco", "loan", "compare")

/** Analyses of a savings plan (what matters most, stress tests). They run on the same inputs as the savings calculator. */
val ANALYSIS_MODELS = listOf("sensitivity", "stress")

private val TAX_FIELDS = listOf(
    FieldSpec("tax_exempt", "I am exempt from this tax", "switch", hint = "For example, a registered pension fund with a KRA exemption certificate.", advanced = true),
    FieldSpec("tax_rate", "Use a different tax rate (%)", "number", hint = "Leave empty to use the rate from the source shown in Sources.", advanced = true),
)

val INSTRUMENTS: List<InstrumentSpec> = listOf(
    InstrumentSpec(
        "tbill", "Treasury bill", "Lend to the government for 91, 182 or 364 days. You pay less than face value and get the face value back.",
        listOf(
            FieldSpec("amount", "Amount", "money", default = "100000"),
            FieldSpec("amount_is", "The amount is", "choice", listOf("face" to "Face value (paid at maturity)", "cost" to "Cash I will pay"), default = "face"),
            FieldSpec("tenor_days", "Bill length", "choice", listOf("91" to "91 days", "182" to "182 days", "364" to "364 days"), default = "364"),
            FieldSpec("yield_pct", "Yield (% a year)", "number", hint = "Use the latest auction result published by CBK."),
            FieldSpec("rollovers", "Buy again this many times in a row", "number", hint = "1 means a single bill.", default = "1", advanced = true),
        ) + TAX_FIELDS,
    ),
    InstrumentSpec(
        "tbond", "Treasury bond", "A longer loan to the government that pays interest (a coupon) every six months.",
        listOf(
            FieldSpec("face", "Face value", "money", default = "100000"),
            FieldSpec("coupon_rate", "Coupon rate (% a year)", "number", hint = "Printed on the bond. Paid half-yearly."),
            FieldSpec("tenor_years", "Years to maturity", "number", hint = "Whole or half years, such as 5 or 7.5."),
            FieldSpec("ytm_pct", "Yield you pay (% a year)", "number", hint = "The yield at which you buy. Equal to the coupon means you pay face value."),
            FieldSpec("infrastructure", "Infrastructure bond", "switch", hint = "CBK states these earn tax-free interest."),
        ) + TAX_FIELDS,
    ),
    InstrumentSpec(
        "mmf", "Money market fund", "A pooled fund that holds short-term debt. Yields change daily.",
        listOf(
            FieldSpec("initial", "Starting amount", "money", default = "50000"),
            FieldSpec("contribution", "Monthly top-up", "money", default = "5000"),
            FieldSpec("years", "Years", "number", default = "5"),
            FieldSpec("yield_pct", "Yield (% a year)", "number", hint = "As advertised by the fund. Effective annual yield."),
            FieldSpec("yield_basis", "The advertised yield is", "choice", listOf("gross" to "Before tax", "net" to "After tax"), hint = "Most funds advertise before withholding tax.", default = "gross"),
            FieldSpec("inflation", "Yearly inflation to compare with (%)", "number", hint = "Optional. Shows the value in today's money.", advanced = true),
        ) + TAX_FIELDS,
    ),
    InstrumentSpec(
        "sacco", "SACCO savings", "Deposits earn a dividend each year, decided by your SACCO at its annual meeting.",
        listOf(
            FieldSpec("initial", "Deposits you already have", "money", default = "0"),
            FieldSpec("contribution", "Monthly deposit", "money", default = "5000"),
            FieldSpec("years", "Years", "number", default = "5"),
            FieldSpec("dividend_pct", "Dividend rate (% a year)", "number", hint = "What your SACCO paid recently is a fair guess. It is not promised."),
            FieldSpec("payout", "Dividends are", "choice", listOf("reinvest" to "Added to my deposits", "cash" to "Paid out in cash"), default = "reinvest"),
        ) + TAX_FIELDS,
    ),
    InstrumentSpec(
        "loan", "Loan", "See what a loan really costs, on a reducing balance or a flat rate, including fees.",
        listOf(
            FieldSpec("principal", "Loan amount", "money", default = "100000"),
            FieldSpec("rate_pct", "Quoted interest rate (% a year)", "number"),
            FieldSpec("term_months", "Repay over (months)", "number", default = "12"),
            FieldSpec("method", "The rate is charged", "choice", listOf("reducing" to "On the balance I still owe", "flat" to "Flat, on the full amount"), default = "reducing"),
            FieldSpec("processing_fee_pct", "Processing fee (% of the loan)", "number", hint = "Taken from the money you receive.", advanced = true),
            FieldSpec("insurance_pct", "Insurance (% of the loan)", "number", advanced = true),
        ),
    ),
)

fun instrumentSpec(model: String): InstrumentSpec =
    if (model == "compare") COMPARE_SPEC else INSTRUMENTS.first { it.model == model }

val COMPARE_SPEC = InstrumentSpec(
    "compare", "Compare options", "Put the same money in different places for the same time and see the numbers side by side.", emptyList(),
)

/** What every calculator form can do, so one screen can host any of them. */
interface LabForm {
    fun toInputs(): JSONObject
    fun load(inputs: JSONObject)
    fun reset()
}

/** Everything typed into an instrument form, kept as text so half-typed numbers are never rejected on the phone. */
class InstrumentForm(val spec: InstrumentSpec) : LabForm {
    val text = mutableStateMapOf<String, String>()
    val flags = mutableStateMapOf<String, Boolean>()

    init { reset() }

    override fun reset() {
        text.clear(); flags.clear()
        for (f in spec.fields) {
            if (f.kind == "switch") flags[f.key] = f.default == "true"
            else if (f.default.isNotEmpty()) text[f.key] = f.default
        }
    }

    override fun toInputs(): JSONObject {
        val o = JSONObject()
        for (f in spec.fields) {
            if (f.kind == "switch") {
                if (flags[f.key] == true) o.put(f.key, true)
            } else {
                val v = text[f.key].orEmpty().replace(",", "").replace(" ", "").trim()
                if (v.isNotEmpty()) o.put(f.key, v)
            }
        }
        return o
    }

    override fun load(inputs: JSONObject) {
        reset()
        for (f in spec.fields) {
            if (!inputs.has(f.key) || inputs.isNull(f.key)) continue
            if (f.kind == "switch") flags[f.key] = inputs.optBoolean(f.key, false) else text[f.key] = inputs.optString(f.key)
        }
    }
}

// ============================================================================================ comparing options
val COMPARE_KINDS = listOf("tbill" to "Treasury bill", "mmf" to "Money market fund", "sacco" to "SACCO", "bank" to "Bank account")

/** One place to put the money, in the comparison. */
class CompareOption(kind: String = "tbill", yieldPct: String = "", tenor: String = "364", basis: String = "gross") {
    var kind by mutableStateOf(kind)
    var yieldPct by mutableStateOf(yieldPct)
    var tenor by mutableStateOf(tenor)
    var basis by mutableStateOf(basis)
}

class CompareForm : LabForm {
    val text = mutableStateMapOf<String, String>()
    var taxExempt by mutableStateOf(false)
    val options = mutableStateListOf<CompareOption>()

    init { reset() }

    override fun reset() {
        text.clear()
        text["amount"] = "100000"
        text["years"] = "5"
        taxExempt = false
        options.clear()
        options.add(CompareOption("tbill"))
        options.add(CompareOption("mmf"))
    }

    override fun toInputs(): JSONObject {
        val o = JSONObject()
        for (k in listOf("amount", "years")) {
            val v = text[k].orEmpty().replace(",", "").replace(" ", "").trim()
            if (v.isNotEmpty()) o.put(k, v)
        }
        if (taxExempt) o.put("tax_exempt", true)
        val arr = JSONArray()
        for (opt in options) {
            val e = JSONObject().put("kind", opt.kind)
            val y = opt.yieldPct.replace(",", "").trim()
            if (y.isNotEmpty()) e.put("yield_pct", y)
            if (opt.kind == "tbill") e.put("tenor_days", opt.tenor.toIntOrNull() ?: 364)
            if (opt.kind == "mmf" || opt.kind == "bank") e.put("yield_basis", opt.basis)
            arr.put(e)
        }
        o.put("options", arr)
        return o
    }

    override fun load(inputs: JSONObject) {
        reset()
        for (k in listOf("amount", "years")) if (inputs.has(k) && !inputs.isNull(k)) text[k] = inputs.optString(k)
        taxExempt = inputs.optBoolean("tax_exempt", false)
        val arr = inputs.optJSONArray("options")
        if (arr != null && arr.length() > 0) {
            options.clear()
            for (i in 0 until arr.length()) {
                val e = arr.getJSONObject(i)
                options.add(CompareOption(e.optString("kind", "tbill"), e.optString("yield_pct"), e.optString("tenor_days", "364"), e.optString("yield_basis", "gross").ifBlank { "gross" }))
            }
        }
    }
}

// ============================================================================================ talking to the website
object InstrumentApi {
    suspend fun run(model: String, inputs: JSONObject): JSONObject {
        val body = JSONObject().put("model", model).put("inputs", inputs)
        return Auth.api("POST", "/api/v1/calc/instrument/", body).getJSONObject("result")
    }
}

// ============================================================================================ evidence blocks
/** Turns instrument results into the same typed note blocks the savings calculator uses. */
object InstrumentEvidence {
    private fun stamp(): String = NoteStore.nowIso()

    fun calcInfo(result: JSONObject): JSONObject =
        JSONObject()
            .put("model", result.optString("model"))
            .put("version", result.optString("version"))
            .put("calculated_at", result.optString("calculated_at", stamp()))
            .put("inputs", JSONObject((result.optJSONObject("inputs") ?: JSONObject()).toString()))

    private fun frame(type: String, mode: String, result: JSONObject, title: String?): JSONObject {
        val o = JSONObject().put("type", type).put("mode", mode).put("calc", calcInfo(result))
        if (mode == "snapshot") o.put("captured_at", stamp())
        if (title != null) o.put("title", title)
        return o
    }

    private fun block(o: JSONObject, id: String?): Block =
        Block(id = id ?: java.util.UUID.randomUUID().toString(), type = o.getString("type"), raw = o)

    fun headline(result: JSONObject, mode: String, id: String? = null): Block {
        val h = result.getJSONObject("headline")
        val o = frame("metric", mode, result, null)
            .put("label", h.optString("label")).put("value", h.optString("value")).put("caption", h.optString("caption"))
            .put("field", "headline")
        return block(o, id)
    }

    fun metric(result: JSONObject, key: String, mode: String, id: String? = null): Block {
        val arr = result.optJSONArray("metrics") ?: JSONArray()
        var found: JSONObject? = null
        for (i in 0 until arr.length()) if (arr.getJSONObject(i).optString("key") == key) found = arr.getJSONObject(i)
        val m = found ?: return headline(result, mode, id)
        val o = frame("metric", mode, result, null)
            .put("label", m.optString("label")).put("value", m.optString("value")).put("raw", m.optString("raw"))
            .put("unit", m.optString("unit")).put("field", key)
        return block(o, id)
    }

    fun chart(result: JSONObject, mode: String, id: String? = null): Block {
        val c = result.getJSONObject("chart")
        val o = frame("chart", mode, result, c.optString("title"))
            .put("x_label", c.optString("x_label", "Year")).put("y_label", c.optString("y_label", "Amount")).put("unit", c.optString("unit", "KES"))
            .put("x", c.getJSONArray("x")).put("series", c.getJSONArray("series"))
            .put("view", JSONObject().put("kind", "instrument"))
        return block(o, id)
    }

    fun table(result: JSONObject, mode: String, id: String? = null): Block {
        val t = result.getJSONObject("table")
        val cols = t.getJSONArray("columns")
        val rows = t.getJSONArray("rows")
        val shown = JSONArray()
        for (r in 0 until rows.length()) {
            val src = rows.getJSONArray(r)
            val row = JSONArray()
            for (c in 0 until src.length()) {
                val kind = cols.optJSONObject(c)?.optString("kind")
                row.put(if (kind == "money") Fmt.money(src.optString(c)) else src.optString(c))
            }
            shown.put(row)
        }
        val o = frame("table", mode, result, t.optString("title"))
            .put("columns", cols).put("rows", shown).put("view", JSONObject().put("kind", "instrument"))
        return block(o, id)
    }

    fun assumptions(result: JSONObject, mode: String, id: String? = null): Block {
        val items = JSONArray()
        val src = result.optJSONArray("assumptions") ?: JSONArray()
        for (i in 0 until src.length()) items.put(src.getJSONObject(i))
        return block(frame("assumptions", mode, result, "Assumptions behind these numbers").put("items", items), id)
    }

    fun methodology(result: JSONObject, mode: String, id: String? = null): Block {
        val lines = JSONArray()
        val src = result.optJSONArray("methodology") ?: JSONArray()
        for (i in 0 until src.length()) lines.put(src.getString(i))
        return block(frame("methodology", mode, result, "How this was calculated").put("lines", lines), id)
    }

    fun interpretation(result: JSONObject, mode: String, id: String? = null): Block {
        val sentences = JSONArray()
        val src = result.optJSONArray("interpretation") ?: JSONArray()
        for (i in 0 until src.length()) sentences.put(src.getString(i))
        return block(frame("interpretation", mode, result, "What this shows").put("sentences", sentences).put("generated", true), id)
    }

    /** One source block per rule used, so a plan records where each tax rate or day count came from. */
    fun sources(result: JSONObject): List<Block> {
        val rules = result.optJSONArray("rules") ?: JSONArray()
        return (0 until rules.length()).map { i ->
            val r = rules.getJSONObject(i)
            val from = r.optString("effective_from")
            val text = r.optString("label") + ": " + r.optString("rate") + (if (r.optString("id").startsWith("tbill_day") || r.optString("id").startsWith("tbond_day")) " days" else "%") +
                ". " + r.optString("status") +
                (if (from.isNotBlank() && from != "null") " In force from $from." else " The source does not state a start date.") +
                " " + r.optString("source") + ", read on " + r.optString("retrieved") + "." +
                (if (r.optString("caveat").isNotBlank()) " Note: " + r.optString("caveat") else "")
            Evidence.source(text, r.optString("url"))
        }
    }

    /** Re-runs the saved calculation with today's rules and rebuilds the block. Throws ApiError when the website cannot be reached. */
    suspend fun refresh(b: Block): Block {
        val raw = b.raw ?: return b
        val calc = raw.optJSONObject("calc") ?: return b
        val model = calc.optString("model")
        val inputs = JSONObject((calc.optJSONObject("inputs") ?: return b).toString())
        inputs.remove("as_of")
        val r = InstrumentApi.run(model, inputs)
        return when (raw.optString("type")) {
            "metric" -> if (raw.optString("field") == "headline") headline(r, "live", b.id) else metric(r, raw.optString("field"), "live", b.id)
            "chart" -> chart(r, "live", b.id)
            "table" -> table(r, "live", b.id)
            "assumptions" -> assumptions(r, "live", b.id)
            "methodology" -> methodology(r, "live", b.id)
            "interpretation" -> interpretation(r, "live", b.id)
            else -> b
        }
    }
}
