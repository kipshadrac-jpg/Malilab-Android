package online.malilab.app.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** The light palette exactly as first designed. Printed notes (PDF) always use this, whatever the app looks like. */
object MaliLight {
    val Teal = Color(0xFF0F9688)
    val TealDeep = Color(0xFF0A4A43)
    val TealTint = Color(0xFFD9F0EC)
    val Sand = Color(0xFFE7DDD4)
    val Ink = Color(0xFF3A2F2B)
    val InkSoft = Color(0xFF6B5B55)
    val Indigo = Color(0xFF3B4CC0)
    val Pink = Color(0xFFB0285A)
    val Amber = Color(0xFF8A5200)
    val Purple = Color(0xFF7A3DB0)
    val Green = Color(0xFF1F7A4D)
}

/**
 * The MaliLab palette. Every colour answers to the Light/Dark choice the learner makes in Me, so changing it
 * repaints the whole app at once. The app starts Light and never follows the phone's own setting.
 */
object Mali {
    private lateinit var prefs: SharedPreferences

    /** True while the dark look is on. Read inside a screen, it makes that screen repaint when the choice changes. */
    var dark by mutableStateOf(false)
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences("malilab_theme", Context.MODE_PRIVATE)
        dark = prefs.getString("mode", "light") == "dark"
    }

    fun choose(on: Boolean) {
        dark = on
        if (::prefs.isInitialized) prefs.edit().putString("mode", if (on) "dark" else "light").apply()
    }

    private fun pick(light: Color, night: Color): Color = if (dark) night else light

    val TealTop get() = pick(Color(0xFF219D92), Color(0xFF14736A))
    val TealBottom get() = pick(Color(0xFF6DBCB1), Color(0xFF2E8A7F))
    val Teal = Color(0xFF0F9688)
    val TealLight = Color(0xFF2AA094)
    /** Text and icons that sit on a pale teal tint. Light teal in the dark look. */
    val TealDeep get() = pick(Color(0xFF0A4A43), Color(0xFF8FDDD2))
    /** The deep teal itself, for buttons and pills that carry white text in both looks. */
    val TealFixed = Color(0xFF0A4A43)
    val TealTint get() = pick(Color(0xFFD9F0EC), Color(0xFF1C3835))
    /** The page behind everything. */
    val Cream get() = pick(Color(0xFFFFFBF5), Color(0xFF11181D))
    /** Cards, sheets and bars that sit on the page. White in the light look. */
    val Surface get() = pick(Color.White, Color(0xFF1A2329))
    /** A softly coloured band, such as a tip or note. */
    val Warm get() = pick(Color(0xFFFFF8E8), Color(0xFF2A2518))
    val Sand get() = pick(Color(0xFFE7DDD4), Color(0xFF36464A))
    val Ink get() = pick(Color(0xFF3A2F2B), Color(0xFFEAE4DE))
    val InkSoft get() = pick(Color(0xFF6B5B55), Color(0xFFAFA59E))
    /** Text on a solid block of deep teal or an accent that is bright in the dark look. */
    val OnSolid get() = pick(Color.White, Color(0xFF0B1418))

    val Indigo get() = pick(Color(0xFF3B4CC0), Color(0xFF8C99F5))
    val Pink get() = pick(Color(0xFFB0285A), Color(0xFFF0709C))
    val Amber get() = pick(Color(0xFF8A5200), Color(0xFFEBA73F))
    val Purple get() = pick(Color(0xFF7A3DB0), Color(0xFFBD8AEB))
    val Green get() = pick(Color(0xFF1F7A4D), Color(0xFF4CC08A))
    val Night = Color(0xFF16202B)
    val accents get() = listOf(Teal, Indigo, Pink, Amber, Purple, Green)

    /** The same item always gets the same accent colour, on every screen. */
    fun accentFor(key: String): Color = accents[(key.hashCode() and Int.MAX_VALUE) % accents.size]
}

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
    val scheme = if (Mali.dark) {
        darkColorScheme(
            primary = Mali.Teal, onPrimary = Color.White, primaryContainer = Mali.TealTint, onPrimaryContainer = Mali.TealDeep,
            secondary = Mali.Indigo, tertiary = Mali.Pink,
            background = Mali.Cream, onBackground = Mali.Ink, surface = Mali.Cream, onSurface = Mali.Ink,
            surfaceVariant = Mali.TealTint, onSurfaceVariant = Mali.InkSoft, outline = Mali.Sand,
            surfaceContainer = Mali.Surface, surfaceContainerHigh = Mali.Surface, surfaceContainerHighest = Mali.Surface,
        )
    } else {
        lightColorScheme(
            primary = Mali.Teal, onPrimary = Color.White, primaryContainer = Mali.TealTint, onPrimaryContainer = Mali.TealDeep,
            secondary = Mali.Indigo, tertiary = Mali.Pink,
            background = Mali.Cream, onBackground = Mali.Ink, surface = Mali.Cream, onSurface = Mali.Ink,
            surfaceVariant = Mali.TealTint, onSurfaceVariant = Mali.InkSoft, outline = Mali.Sand,
        )
    }
    MaterialTheme(colorScheme = scheme, typography = MaliTypography) {
        // Gives every plain Text a readable colour in either look.
        Surface(modifier = Modifier.fillMaxSize(), color = Mali.Cream, contentColor = Mali.Ink, content = content)
    }
}
