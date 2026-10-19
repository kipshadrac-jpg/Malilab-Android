package online.malilab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import online.malilab.app.data.NoteStore
import online.malilab.app.ui.theme.Mali

@Composable
fun MeScreen(active: Boolean) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    StatusBarIcons(darkIcons = false, active = active)

    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState())) {
        ScreenHeader("Me", "Your MaliLab account.")
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            InfoCard("SIGNED IN AS", Auth.email.ifBlank { "Your account" }, Mali.Teal)
            InfoCard(
                "SUBSCRIPTION",
                "Free for now. Paid plans with extra features will be offered here later.",
                Mali.Indigo,
            )
            InfoCard(
                "MY PLAN",
                "Your notes are saved to your account, so they follow you to every phone. Find them in the Plan tab.",
                Mali.Amber,
            )
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
                            busy = false
                        }
                    }
                    .padding(vertical = 15.dp),
            )
        }
    }
}

@Composable
private fun InfoCard(label: String, text: String, color: Color) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.07f))
            .padding(16.dp),
    ) {
        Text(label, fontSize = 12.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Spacer(Modifier.height(6.dp))
        Text(text, fontSize = 16.sp, lineHeight = 23.sp, color = Mali.Ink)
    }
}
