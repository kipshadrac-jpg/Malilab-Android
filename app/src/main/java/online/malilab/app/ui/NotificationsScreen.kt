package online.malilab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.malilab.app.data.Billing
import online.malilab.app.data.Day
import online.malilab.app.data.PortfolioEngine
import online.malilab.app.data.PortfolioStore
import online.malilab.app.data.kes
import online.malilab.app.ui.theme.Mali

/** One line in the notifications list. [onOpen] is what a tap does (or null when there is nothing to open). */
class NoticeItem(val title: String, val body: String, val color: Color, val onOpen: (() -> Unit)?)

/** Everything worth telling the person right now: where their free plan stands, and payments that are close. */
@Composable
fun noticeItems(onOpenPaywall: () -> Unit, onOpenPlan: (String) -> Unit): List<NoticeItem> {
    val out = mutableListOf<NoticeItem>()
    val st = Billing.status
    if (st.paywall && !st.allModules) {
        if (st.trialActive) {
            val days = ((st.trialSeconds + 86399) / 86400).toInt()
            val title = when {
                days <= 0 -> "Your free plan ends today"
                days == 1 -> "Your free plan ends tomorrow"
                else -> "Your free plan ends in $days days"
            }
            out += NoticeItem(
                title,
                "After that MaliLab is free with ads. Modules are KES ${st.priceModule} each, or KES ${st.priceAll} for all of them. You pay once and keep them.",
                Mali.Amber, onOpenPaywall,
            )
        } else {
            out += NoticeItem(
                "You are on the free plan",
                "Free with ads. Unlock a module for KES ${st.priceModule}, or all of them for KES ${st.priceAll}. You pay once and keep them.",
                Mali.Amber, onOpenPaywall,
            )
        }
    }
    val today = Day.today()
    for (p in PortfolioStore.plans) {
        if (p.contribution <= 0.0) continue
        val s = PortfolioEngine.standing(p, today)
        val due = PortfolioEngine.nextOpenDue(p, today) ?: continue
        if (s.matured) continue
        val left = (due.epoch() - today.epoch()).toInt()
        if (left < 0 || left > 7) continue
        if (p.payments.any { it.date == due.iso() || it.date == today.iso() }) continue
        val when_ = when (left) { 0 -> "today"; 1 -> "tomorrow"; else -> "in $left days" }
        out += NoticeItem("Payment due: " + p.name, kes(p.contribution) + " on " + due.pretty() + " (" + when_ + ")", Mali.Teal) { onOpenPlan(p.uid) }
    }
    return out
}

@Composable
fun NotificationsScreen(active: Boolean, onBack: () -> Unit, onOpenPaywall: () -> Unit, onOpenPlan: (String) -> Unit) {
    StatusBarIcons(darkIcons = true, active = active)
    val items = noticeItems(onOpenPaywall, onOpenPlan)
    Column(Modifier.fillMaxSize().background(Mali.Cream).pointerInput(Unit) { detectTapGestures { } }) {
        Row(
            Modifier.fillMaxWidth().background(Mali.Surface).statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackCircle(onBack)
            Text("Notifications", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink, modifier = Modifier.padding(start = 6.dp))
        }
        if (items.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(60.dp))
                Icon(NavIcons.Bell, null, tint = Mali.InkSoft.copy(alpha = 0.5f), modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
                Text("You are all caught up", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                Spacer(Modifier.height(6.dp))
                Text("Payment reminders and news about your plan appear here.", fontSize = 15.sp, lineHeight = 22.sp, color = Mali.InkSoft)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().navigationBarsPadding(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items) { n ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Mali.Surface)
                            .background(n.color.copy(alpha = 0.07f))
                            .then(if (n.onOpen != null) Modifier.clickable(onClick = n.onOpen) else Modifier)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(Modifier.size(42.dp).clip(CircleShape).background(n.color.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                            Icon(NavIcons.Bell, null, tint = n.color, modifier = Modifier.size(24.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(n.title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
                            Spacer(Modifier.height(4.dp))
                            Text(n.body, fontSize = 14.sp, lineHeight = 20.sp, color = Mali.InkSoft)
                        }
                    }
                }
            }
        }
    }
}
