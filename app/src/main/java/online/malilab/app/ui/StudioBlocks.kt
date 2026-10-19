package online.malilab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.malilab.app.data.Block
import online.malilab.app.ui.theme.Mali

/** Puts a bullet or a number in front of every line of a list, without changing what is stored. */
private class ListMarkers(private val ordered: Boolean) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val lines = text.text.split("\n")
        val markers = lines.indices.map { if (ordered) "${it + 1}. " else "• " }
        val starts = IntArray(lines.size)
        val tStarts = IntArray(lines.size)
        val sb = StringBuilder()
        var o = 0
        lines.forEachIndexed { i, line ->
            starts[i] = o
            tStarts[i] = sb.length
            sb.append(markers[i]).append(line)
            if (i < lines.lastIndex) sb.append('\n')
            o += line.length + 1
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var i = lines.lastIndex
                while (i > 0 && starts[i] > offset) i--
                return tStarts[i] + markers[i].length + (offset - starts[i])
            }

            override fun transformedToOriginal(offset: Int): Int {
                var i = lines.lastIndex
                while (i > 0 && tStarts[i] > offset) i--
                val inLine = offset - tStarts[i] - markers[i].length
                return starts[i] + inLine.coerceIn(0, lines[i].length)
            }
        }
        return TransformedText(AnnotatedString(sb.toString()), mapping)
    }
}

fun toneColor(tone: String): Color = when (tone) {
    "tip" -> Mali.Teal
    "warning" -> Mali.Amber
    "key" -> Mali.Indigo
    "example" -> Mali.Purple
    else -> Mali.InkSoft
}

fun toneLabel(tone: String): String = when (tone) {
    "tip" -> "TIP"
    "warning" -> "WATCH OUT"
    "key" -> "KEY IDEA"
    "example" -> "EXAMPLE"
    else -> "NOTE"
}

val TONES = listOf("tip", "warning", "key", "example", "note")

/** A text field that can be asked to take the cursor (after a block is added). */
@Composable
fun StudioField(
    value: String,
    onValue: (String) -> Unit,
    style: TextStyle,
    placeholder: String,
    blockId: String,
    pendingFocus: String?,
    onFocusConsumed: () -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
    visual: VisualTransformation = VisualTransformation.None,
) {
    val requester = remember { FocusRequester() }
    LaunchedEffect(pendingFocus) {
        if (pendingFocus == blockId) {
            withFrameNanos { }
            try {
                requester.requestFocus()
            } catch (e: IllegalStateException) {
                // The field was not on screen yet; the person can tap it.
            }
            onFocusConsumed()
        }
    }
    Box(modifier) {
        if (value.isEmpty() && placeholder.isNotEmpty()) {
            Text(placeholder, style = style.copy(color = Mali.InkSoft.copy(alpha = 0.55f)))
        }
        BasicTextField(
            value = value,
            onValueChange = onValue,
            textStyle = style,
            cursorBrush = SolidColor(Mali.Teal),
            visualTransformation = visual,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(requester)
                .onFocusChanged { if (it.isFocused) onFocus() },
        )
    }
}

/** One block of the note, editable in place. Evidence blocks from the calculator are drawn by [EvidenceBlock]. */
@Composable
fun NoteBlockView(
    block: Block,
    focused: Boolean,
    pendingFocus: String?,
    onFocusConsumed: () -> Unit,
    onFocus: () -> Unit,
    onEdit: (Block) -> Unit,
    onRestyle: (Block) -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    evidence: @Composable (Block, Boolean, (Block) -> Unit) -> Unit,
) {
    val body = TextStyle(fontSize = 17.sp, lineHeight = 27.sp, color = Mali.Ink)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp),
    ) {
        when (block.type) {
            "paragraph" -> StudioField(
                block.text, { onEdit(block.copy(text = it)) }, body,
                if (focused) "Write here…" else "", block.id, pendingFocus, onFocusConsumed, onFocus,
            )
            "heading" -> {
                val size = when (block.level) { 1 -> 28.sp; 3 -> 19.sp; else -> 23.sp }
                StudioField(
                    block.text, { onEdit(block.copy(text = it)) },
                    TextStyle(fontSize = size, lineHeight = size * 1.25f, fontWeight = FontWeight.ExtraBold, color = Mali.Ink),
                    "Heading", block.id, pendingFocus, onFocusConsumed, onFocus,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            "list" -> StudioField(
                block.items.joinToString("\n"), { onEdit(block.copy(items = it.split("\n"))) }, body,
                "", block.id, pendingFocus, onFocusConsumed, onFocus,
                visual = ListMarkers(block.ordered),
            )
            "quote" -> Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Box(Modifier.width(4.dp).fillMaxHeight().background(Mali.Sand))
                Spacer(Modifier.width(12.dp))
                StudioField(
                    block.text, { onEdit(block.copy(text = it)) },
                    body.copy(fontStyle = FontStyle.Italic, color = Mali.InkSoft),
                    "Quote…", block.id, pendingFocus, onFocusConsumed, onFocus,
                )
            }
            "callout" -> {
                val c = toneColor(block.tone)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.copy(alpha = 0.09f))
                        .padding(14.dp),
                ) {
                    Text(toneLabel(block.tone), fontSize = 12.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.ExtraBold, color = c)
                    Spacer(Modifier.height(4.dp))
                    StudioField(
                        block.text, { onEdit(block.copy(text = it)) }, body,
                        "Type here…", block.id, pendingFocus, onFocusConsumed, onFocus,
                    )
                }
            }
            "divider" -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(2.dp)
                    .background(Mali.Sand)
                    .clickable(onClick = onFocus),
            )
            else -> Box(Modifier.clickable(onClick = onFocus)) { evidence(block, focused) { nb -> onRestyle(nb) } }
        }
        if (focused) {
            BlockActions(block, onRestyle, onMoveUp, onMoveDown, onDuplicate, onDelete)
        }
    }
}

@Composable
private fun BlockActions(
    block: Block,
    onRestyle: (Block) -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .padding(top = 6.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (block.type) {
            "heading" -> listOf(1, 2, 3).forEach { l ->
                ActionChip("H$l", selected = block.level == l) { onRestyle(block.copy(level = l)) }
            }
            "list" -> {
                ActionChip("Bullets", selected = !block.ordered) { onRestyle(block.copy(ordered = false)) }
                ActionChip("Numbers", selected = block.ordered) { onRestyle(block.copy(ordered = true)) }
            }
            "callout" -> TONES.forEach { t ->
                ActionChip(toneLabel(t).lowercase().replaceFirstChar { it.uppercase() }, selected = block.tone == t, color = toneColor(t)) {
                    onRestyle(block.copy(tone = t))
                }
            }
        }
        if (onMoveUp != null) ActionChip("↑ Up") { onMoveUp() }
        if (onMoveDown != null) ActionChip("↓ Down") { onMoveDown() }
        ActionChip("Copy") { onDuplicate() }
        ActionChip("Delete", color = Mali.Pink) { onDelete() }
    }
}

@Composable
fun ActionChip(label: String, selected: Boolean = false, color: Color = Mali.TealDeep, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = if (selected) Color.White else color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) color else color.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}
