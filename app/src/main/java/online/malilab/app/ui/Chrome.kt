package online.malilab.app.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import online.malilab.app.ui.theme.Mali

/** Dark or light clock/battery icons in the phone's status bar, to stay readable on what is behind them. */
@Composable
fun StatusBarIcons(darkIcons: Boolean, active: Boolean = true) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window
        if (active && window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = darkIcons && !Mali.dark
            controller.isAppearanceLightNavigationBars = !Mali.dark
        }
    }
}

/**
 * How far the FIRST item of the list (the cover or header) has scrolled away, as 0..1 of its own height, mapped
 * so that [from] gives 0 and [to] gives 1. Once the first item is gone it stays at 1.
 */
@Composable
fun firstItemProgress(state: LazyListState, from: Float, to: Float): Float {
    val progress by remember(state, from, to) {
        derivedStateOf {
            val first = state.layoutInfo.visibleItemsInfo.firstOrNull()
            if (first == null || first.index > 0 || first.size <= 0) {
                if (first != null && first.index > 0) 1f else 0f
            } else {
                ((-first.offset).toFloat() / first.size - from).div(to - from).coerceIn(0f, 1f)
            }
        }
    }
    return progress
}

@Composable
fun BackCircle(onBack: () -> Unit, circleAlpha: Float = 0.9f) {
    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Mali.Surface.copy(alpha = circleAlpha))
            .clickable(onClick = onBack)
            .semantics { contentDescription = "Back" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Mali.Ink)
    }
}

/** The one fixed control on a detail page: a round back arrow in the top-left corner. */
@Composable
fun BackFab(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.statusBarsPadding().padding(start = 12.dp, top = 8.dp)) { BackCircle(onBack) }
}
