package online.malilab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.malilab.app.ui.theme.Mali

/** The same small text language the website uses: blank line = new paragraph, "- " = bullet, **bold**, ==colour:text==. */
private val highlightColors = mapOf(
    "yellow" to Color(0xFFFFF1A0),
    "green" to Color(0xFFC4ECD2),
    "blue" to Color(0xFFCDE2FF),
    "pink" to Color(0xFFFFD0E2),
    "orange" to Color(0xFFFFD9B3),
)

private val inlinePattern = Regex("""==(?:(yellow|green|blue|pink|orange):)?(.+?)==|\*\*(.+?)\*\*""")
private val bulletPattern = Regex("""^\s*[-*•]\s+(.*)$""")

fun inlineText(text: String): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in inlinePattern.findAll(text)) {
        append(text.substring(last, m.range.first))
        val highlighted = m.groups[2]
        if (highlighted != null) {
            val color = highlightColors[m.groups[1]?.value ?: "yellow"] ?: highlightColors.getValue("yellow")
            withStyle(SpanStyle(background = color, color = Mali.Ink)) { append(highlighted.value) }
        } else {
            val bold = m.groups[3]?.value ?: ""
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
        }
        last = m.range.last + 1
    }
    append(text.substring(last))
}

sealed interface TextBlock {
    data class Para(val text: String) : TextBlock
    data class Bullets(val items: List<String>) : TextBlock
}

fun parseBlocks(text: String): List<TextBlock> {
    val blocks = mutableListOf<TextBlock>()
    val para = mutableListOf<String>()
    val bullets = mutableListOf<String>()

    fun flush() {
        if (para.isNotEmpty()) {
            blocks.add(TextBlock.Para(para.joinToString(" ")))
            para.clear()
        }
        if (bullets.isNotEmpty()) {
            blocks.add(TextBlock.Bullets(bullets.toList()))
            bullets.clear()
        }
    }

    for (raw in text.replace("\r", "").split("\n")) {
        val line = raw.trim()
        if (line.isEmpty()) {
            flush()
            continue
        }
        val bullet = bulletPattern.find(line)
        if (bullet != null) {
            if (para.isNotEmpty()) flush()
            bullets.add(bullet.groupValues[1].trim())
        } else {
            if (bullets.isNotEmpty()) flush()
            para.add(line)
        }
    }
    flush()
    return blocks
}

@Composable
fun RichBlocks(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Mali.Ink,
    bulletColor: Color = Mali.Teal,
    style: TextStyle = TextStyle(fontSize = 16.sp, lineHeight = 26.sp),
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (block in parseBlocks(text)) {
            when (block) {
                is TextBlock.Para -> Text(inlineText(block.text), style = style, color = color)
                is TextBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(start = 2.dp)) {
                    for (item in block.items) {
                        Row {
                            Text("•", style = style, color = bulletColor, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp))
                            Text(inlineText(item), style = style, color = color)
                        }
                    }
                }
            }
        }
    }
}
