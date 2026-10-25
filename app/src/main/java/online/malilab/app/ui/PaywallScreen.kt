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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import online.malilab.app.data.ApiError
import online.malilab.app.data.Billing
import online.malilab.app.data.PaymentInfo
import online.malilab.app.ui.theme.Mali

/**
 * Pay for one module, or for all of them, with an M-Pesa prompt. Nothing renews: each purchase is made on purpose
 * and kept for good. [moduleSlug] is null when the screen is opened without a module in mind.
 */
@Composable
fun PaywallScreen(moduleSlug: String?, moduleTitle: String, active: Boolean, onClose: () -> Unit) {
    val st = Billing.status
    val scope = rememberCoroutineScope()
    var choice by remember { mutableStateOf(if (moduleSlug != null) "module" else "all") }
    var phone by remember { mutableStateOf(Billing.lastPhone()) }
    var phase by remember { mutableStateOf("form") }          // form, sending, waiting, stopped, paid
    var message by remember { mutableStateOf<String?>(null) }
    var paymentId by remember { mutableIntStateOf(0) }
    var pollTick by remember { mutableIntStateOf(0) }

    BackHandler(enabled = active) { if (phase != "sending") onClose() }
    StatusBarIcons(darkIcons = true, active = active)

    LaunchedEffect(paymentId, pollTick) {
        if (paymentId == 0) return@LaunchedEffect
        phase = "waiting"
        var tries = 0
        while (tries < 40) {
            delay(3000)
            try {
                val p = Billing.check(paymentId)
                when (p.status) {
                    "paid" -> {
                        phase = "paid"
                        return@LaunchedEffect
                    }
                    "pending" -> {}
                    else -> {
                        message = failureText(p)
                        phase = "form"
                        paymentId = 0
                        return@LaunchedEffect
                    }
                }
            } catch (e: ApiError) {
                if (e.status != 0) {
                    message = e.message
                    phase = "form"
                    paymentId = 0
                    return@LaunchedEffect
                }
            }
            tries++
        }
        phase = "stopped"
    }

    fun pay() {
        phase = "sending"
        message = null
        scope.launch {
            try {
                val p = Billing.startPayment(choice, moduleSlug, phone)
                Billing.rememberPhone(phone)
                paymentId = p.id
            } catch (e: ApiError) {
                message = e.message
                phase = "form"
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Color.White).imePadding()) {
        Row(
            Modifier.fillMaxWidth().background(Mali.TealTint).statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BackCircle({ if (phase != "sending") onClose() }, circleAlpha = 1f)
            Column(Modifier.weight(1f)) {
                Text("UNLOCK", fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.ExtraBold, color = Mali.TealDeep)
                Text(if (moduleSlug != null) moduleTitle.ifBlank { "This module" } else "All modules", fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink, maxLines = 2)
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (phase) {
                "paid" -> {
                    Text(
                        "Unlocked ✓",
                        fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Green,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Mali.Green.copy(alpha = 0.10f)).padding(18.dp),
                    )
                    Text(
                        if (choice == "all") "Every module is yours now, with no ads inside them." else "This module is yours for good, with no ads inside it.",
                        fontSize = 16.sp, lineHeight = 23.sp, color = Mali.Ink,
                    )
                    PayButton("Start learning", Mali.Teal) { onClose() }
                }
                "waiting", "stopped", "sending" -> {
                    Text(
                        when (phase) {
                            "sending" -> "Sending the payment prompt…"
                            "waiting" -> "Check your phone and enter your M-Pesa PIN."
                            else -> "Still waiting. If you approved the payment, tap Check again."
                        },
                        fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Mali.Cream).padding(18.dp),
                    )
                    if (phase == "waiting") Text("This page updates by itself when the payment goes through. It can take up to a minute.", fontSize = 14.sp, lineHeight = 20.sp, color = Mali.InkSoft)
                    if (st.sandbox) Text("Test mode: no real money is taken.", fontSize = 13.sp, color = Mali.Amber, fontWeight = FontWeight.Bold)
                    if (phase == "stopped") {
                        PayButton("Check again", Mali.Teal) { pollTick++ }
                        PayButton("Close", Mali.InkSoft) { onClose() }
                    }
                }
                else -> {
                    Text(
                        "Pay once, keep it for good. There is no subscription and nothing is charged again.",
                        fontSize = 15.sp, lineHeight = 22.sp, color = Mali.Ink,
                    )
                    if (moduleSlug != null) {
                        OptionCard("This module", "KES ${st.priceModule}", "Read every lesson and do the practice, ad-free, for good.", choice == "module") { choice = "module" }
                    }
                    OptionCard("All modules", "KES ${st.priceAll}", "Every module, now and after, ad-free, for good.", choice == "all") { choice = "all" }
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' || c == ' ' }.take(16) },
                        label = { Text("M-Pesa phone number") },
                        placeholder = { Text("0712 345 678") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                    )
                    val m = message
                    if (m != null) {
                        Text(
                            m, fontSize = 14.sp, lineHeight = 20.sp, color = Mali.Pink, fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Mali.Pink.copy(alpha = 0.08f)).padding(12.dp),
                        )
                    }
                    if (st.sandbox) Text("Test mode: no real money is taken.", fontSize = 13.sp, color = Mali.Amber, fontWeight = FontWeight.Bold)
                    val price = if (choice == "all") st.priceAll else st.priceModule
                    PayButton("Pay KES $price with M-Pesa", Mali.Teal) { pay() }
                    Text("You will get a prompt on your phone. Enter your M-Pesa PIN to approve it.", fontSize = 13.sp, lineHeight = 19.sp, color = Mali.InkSoft, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun failureText(p: PaymentInfo): String = when (p.status) {
    "cancelled" -> "You cancelled the payment on your phone. Nothing was charged."
    "expired" -> "The payment prompt timed out. Nothing was charged. You can try again."
    "review" -> "We received a payment that does not match the price. Please contact support with your M-Pesa message."
    else -> p.detail.ifBlank { "The payment did not go through. Nothing was charged." }
}

@Composable
private fun OptionCard(title: String, price: String, text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Mali.Teal.copy(alpha = 0.10f) else Mali.Cream)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier.size(22.dp).clip(CircleShape).background(if (selected) Mali.Teal else Mali.Sand),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink)
            Text(text, fontSize = 13.sp, lineHeight = 19.sp, color = Mali.InkSoft)
        }
        Text(price, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Mali.TealDeep)
    }
}

@Composable
private fun PayButton(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text,
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(color)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    )
}
