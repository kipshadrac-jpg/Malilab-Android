package online.malilab.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** The MaliLab palette: the teal-on-cream look, plus the five accent colours used for cards on the website. */
object Mali {
    val TealTop = Color(0xFF219D92)
    val TealBottom = Color(0xFF6DBCB1)
    val Teal = Color(0xFF0F9688)
    val TealLight = Color(0xFF2AA094)
    val TealDeep = Color(0xFF0A4A43)
    val TealTint = Color(0xFFD9F0EC)
    val Cream = Color(0xFFFFFBF5)
    val Sand = Color(0xFFE7DDD4)
    val Ink = Color(0xFF3A2F2B)
    val InkSoft = Color(0xFF6B5B55)

    val Indigo = Color(0xFF3B4CC0)
    val Pink = Color(0xFFB0285A)
    val Amber = Color(0xFF8A5200)
    val Purple = Color(0xFF7A3DB0)
    val Green = Color(0xFF1F7A4D)
    val Night = Color(0xFF16202B)
    val accents = listOf(Teal, Indigo, Pink, Amber, Purple, Green)
}

private val Scheme = lightColorScheme(
    primary = Mali.Teal,
    onPrimary = Color.White,
    primaryContainer = Mali.TealTint,
    onPrimaryContainer = Mali.TealDeep,
    secondary = Mali.Indigo,
    tertiary = Mali.Pink,
    background = Mali.Cream,
    onBackground = Mali.Ink,
    surface = Mali.Cream,
    onSurface = Mali.Ink,
    surfaceVariant = Mali.TealTint,
    onSurfaceVariant = Mali.InkSoft,
    outline = Mali.Sand,
)

private val MaliTypography = Typography(
    headlineLarge = TextStyle(fontSize = 40.sp, lineHeight = 46.sp, fontWeight = FontWeight.ExtraBold),
    headlineMedium = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun MaliLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = MaliTypography, content = content)
}
