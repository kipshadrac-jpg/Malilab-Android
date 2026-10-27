package online.malilab.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
fun ScreenHeader(title: String, subtitle: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Mali.TealTop)
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val back = LocalHeaderBack.current
            val profile = LocalOpenProfile.current
            when {
                back != null -> Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) { BackCircle(back, tint = Color.White) }
                profile != null -> ProfileButton(profile)
                else -> Box(
                    Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(Mali.Surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(painterResource(R.drawable.logo_mark), contentDescription = "MaliLab", modifier = Modifier.size(44.dp))
                }
            }
            Column {
                Text(title, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(subtitle, fontSize = 14.sp, color = Mali.TealDeep)
            }
        }
    }
}

/** Set by the main screen so every page header shows the round profile button at its top-left. */
val LocalOpenProfile = staticCompositionLocalOf<(() -> Unit)?> { null }

/** Set while the profile page is open, so its header shows a back arrow where the profile button was. */
val LocalHeaderBack = staticCompositionLocalOf<(() -> Unit)?> { null }

/** The round profile icon in the top-left corner of the main pages. It opens the Me page. */
@Composable
fun ProfileButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(Mali.Surface)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Profile" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Person, contentDescription = null, tint = Mali.TealDeep, modifier = Modifier.size(30.dp))
    }
}
