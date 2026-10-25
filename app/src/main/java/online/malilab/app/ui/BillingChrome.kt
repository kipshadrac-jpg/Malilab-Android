package online.malilab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.malilab.app.data.Billing
import online.malilab.app.ui.theme.Mali

/**
 * The strip above the tab bar. During the free trial it says how long is left. After the trial it is the place where an
 * ad appears; until the ad network is connected it shows a plain placeholder. Owned modules never show ads.
 */
@Composable
fun BillingStrip(onOpenPaywall: () -> Unit) {
    val st = Billing.status
    if (!st.paywall) return
    if (st.ads) {
        var adFailed by remember { mutableStateOf(false) }
        Column(Modifier.fillMaxWidth().background(Mali.Surface)) {
            if (!adFailed) AdBanner(onFailed = { adFailed = true })
            Row(
                Modifier.fillMaxWidth().background(Mali.Sand.copy(alpha = 0.55f)).clickable(onClick = onOpenPaywall).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Modules you unlock are free of ads, for good. Tap to unlock.",
                    fontSize = 13.sp, color = Mali.Ink, textAlign = TextAlign.Center,
                )
            }
        }
    } else if (st.trialActive) {
        val days = (st.trialSeconds + 86399) / 86400
        Row(
            Modifier.fillMaxWidth().background(Mali.Amber.copy(alpha = 0.12f)).clickable(onClick = onOpenPaywall).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                "Free trial: " + (if (days <= 1) "less than a day" else "$days days") + " left. Modules are KES ${st.priceModule} each.",
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Mali.Amber, textAlign = TextAlign.Center,
            )
        }
    }
}

/** One line for the Me tab. */
fun billingSummary(): String {
    val st = Billing.status
    return when {
        !st.paywall -> "Everything is open while MaliLab is being tested."
        st.allModules -> "You have every module, for good. No ads inside modules."
        st.trialActive -> "Free trial. Modules are KES ${st.priceModule} each, or KES ${st.priceAll} for all of them. You pay once and keep them."
        else -> "Free with ads. Unlock a module for KES ${st.priceModule}, or all of them for KES ${st.priceAll}. You pay once and keep them."
    } + if (st.modules.isNotEmpty() && !st.allModules) "\n\nModules you own: ${st.modules.size}." else ""
}
