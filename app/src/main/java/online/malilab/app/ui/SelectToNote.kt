package online.malilab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider

/**
 * Lets the person select text while reading (long press). The little menu above the selection offers Insert, which adds the
 * selected words to their notes for this page, and Copy.
 */
@Composable
fun SelectToNote(onInsert: (String) -> Unit, content: @Composable () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var anchor by remember { mutableStateOf<Rect?>(null) }
    var copyNow by remember { mutableStateOf<(() -> Unit)?>(null) }
    val toolbar = remember {
        object : TextToolbar {
            override val status: TextToolbarStatus get() = if (anchor != null) TextToolbarStatus.Shown else TextToolbarStatus.Hidden
            override fun hide() { anchor = null; copyNow = null }
            override fun showMenu(
                rect: Rect,
                onCopyRequested: (() -> Unit)?,
                onPasteRequested: (() -> Unit)?,
                onCutRequested: (() -> Unit)?,
                onSelectAllRequested: (() -> Unit)?,
            ) {
                anchor = rect
                copyNow = onCopyRequested
            }
        }
    }
    CompositionLocalProvider(LocalTextToolbar provides toolbar) {
        SelectionContainer { content() }
    }
    val rect = anchor
    val copy = copyNow
    if (rect != null && copy != null) {
        Popup(popupPositionProvider = MenuAbove(rect)) {
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Color(0xFF1F2A30)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MenuWord("Insert") {
                    copy()
                    val text = clipboard.getText()?.text.orEmpty().trim()
                    toolbar.hide()
                    if (text.isNotEmpty()) onInsert(text)
                }
                Box(Modifier.width(1.dp).height(18.dp).background(Color.White.copy(alpha = 0.25f)))
                MenuWord("Copy") { copy(); toolbar.hide() }
            }
        }
    }
}

@Composable
private fun MenuWord(label: String, onClick: () -> Unit) {
    Text(
        label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 11.dp),
    )
}

/** Puts the menu just above the selected words, or just below them when there is no room above. */
private class MenuAbove(private val rect: Rect) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val gap = 12
        val x = (rect.center.x.toInt() - popupContentSize.width / 2).coerceIn(8, (windowSize.width - popupContentSize.width - 8).coerceAtLeast(8))
        val above = rect.top.toInt() - popupContentSize.height - gap
        val y = if (above >= 60) above else rect.bottom.toInt() + gap + 40
        return IntOffset(x, y)
    }
}
