package online.malilab.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.malilab.app.data.Day
import online.malilab.app.data.Fmt
import online.malilab.app.data.INTERVAL_CHOICES
import online.malilab.app.data.Milestone
import online.malilab.app.data.Payment
import online.malilab.app.data.Plan
import online.malilab.app.data.PlaybookSummary
import online.malilab.app.data.PortfolioEngine
import online.malilab.app.data.PortfolioStore
import online.malilab.app.data.RateChange
import online.malilab.app.data.Reminders
import online.malilab.app.data.Repo
import online.malilab.app.data.SeriesPoint
import online.malilab.app.data.Standing
import online.malilab.app.data.Valuation
import online.malilab.app.data.Vehicle
import online.malilab.app.data.Vehicles
import online.malilab.app.data.intervalWord
import online.malilab.app.data.kes
import online.malilab.app.data.plainNum
import online.malilab.app.ui.theme.Mali
import java.util.UUID

/** The ways a rate can be quoted: the label, and how many of those periods make a year. */
private val RATE_PERIODS = listOf("day" to "a day", "week" to "a week", "month" to "a month", "quarter" to "every 3 months", "half" to "every 6 months", "year" to "a year")
private val RATE_CHIPS = mapOf("day" to "Daily", "week" to "Weekly", "month" to "Monthly", "quarter" to "Every 3 months", "half" to "Every 6 months", "year" to "Yearly")
private val RATE_PER_YEAR = mapOf("day" to 365.0, "week" to 52.0, "month" to 12.0, "quarter" to 4.0, "half" to 2.0, "year" to 1.0)

/** A rate quoted per day, week, month and so on, as the equivalent rate over a year (the rate earns on itself each period). */
private fun yearlyRate(pct: Double, every: String): Double {
    val n = RATE_PER_YEAR[every] ?: 1.0
    if (n == 1.0) return pct
    return Math.round((Math.pow(1.0 + pct / 100.0, n) - 1.0) * 100.0 * 10000.0) / 10000.0
}

// ============================================================================================ small pieces
@Composable
private fun PfPill(text: String, color: Color) {
    Text(
        text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun PfLabel(text: String, color: Color = Mali.InkSoft) {
    Text(text, fontSize = 12.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.ExtraBold, color = color)
}

@Composable
private fun PfCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Mali.Surface)
            .border(1.dp, Mali.Sand.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) { content() }
}

@Composable
private fun PfButton(text: String, filled: Boolean = true, color: Color = Mali.TealFixed, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp,
        color = if (filled) Color.White else color,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) color.copy(alpha = if (enabled) 1f else 0.4f) else color.copy(alpha = 0.10f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 13.dp),
    )
}

@Composable
private fun pfFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Mali.Ink, unfocusedTextColor = Mali.Ink,
    focusedBorderColor = Mali.Teal, unfocusedBorderColor = Mali.Sand,
    focusedLabelColor = Mali.TealDeep, unfocusedLabelColor = Mali.InkSoft,
    cursorColor = Mali.Teal,
    focusedContainerColor = Mali.Surface, unfocusedContainerColor = Mali.Surface,
    focusedSupportingTextColor = Mali.InkSoft, unfocusedSupportingTextColor = Mali.InkSoft,
)

@Composable
private fun PfField(label: String, value: String, onChange: (String) -> Unit, number: Boolean = false, hint: String = "") {
    val keys = KeyboardOptions(keyboardType = if (number) KeyboardType.Decimal else KeyboardType.Text)
    if (hint.isBlank()) {
        OutlinedTextField(
            value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
            keyboardOptions = keys, colors = pfFieldColors(), modifier = Modifier.fillMaxWidth(),
        )
    } else {
        OutlinedTextField(
            value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
            keyboardOptions = keys, colors = pfFieldColors(), modifier = Modifier.fillMaxWidth(),
            supportingText = { Text(hint, fontSize = 12.sp, lineHeight = 16.sp) },
        )
    }
}

private fun parseNum(text: String): Double? = text.replace(",", "").trim().toDoubleOrNull()

private fun shortKes(v: Double): String = "KES " + Fmt.compact(v)

// ============================================================================================ charts
/** A ring split into slices. Each slice is (size, colour); a thin gap keeps neighbours apart. */
@Composable
fun DonutChart(slices: List<Pair<Double, Color>>, modifier: Modifier = Modifier, thickness: Dp = 26.dp) {
    Canvas(modifier) {
        val total = slices.sumOf { it.first }
        if (total <= 0.0) return@Canvas
        val stroke = thickness.toPx()
        val diameter = minOf(size.width, size.height) - stroke
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        var start = -90f
        for ((value, color) in slices) {
            val sweep = (value / total * 360.0).toFloat()
            drawArc(
                color = color, startAngle = start, sweepAngle = maxOf(sweep - 2f, 0.6f), useCenter = false,
                topLeft = topLeft, size = Size(diameter, diameter), style = Stroke(width = stroke),
            )
            start += sweep
        }
    }
}

/** What the plan is worth over its whole tenure (solid line) against what was paid in (dashed). */
@Composable
fun GrowthChart(series: List<SeriesPoint>, accent: Color, modifier: Modifier = Modifier) {
    if (series.size < 2) return
    val grid = Mali.Sand
    val lastMonth = series.last().month.toFloat().coerceAtLeast(1f)
    val top = series.maxOf { maxOf(it.balance, it.paidIn) }.coerceAtLeast(1.0)
    Canvas(modifier) {
        for (i in 0..3) {
            val y = size.height * i / 3f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        fun x(month: Int): Float = size.width * month / lastMonth
        fun y(v: Double): Float = size.height * (1f - (v / top).toFloat())
        val line = Path()
        series.forEachIndexed { i, p -> if (i == 0) line.moveTo(x(p.month), y(p.balance)) else line.lineTo(x(p.month), y(p.balance)) }
        val fill = Path()
        fill.addPath(line)
        fill.lineTo(x(series.last().month), size.height)
        fill.lineTo(0f, size.height)
        fill.close()
        drawPath(fill, Brush.verticalGradient(listOf(accent.copy(alpha = 0.28f), accent.copy(alpha = 0.02f))))
        val paid = Path()
        series.forEachIndexed { i, p -> if (i == 0) paid.moveTo(x(p.month), y(p.paidIn)) else paid.lineTo(x(p.month), y(p.paidIn)) }
        drawPath(paid, Mali.InkSoft, style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))))
        drawPath(line, accent, style = Stroke(width = 3.dp.toPx()))
    }
}

@Composable
private fun PfGrowth(series: List<SeriesPoint>, accent: Color) {
    val top = series.maxOfOrNull { maxOf(it.balance, it.paidIn) } ?: 0.0
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(shortKes(top), fontSize = 12.sp, color = Mali.InkSoft)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("━ Worth", fontSize = 12.sp, color = accent, fontWeight = FontWeight.Bold)
                Text("┅ Paid in", fontSize = 12.sp, color = Mali.InkSoft, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(6.dp))
        GrowthChart(series, accent, Modifier.fillMaxWidth().height(170.dp))
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Start", fontSize = 12.sp, color = Mali.InkSoft)
            Text(PortfolioEngine.label(series.last().month), fontSize = 12.sp, color = Mali.InkSoft)
        }
    }
}

// ============================================================================================ what the portfolio adds up to
private class PfHeld(val plan: Plan, val standing: Standing)
private class PfSlice(val vehicle: Vehicle, val value: Double, val weight: Double)
private class PfTotals(
    val value: Double, val paid: Double, val monthly: Double, val slices: List<PfSlice>, val held: List<PfHeld>,
    val matures: Double, val blendedRate: Double, val incomeYear: Double,
)

private fun totalsOf(plans: List<Plan>): PfTotals {
    val held = plans.map { PfHeld(it, PortfolioEngine.standing(it)) }
    val value = held.sumOf { it.standing.value }
    val paid = held.sumOf { it.standing.paidIn }
    val monthly = held.filter { !it.standing.matured }.sumOf { PortfolioEngine.monthlyCommitment(it.plan) }
    val byCategory = LinkedHashMap<String, Double>()
    for (h in held) byCategory[h.plan.category] = (byCategory[h.plan.category] ?: 0.0) + h.standing.value
    val slices = byCategory.map { (key, v) -> PfSlice(Vehicles.get(key), v, if (value > 0) v / value * 100.0 else 0.0) }
        .sortedByDescending { it.value }
    // What it all grows to by the end of every plan, the value-weighted rate, and the interest a year at today's values.
    val matures = held.sumOf { if (it.standing.matured) it.standing.value else PortfolioEngine.project(it.plan).final.balance }
    val live = held.filter { !it.standing.matured }
    val weight = live.sumOf { it.standing.value }
    val blended = if (live.isEmpty()) 0.0 else if (weight > 0) live.sumOf { currentRate(it.plan) * it.standing.value } / weight else live.sumOf { currentRate(it.plan) } / live.size
    val incomeYear = live.sumOf { it.standing.value * currentRate(it.plan) / 100.0 * (1.0 - it.plan.taxPct / 100.0) }
    return PfTotals(value, paid, monthly, slices, held, matures, blended, incomeYear)
}

private fun percent(v: Double): String = String.format(java.util.Locale.US, "%.1f%%", v)

// ============================================================================================ the Home tab
@Composable
fun PortfolioHome(active: Boolean, onOpenPlan: (String) -> Unit, onAdd: () -> Unit, onOpenPlaybook: (String) -> Unit, onOpenArticle: (String) -> Unit, onAllPlaybooks: () -> Unit = {}) {
    StatusBarIcons(darkIcons = false, active = active)
    LaunchedEffect(active) { if (active) PortfolioStore.refresh() }
    val plans = PortfolioStore.plans
    val totals = remember(plans) { totalsOf(plans) }
    val context = LocalContext.current
    val askLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { Reminders.runNow(context) }
    LaunchedEffect(plans) {
        if (plans.isNotEmpty()) {
            val asked = context.getSharedPreferences("malilab_reminders", Context.MODE_PRIVATE)
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
                !asked.getBoolean("asked", false)
            ) {
                asked.edit().putBoolean("asked", true).apply()
                askLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Reminders.runNow(context)
            }
        }
    }
    var featured by remember { mutableStateOf(Repo.peekList().orEmpty()) }
    LaunchedEffect(Unit) {
        try {
            featured = Repo.playbooks().value
        } catch (e: Exception) {
            // The saved copy (if any) stays on screen.
        }
    }
    var articles by remember { mutableStateOf(Repo.peekArticles().orEmpty()) }
    LaunchedEffect(Unit) {
        try {
            articles = Repo.articles().value
        } catch (e: Exception) {
            // The saved copy (if any) stays on screen.
        }
    }

    Box(Modifier.fillMaxSize().background(Mali.Cream)) {
        LazyColumn(Modifier.fillMaxSize()) {
            item { PfHeader(totals, empty = plans.isEmpty()) }
            if (!PortfolioStore.online) item {
                Text(
                    "Offline. Showing the plans saved on this phone; changes are sent when you are back online.",
                    fontSize = 13.sp, lineHeight = 18.sp, color = Mali.Amber, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 0.dp),
                )
            }
            if (plans.isEmpty()) {
                item { PfEmpty(onAdd) }
            } else {
                item { PfAllocation(totals) }
                if (totals.blendedRate > 0.0) item { PfIncome(totals) }
                val dues = totals.held.filter { !it.standing.matured }
                    .mapNotNull { h -> PortfolioEngine.nextOpenDue(h.plan)?.let { d -> h to d } }
                    .sortedBy { it.second }.take(4)
                if (dues.isNotEmpty()) item {
                    PfDue(dues, onOpenPlan) { plan, due ->
                        // The payment is dated on the due date it settles, so that date moves forward and the money counts now.
                        PortfolioStore.save(plan.copy(payments = plan.payments + Payment(UUID.randomUUID().toString(), due.iso(), plan.contribution)))
                        android.widget.Toast.makeText(context, "Recorded " + kes(plan.contribution) + " for " + plan.name, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                item {
                    Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 4.dp)) { PfLabel("MY PLANS") }
                }
                items(totals.held, key = { it.plan.uid }) { h ->
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) { PfPlanRow(h) { onOpenPlan(h.plan.uid) } }
                }
            }
            val featuredArticles = articles.filter { it.featured }.ifEmpty { articles.take(6) }
            if (featuredArticles.isNotEmpty()) item { FeaturedArticles(featuredArticles, onOpenArticle, onAllPlaybooks) }
            else if (featured.isNotEmpty()) item { PfFeatured(featured, onOpenPlaybook) }
            item { Spacer(Modifier.height(120.dp)) }
        }

        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(18.dp)
                .shadow(8.dp, RoundedCornerShape(50))
                .clip(RoundedCornerShape(50))
                .background(Brush.linearGradient(listOf(Mali.OceanTop, Mali.OceanBottom)))
                .clickable(onClick = onAdd)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Add, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Add plan", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun PfHeader(t: PfTotals, empty: Boolean) {
    // The card keeps its old height and grows downward: the extra 30dp is the curved edge, like a bank balance card.
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp))
            .background(Brush.verticalGradient(listOf(Mali.OceanTop, Mali.OceanBottom)))
            .statusBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 48.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val profile = LocalOpenProfile.current
            val bell = LocalOpenNotifications.current
            if (profile != null) ProfileButton(profile)
            Column(Modifier.weight(1f)) {
                Text("My portfolio", fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("Everything you save and invest, in one place.", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
            }
            if (bell != null) {
                val unread = noticeItems(onOpenPaywall = {}, onOpenPlan = {}).isNotEmpty()
                BellButton(unread, bell)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("WORTH TODAY", fontSize = 12.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.ExtraBold, color = Color.White.copy(alpha = 0.8f))
        Text(kes(t.value), fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        if (!empty) {
            val n = t.held.size
            Text(
                "across $n plan" + (if (n == 1) "" else "s") + " · grows to " + shortKes(t.matures) + " by the end",
                fontSize = 13.sp, color = Color.White.copy(alpha = 0.88f),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PfHeaderStat("Paid in", shortKes(t.paid), Modifier.weight(1f))
                PfHeaderStat("Gained", shortKes(t.value - t.paid), Modifier.weight(1f))
                PfHeaderStat("Blended rate", percent(t.blendedRate), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PfHeaderStat(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.16f)).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PfEmpty(onAdd: () -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PfCard {
            Text("Start your first plan", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
            Spacer(Modifier.height(6.dp))
            Text(
                "Add what you save or invest, how often you pay in, and the rate you secured. MaliLab shows how it grows, what you could earn if you stopped paying in, and how your money is spread.",
                fontSize = 15.sp, lineHeight = 22.sp, color = Mali.InkSoft,
            )
            Spacer(Modifier.height(14.dp))
            PfButton("Add a plan", color = Mali.OceanTop, onClick = onAdd)
        }
        PfLabel("PLACES YOU CAN TRACK")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Vehicles.ALL.forEach { PfPill(it.title, it.color) }
        }
    }
}

@Composable
private fun PfAllocation(t: PfTotals) {
    Box(Modifier.padding(16.dp)) {
        PfCard {
            PfLabel("WHERE YOUR MONEY IS")
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                    DonutChart(t.slices.map { it.value to it.vehicle.color }, Modifier.fillMaxSize(), 24.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${t.slices.size}", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                        Text(if (t.slices.size == 1) "place" else "places", fontSize = 12.sp, color = Mali.InkSoft)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    t.slices.take(6).forEach { s ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(s.vehicle.color))
                            Spacer(Modifier.width(8.dp))
                            Text(s.vehicle.title, fontSize = 13.sp, color = Mali.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(percent(s.weight), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                        }
                    }
                    if (t.slices.size > 6) Text("and ${t.slices.size - 6} more", fontSize = 12.sp, color = Mali.InkSoft)
                }
            }
            Spacer(Modifier.height(14.dp))
            // The same weights as bars, so no one has to compare colours alone.
            t.slices.forEach { s ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(50)).background(Mali.Sand.copy(alpha = 0.5f))) {
                        Box(Modifier.fillMaxWidth((s.weight / 100.0).toFloat().coerceIn(0.02f, 1f)).height(8.dp).clip(RoundedCornerShape(50)).background(s.vehicle.color))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(shortKes(s.value), fontSize = 12.sp, color = Mali.InkSoft, modifier = Modifier.width(92.dp), textAlign = TextAlign.End)
                }
            }
        }
    }
}

/** What the portfolio is worth, the average rate on it, and the interest that rate would pay, each with its own explanation. */
@Composable
private fun PfIncome(t: PfTotals) {
    Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        PfCard {
            PfLabel("WHAT YOUR MONEY EARNS", Mali.OceanTop)
            PfIncomeRow("Your money today", kes(t.value), "What all your plans are worth right now.")
            PfIncomeLine()
            PfIncomeRow("Average rate", percent(t.blendedRate) + " a year", "Your plans' rates averaged together. A plan holding more money counts for more.")
            PfIncomeLine()
            PfIncomeRow(
                "Interest each month", kes(t.incomeYear / 12.0),
                "That is about " + kes(t.incomeYear) + " a year, if you stopped paying in and left the money alone. Tax you set is taken off.",
                valueColor = Mali.Amber,
            )
            if (t.monthly > 0) {
                PfIncomeLine()
                PfIncomeRow("You add each month", kes(t.monthly), "Your regular payments across the plans still running, shown as one monthly amount.")
            }
        }
    }
}

@Composable
private fun PfIncomeRow(label: String, value: String, note: String, valueColor: Color = Mali.Ink) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Mali.Ink, modifier = Modifier.weight(1f))
            Text(value, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
        }
        Spacer(Modifier.height(4.dp))
        Text(note, fontSize = 13.sp, lineHeight = 18.sp, color = Mali.InkSoft)
    }
}

@Composable
private fun PfIncomeLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Mali.Sand.copy(alpha = 0.7f)))
}

/** Upcoming top-ups. Tick one when you have paid it: it is logged as a payment and the reminder moves to the next date. */
@Composable
private fun PfDue(dues: List<Pair<PfHeld, Day>>, onOpen: (String) -> Unit, onTick: (Plan, Day) -> Unit) {
    Box(Modifier.padding(start = 16.dp, end = 16.dp)) {
        PfCard {
            PfLabel("CONTRIBUTION REMINDERS", Mali.Brown)
            Spacer(Modifier.height(8.dp))
            dues.forEach { (h, due) ->
                val left = (due.epoch() - Day.today().epoch()).toInt()
                val chip = when {
                    left < 0 -> "late"
                    left == 0 -> "today"
                    left == 1 -> "tomorrow"
                    else -> "in ${left}d"
                }
                val v = h.plan.vehicle
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(14.dp)).background(Mali.Cream)
                        .clickable { onOpen(h.plan.uid) }.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VehicleBadge(v, 40.dp, filled = true)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(h.plan.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Mali.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(kes(h.plan.contribution) + " · " + due.pretty(), fontSize = 12.sp, color = Mali.InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    PfPill(chip, if (left < 0) Mali.Pink else Mali.OceanTop)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(Mali.OceanTint)
                            .clickable { onTick(h.plan, due) }
                            .semantics { contentDescription = "I have paid ${h.plan.name}" },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Check, null, tint = Mali.OceanTop, modifier = Modifier.size(20.dp)) }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Due dates follow each plan's interval. Tap the tick when you have made a contribution to move it forward.",
                fontSize = 12.sp, lineHeight = 17.sp, color = Mali.InkSoft,
            )
        }
    }
}

@Composable
private fun PfPlanRow(h: PfHeld, onClick: () -> Unit) {
    val v = h.plan.vehicle
    PfCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VehicleBadge(v, 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(h.plan.name, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    v.title + if (h.plan.provider.isNotBlank()) " · " + h.plan.provider else "",
                    fontSize = 12.sp, color = Mali.InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(kes(h.standing.value), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                Text("${plainNum(currentRate(h.plan))}% a year", fontSize = 12.sp, color = v.color, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        val progress = (h.standing.monthsElapsed.toFloat() / h.plan.tenureMonths.coerceAtLeast(1)).coerceIn(0f, 1f)
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(Mali.Sand.copy(alpha = 0.5f))) {
            Box(Modifier.fillMaxWidth(progress.coerceAtLeast(0.02f)).height(6.dp).clip(RoundedCornerShape(50)).background(v.color))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (h.standing.matured) "Finished" else PortfolioEngine.label(h.plan.tenureMonths - h.standing.monthsElapsed) + " to go",
            fontSize = 12.sp, color = Mali.InkSoft,
        )
    }
}

private fun currentRate(p: Plan): Double = p.rateChanges.maxByOrNull { it.month }?.ratePct ?: p.ratePct

@Composable
private fun PfFeatured(list: List<PlaybookSummary>, onOpen: (String) -> Unit) {
    Column(Modifier.padding(top = 24.dp)) {
        Box(Modifier.padding(horizontal = 16.dp)) { PfLabel("FEATURED") }
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(list, key = { it.slug }) { p ->
                val accent = Mali.accentFor(p.slug)
                Column(
                    Modifier
                        .width(250.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Mali.Surface)
                        .border(1.dp, Mali.Sand.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        .clickable { onOpen(p.slug) },
                ) {
                    Box(Modifier.fillMaxWidth().aspectRatio(1200f / 630f)) { CoverImage(p.coverUrl, p.coverAlt, accent, Modifier.fillMaxSize()) }
                    Column(Modifier.padding(12.dp)) {
                        Text(p.title, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                        Text("Read more", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = accent)
                    }
                }
            }
        }
    }
}

// ============================================================================================ milestones
@Composable
private fun PfMilestoneCard(m: Milestone, index: Int, accent: Color) {
    PfCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
                Text("${index + 1}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            Spacer(Modifier.width(10.dp))
            Text("After ${m.label}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Mali.InkSoft)
        }
        Spacer(Modifier.height(6.dp))
        Text(kes(m.balance), fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
        Text("You paid in ${kes(m.paidIn)}  ·  Interest ${kes(m.interest)}", fontSize = 13.sp, lineHeight = 19.sp, color = Mali.InkSoft)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.10f)).padding(12.dp)) {
            Text("IF YOU STOP HERE AND KEEP THE SAME RATE", fontSize = 11.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.ExtraBold, color = Mali.TealDeep)
            Spacer(Modifier.height(6.dp))
            Row {
                Column(Modifier.weight(1f)) {
                    Text("Every month", fontSize = 12.sp, color = Mali.InkSoft)
                    Text(kes(m.incomeMonthly), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                }
                Column(Modifier.weight(1f)) {
                    Text("Every year", fontSize = 12.sp, color = Mali.InkSoft)
                    Text(kes(m.incomeAnnual), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                }
            }
        }
    }
}

// ============================================================================================ add or change a plan
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanEditorScreen(uid: String?, onClose: () -> Unit, onSaved: (String) -> Unit) {
    val existing = remember(uid) { uid?.let { PortfolioStore.get(it) } }
    var step by remember { mutableIntStateOf(if (existing != null) 1 else 0) }
    var category by remember { mutableStateOf(existing?.category.orEmpty()) }
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var provider by remember { mutableStateOf(existing?.provider.orEmpty()) }
    var startDate by remember { mutableStateOf(existing?.startDate ?: Day.today().iso()) }
    var initial by remember { mutableStateOf(existing?.initial?.takeIf { it > 0 }?.let { plainNum(it) }.orEmpty()) }
    var contribution by remember { mutableStateOf(existing?.contribution?.takeIf { it > 0 }?.let { plainNum(it) }.orEmpty()) }
    var interval by remember { mutableStateOf(existing?.interval ?: "monthly") }
    var rate by remember { mutableStateOf(existing?.let { plainNum(it.ratePct) }.orEmpty()) }
    var years by remember { mutableStateOf(existing?.let { plainNum(it.tenureMonths / 12.0) }.orEmpty()) }
    var basis by remember { mutableStateOf(existing?.rateBasis ?: "nominal") }
    var rateEvery by remember { mutableStateOf("year") }
    var timing by remember { mutableStateOf(existing?.timing ?: "end") }
    var payout by remember { mutableStateOf(existing?.payout ?: "reinvest") }
    var tax by remember { mutableStateOf(existing?.taxPct?.takeIf { it > 0 }?.let { plainNum(it) }.orEmpty()) }
    var remind by remember { mutableStateOf(existing?.reminderOn ?: true) }
    var remindHour by remember { mutableIntStateOf(existing?.reminderHour ?: 8) }
    var remindBefore by remember { mutableIntStateOf(existing?.reminderDaysBefore ?: 0) }
    var more by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf("") }
    var built by remember { mutableStateOf<Plan?>(null) }

    BackHandler {
        when {
            step == 2 -> step = 1
            step == 1 && existing == null -> step = 0
            else -> onClose()
        }
    }

    fun pick(v: Vehicle) {
        category = v.key
        interval = v.interval
        payout = v.payout
        step = 1
    }

    fun build(): Plan? {
        problem = ""
        val day = Day.parse(startDate)
        if (day == null) { problem = "Write the start date like 2026-10-25."; return null }
        val ini = if (initial.isBlank()) 0.0 else parseNum(initial)
        val con = if (contribution.isBlank() || interval == "once") 0.0 else parseNum(contribution)
        if (ini == null || ini < 0) { problem = "The starting amount must be a number."; return null }
        if (con == null || con < 0) { problem = "The regular amount must be a number."; return null }
        if (ini + con <= 0) { problem = "Enter a starting amount, a regular amount, or both."; return null }
        val typed = parseNum(rate)
        if (typed == null || typed < 0 || typed > 100) { problem = "Enter the rate as a number from 0 to 100."; return null }
        val r = yearlyRate(typed, rateEvery)
        if (r > 100) { problem = "That works out to more than 100% a year. Check the rate and how it is quoted."; return null }
        val y = parseNum(years)
        val months = if (y == null) 0 else Math.round(y * 12).toInt()
        if (months < 1 || months > 720) { problem = "Enter how many years, from 1 month up to 60 years."; return null }
        val t = if (tax.isBlank()) 0.0 else parseNum(tax)
        if (t == null || t < 0 || t > 100) { problem = "Tax must be a number from 0 to 100."; return null }
        val base = existing ?: Plan()
        return base.copy(
            category = category, name = name.trim().ifBlank { Vehicles.get(category).title }.take(120), provider = provider.trim().take(120), startDate = day.iso(),
            initial = ini, contribution = con, interval = interval, ratePct = r, rateBasis = if (rateEvery == "year") basis else "effective", tenureMonths = months,
            payout = payout, taxPct = t, timing = timing,
            reminderOn = remind, reminderHour = remindHour, reminderDaysBefore = remindBefore,
        )
    }

    val vehicle = if (category.isBlank()) null else Vehicles.get(category)
    val accent = vehicle?.color ?: Mali.OceanTop

    Column(Modifier.fillMaxSize().background(Mali.Cream)) {
        StatusBarIcons(darkIcons = false)
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Mali.OceanTop, Mali.OceanBottom)))
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackCircle(onBack = {
                    when {
                        step == 2 -> step = 1
                        step == 1 && existing == null -> step = 0
                        else -> onClose()
                    }
                }, tint = Color.White)
                Column {
                    Text(if (existing != null) "Change plan" else "New plan", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(
                        when (step) { 0 -> "Step 1 of 3: where is the money going?"; 1 -> "Step 2 of 3: the details"; else -> "Step 3 of 3: what it could become" },
                        fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (step) {
                0 -> {
                    Text("Pick the kind of plan. You can track as many as you like.", fontSize = 15.sp, lineHeight = 22.sp, color = Mali.InkSoft)
                    Vehicles.ALL.forEach { v ->
                        PfCard(onClick = { pick(v) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                VehicleBadge(v, 46.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(v.title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                                    Text(v.short, fontSize = 13.sp, color = Mali.InkSoft)
                                }
                                PfPill(v.rateKindLabel, v.color)
                            }
                        }
                    }
                }

                1 -> {
                    if (vehicle != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(vehicle.color))
                            Spacer(Modifier.width(8.dp))
                            Text(vehicle.title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink, modifier = Modifier.weight(1f))
                            if (existing == null) ActionChip("Change", color = vehicle.color) { step = 0 }
                        }
                        Text(vehicle.hint, fontSize = 14.sp, lineHeight = 21.sp, color = Mali.InkSoft)
                    }
                    PfField("Name of this plan / goal of this investment", name, { name = it }, hint = "For example: School fees, Emergency fund, Land in Kitengela. Left empty, it is called \"${vehicle?.title.orEmpty()}\".")
                    PfField("Where it is held (optional)", provider, { provider = it }, hint = "For example the name of your SACCO, fund or bank.")
                    PfField("Start date", startDate, { startDate = it }, hint = "Year-month-day, like 2026-10-25. Use the day the plan began.")
                    PfField("Starting amount (KES)", initial, { initial = it }, number = true, hint = "What you put in on day one. Leave empty if you start with regular payments only.")
                    PfLabel("HOW OFTEN DO YOU PAY IN?")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        INTERVAL_CHOICES.forEach { (key, word) -> ActionChip(word, selected = interval == key, color = accent) { interval = key } }
                    }
                    if (interval != "once") PfField("Amount each time (KES)", contribution, { contribution = it }, number = true)
                    val perWord = RATE_PERIODS.first { it.first == rateEvery }.let { it.second }
                    val typedRate = parseNum(rate)
                    PfField(
                        (vehicle?.rateLabel ?: "Rate (% a year)").replace("% a year", "% $perWord"), rate, { rate = it }, number = true,
                        hint = "The rate you secured or expect. ${vehicle?.rateKindLabel.orEmpty()}." +
                            if (rateEvery != "year" && typedRate != null) " That works out to about ${plainNum(Math.round(yearlyRate(typedRate, rateEvery) * 100.0) / 100.0)}% a year." else "",
                    )
                    PfField("For how many years?", years, { years = it }, number = true, hint = "Use a decimal for part of a year, like 2.5.")
                    if (interval != "once") {
                        PfLabel("PAYMENT REMINDER")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionChip("Remind me", selected = remind, color = accent) { remind = true }
                            ActionChip("No reminder", selected = !remind, color = accent) { remind = false }
                        }
                        if (remind) {
                            Text("What time?", fontSize = 13.sp, color = Mali.InkSoft)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(6 to "6 am", 8 to "8 am", 12 to "12 noon", 17 to "5 pm", 20 to "8 pm").forEach { (h, word) ->
                                    ActionChip(word, selected = remindHour == h, color = accent) { remindHour = h }
                                }
                            }
                            Text("How early?", fontSize = 13.sp, color = Mali.InkSoft)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(0 to "On the day", 1 to "1 day before", 3 to "3 days before").forEach { (d, word) ->
                                    ActionChip(word, selected = remindBefore == d, color = accent) { remindBefore = d }
                                }
                            }
                        }
                    }
                    ActionChip(if (more) "Hide more options" else "More options", color = Mali.TealDeep) { more = !more }
                    if (more) {
                        PfLabel("HOW THE RATE IS QUOTED")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            RATE_CHIPS.forEach { (key, word) -> ActionChip(word, selected = rateEvery == key, color = accent) { rateEvery = key } }
                        }
                        if (rateEvery == "year") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ActionChip("Nominal (12 equal months)", selected = basis == "nominal", color = accent) { basis = "nominal" }
                                ActionChip("Effective yearly", selected = basis == "effective", color = accent) { basis = "effective" }
                            }
                        }
                        PfLabel("WHEN EACH PAYMENT IS ADDED")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionChip("End of period", selected = timing == "end", color = accent) { timing = "end" }
                            ActionChip("Start of period", selected = timing == "start", color = accent) { timing = "start" }
                        }
                        PfLabel("WHAT HAPPENS TO THE INTEREST")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionChip("Reinvest it", selected = payout == "reinvest", color = accent) { payout = "reinvest" }
                            ActionChip("Pay it out to me", selected = payout == "income", color = accent) { payout = "income" }
                        }
                        PfField("Tax taken off the interest (%)", tax, { tax = it }, number = true, hint = "Leave empty to ignore tax.")
                        if (vehicle != null && vehicle.taxPct > 0) {
                            ActionChip("Use ${plainNum(vehicle.taxPct)}% withholding tax (KRA/CBK guidance)", color = Mali.TealDeep) { tax = plainNum(vehicle.taxPct) }
                        }
                    }
                }

                else -> {
                    val plan = built
                    if (plan != null) {
                        val proj = remember(plan) { PortfolioEngine.project(plan) }
                        Text(plan.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                        Text(
                            "${kes(plan.initial)} to start" + if (plan.contribution > 0 && plan.interval != "once") ", then ${kes(plan.contribution)} ${intervalWord(plan.interval).lowercase()}" else "",
                            fontSize = 14.sp, color = Mali.InkSoft,
                        )
                        Text("at ${plainNum(plan.ratePct)}% a year for ${PortfolioEngine.label(plan.tenureMonths)}.", fontSize = 14.sp, color = Mali.InkSoft)
                        PfCard { PfGrowth(proj.series, accent) }
                        proj.milestones.forEachIndexed { i, m -> PfMilestoneCard(m, i, accent) }
                        PfCard {
                            PfLabel("HOW THIS IS WORKED OUT")
                            Spacer(Modifier.height(6.dp))
                            proj.notes.forEach { Text("• $it", fontSize = 13.sp, lineHeight = 19.sp, color = Mali.InkSoft, modifier = Modifier.padding(vertical = 2.dp)) }
                        }
                    }
                }
            }
            if (problem.isNotBlank()) Text(problem, fontSize = 14.sp, color = Mali.Pink, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(80.dp))
        }

        if (step > 0) {
            Row(
                Modifier.fillMaxWidth().background(Mali.Surface).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (step == 2) {
                    PfButton("Back", filled = false, color = Mali.TealDeep, modifier = Modifier.weight(1f)) { step = 1 }
                    PfButton("Save plan", color = Mali.OceanTop, modifier = Modifier.weight(1f)) {
                        built?.let { onSaved(PortfolioStore.save(it)) }
                    }
                } else {
                    PfButton("See what it could become", color = Mali.OceanTop, modifier = Modifier.fillMaxWidth()) {
                        val p = build()
                        if (p != null) { built = p; step = 2 }
                    }
                }
            }
        }
    }
}

// ============================================================================================ one plan
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanDetailScreen(uid: String, active: Boolean, onBack: () -> Unit, onEdit: (String) -> Unit) {
    BackHandler(enabled = active) { onBack() }
    StatusBarIcons(darkIcons = false, active = active)
    val plan = PortfolioStore.plans.firstOrNull { it.uid == uid }
    if (plan == null) {
        Box(Modifier.fillMaxSize().background(Mali.Cream), contentAlignment = Alignment.Center) {
            PfButton("This plan is gone. Go back", color = Mali.OceanTop, onClick = onBack)
        }
        return
    }
    val v = plan.vehicle
    val proj = remember(plan) { PortfolioEngine.project(plan) }
    val st = remember(plan) { PortfolioEngine.standing(plan) }
    var dialog by remember { mutableStateOf("") }
    val rateNow = currentRate(plan)
    val context = LocalContext.current
    var testNote by remember { mutableStateOf("") }
    val askTest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        testNote = if (ok && Reminders.sendTest(context, plan)) "Test sent. Look in your notifications." else "Notifications are off for MaliLab. Turn them on in your phone's settings."
    }

    fun change(p: Plan) { PortfolioStore.save(p) }

    Column(Modifier.fillMaxSize().background(Mali.Cream).verticalScroll(rememberScrollState())) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(v.color, Mali.OceanTop)))
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BackCircle(onBack, tint = Color.White)
                Column(Modifier.weight(1f)) {
                    Text(plan.name, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(v.title + if (plan.provider.isNotBlank()) " · " + plan.provider else "", fontSize = 13.sp, color = Color.White.copy(alpha = 0.88f))
                }
                Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)).clickable { onEdit(plan.uid) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Edit, contentDescription = "Change plan", tint = Mali.TealFixed)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("WORTH TODAY", fontSize = 12.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.ExtraBold, color = Color.White.copy(alpha = 0.8f))
            Text(kes(st.value), fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(
                "Paid in ${kes(st.paidIn)}  ·  " + (if (st.gain >= 0) "Gained " else "Down ") + kes(Math.abs(st.gain)) +
                    if (st.modelled) "  ·  Worked out from your plan" else "  ·  From the value you entered",
                fontSize = 13.sp, lineHeight = 18.sp, color = Color.White.copy(alpha = 0.9f),
            )
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PfCard {
                PfLabel("THIS PLAN")
                Spacer(Modifier.height(8.dp))
                PfFact("Rate now", "${plainNum(rateNow)}% a year  (${v.rateKindLabel.lowercase()})")
                PfFact("Pays in", if (plan.contribution > 0 && plan.interval != "once") "${kes(plan.contribution)}, ${intervalWord(plan.interval).lowercase()}" else "One-off")
                PfFact("Started", Day.parse(plan.startDate)?.pretty() ?: plan.startDate)
                PfFact("Runs for", PortfolioEngine.label(plan.tenureMonths) + if (st.matured) " (finished)" else "")
                val open = PortfolioEngine.nextOpenDue(plan)
                if (open != null && !st.matured) PfFact("Next payment", open.pretty())
                if (plan.contribution > 0 && plan.interval != "once") PfFact("Reminder", if (plan.reminderOn) (if (plan.reminderDaysBefore == 0) "On the day" else "${plan.reminderDaysBefore} day(s) before") + ", " + (if (plan.reminderHour < 12) "${plan.reminderHour} am" else if (plan.reminderHour == 12) "12 noon" else "${plan.reminderHour - 12} pm") else "Off")
                if (plan.taxPct > 0) PfFact("Tax taken off", "${plainNum(plan.taxPct)}% of interest")
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (plan.contribution > 0 && plan.interval != "once") ActionChip("I paid in", color = Mali.Green) { dialog = "pay" }
                ActionChip("The rate changed", color = Mali.Indigo) { dialog = "rate" }
                ActionChip("Update its value", color = Mali.Brown) { dialog = "value" }
                if (plan.contribution > 0 && plan.interval != "once") ActionChip("Send a test reminder", color = Mali.TealDeep) {
                    if (Reminders.canNotify(context)) {
                        testNote = if (Reminders.sendTest(context, plan)) "Test sent. Look in your notifications." else "Notifications are off for MaliLab. Turn them on in your phone's settings."
                    } else if (Build.VERSION.SDK_INT >= 33) {
                        askTest.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        testNote = "Notifications are off for MaliLab. Turn them on in your phone's settings."
                    }
                }
            }
            if (testNote.isNotEmpty()) Text(testNote, fontSize = 13.sp, lineHeight = 18.sp, color = Mali.InkSoft)

            PfLabel("WHAT IT COULD BECOME")
            PfCard { PfGrowth(proj.series, v.color) }
            proj.milestones.forEachIndexed { i, m -> PfMilestoneCard(m, i, v.color) }

            if (plan.rateChanges.isNotEmpty()) {
                PfCard {
                    PfLabel("RATE HISTORY")
                    Spacer(Modifier.height(8.dp))
                    PfFact("Started at", "${plainNum(plan.ratePct)}%")
                    plan.rateChanges.sortedBy { it.month }.forEach { c ->
                        PfFact("From month ${c.month}", "${plainNum(c.ratePct)}%")
                    }
                }
            }
            if (plan.payments.isNotEmpty()) {
                PfCard {
                    PfLabel("PAYMENTS YOU LOGGED")
                    Spacer(Modifier.height(8.dp))
                    plan.payments.sortedByDescending { it.date }.take(8).forEach { PfFact(Day.parse(it.date)?.pretty() ?: it.date, kes(it.amount)) }
                    Spacer(Modifier.height(10.dp))
                    ActionChip(
                        if (plan.useActuals) "Counting only my logged payments" else "Count only my logged payments",
                        selected = plan.useActuals, color = Mali.Green,
                    ) { change(plan.copy(useActuals = !plan.useActuals)) }
                    Text(
                        "By default the worth counts every payment as made on time. Turn this on to count only the ones you logged.",
                        fontSize = 12.sp, lineHeight = 17.sp, color = Mali.InkSoft, modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            PfCard {
                PfLabel("HOW THIS IS WORKED OUT")
                Spacer(Modifier.height(6.dp))
                proj.notes.forEach { Text("• $it", fontSize = 13.sp, lineHeight = 19.sp, color = Mali.InkSoft, modifier = Modifier.padding(vertical = 2.dp)) }
            }
            Text(
                "Delete this plan",
                color = Mali.Pink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Mali.Pink.copy(alpha = 0.10f)).clickable { dialog = "delete" }.padding(vertical = 14.dp),
            )
            Spacer(Modifier.height(40.dp))
        }
    }

    when (dialog) {
        "pay" -> PfAmountDialog(
            title = "I paid in", label = "Amount (KES)", start = plainNum(plan.contribution), confirm = "Save",
            onDismiss = { dialog = "" },
        ) { amount ->
            change(plan.copy(payments = plan.payments + Payment(UUID.randomUUID().toString(), Day.today().iso(), amount)))
            dialog = ""
        }
        "rate" -> PfAmountDialog(
            title = "The rate changed", label = "New rate (% a year)", start = plainNum(rateNow), confirm = "Save",
            note = "The new rate counts from this month on. Earlier months keep the old rate.", onDismiss = { dialog = "" },
        ) { newRate ->
            if (newRate in 0.0..100.0) {
                val month = maxOf(1, Day.monthsBetween(plan.start, Day.today()) + 1)
                val kept = plan.rateChanges.filter { it.month != month }
                change(plan.copy(rateChanges = kept + RateChange(month, newRate)))
            }
            dialog = ""
        }
        "value" -> PfAmountDialog(
            title = "Update its value", label = "Worth today (KES)", start = plainNum(Math.round(st.value).toDouble()), confirm = "Save",
            note = "Use your statement, broker or a valuer. MaliLab grows this figure from today on.", onDismiss = { dialog = "" },
        ) { value ->
            change(plan.copy(valuation = Valuation(Day.today().iso(), value)))
            dialog = ""
        }
        "delete" -> AlertDialog(
            onDismissRequest = { dialog = "" },
            title = { Text("Delete this plan?") },
            text = { Text("It is removed from your portfolio on every device.") },
            confirmButton = { TextButton(onClick = { PortfolioStore.remove(plan.uid); dialog = ""; onBack() }) { Text("Delete", color = Mali.Pink) } },
            dismissButton = { TextButton(onClick = { dialog = "" }) { Text("Keep it") } },
            containerColor = Mali.Surface,
        )
    }
}

@Composable
private fun PfFact(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = Mali.InkSoft, modifier = Modifier.weight(0.45f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Mali.Ink, textAlign = TextAlign.End, modifier = Modifier.weight(0.55f))
    }
}

@Composable
private fun PfAmountDialog(
    title: String,
    label: String,
    start: String,
    confirm: String,
    note: String = "",
    onDismiss: () -> Unit,
    onDone: (Double) -> Unit,
) {
    var text by remember { mutableStateOf(start) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (note.isNotBlank()) Text(note, fontSize = 13.sp, lineHeight = 18.sp, color = Mali.InkSoft)
                PfField(label, text, { text = it }, number = true)
            }
        },
        confirmButton = { TextButton(onClick = { parseNum(text)?.let(onDone) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Mali.Surface,
    )
}
