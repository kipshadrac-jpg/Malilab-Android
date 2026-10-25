package online.malilab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import online.malilab.app.data.Auth
import online.malilab.app.data.Billing
import online.malilab.app.data.NoteStore
import online.malilab.app.ui.theme.Mali

@Composable
fun MeScreen(active: Boolean, onOpenPractice: () -> Unit = {}, onOpenPaywall: () -> Unit = {}, onOpenPurchases: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    StatusBarIcons(darkIcons = false, active = active)

    Column(Modifier.fillMaxSize().background(Mali.Surface).verticalScroll(rememberScrollState())) {
        ScreenHeader("Me", "Your MaliLab account.")
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            InfoCard("SIGNED IN AS", Auth.email.ifBlank { "Your account" }, Mali.Teal)
            InfoCard(
                "MODULES",
                billingSummary(),
                Mali.Indigo,
                onClick = if (Billing.status.paywall && !Billing.status.allModules) onOpenPaywall else null,
            )
            InfoCard(
                "MY PURCHASES",
                "Your M-Pesa payments and receipts, and the modules that are yours for good. Tap to open.",
                Mali.Teal,
                onClick = onOpenPurchases,
            )
            InfoCard(
                "MY PLAN",
                "Your notes are saved to your account, so they follow you to every phone. Find them in the Plan tab.",
                Mali.Amber,
            )
            InfoCard(
                "MY PRACTICE",
                "See the answers you gave and any activity you have not finished. Tap to open.",
                Mali.Green,
                onClick = onOpenPractice,
            )
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Mali.Purple.copy(alpha = 0.07f)).padding(16.dp),
            ) {
                Text("APPEARANCE", fontSize = 12.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.ExtraBold, color = Mali.Purple)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Choose how MaliLab looks. It stays as you set it, whatever your phone is set to.",
                    fontSize = 16.sp, lineHeight = 23.sp, color = Mali.Ink,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionChip("Light", selected = !Mali.dark) { Mali.setDark(false) }
                    ActionChip("Dark", selected = Mali.dark) { Mali.setDark(true) }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (busy) "Signing out…" else "Sign out",
                color = Mali.Pink,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(Mali.Pink.copy(alpha = 0.10f))
                    .clickable(enabled = !busy) {
                        scope.launch {
                            busy = true
                            Auth.signOut()
                            NoteStore.reset()
                            Billing.reset()
                            busy = false
                        }
                    }
                    .padding(vertical = 15.dp),
            )
        }
    }
}

@Composable
private fun InfoCard(label: String, text: String, color: Color, onClick: (() -> Unit)? = null) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.07f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) {
        Text(label, fontSize = 12.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Spacer(Modifier.height(6.dp))
        Text(text, fontSize = 16.sp, lineHeight = 23.sp, color = Mali.Ink)
    }
}
