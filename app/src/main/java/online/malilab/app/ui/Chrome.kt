package online.malilab.app.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons
        }
    }
}

/** 0 at the top of the list, 1 once it has scrolled past [endPx]. Used to fade a top bar in. */
@Composable
fun scrollProgress(state: LazyListState, startPx: Float, endPx: Float): Float {
    val progress by remember(state, startPx, endPx) {
        derivedStateOf {
            if (state.firstVisibleItemIndex > 0) 1f
            else ((state.firstVisibleItemScrollOffset - startPx) / (endPx - startPx)).coerceIn(0f, 1f)
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
            .background(Color.White.copy(alpha = circleAlpha))
            .clickable(onClick = onBack)
            .semantics { contentDescription = "Back" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Mali.Ink)
    }
}

/**
 * The bar over the top of a scrolling page. Transparent at first, so a cover picture can run all the way up
 * behind the status bar; it fills in and shows the title as the page scrolls.
 */
@Composable
fun FadingBar(
    progress: Float,
    background: Color,
    title: String,
    titleColor: Color,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth().background(background.copy(alpha = progress))) {
        Row(
            Modifier
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) BackCircle(onBack, circleAlpha = 0.9f * (1f - progress))
            Text(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = titleColor,
                modifier = Modifier
                    .padding(start = if (onBack != null) 12.dp else 10.dp)
                    .alpha(progress),
            )
        }
    }
}
