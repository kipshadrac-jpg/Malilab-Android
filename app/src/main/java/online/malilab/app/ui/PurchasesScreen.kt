package online.malilab.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import online.malilab.app.data.ApiError
import online.malilab.app.data.Billing
import online.malilab.app.data.PurchaseHistory
import online.malilab.app.ui.theme.Mali

/** Turns the website's timestamp into a short date on this phone, such as "25 Oct 2026, 09:30". */
private fun niceDate(iso: String): String {
    if (iso.length < 19) return iso
    return try {
        val parse = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val d = parse.parse(iso.substring(0, 19)) ?: return iso.take(10)
        SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(d)
    } catch (e: Exception) {
        iso.take(10)
    }
}

/** What the learner has paid for, with M-Pesa receipts, and what they own for good. */
@Composable
fun PurchasesScreen(active: Boolean, onClose: () -> Unit, onBuy: () -> Unit) {
    var history by remember { mutableStateOf<PurchaseHistory?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableStateOf(0) }

    BackHandler(enabled = active) { onClose() }
    StatusBarIcons(darkIcons = true, active = active)

    LaunchedEffect(tick) {
        error = null
        try {
            history = Billing.purchases()
        } catch (e: ApiError) {
            error = if (e.status == 0) "No connection right now. Your purchases are kept safely on MaliLab." else e.message
        }
    }

    Column(Modifier.fillMaxSize().background(Mali.Surface)) {
        Row(
            Modifier.fillMaxWidth().background(Mali.TealTint).statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackCircle(onClose, circleAlpha = 1f)
            Column(Modifier.weight(1f)) {
                Text("ME", fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.ExtraBold, color = Mali.TealDeep)
                Text("My purchases", fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
            }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val h = history
            val err = error
            if (h == null && err == null) {
                Text("Loading…", fontSize = 16.sp, color = Mali.InkSoft)
            }
            if (err != null) {
                Text(
                    err, fontSize = 15.sp, lineHeight = 22.sp, color = Mali.Ink,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Mali.Amber.copy(alpha = 0.12f)).padding(14.dp),
                )
                Text(
                    "Try again", color = Mali.TealDeep, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Mali.TealTint).clickable { tick++ }.padding(vertical = 13.dp),
                )
            }
            if (h != null) {
                if (h.allModulesAt != null) {
                    Card(Mali.Green, "ALL MODULES", "Every module is yours for good, including new ones.", "Since " + niceDate(h.allModulesAt))
                }
                h.granted.forEach { g ->
                    Card(Mali.Indigo, "GIVEN TO YOU", g.title.ifBlank { "A module" }, "Yours for good · " + niceDate(g.at))
                }
                h.purchases.forEach { p ->
                    val what = if (p.product == "all") "All modules" else p.title.ifBlank { "A module" }
                    val (label, color) = when (p.status) {
                        "paid" -> "PAID" to Mali.Green
                        "pending" -> "WAITING" to Mali.Amber
                        "review" -> "BEING CHECKED" to Mali.Amber
                        "cancelled" -> "CANCELLED" to Mali.Pink
                        "expired" -> "EXPIRED" to Mali.Pink
                        else -> "NOT PAID" to Mali.Pink
                    }
                    val line = buildString {
                        append("KES ").append(p.amount).append(" · ").append(niceDate(p.at))
                        if (p.receipt.isNotBlank()) append("\nM-Pesa receipt ").append(p.receipt)
                        if (p.sandbox) append("\nTest payment")
                        if (p.status == "review") append("\nThe amount did not match. MaliLab will look at it and sort it out.")
                    }
                    Card(color, label, what, line)
                }
                if (h.purchases.isEmpty() && h.granted.isEmpty() && h.allModulesAt == null) {
                    Text(
                        "You have not bought anything yet. Modules cost KES ${Billing.status.priceModule} each, or KES ${Billing.status.priceAll} for all of them. You pay once and keep them for good, and nothing renews.",
                        fontSize = 16.sp, lineHeight = 23.sp, color = Mali.Ink,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Unlock a module", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Mali.Teal).clickable { onBuy() }.padding(vertical = 15.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Card(color: Color, label: String, title: String, detail: String) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.07f)).padding(16.dp)) {
        Text(label, fontSize = 12.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Spacer(Modifier.height(4.dp))
        Text(title, fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
        Spacer(Modifier.height(4.dp))
        Text(detail, fontSize = 14.sp, lineHeight = 20.sp, color = Mali.InkSoft)
    }
}
