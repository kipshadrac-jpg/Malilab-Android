package online.malilab.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.malilab.app.R
import online.malilab.app.ui.theme.Mali

/** Top of the Home and Learn pages: one flat teal block, identical to the status bar, with a straight bottom edge. */
@Composable
fun ScreenHeader(title: String, subtitle: String, showProfile: Boolean = true, trailing: (@Composable () -> Unit)? = null) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Mali.TealTop)
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 20.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val back = LocalHeaderBack.current
            val profile = LocalOpenProfile.current
            when {
                back != null -> Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) { BackCircle(back, tint = Color.White) }
                !showProfile -> Unit
                profile != null -> ProfileButton(profile)
                else -> Box(
                    Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(Mali.Surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(painterResource(R.drawable.logo_wallet), contentDescription = "MaliLab", modifier = Modifier.size(44.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(subtitle, fontSize = 14.sp, color = Mali.TealDeep)
            }
            trailing?.invoke()
        }
    }
}

/** Set by the main screen so every page header shows the round profile button at its top-left. */
val LocalOpenProfile = staticCompositionLocalOf<(() -> Unit)?> { null }

/** Set while the profile page is open, so its header shows a back arrow where the profile button was. */
val LocalHeaderBack = staticCompositionLocalOf<(() -> Unit)?> { null }

/** The small round profile icon: white disc, thin teal ring, outline person. It opens Profile & Settings. */
@Composable
fun ProfileButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(2.dp, Mali.TealLight, CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Profile and settings" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(NavIcons.Person, contentDescription = null, tint = Mali.TealFixed, modifier = Modifier.size(24.dp))
    }
}

/** Set by the main screen so the portfolio header can open the notifications list. */
val LocalOpenNotifications = staticCompositionLocalOf<(() -> Unit)?> { null }

/** The bell: an outline icon with a small red dot when there is something to read. */
@Composable
fun BellButton(unread: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = if (unread) "Notifications, new" else "Notifications" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(NavIcons.Bell, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        if (unread) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 9.dp).size(10.dp)
                    .clip(CircleShape).background(Color(0xFFFF5A4D)).border(1.5.dp, Mali.OceanTop, CircleShape),
            )
        }
    }
}

/**
 * The introduction at the top of Learn, Discover and Playbooks: a teal card with one line of words,
 * and a white square tile beside it holding the page's icon. It scrolls away with the page.
 */
@Composable
fun IntroCard(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(78.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Mali.TealTop, Mali.Teal))),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(text, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.padding(horizontal = 20.dp))
        }
        Box(
            Modifier
                .size(78.dp)
                .shadow(6.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(Mali.Surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Mali.TealTop, modifier = Modifier.size(38.dp))
        }
    }
}
