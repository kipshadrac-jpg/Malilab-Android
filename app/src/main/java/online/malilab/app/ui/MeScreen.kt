package online.malilab.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import online.malilab.app.data.Auth
import online.malilab.app.data.Billing
import online.malilab.app.data.NoteStore
import online.malilab.app.data.PortfolioStore
import online.malilab.app.data.Reminders
import online.malilab.app.ui.theme.Mali

/** Profile & Settings: the person's account, what they own, and a few settings, as white cards on a soft background. */
@Composable
fun MeScreen(active: Boolean, onOpenPractice: () -> Unit = {}, onOpenPaywall: () -> Unit = {}, onOpenPurchases: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val back = LocalHeaderBack.current
    var busy by remember { mutableStateOf(false) }
    var showLook by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    StatusBarIcons(darkIcons = true, active = active)

    Column(
        Modifier
            .fillMaxSize()
            .background(Mali.Cream)
            .background(Brush.verticalGradient(listOf(Mali.TealTint.copy(alpha = 0.55f), Color.Transparent, Mali.Amber.copy(alpha = 0.06f)))),
    ) {
        // top bar: outline back button on the left, the title in the middle
        Box(Modifier.fillMaxWidth().background(Mali.Surface).statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp)) {
            if (back != null) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(1.dp, Mali.Sand, CircleShape)
                        .clickable(onClick = back)
                        .semantics { contentDescription = "Back" }
                        .align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Mali.Ink, modifier = Modifier.size(22.dp)) }
            }
            Text("Profile & Settings", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Mali.Ink, modifier = Modifier.align(Alignment.Center))
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            // who is signed in
            WhiteCard {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(58.dp).clip(CircleShape).background(Mali.TealTint), contentAlignment = Alignment.Center) {
                        Icon(NavIcons.Person, null, tint = Mali.Teal, modifier = Modifier.size(34.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(Auth.email.ifBlank { "Your account" }, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Mali.Ink)
                        Text(planLabel(), fontSize = 13.sp, color = Mali.InkSoft)
                    }
                }
            }

            Heading("Profile")
            WhiteCard {
                SettingRow(NavIcons.Book, "Modules and billing", planLabel()) { if (Billing.status.paywall && !Billing.status.allModules) onOpenPaywall() else onOpenPurchases() }
                SettingRow(NavIcons.Receipt, "My purchases", "M-Pesa payments and receipts", onOpenPurchases)
                SettingRow(NavIcons.CheckCircle, "My practice", "Your answers and unfinished activities", onOpenPractice)
            }

            Heading("Settings")
            WhiteCard {
                SettingRow(NavIcons.Bell, "Payment reminders", if (Reminders.canNotify(context)) "On" else "Off. Tap to allow") {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
                SettingRow(NavIcons.Sliders, "Appearance", if (Mali.dark) "Dark" else "Light") { showLook = !showLook }
                if (showLook) {
                    Row(Modifier.padding(start = 62.dp, end = 16.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionChip("Light", selected = !Mali.dark) { Mali.choose(false) }
                        ActionChip("Dark", selected = Mali.dark) { Mali.choose(true) }
                    }
                }
            }

            Heading("About MaliLab")
            WhiteCard {
                SettingRow(NavIcons.Info, "About MaliLab", "Version " + versionOf(context)) { showAbout = !showAbout }
                if (showAbout) {
                    Text(
                        "MaliLab helps you learn how money works in Kenya and keep track of your savings and investments in one place. Values shown are estimates from the rates you enter.",
                        fontSize = 14.sp, lineHeight = 21.sp, color = Mali.InkSoft,
                        modifier = Modifier.padding(start = 62.dp, end = 16.dp, bottom = 14.dp),
                    )
                }
                SettingRow(NavIcons.Compass, "Visit malilab.online", "Read more on the website") {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://malilab.online")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }

            WhiteCard {
                SettingRow(NavIcons.Logout, if (busy) "Signing out…" else "Sign out", null, tint = Mali.Pink, chevron = false) {
                    if (!busy) scope.launch {
                        busy = true
                        Auth.signOut()
                        NoteStore.reset()
                        PortfolioStore.reset()
                        Billing.reset()
                        busy = false
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WhiteCard(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Mali.Surface)) { content() }
}

@Composable
private fun Heading(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Mali.Ink, modifier = Modifier.padding(start = 2.dp, top = 6.dp))
}

/** One tappable line in a card: outline icon, label (with a small second line), and a chevron. */
@Composable
private fun SettingRow(icon: ImageVector, label: String, sub: String?, tint: Color = Mali.TealDeep, chevron: Boolean = true, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null, tint = if (tint == Mali.TealDeep) Mali.Ink else tint, modifier = Modifier.size(28.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = tint)
            if (sub != null) Text(sub, fontSize = 12.sp, lineHeight = 16.sp, color = Mali.InkSoft)
        }
        if (chevron) Icon(NavIcons.Chevron, null, tint = Mali.Ink, modifier = Modifier.size(22.dp))
    }
}

private fun planLabel(): String {
    val st = Billing.status
    return when {
        !st.paywall -> "Everything is open while MaliLab is being tested"
        st.allModules -> "You have every module"
        st.trialActive -> "Free plan"
        else -> "Free plan with ads"
    }
}

private fun versionOf(context: Context): String =
    try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    } catch (e: Exception) {
        ""
    }
