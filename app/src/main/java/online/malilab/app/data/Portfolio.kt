package online.malilab.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import java.io.File
import java.math.BigDecimal
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.math.pow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

// ============================================================================================ dates (no java.time: minSdk 24)
/** A calendar day. Kept simple on purpose: the app runs on Android 7 and up. */
data class Day(val y: Int, val m: Int, val d: Int) : Comparable<Day> {
    override fun compareTo(other: Day): Int = when {
        y != other.y -> y.compareTo(other.y)
        m != other.m -> m.compareTo(other.m)
        else -> d.compareTo(other.d)
    }

    fun iso(): String = String.format(Locale.US, "%04d-%02d-%02d", y, m, d)

    /** Days since 1970-01-01. */
    fun epoch(): Long {
        val yy = if (m <= 2) y - 1 else y
        val era = (if (yy >= 0) yy else yy - 399) / 400
        val yoe = yy - era * 400
        val mp = (m + 9) % 12
        val doy = (153 * mp + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era.toLong() * 146097L + doe - 719468L
    }

    fun plusMonths(n: Int): Day {
        val index = y * 12 + (m - 1) + n
        val ny = index / 12
        val nm = index % 12 + 1
        return Day(ny, nm, minOf(d, daysIn(ny, nm)))
    }

    fun plusDays(n: Int): Day = fromEpoch(epoch() + n)

    fun pretty(): String = "$d ${MONTHS[m - 1]} $y"

    companion object {
        private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

        fun daysIn(y: Int, m: Int): Int = when (m) {
            2 -> if ((y % 4 == 0 && y % 100 != 0) || y % 400 == 0) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

        fun parse(text: String?): Day? {
            val t = text?.trim()?.take(10) ?: return null
            val parts = t.split("-")
            if (parts.size != 3) return null
            val y = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            val d = parts[2].toIntOrNull() ?: return null
            if (y < 1900 || y > 2200 || m < 1 || m > 12 || d < 1 || d > daysIn(y, m)) return null
            return Day(y, m, d)
        }

        fun today(): Day {
            val c = Calendar.getInstance()
            return Day(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
        }

        fun fromEpoch(days: Long): Day {
            val z = days + 719468L
            val era = (if (z >= 0) z else z - 146096L) / 146097L
            val doe = z - era * 146097L
            val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L
            val yy = yoe + era * 400L
            val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
            val mp = (5L * doy + 2L) / 153L
            val d = (doy - (153L * mp + 2L) / 5L + 1L).toInt()
            val m = (if (mp < 10) mp + 3 else mp - 9).toInt()
            val y = (if (m <= 2) yy + 1 else yy).toInt()
            return Day(y, m, d)
        }

        /** Whole months from one day to another, never negative. */
        fun monthsBetween(start: Day, end: Day): Int {
            var months = (end.y - start.y) * 12 + (end.m - start.m)
            if (end.d < start.d) months -= 1
            return maxOf(0, months)
        }
    }
}

// ============================================================================================ the eleven places
/**
 * One of the places Kenyans put their money. [taxPct] is only a suggestion taken from KRA and CBK withholding tax
 * guidance (the website's Rules screen has the sources); no vehicle comes with a promised rate.
 */
class Vehicle(
    val key: String,
    val title: String,
    val short: String,
    val color: Color,
    val group: String,
    val rateKind: String,
    val rateLabel: String,
    val payout: String,
    val interval: String,
    val taxPct: Double,
    val hint: String,
) {
    val rateKindLabel: String
        get() = when (rateKind) {
            "fixed" -> "Fixed when you buy"
            "variable" -> "Changes over time"
            else -> "Your own assumption"
        }
}

object Vehicles {
    val ALL: List<Vehicle> = listOf(
        Vehicle("tbonds", "Treasury Bonds", "Government securities", Color(0xFF0F9688), "Government", "fixed", "Yield or coupon (% a year)", "income", "once", 15.0,
            "Bonds pay a fixed coupon every six months. Enter the yield you bought at. Choose \"Pay out\" if you spend the coupons, or \"Reinvest\" if you put them back to work. Infrastructure bonds may be taxed differently."),
        Vehicle("sacco", "SACCOs", "Savings and credit cooperatives", Color(0xFF3B4CC0), "Cooperative", "variable", "Dividend or interest on deposits (% a year)", "reinvest", "monthly", 5.0,
            "Your SACCO decides the dividend each year at its annual meeting. What it paid recently is a fair guess, but it is not promised."),
        Vehicle("mmf", "Money Market Funds", "MMFs", Color(0xFF1F7A4D), "Pooled funds", "variable", "Current yield (% a year)", "reinvest", "monthly", 15.0,
            "Yields change often. Enter today's yield and log changes later. Many funds advertise the yield before tax, so check whether yours is gross or net."),
        Vehicle("pension", "Pension and IPPs", "Pension schemes, individual pension plans", Color(0xFF7A3DB0), "Retirement", "variable", "Expected return (% a year)", "reinvest", "monthly", 0.0,
            "Include your employer's share in the contribution if you want the whole picture. Pension money is usually locked until retirement, so check your scheme's rules."),
        Vehicle("equities", "NSE Stocks", "Equities", Color(0xFFB0285A), "Markets", "assumed", "Expected return (% a year)", "reinvest", "monthly", 0.0,
            "Share prices rise and fall, and dividends are not fixed. The rate here is your own assumption, not a forecast. Update the value from your broker or CDS statement."),
        Vehicle("realestate", "Real Estate and Land", "Property", Color(0xFFB5651D), "Property", "assumed", "Expected yearly growth in value (% a year)", "reinvest", "once", 0.0,
            "Property is hard to sell quickly and values are estimates. Enter what you paid as the starting amount, then update the value when you know a better figure."),
        Vehicle("chama", "Chamas", "Investment groups", Color(0xFFD98E04), "Group", "variable", "Yearly return on your share (% a year)", "reinvest", "monthly", 0.0,
            "Use your own monthly contribution and the return your chama's investments have given you. If your chama does merry-go-round only, the rate is 0."),
        Vehicle("insurance", "Insurance Policies", "Education and endowment plans", Color(0xFF2F80C0), "Protection", "fixed", "Effective return on premiums (% a year)", "reinvest", "monthly", 0.0,
            "Part of each premium pays for cover, so the return is often lower than a savings product. Work the rate out from the maturity value in your policy document if you can."),
        Vehicle("mobile", "Mobile Money Investing", "For example Ziidi MMF", Color(0xFF12B5C4), "Pooled funds", "variable", "Current yield (% a year)", "reinvest", "weekly", 15.0,
            "Small, frequent top-ups from your phone. Yields change, so enter today's figure and update it when it moves."),
        Vehicle("cis", "Unit Trusts (CIS)", "Collective investment schemes", Color(0xFF6FBF4A), "Pooled funds", "assumed", "Expected return (% a year)", "reinvest", "monthly", 0.0,
            "Unit prices move up and down. Equity and balanced funds are not steady like money market funds, so treat the rate as an assumption."),
        Vehicle("banks", "Banks", "Savings and fixed deposits", Color(0xFF5B6B7A), "Bank", "fixed", "Interest rate (% a year)", "reinvest", "monthly", 15.0,
            "Fixed deposits lock a rate for a set time. Ordinary savings rates can change, so log a change if your bank moves it."),
    )

    fun get(key: String): Vehicle = ALL.firstOrNull { it.key == key } ?: ALL[0]
}

val INTERVAL_CHOICES = listOf(
    "once" to "One-off",
    "daily" to "Daily",
    "weekly" to "Weekly",
    "monthly" to "Monthly",
    "quarterly" to "Every 3 months",
    "semiannual" to "Every 6 months",
    "annual" to "Yearly",
)

fun intervalWord(key: String): String = INTERVAL_CHOICES.firstOrNull { it.first == key }?.second ?: key

// ============================================================================================ what a plan is
data class RateChange(val month: Int, val ratePct: Double)
data class Payment(val id: String, val date: String, val amount: Double)
data class Valuation(val date: String, val value: Double)

data class Plan(
    val uid: String = UUID.randomUUID().toString(),
    val category: String = "mmf",
    val name: String = "",
    val provider: String = "",
    val startDate: String = Day.today().iso(),
    val initial: Double = 0.0,
    val contribution: Double = 0.0,
    val interval: String = "monthly",
    val ratePct: Double = 0.0,
    val rateBasis: String = "nominal",
    val tenureMonths: Int = 60,
    val payout: String = "reinvest",
    val taxPct: Double = 0.0,
    val timing: String = "end",
    val rateChanges: List<RateChange> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val valuation: Valuation? = null,
    val useActuals: Boolean = false,
    val reminderOn: Boolean = true,
    val reminderHour: Int = 8,
    val reminderDaysBefore: Int = 0,
    val note: String = "",
    val version: Int = 0,
    val updatedAt: String = "",
    val dirty: Boolean = false,
) {
    val vehicle: Vehicle get() = Vehicles.get(category)
    val start: Day get() = Day.parse(startDate) ?: Day.today()
}

// ============================================================================================ the engine
class Milestone(
    val month: Int,
    val label: String,
    val balance: Double,
    val paidIn: Double,
    val interest: Double,
    val incomeMonthly: Double,
    val incomeAnnual: Double,
)

class SeriesPoint(val month: Int, val balance: Double, val paidIn: Double)

class Projection(val milestones: List<Milestone>, val final: Milestone, val series: List<SeriesPoint>, val notes: List<String>)

class Standing(
    val monthsElapsed: Int,
    val matured: Boolean,
    val value: Double,
    val paidIn: Double,
    val gain: Double,
    val modelled: Boolean,
    val nextDue: Day?,
)

/**
 * Grows a plan month by month. This is a port of the website's portfolio/engine.py and must give the same numbers
 * (within KES 1) for the cases in that file's tests. Money is added at the end of each period unless the plan says
 * "start"; daily and weekly top-ups are added once a month as their monthly equivalent.
 */
object PortfolioEngine {
    private class Row(val month: Int, val bal: Double, val paid: Double, val out: Double)

    private val PERIODS = mapOf("monthly" to 1, "quarterly" to 3, "semiannual" to 6, "annual" to 12)

    fun monthlyRate(ratePct: Double, basis: String, taxPct: Double): Double {
        val g = ratePct / 100.0
        val month = if (basis == "effective") (1.0 + g).pow(1.0 / 12.0) - 1.0 else g / 12.0
        return month * (1.0 - taxPct / 100.0)
    }

    private fun rateFn(p: Plan): (Int) -> Double {
        val base = monthlyRate(p.ratePct, p.rateBasis, p.taxPct)
        val steps = p.rateChanges.sortedBy { it.month }.map { it.month to monthlyRate(it.ratePct, p.rateBasis, p.taxPct) }
        return { m ->
            var current = base
            for ((start, value) in steps) if (start <= m) current = value
            current
        }
    }

    /** Months between payments, and the amount each time (zero for a one-off plan). */
    private fun payment(p: Plan): Pair<Int, Double> {
        if (p.contribution <= 0.0 || p.interval == "once") return 1 to 0.0
        return when (p.interval) {
            "daily" -> 1 to p.contribution * 365.0 / 12.0
            "weekly" -> 1 to p.contribution * 52.0 / 12.0
            else -> (PERIODS[p.interval] ?: 1) to p.contribution
        }
    }

    private fun due(month: Int, period: Int, timing: String): Boolean =
        if (timing == "start") (month - 1) % period == 0 else month % period == 0

    private fun run(
        p: Plan,
        months: Int,
        payMap: Map<Int, Double>? = null,
        startMonth: Int = 0,
        startBalance: Double? = null,
        startPaid: Double? = null,
    ): List<Row> {
        val rateAt = rateFn(p)
        val (period, amount) = payment(p)
        var bal = startBalance ?: p.initial
        var paid = startPaid ?: p.initial
        var out = 0.0
        val rows = ArrayList<Row>()
        rows.add(Row(startMonth, bal, paid, out))
        for (m in (startMonth + 1)..months) {
            val pay = if (payMap != null) (payMap[m] ?: 0.0) else if (due(m, period, p.timing)) amount else 0.0
            if (p.timing == "start" && payMap == null) {
                bal += pay
                paid += pay
            }
            val interest = bal * rateAt(m)
            if (p.payout == "income") out += interest else bal += interest
            if (p.timing == "end" || payMap != null) {
                bal += pay
                paid += pay
            }
            rows.add(Row(m, bal, paid, out))
        }
        return rows
    }

    /** Four stepping stones along the tenure at the nearest round numbers; the last is the full tenure. */
    fun milestoneMonths(tenure: Int): List<Int> {
        for (unit in intArrayOf(12, 3, 1)) {
            val found = (1..3).map { i -> ((2L * tenure * i + 4L * unit) / (8L * unit)).toInt() * unit }
            var ok = found.last() < tenure
            var prev = 0
            for (f in found) {
                if (f <= prev) ok = false
                prev = f
            }
            if (ok) return found + tenure
        }
        val distinct = ((1..3).map { i -> maxOf(1, ((2L * tenure * i + 4L) / 8L).toInt()) } + tenure).toSortedSet()
        return distinct.filter { it in 1..tenure }
    }

    fun label(months: Int): String {
        val years = months / 12
        val rest = months % 12
        val parts = ArrayList<String>()
        if (years > 0) parts.add("$years year" + if (years == 1) "" else "s")
        if (rest > 0) parts.add("$rest month" + if (rest == 1) "" else "s")
        return parts.joinToString(" ")
    }

    private fun point(p: Plan, row: Row, rateAt: (Int) -> Double): Milestone {
        val incomeMonth = row.bal * rateAt(if (row.month > 0) row.month else 1)
        return Milestone(
            month = row.month, label = label(row.month), balance = row.bal, paidIn = row.paid,
            interest = row.bal + row.out - row.paid, incomeMonthly = incomeMonth, incomeAnnual = incomeMonth * 12.0,
        )
    }

    fun project(p: Plan): Projection {
        val tenure = p.tenureMonths
        val rows = run(p, tenure)
        val rateAt = rateFn(p)
        val step = maxOf(1, tenure / 60)
        val series = rows.filter { it.month % step == 0 || it.month == tenure }.map { SeriesPoint(it.month, it.bal, it.paid) }
        return Projection(
            milestones = milestoneMonths(tenure).map { point(p, rows[it], rateAt) },
            final = point(p, rows[tenure], rateAt),
            series = series,
            notes = notes(p),
        )
    }

    fun notes(p: Plan): List<String> {
        val out = ArrayList<String>()
        out.add("The rate of ${plainNum(p.ratePct)}% a year is the one you entered. It is not a promise.")
        out.add("Interest is worked out monthly, from a " + if (p.rateBasis == "nominal") "nominal rate (spread over twelve months)." else "yearly effective rate.")
        if (p.interval == "daily" || p.interval == "weekly") out.add("${intervalWord(p.interval)} top-ups are added once a month as their monthly equivalent.")
        if (p.contribution > 0 && p.interval != "once") out.add("Each top-up is added at the " + (if (p.timing == "start") "start" else "end") + " of its period.")
        if (p.taxPct > 0) out.add("${plainNum(p.taxPct)}% tax is taken off the interest every month.") else out.add("No tax is taken off. Real returns may be lower after tax.")
        if (p.payout == "income") out.add("Interest is paid out to you and not added back, so the balance stays at what you paid in.")
        if (p.rateChanges.isNotEmpty()) out.add("Rate changes you logged apply from the month they happened.")
        out.add("\"Income if you stop\" is the monthly interest your balance would earn at the rate then, with the money left untouched.")
        return out
    }

    /** The plan's worth today. Without a valuation it is modelled from the plan; with [Plan.useActuals] only logged payments count. */
    fun standing(p: Plan, today: Day = Day.today()): Standing {
        val start = p.start
        val elapsed = minOf(Day.monthsBetween(start, today), p.tenureMonths)
        var payMap: MutableMap<Int, Double>? = null
        if (p.useActuals) {
            payMap = HashMap()
            for (pay in p.payments) {
                val day = Day.parse(pay.date) ?: continue
                if (day < start) continue
                val m = maxOf(1, Day.monthsBetween(start, day))
                payMap[m] = (payMap[m] ?: 0.0) + pay.amount
            }
        }
        val rows = run(p, elapsed, payMap)
        val last = rows.last()
        var balance = last.bal
        var modelled = true
        val v = p.valuation
        if (v != null) {
            val vDay = Day.parse(v.date)
            if (vDay != null) {
                val vMonth = Day.monthsBetween(start, vDay)
                if (vMonth <= elapsed) {
                    val later: Map<Int, Double> = if (payMap != null) {
                        payMap.filterKeys { it > vMonth }
                    } else {
                        val (period, amount) = payment(p)
                        val m = HashMap<Int, Double>()
                        for (k in (vMonth + 1)..elapsed) if (due(k, period, p.timing)) m[k] = amount
                        m
                    }
                    val tail = run(p, elapsed, later, vMonth, v.value, last.paid)
                    balance = tail.last().bal
                    modelled = false
                }
            }
        }
        return Standing(
            monthsElapsed = elapsed, matured = elapsed >= p.tenureMonths, value = balance, paidIn = last.paid,
            gain = balance + last.out - last.paid, modelled = modelled, nextDue = nextDue(p, today),
        )
    }

    /** The next day a top-up is due on or after [today], or null for a one-off plan or one that has finished. */
    fun nextDue(p: Plan, today: Day = Day.today()): Day? {
        if (p.contribution <= 0.0 || p.interval == "once") return null
        val start = p.start
        val first = if (p.timing == "start") 0 else 1
        val end = start.plusMonths(p.tenureMonths)
        val whenDue: Day
        if (p.interval == "daily" || p.interval == "weekly") {
            val gap = if (p.interval == "daily") 1 else 7
            val days = (today.epoch() - start.epoch()).toInt()
            val n = if (today > start) maxOf(first, (days + gap - 1) / gap) else first
            whenDue = start.plusDays(n * gap)
        } else {
            val period = PERIODS[p.interval] ?: 1
            var k = maxOf(Day.monthsBetween(start, today) / period, first)
            while (start.plusMonths(k * period) < today) k += 1
            whenDue = start.plusMonths(k * period)
        }
        return if (whenDue <= end) whenDue else null
    }

    /** The money paid in each month on average, for the "you commit" line. */
    fun monthlyCommitment(p: Plan): Double {
        if (p.contribution <= 0.0 || p.interval == "once") return 0.0
        return when (p.interval) {
            "daily" -> p.contribution * 365.0 / 12.0
            "weekly" -> p.contribution * 52.0 / 12.0
            else -> p.contribution / (PERIODS[p.interval] ?: 1)
        }
    }
}

// ============================================================================================ numbers on screen
/** Money shown the Kenyan way, to the shilling: KES 1,047,099. */
fun kes(v: Double): String = "KES " + String.format(Locale.US, "%,d", Math.round(v))

fun plainNum(v: Double): String = BigDecimal.valueOf(v).stripTrailingZeros().toPlainString()



// ============================================================================================ saved on the phone, sent to the account
/**
 * Plans live on the phone first, so adding one never waits for the network, then are sent to the person's account so the
 * website shows the same portfolio. If a plan changed on another device meanwhile, the account's version wins.
 */
object PortfolioStore {
    private lateinit var root: File
    private lateinit var prefs: SharedPreferences
    private val lock = Mutex()
    private val cache = LinkedHashMap<String, Plan>()
    private var cacheOwner = ""

    var plans by mutableStateOf<List<Plan>>(emptyList())
        private set

    var online by mutableStateOf(true)
        private set

    fun init(context: Context) {
        root = File(context.filesDir, "portfolio")
        prefs = context.getSharedPreferences("malilab_portfolio", Context.MODE_PRIVATE)
    }

    private fun dir(): File = File(root, Auth.accountKey.ifBlank { "guest" }).also { it.mkdirs() }
    private fun tombKey() = "deleted_" + Auth.accountKey
    private fun tombstones(): Set<String> = prefs.getStringSet(tombKey(), emptySet()) ?: emptySet()
    private fun setTombstones(s: Set<String>) = prefs.edit().putStringSet(tombKey(), s).apply()

    private fun ensureLoaded() {
        val owner = Auth.accountKey
        if (owner == cacheOwner) return
        cache.clear()
        cacheOwner = owner
        dir().listFiles { f -> f.extension == "json" }?.forEach { f ->
            try {
                val p = fromJson(JSONObject(f.readText()))
                cache[p.uid] = p
            } catch (e: Exception) {
                // A damaged file is skipped rather than crashing the app.
            }
        }
        rebuild()
    }

    /** Call when the signed-in account changes, so another person's plans never show. */
    fun reset() {
        cacheOwner = ""
        plans = emptyList()
    }

    private fun rebuild() {
        val gone = tombstones()
        plans = cache.values.filter { it.uid !in gone }.sortedByDescending { it.updatedAt }
    }

    private fun store(p: Plan) {
        cache[p.uid] = p
        try {
            File(dir(), p.uid + ".json").writeText(toJson(p, true).toString())
        } catch (e: Exception) {
            // Out of space: the plan stays in memory and is shown as not yet saved to the account.
        }
    }

    private fun forget(uid: String) {
        cache.remove(uid)
        File(dir(), "$uid.json").delete()
    }

    fun get(uid: String): Plan? {
        ensureLoaded()
        return cache[uid]
    }

    /** Saves a new or changed plan on the phone straight away. The account is updated on the next [refresh]. */
    fun save(plan: Plan): String {
        ensureLoaded()
        store(plan.copy(updatedAt = nowIso(), dirty = true))
        rebuild()
        return plan.uid
    }

    fun remove(uid: String) {
        ensureLoaded()
        val p = cache[uid]
        forget(uid)
        if (p != null && p.version > 0) setTombstones(tombstones() + uid)
        rebuild()
    }

    private fun nowIso(): String = NoteStore.nowIso()

    // ------------------------------------------------------------------ talking to the account
    /** Sends waiting changes, then asks the account what exists. Returns false when the phone is offline. */
    suspend fun refresh(): Boolean = lock.withLock {
        ensureLoaded()
        try {
            pushAll()
            val o = Auth.api("GET", "/api/v1/portfolio/plans/")
            val arr = o.optJSONArray("plans") ?: JSONArray()
            val seen = HashSet<String>()
            val gone = tombstones()
            for (i in 0 until arr.length()) {
                val s = arr.getJSONObject(i)
                val uid = s.getString("uid")
                seen.add(uid)
                if (uid in gone) continue
                val local = cache[uid]
                if (local == null || (!local.dirty && s.optInt("version") > local.version)) {
                    val full = Auth.api("GET", "/api/v1/portfolio/plans/$uid/")
                    store(fromJson(full))
                }
            }
            // A clean local copy the account no longer has was removed on another device.
            cache.values.filter { !it.dirty && it.version > 0 && it.uid !in seen }.map { it.uid }.forEach { forget(it) }
            online = true
            rebuild()
            true
        } catch (e: ApiError) {
            if (e.status == 0) online = false
            false
        }
    }

    private suspend fun pushAll() {
        for (uid in tombstones().toList()) {
            try {
                Auth.api("DELETE", "/api/v1/portfolio/plans/$uid/")
                setTombstones(tombstones() - uid)
            } catch (e: ApiError) {
                if (e.status == 404) setTombstones(tombstones() - uid) else throw e
            }
        }
        for (p in cache.values.filter { it.dirty }.toList()) {
            try {
                if (p.version == 0) {
                    val saved = Auth.api("POST", "/api/v1/portfolio/plans/", toJson(p, false))
                    store(p.copy(version = saved.optInt("version", 1), dirty = false))
                } else {
                    val body = toJson(p, false).put("base_version", p.version)
                    val saved = Auth.api("PUT", "/api/v1/portfolio/plans/${p.uid}/", body)
                    store(p.copy(version = saved.optInt("version", p.version + 1), dirty = false))
                }
            } catch (e: ApiError) {
                if (e.status == 409) {
                    // Changed on another device: the account's version wins.
                    val theirs = e.body.optJSONObject("plan")
                    if (theirs != null) store(fromJson(theirs)) else throw e
                } else if (e.status == 400 && e.code == "invalid_plan") {
                    // The account refused this plan; keep it on the phone, marked as sent, so it does not retry forever.
                    store(p.copy(dirty = false))
                } else throw e
            }
        }
    }

    // ------------------------------------------------------------------ JSON, in the same shape the website uses
    private fun num(v: Double): String = BigDecimal.valueOf(v).stripTrailingZeros().toPlainString()

    private fun toJson(p: Plan, local: Boolean): JSONObject {
        val o = JSONObject()
            .put("uid", p.uid).put("category", p.category).put("name", p.name).put("provider", p.provider)
            .put("start_date", p.startDate).put("initial", num(p.initial)).put("contribution", num(p.contribution))
            .put("interval", p.interval).put("rate_pct", num(p.ratePct)).put("rate_basis", p.rateBasis)
            .put("tenure_months", p.tenureMonths).put("payout", p.payout).put("tax_pct", num(p.taxPct)).put("timing", p.timing)
            .put("rate_changes", JSONArray().also { a -> p.rateChanges.forEach { a.put(JSONObject().put("month", it.month).put("rate_pct", num(it.ratePct))) } })
            .put("contributions", JSONArray().also { a -> p.payments.forEach { a.put(JSONObject().put("id", it.id).put("date", it.date).put("amount", num(it.amount))) } })
            .put("use_actuals", p.useActuals).put("reminder_on", p.reminderOn).put("reminder_hour", p.reminderHour)
            .put("reminder_days_before", p.reminderDaysBefore).put("note", p.note)
        val v = p.valuation
        o.put("valuation", if (v == null) JSONObject() else JSONObject().put("date", v.date).put("value", num(v.value)))
        if (local) o.put("version", p.version).put("updated_at", p.updatedAt).put("dirty", p.dirty)
        return o
    }

    private fun fromJson(o: JSONObject): Plan {
        fun d(key: String): Double = o.optString(key).toDoubleOrNull() ?: 0.0
        val changes = o.optJSONArray("rate_changes")
        val pays = o.optJSONArray("contributions")
        val val0 = o.optJSONObject("valuation")
        val valuation = if (val0 != null && val0.has("date") && val0.has("value")) {
            Valuation(val0.optString("date"), val0.optString("value").toDoubleOrNull() ?: 0.0)
        } else null
        return Plan(
            uid = o.getString("uid"), category = o.optString("category", "mmf"), name = o.optString("name"),
            provider = o.optString("provider"), startDate = o.optString("start_date").ifBlank { Day.today().iso() },
            initial = d("initial"), contribution = d("contribution"), interval = o.optString("interval", "monthly"),
            ratePct = d("rate_pct"), rateBasis = o.optString("rate_basis", "nominal"), tenureMonths = o.optInt("tenure_months", 60),
            payout = o.optString("payout", "reinvest"), taxPct = d("tax_pct"), timing = o.optString("timing", "end"),
            rateChanges = (0 until (changes?.length() ?: 0)).map {
                val c = changes!!.getJSONObject(it)
                RateChange(c.optInt("month", 1), c.optString("rate_pct").toDoubleOrNull() ?: 0.0)
            },
            payments = (0 until (pays?.length() ?: 0)).map {
                val c = pays!!.getJSONObject(it)
                Payment(c.optString("id").ifBlank { UUID.randomUUID().toString() }, c.optString("date"), c.optString("amount").toDoubleOrNull() ?: 0.0)
            },
            valuation = valuation, useActuals = o.optBoolean("use_actuals"), reminderOn = o.optBoolean("reminder_on", true),
            reminderHour = o.optInt("reminder_hour", 8), reminderDaysBefore = o.optInt("reminder_days_before", 0),
            note = o.optString("note"), version = o.optInt("version", 0), updatedAt = o.optString("updated_at"),
            dirty = o.optBoolean("dirty"),
        )
    }
}
