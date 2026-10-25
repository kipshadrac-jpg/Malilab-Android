package online.malilab.app.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import online.malilab.app.data.Block
import online.malilab.app.data.Fmt
import online.malilab.app.data.Note
import online.malilab.app.data.NoteStore
import online.malilab.app.ui.theme.MaliLight
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Turns a note into a PDF on the phone, with no website needed. Text, lists, callouts, key numbers, charts, tables,
 * assumptions and sources are all drawn from the same saved blocks the note shows, so the document matches the screen.
 */
object NotePdf {
    fun build(context: Context, note: Note): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val safe = note.title.replace(Regex("[^A-Za-z0-9 _-]"), "").trim().replace(' ', '_').take(60).ifBlank { "my_plan" }
        val file = File(dir, "$safe.pdf")
        val w = Writer(note)
        w.run()
        file.outputStream().use { w.doc.writeTo(it) }
        w.doc.close()
        return file
    }

    fun share(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Save or share PDF").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }

}

private const val PW = 595
private const val PH = 842
private const val MARGIN = 48f
private const val MAX_ROWS = 200

private fun argb(c: androidx.compose.ui.graphics.Color) = c.toArgb()
private fun withAlpha(color: Int, alpha: Int) = (color and 0x00FFFFFF) or (alpha shl 24)

private fun strings(a: JSONArray?): List<String> = (0 until (a?.length() ?: 0)).map { a!!.optString(it) }

private fun stampText(iso: String): String {
    val ms = NoteStore.parseIso(iso)
    return if (ms == 0L) "" else SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(ms))
}


private class Writer(val note: Note) {
        val doc = PdfDocument()
        private var page: PdfDocument.Page? = null
        private lateinit var canvas: Canvas
        private var y = 0f
        private var pageNo = 0
        private val left = MARGIN
        private val width = PW - 2 * MARGIN
        private val bottom = PH - MARGIN - 14f

        private val ink = argb(MaliLight.Ink)
        private val soft = argb(MaliLight.InkSoft)
        private val teal = argb(MaliLight.Teal)
        private val indigo = argb(MaliLight.Indigo)
        private val pink = argb(MaliLight.Pink)
        private val sand = argb(MaliLight.Sand)
        private val tint = argb(MaliLight.TealTint)
        private val chartColors = listOf(MaliLight.Teal, MaliLight.Indigo, MaliLight.Pink, MaliLight.Amber, MaliLight.Purple, MaliLight.Green).map { argb(it) }

        private fun paint(size: Float, color: Int = ink, bold: Boolean = false, italic: Boolean = false): TextPaint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                this.color = color
                typeface = Typeface.create(Typeface.SANS_SERIF, if (bold && italic) Typeface.BOLD_ITALIC else if (bold) Typeface.BOLD else if (italic) Typeface.ITALIC else Typeface.NORMAL)
            }

        private val body = paint(10.5f)
        private val small = paint(8.5f, soft)
        private val smallBold = paint(8.5f, soft, bold = true)

        // ------------------------------------------------------------------------------------------ pages
        private fun newPage() {
            finishPage()
            pageNo++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(PW, PH, pageNo).create())
            page = p
            canvas = p.canvas
            y = MARGIN
        }

        private fun finishPage() {
            val p = page ?: return
            val f = paint(8f, soft)
            canvas.drawText("MaliLab  ·  " + note.title.ifBlank { "My plan" }.take(70), left, PH - MARGIN + 6f, f)
            f.textAlign = Paint.Align.RIGHT
            canvas.drawText(pageNo.toString(), PW - MARGIN, PH - MARGIN + 6f, f)
            doc.finishPage(p)
            page = null
        }

        private fun keepTogether(h: Float) {
            if (y + h > bottom && h < bottom - MARGIN) newPage()
        }

        private fun layout(text: CharSequence, paint: TextPaint, w: Float, spacing: Float = 1.2f): StaticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, Math.max(10, w.toInt()))
                .setLineSpacing(0f, spacing).setIncludePad(false).build()

        /** Draws wrapped text line by line, starting a new page whenever the next line would not fit. */
        private fun text(text: String, paint: TextPaint, x: Float = left, w: Float = width, after: Float = 6f) {
            if (text.isBlank()) { y += after; return }
            val l = layout(text, paint, w)
            for (i in 0 until l.lineCount) {
                val h = (l.getLineBottom(i) - l.getLineTop(i)).toFloat()
                if (y + h > bottom) newPage()
                val start = l.getLineStart(i)
                var end = l.getLineEnd(i)
                while (end > start && (text[end - 1] == '\n' || text[end - 1] == ' ')) end--
                canvas.drawText(text, start, end, x, y + (l.getLineBaseline(i) - l.getLineTop(i)), paint)
                y += h
            }
            y += after
        }

        private fun rule(color: Int = sand) {
            val p = Paint().apply { this.color = color; strokeWidth = 0.8f }
            canvas.drawLine(left, y, left + width, y, p)
        }

        // ------------------------------------------------------------------------------------------ the document
        fun run() {
            newPage()
            text(note.title.ifBlank { "My plan" }, paint(22f, ink, bold = true), after = 4f)
            val meta = buildList {
                if (note.collection.isNotBlank()) add(note.collection)
                if (note.tags.isNotEmpty()) add(note.tags.joinToString(", ") { "#$it" })
                add("Exported " + SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date()))
            }.joinToString("  ·  ")
            text(meta, paint(9f, soft), after = 8f)
            rule(teal)
            y += 12f
            for (b in note.blocks) {
                if (b.isText && b.type != "divider" && b.type != "list" && b.text.isBlank()) continue
                if (b.type == "list" && b.items.all { it.isBlank() }) continue
                block(b)
            }
            if (note.blocks.none { !it.isText || it.text.isNotBlank() || it.type == "list" }) text("This note is empty.", paint(10.5f, soft, italic = true))
            y += 10f
            text("Made with MaliLab. Figures come from the calculators and use the assumptions shown; they are illustrations, not promises or financial advice.", paint(8f, soft, italic = true))
            finishPage()
        }

        private fun block(b: Block) {
            when (b.type) {
                "paragraph" -> text(b.text, body, after = 8f)
                "heading" -> {
                    val size = when (b.level) { 1 -> 20f; 2 -> 16f; else -> 13f }
                    y += 6f
                    keepTogether(size * 2.5f)
                    text(b.text, paint(size, ink, bold = true), after = 5f)
                }
                "list" -> {
                    b.items.filter { it.isNotBlank() }.forEachIndexed { i, item -> listItem(if (b.ordered) "${i + 1}." else "•", item) }
                    y += 4f
                }
                "callout" -> callout(b)
                "quote" -> {
                    val l = layout(b.text, paint(10.5f, soft, italic = true), width - 16f)
                    keepTogether(l.height + 8f)
                    val top = y
                    text(b.text, paint(10.5f, soft, italic = true), x = left + 14f, w = width - 16f, after = 8f)
                    if (y - 8f > top) canvas.drawRect(left, top, left + 3f, y - 8f, Paint().apply { color = sand })
                }
                "divider" -> { y += 4f; rule(); y += 12f }
                else -> evidence(b)
            }
        }

        private fun listItem(marker: String, item: String) {
            val p = body
            val l = layout(item, p, width - 18f)
            keepTogether(Math.min(l.height.toFloat(), 40f))
            canvas.drawText(marker, left + 2f, y + (l.getLineBaseline(0) - l.getLineTop(0)), p)
            text(item, p, x = left + 18f, w = width - 18f, after = 3f)
        }

        private fun callout(b: Block) {
            val color = argb(toneColor(b.tone))
            val l = layout(b.text, body, width - 28f)
            val h = 16f + l.height + 14f
            keepTogether(h)
            if (y + h <= bottom) {
                canvas.drawRoundRect(RectF(left, y, left + width, y + h), 8f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = withAlpha(color, 30) })
                canvas.drawRect(left, y, left + 3.5f, y + h, Paint().apply { this.color = color })
                y += 8f
                text(toneLabel(b.tone), paint(8f, color, bold = true), x = left + 14f, w = width - 28f, after = 2f)
                text(b.text, body, x = left + 14f, w = width - 28f, after = 0f)
                y += 12f
            } else {
                text(toneLabel(b.tone), paint(8f, color, bold = true), after = 2f)
                text(b.text, body, after = 8f)
            }
            y += 4f
        }

        // ------------------------------------------------------------------------------------------ evidence from the calculator
        private fun evidence(b: Block) {
            val raw = b.raw ?: return
            val type = raw.optString("type")
            val tag = when (type) {
                "metric" -> "KEY NUMBER"; "chart" -> "CHART"; "table" -> "TABLE"; "scenarios" -> "WHAT-IFS"; "assumptions" -> "ASSUMPTIONS"
                "methodology" -> "METHOD"; "interpretation" -> "WHAT THIS SHOWS"; "source" -> "SOURCE"; else -> type.uppercase()
            }
            val title = raw.optString("title")
            keepTogether(if (type == "chart") 280f else 60f)
            text(tag, paint(8f, indigo, bold = true), after = 2f)
            if (title.isNotBlank() && type != "metric" && type != "interpretation" && type != "assumptions" && type != "methodology") text(title, paint(12f, ink, bold = true), after = 4f)
            when (type) {
                "metric" -> metric(raw)
                "chart" -> chart(raw)
                "table" -> table(raw)
                "scenarios" -> scenarios(raw)
                "assumptions" -> assumptions(raw)
                "methodology" -> {
                    text("How this was calculated", paint(11f, ink, bold = true), after = 3f)
                    strings(raw.optJSONArray("lines")).forEach { listItem("•", it) }
                }
                "interpretation" -> strings(raw.optJSONArray("sentences")).joinToString("\n\n").split("\n\n").forEach { text(it, body, after = 5f) }
                "source" -> {
                    if (raw.optString("text").isNotBlank()) text(raw.optString("text"), body, after = 2f)
                    if (raw.optString("url").isNotBlank()) text(raw.optString("url"), paint(9f, indigo), after = 4f)
                }
            }
            val calc = raw.optJSONObject("calc")
            if (calc != null && type != "source") {
                val live = raw.optString("mode") == "live"
                val stamp = stampText(if (live) calc.optString("calculated_at") else raw.optString("captured_at").ifBlank { calc.optString("calculated_at") })
                text((if (live) "Calculated " else "Snapshot captured ") + stamp + "  ·  model " + calc.optString("model") + " " + calc.optString("version"), small, after = 10f)
            } else {
                y += 6f
            }
        }

        private fun metric(raw: JSONObject) {
            val label = layout(raw.optString("label"), paint(9f, soft, bold = true), width - 24f)
            val value = layout(raw.optString("value"), paint(20f, ink, bold = true), width - 24f)
            val cap = if (raw.optString("caption").isNotBlank()) layout(raw.optString("caption"), paint(9f, soft), width - 24f).height else 0
            val h = 12f + label.height + value.height + cap + 12f
            keepTogether(h)
            if (y + h <= bottom) {
                canvas.drawRoundRect(RectF(left, y, left + width, y + h), 8f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tint })
                y += 10f
                text(raw.optString("label"), paint(9f, soft, bold = true), x = left + 12f, w = width - 24f, after = 1f)
                text(raw.optString("value"), paint(20f, ink, bold = true), x = left + 12f, w = width - 24f, after = 1f)
                if (cap > 0) text(raw.optString("caption"), paint(9f, soft), x = left + 12f, w = width - 24f, after = 0f)
                y += 12f
            } else {
                text(raw.optString("label"), paint(9f, soft, bold = true), after = 1f)
                text(raw.optString("value"), paint(20f, ink, bold = true), after = 2f)
                text(raw.optString("caption"), paint(9f, soft), after = 2f)
            }
            y += 4f
        }

        private fun niceStep(range: Double, ticks: Int): Double {
            val raw = if (range <= 0) 1.0 else range / ticks
            val mag = Math.pow(10.0, Math.floor(Math.log10(raw)))
            val n = raw / mag
            val m = if (n <= 1) 1.0 else if (n <= 2) 2.0 else if (n <= 5) 5.0 else 10.0
            return m * mag
        }

        private fun dash(style: Int): DashPathEffect? = when (style % 4) {
            1 -> DashPathEffect(floatArrayOf(9f, 5f), 0f)
            2 -> DashPathEffect(floatArrayOf(2f, 4f), 0f)
            3 -> DashPathEffect(floatArrayOf(10f, 4f, 2f, 4f), 0f)
            else -> null
        }

        private fun marker(style: Int, cx: Float, cy: Float, paint: Paint) {
            val r = 2.6f
            when (style % 4) {
                0 -> canvas.drawCircle(cx, cy, r, paint)
                1 -> canvas.drawRect(cx - r, cy - r, cx + r, cy + r, paint)
                2 -> canvas.drawPath(Path().apply { moveTo(cx, cy - r - 0.6f); lineTo(cx + r + 0.6f, cy + r); lineTo(cx - r - 0.6f, cy + r); close() }, paint)
                else -> canvas.drawPath(Path().apply { moveTo(cx, cy - r - 0.8f); lineTo(cx + r + 0.8f, cy); lineTo(cx, cy + r + 0.8f); lineTo(cx - r - 0.8f, cy); close() }, paint)
            }
        }

        private fun chart(raw: JSONObject) {
            val xs = strings(raw.optJSONArray("x")).map { it.toDoubleOrNull() ?: 0.0 }
            val arr = raw.optJSONArray("series") ?: JSONArray()
            val names = (0 until arr.length()).map { arr.getJSONObject(it).optString("name") }
            val values = (0 until arr.length()).map { i -> strings(arr.getJSONObject(i).optJSONArray("values")).map { it.toDoubleOrNull() ?: 0.0 } }
            if (xs.size < 2 || values.isEmpty()) return
            val plotH = 180f
            val total = plotH + 40f + 13f * names.size
            keepTogether(total)
            val all = values.flatten()
            val minV = Math.min(0.0, all.min())
            val maxRaw = all.max()
            val step = niceStep(maxRaw - minV, 4)
            val top = Math.ceil(maxRaw / step) * step
            val bot = Math.floor(minV / step) * step
            val range = if (top - bot <= 0) 1.0 else top - bot
            val px0 = left + 50f
            val px1 = left + width - 8f
            val py0 = y + 6f
            val py1 = y + 6f + plotH
            val xMin = xs.first(); val xMax = xs.last()
            val xRange = if (xMax - xMin <= 0) 1.0 else xMax - xMin
            fun sx(x: Double) = (px0 + (x - xMin) / xRange * (px1 - px0)).toFloat()
            fun sy(v: Double) = (py1 - (v - bot) / range * (py1 - py0)).toFloat()

            val grid = Paint().apply { color = sand; strokeWidth = 0.6f }
            val axisText = paint(7.5f, soft)
            axisText.textAlign = Paint.Align.RIGHT
            var v = bot
            while (v <= top + step / 2) {
                canvas.drawLine(px0, sy(v), px1, sy(v), grid)
                canvas.drawText(Fmt.compact(v), px0 - 4f, sy(v) + 3f, axisText)
                v += step
            }
            val xText = paint(7.5f, soft)
            xText.textAlign = Paint.Align.CENTER
            val tickCount = Math.min(xs.size, 6)
            for (t in 0 until tickCount) {
                val idx = if (tickCount == 1) 0 else Math.round(t * (xs.size - 1).toFloat() / (tickCount - 1))
                val xv = xs[idx]
                canvas.drawText(if (xv % 1.0 == 0.0) xv.toInt().toString() else String.format(Locale.US, "%.1f", xv), sx(xv), py1 + 11f, xText)
            }
            xText.textAlign = Paint.Align.CENTER
            canvas.drawText(raw.optString("x_label", "Year"), (px0 + px1) / 2, py1 + 22f, xText)

            values.forEachIndexed { si, vals ->
                val color = chartColors[si % chartColors.size]
                val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = 1.7f; pathEffect = dash(si) }
                val path = Path()
                val n = Math.min(xs.size, vals.size)
                for (i in 0 until n) if (i == 0) path.moveTo(sx(xs[i]), sy(vals[i])) else path.lineTo(sx(xs[i]), sy(vals[i]))
                canvas.drawPath(path, line)
                val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
                val every = Math.max(1, n / 6)
                for (i in 0 until n step every) marker(si, sx(xs[i]), sy(vals[i]), fill)
                marker(si, sx(xs[n - 1]), sy(vals[n - 1]), fill)
            }
            y = py1 + 30f
            names.forEachIndexed { si, name ->
                val color = chartColors[si % chartColors.size]
                val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = 1.7f; pathEffect = dash(si) }
                canvas.drawLine(left + 2f, y - 3f, left + 24f, y - 3f, line)
                marker(si, left + 13f, y - 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
                val last = values[si].lastOrNull() ?: 0.0
                canvas.drawText(name + "  (at the end: " + Fmt.compact(last) + ")", left + 30f, y, paint(8.5f, ink))
                y += 13f
            }
            y += 4f
        }

        private fun table(raw: JSONObject) {
            val cols = raw.optJSONArray("columns") ?: return
            val rows = raw.optJSONArray("rows") ?: JSONArray()
            val n = cols.length()
            if (n == 0) return
            val wide = setOf("what", "factor", "scenario", "option")
            val weights = (0 until n).map { if (cols.getJSONObject(it).optString("key") in wide) 2.4f else 1f }
            val sum = weights.sum()
            val widths = weights.map { width * it / sum }
            val size = if (n >= 7) 7.2f else if (n >= 5) 8f else 9f
            val head = paint(size, argb(MaliLight.TealDeep), bold = true)
            val cell = paint(size, ink)

            fun drawHeader() {
                val ls = (0 until n).map { layout(cols.getJSONObject(it).optString("label"), head, widths[it] - 6f, 1.1f) }
                val h = (ls.maxOf { it.height } + 8f)
                if (y + h > bottom) newPage()
                canvas.drawRect(left, y, left + width, y + h, Paint().apply { color = tint })
                var x = left
                ls.forEachIndexed { i, l ->
                    canvas.save(); canvas.translate(x + 3f, y + 4f); l.draw(canvas); canvas.restore()
                    x += widths[i]
                }
                y += h
            }
            drawHeader()
            val shown = Math.min(rows.length(), MAX_ROWS)
            for (r in 0 until shown) {
                val row = rows.getJSONArray(r)
                val ls = (0 until n).map { layout(row.optString(it), cell, widths[it] - 6f, 1.1f) }
                val h = (ls.maxOf { it.height } + 6f)
                if (y + h > bottom) { newPage(); drawHeader() }
                if (r % 2 == 1) canvas.drawRect(left, y, left + width, y + h, Paint().apply { color = withAlpha(sand, 70) })
                var x = left
                ls.forEachIndexed { i, l ->
                    canvas.save(); canvas.translate(x + 3f, y + 3f); l.draw(canvas); canvas.restore()
                    x += widths[i]
                }
                y += h
            }
            if (rows.length() > shown) text("Showing the first $shown of ${rows.length()} rows. Open the note in the app for the rest.", small, after = 2f)
            y += 4f
        }

        private fun scenarios(raw: JSONObject) {
            val arr = raw.optJSONArray("scenarios") ?: return
            for (i in 0 until arr.length()) {
                val s = arr.getJSONObject(i)
                keepTogether(60f)
                text(s.optString("name"), paint(10.5f, ink, bold = true), after = 1f)
                text("Final balance " + Fmt.kes(s.optString("final_balance")), body, after = 1f)
                if (i > 0) {
                    val delta = s.optString("delta").toDoubleOrNull() ?: 0.0
                    val word = if (delta > 0) "▲ " + Fmt.kes(s.optString("delta")) + " more than the base case"
                    else if (delta < 0) "▼ " + Fmt.kes(s.optString("delta").removePrefix("-")) + " less than the base case" else "Same as the base case"
                    text(word, paint(9.5f, ink, bold = true), after = 1f)
                    val ch = s.optJSONArray("changes") ?: JSONArray()
                    for (j in 0 until ch.length()) {
                        val c = ch.getJSONObject(j)
                        text(c.optString("label") + ": " + c.optString("from") + " → " + c.optString("to"), small, after = 0f)
                    }
                } else {
                    text("Starting point", small, after = 0f)
                }
                y += 7f
            }
        }

        private fun assumptions(raw: JSONObject) {
            text("Assumptions behind these numbers", paint(11f, ink, bold = true), after = 4f)
            val arr = raw.optJSONArray("items") ?: return
            for (i in 0 until arr.length()) {
                val a = arr.getJSONObject(i)
                val kind = when (a.optString("kind")) { "user-entered" -> "you entered"; "assumption" -> "an assumption"; "sourced" -> "from a source"; else -> a.optString("kind") }
                keepTogether(34f)
                text(a.optString("label"), smallBold, after = 0f)
                text(a.optString("value") + "  ·  " + kind, body, after = if (a.optString("source").isBlank()) 4f else 0f)
                if (a.optString("source").isNotBlank()) text(a.optString("source"), small, after = 4f)
            }
        }
    }
