package online.malilab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import online.malilab.app.data.ApiError
import online.malilab.app.data.Auth
import online.malilab.app.data.Billing
import online.malilab.app.data.NoteStore
import online.malilab.app.data.PaymentInfo
import online.malilab.app.data.PortfolioStore
import online.malilab.app.ui.theme.Mali

private val MpesaGreen = Color(0xFF3FAE2A)
private val SuccessGreen = Color(0xFF16C45A)

/** The small payment box. Opens over whatever the learner is doing. [moduleNotice] adds the "modules are for Premium" line. */
@Composable
fun PremiumDialog(moduleNotice: Boolean, onClose: () -> Unit) {
    var busy by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = { if (!busy) onClose() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 22.dp).imePadding()) {
            PremiumCard(moduleNotice = moduleNotice, onBusy = { busy = it }, onFinish = onClose, onClose = { if (!busy) onClose() })
        }
    }
}

/** Shown over the whole app once the free and bonus months are over, until Premium is paid. */
@Composable
fun LockedGate() {
    var paid by remember { mutableStateOf(false) }
    if (!Billing.status.locked && !paid) return
    val scope = rememberCoroutineScope()
    Box(
        Modifier.fillMaxSize().background(Mali.Cream).pointerInput(Unit) { detectTapGestures { } }.imePadding().padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PremiumCard(moduleNotice = false, locked = true, onPaid = { paid = true }, onFinish = { paid = false }, onClose = null)
            if (!paid) {
                Spacer(Modifier.height(18.dp))
                Text(
                    "Sign out",
                    fontSize = 14.sp, color = Mali.InkSoft, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable {
                        scope.launch {
                            Auth.signOut()
                            NoteStore.reset()
                            PortfolioStore.reset()
                            Billing.reset()
                        }
                    }.padding(horizontal = 18.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/** The congratulations on entering the bonus month. */
@Composable
fun BonusDialog(onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp).clip(RoundedCornerShape(24.dp)).background(Color.White).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Congratulations.", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink, textAlign = TextAlign.Center)
            Text("We have awarded you another month.", fontSize = 17.sp, lineHeight = 24.sp, color = Mali.Ink, textAlign = TextAlign.Center)
            Text(
                "Everything stays open, except modules, which are for Premium members.",
                fontSize = 14.sp, lineHeight = 20.sp, color = Mali.InkSoft, textAlign = TextAlign.Center,
            )
            BoxButton("Continue", Mali.Teal, onClose)
        }
    }
}

@Composable
private fun PremiumCard(
    moduleNotice: Boolean,
    locked: Boolean = false,
    onBusy: (Boolean) -> Unit = {},
    onPaid: () -> Unit = {},
    onFinish: () -> Unit,
    onClose: (() -> Unit)?,
) {
    val st = Billing.status
    val scope = rememberCoroutineScope()
    var phone by remember { mutableStateOf(Billing.lastPhone()) }
    var phase by remember { mutableStateOf("form") }          // form, sending, waiting, stopped, paid
    var message by remember { mutableStateOf<String?>(null) }
    var paymentId by remember { mutableIntStateOf(0) }
    var pollTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(phase) { onBusy(phase == "sending" || phase == "waiting") }

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
                        onPaid()
                        delay(2600)
                        onFinish()
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
                val p = Billing.startPayment("all", null, phone)
                Billing.rememberPhone(phone)
                paymentId = p.id
            } catch (e: ApiError) {
                message = e.message
                phase = "form"
            }
        }
    }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White).padding(horizontal = 22.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (phase == "paid") {
            Box(Modifier.size(104.dp).clip(CircleShape).background(SuccessGreen), contentAlignment = Alignment.Center) {
                Text("✓", color = Color.White, fontSize = 62.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text("Successful", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = SuccessGreen)
            Text("Premium is yours, for good.", fontSize = 15.sp, color = Mali.InkSoft, textAlign = TextAlign.Center)
            return@Column
        }

        MpesaBadge()
        if (locked) {
            Text("Your free months have ended.", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink, textAlign = TextAlign.Center)
        } else if (moduleNotice) {
            Text("Modules are for Premium members. Unlock Premium to open them.", fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Ink, textAlign = TextAlign.Center)
        }
        Text(
            "Pay once, keep it for good. There is no subscription and nothing is charged again.",
            fontSize = 14.sp, lineHeight = 20.sp, color = Mali.Ink, textAlign = TextAlign.Center,
        )
        Text("KES ${st.priceAll}", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Mali.TealDeep)

        when (phase) {
            "sending", "waiting", "stopped" -> {
                if (phase == "sending") {
                    CircularProgressIndicator(color = MpesaGreen, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    Text("Sending the payment prompt…", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Mali.Ink, textAlign = TextAlign.Center)
                } else if (phase == "waiting") {
                    CircularProgressIndicator(color = MpesaGreen, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    Text("The STK prompt has been sent to your phone. Enter your M-Pesa PIN.", fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold, color = Mali.Ink, textAlign = TextAlign.Center)
                } else {
                    Text("Still waiting. If you approved the payment, tap Check again.", fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold, color = Mali.Ink, textAlign = TextAlign.Center)
                    BoxButton("Check again", MpesaGreen) { pollTick++ }
                    if (onClose != null) BoxButton("Close", Mali.InkSoft, onClose)
                }
                if (st.sandbox) Text("Test mode: no real money is taken.", fontSize = 12.sp, color = Mali.Amber, fontWeight = FontWeight.Bold)
            }
            else -> {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' || c == ' ' }.take(16) },
                    label = { Text("Enter your M-Pesa number") },
                    placeholder = { Text("0712 345 678") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors(),
                )
                val m = message
                if (m != null) {
                    Text(
                        m, fontSize = 13.sp, lineHeight = 18.sp, color = Mali.Pink, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Mali.Pink.copy(alpha = 0.08f)).padding(10.dp),
                    )
                }
                if (st.sandbox) Text("Test mode: no real money is taken.", fontSize = 12.sp, color = Mali.Amber, fontWeight = FontWeight.Bold)
                BoxButton("Pay KES ${st.priceAll}", MpesaGreen) { pay() }
                if (onClose != null) {
                    Text(
                        "Not now",
                        fontSize = 14.sp, color = Mali.InkSoft, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClose).padding(horizontal = 18.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

/** A drawn M-PESA mark, because the official logo is not bundled. */
@Composable
private fun MpesaBadge() {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).background(MpesaGreen).padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("M-PESA", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
    }
}

@Composable
private fun BoxButton(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text,
        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(color).clickable(onClick = onClick).padding(vertical = 13.dp),
    )
}

private fun failureText(p: PaymentInfo): String = when (p.status) {
    "cancelled" -> "You cancelled the payment on your phone. Nothing was charged."
    "expired" -> "The payment prompt timed out. Nothing was charged. You can try again."
    "review" -> "We received a payment that does not match the price. Please contact support with your M-Pesa message."
    else -> p.detail.ifBlank { "The payment did not go through. Nothing was charged." }
}
