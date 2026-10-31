package online.malilab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.malilab.app.data.Billing
import online.malilab.app.ui.theme.Mali

/** The strip above the tab bar. During the free and bonus months it is where payment starts; owners see nothing. */
@Composable
fun BillingStrip(onOpenPaywall: () -> Unit) {
    val st = Billing.status
    if (!st.inFreeWindow || st.allModules) return
    Row(
        Modifier.fillMaxWidth().background(Mali.Sand.copy(alpha = 0.55f)).clickable(onClick = onOpenPaywall).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            "Unlock Premium once for KES ${st.priceAll}, for good. Tap to unlock.",
            fontSize = 13.sp, color = Mali.Ink, textAlign = TextAlign.Center,
        )
    }
}

/** One line for the Me tab. */
fun billingSummary(): String {
    val st = Billing.status
    return when {
        !st.paywall -> "Everything is open while MaliLab is being tested."
        st.allModules -> "You have Premium, for good."
        st.phase == "free" -> "Free month: ${st.daysLeft} days left. Premium is KES ${st.priceAll}, paid once."
        st.phase == "bonus" -> "Bonus month: ${st.daysLeft} days left. Premium is KES ${st.priceAll}, paid once."
        else -> "Premium is KES ${st.priceAll}, paid once and kept for good."
    }
}
