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
import androidx.compose.ui.graphics.graphicsLayer
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

/** The back arrow: no frame, just the icon. [tint] is white on the teal bars. */
@Composable
fun BackCircle(onBack: () -> Unit, circleAlpha: Float = 0.9f, tint: Color = Mali.Ink) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onBack)
            .semantics { contentDescription = "Back" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
    }
}

/** 0 while the page is at the top, rising to 1 as the content scrolls up under the phone's status bar. */
@Composable
fun statusScrimAlpha(state: LazyListState): Float {
    val statusPx = WindowInsets.statusBars.getTop(androidx.compose.ui.platform.LocalDensity.current).coerceAtLeast(1)
    val a by remember(state, statusPx) {
        derivedStateOf {
            if (state.firstVisibleItemIndex > 0) 1f
            else (state.firstVisibleItemScrollOffset.toFloat() / statusPx).coerceIn(0f, 1f)
        }
    }
    return a
}

/** A solid band behind the phone's status bar that fades in once content scrolls up under it, so text never runs through the clock. */
@Composable
fun StatusScrim(state: LazyListState, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val a = statusScrimAlpha(state)
    if (a <= 0f) return
    Box(
        modifier
            .fillMaxWidth()
            .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .alpha(a)
            .background(color),
    )
}

/** Back arrow at the top-left of a detail page. It lives with the content: it scrolls away as you read. */
@Composable
fun BackFab(onBack: () -> Unit, state: LazyListState, modifier: Modifier = Modifier) {
    val atTop by remember(state) { derivedStateOf { state.firstVisibleItemIndex == 0 } }
    if (!atTop) return
    Box(
        modifier
            .statusBarsPadding()
            .padding(start = 12.dp, top = 8.dp)
            .graphicsLayer { translationY = -state.firstVisibleItemScrollOffset.toFloat() },
    ) { BackCircle(onBack) }
}
