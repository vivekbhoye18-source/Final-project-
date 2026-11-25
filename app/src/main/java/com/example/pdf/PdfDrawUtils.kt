package com.example.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.model.FilledFormRecord

internal object PdfDrawUtils {
    // Standard A4 size in PostScript points (72 dpi)
    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842

    val BLACK = Color.parseColor("#111111")
    val INK_VALUE = Color.parseColor("#0B2A38")
    val SHADE_LIGHT = Color.parseColor("#EFEFEF")
    val LINE_GRAY = Color.parseColor("#333333")

    fun serifPaint(size: Float, bold: Boolean = false, italic: Boolean = false, color: Int = BLACK): TextPaint {
        val style = when {
            bold && italic -> Typeface.BOLD_ITALIC
            bold -> Typeface.BOLD
            italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.textSize = size
            this.typeface = Typeface.create(Typeface.SERIF, style)
        }
    }

    fun sansPaint(size: Float, bold: Boolean = false, color: Int = BLACK): TextPaint {
        return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.textSize = size
            this.typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    fun strokePaint(width: Float = 0.85f, color: Int = BLACK): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.style = Paint.Style.STROKE
        this.strokeWidth = width
    }

    fun fillPaint(color: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.style = Paint.Style.FILL
    }

    fun entryMap(record: FilledFormRecord): Map<String, String> =
        record.entries.associate { it.key.lowercase() to it.value.trim() }

    fun isChecked(values: Map<String, String>, key: String): Boolean {
        val v = values[key.lowercase()].orEmpty()
        return v.equals("yes", ignoreCase = true) || v.equals("true", ignoreCase = true) || v == "1"
    }

    fun formatAgeSex(record: FilledFormRecord): String {
        return listOf(record.patientAge, record.patientGender)
            .filter { it.isNotBlank() }
            .joinToString(" / ")
    }

    fun formatDeptUnit(record: FilledFormRecord): String {
        return listOf(record.patientDepartment, record.patientUnit)
            .filter { it.isNotBlank() }
            .joinToString(" / ")
    }

    fun drawCenteredText(canvas: Canvas, text: String, centerX: Float, baselineY: Float, paint: Paint) {
        val prev = paint.textAlign
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, centerX, baselineY, paint)
        paint.textAlign = prev
    }

    fun drawRightAlignedText(canvas: Canvas, text: String, rightX: Float, baselineY: Float, paint: Paint) {
        val prev = paint.textAlign
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(text, rightX, baselineY, paint)
        paint.textAlign = prev
    }

    fun drawCellText(
        canvas: Canvas,
        text: String,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        paint: Paint,
        paddingH: Float = 5f,
        centered: Boolean = false
    ) {
        if (text.isEmpty()) return
        val maxW = (width - paddingH * 2).coerceAtLeast(8f)
        val clipped = truncateText(text, paint, maxW)
        val fm = paint.fontMetrics
        val baseline = top + (height - (fm.descent - fm.ascent)) / 2f - fm.ascent
        if (centered) {
            drawCenteredText(canvas, clipped, left + width / 2f, baseline, paint)
        } else {
            canvas.drawText(clipped, left + paddingH, baseline, paint)
        }
    }

    fun drawWrappedBlock(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        paint: TextPaint,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        lineSpacingMultiplier: Float = 1.12f
    ): Float {
        if (text.isEmpty()) return 0f
        val layout = StaticLayout.Builder.obtain(
            text, 0, text.length, paint, width.toInt().coerceAtLeast(24)
        )
            .setAlignment(alignment)
            .setLineSpacing(0f, lineSpacingMultiplier)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
        return layout.height.toFloat()
    }

    fun drawCheckBox(
        canvas: Canvas,
        x: Float,
        y: Float,
        size: Float,
        checked: Boolean
    ) {
        val boxStroke = strokePaint(0.95f, BLACK)
        canvas.drawRect(x, y, x + size, y + size, boxStroke)
        if (checked) {
            val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = INK_VALUE
                style = Paint.Style.STROKE
                strokeWidth = 1.8f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            val path = Path().apply {
                moveTo(x + size * 0.20f, y + size * 0.54f)
                lineTo(x + size * 0.43f, y + size * 0.78f)
                lineTo(x + size * 0.84f, y + size * 0.24f)
            }
            canvas.drawPath(path, tickPaint)
        }
    }

    fun drawLabelValueInline(
        canvas: Canvas,
        label: String,
        value: String,
        x: Float,
        baselineY: Float,
        maxWidth: Float,
        labelPaint: Paint,
        valuePaint: Paint
    ) {
        canvas.drawText(label, x, baselineY, labelPaint)
        if (value.isNotBlank()) {
            val labelW = labelPaint.measureText(label) + 4f
            val avail = (maxWidth - labelW).coerceAtLeast(12f)
            canvas.drawText(truncateText(value, valuePaint, avail), x + labelW, baselineY, valuePaint)
        }
    }

    fun drawUnderlinedField(
        canvas: Canvas,
        label: String,
        value: String,
        x: Float,
        baselineY: Float,
        totalWidth: Float,
        labelPaint: Paint,
        valuePaint: Paint,
        linePaint: Paint
    ) {
        canvas.drawText(label, x, baselineY, labelPaint)
        val labelW = labelPaint.measureText(label) + 5f
        val lineStart = x + labelW
        val lineEnd = x + totalWidth
        canvas.drawLine(lineStart, baselineY + 2f, lineEnd, baselineY + 2f, linePaint)
        if (value.isNotBlank()) {
            val avail = (lineEnd - lineStart - 8f).coerceAtLeast(12f)
            canvas.drawText(truncateText(value, valuePaint, avail), lineStart + 4f, baselineY - 1f, valuePaint)
        }
    }

    /**
     * Draws a crisp monochrome medical crest shield matching the GGMC & Sir JJ Hospital seal placement.
     */
    fun drawHospitalCrest(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val stroke = strokePaint(1.0f, BLACK)
        val thin = strokePaint(0.65f, BLACK)

        // Outer shield silhouette
        val shield = Path().apply {
            moveTo(cx - radius * 0.72f, cy - radius * 0.75f)
            lineTo(cx + radius * 0.72f, cy - radius * 0.75f)
            lineTo(cx + radius * 0.72f, cy + radius * 0.05f)
            quadTo(cx + radius * 0.68f, cy + radius * 0.68f, cx, cy + radius * 0.92f)
            quadTo(cx - radius * 0.68f, cy + radius * 0.68f, cx - radius * 0.72f, cy + radius * 0.05f)
            close()
        }
        canvas.drawPath(shield, stroke)

        // Inner cross divider
        canvas.drawLine(cx - radius * 0.52f, cy - radius * 0.12f, cx + radius * 0.52f, cy - radius * 0.12f, thin)
        canvas.drawLine(cx, cy - radius * 0.62f, cx, cy + radius * 0.62f, thin)

        // Center medical emblem circle + cross
        canvas.drawCircle(cx, cy - radius * 0.12f, radius * 0.24f, fillPaint(Color.WHITE))
        canvas.drawCircle(cx, cy - radius * 0.12f, radius * 0.24f, thin)
        val crossPaint = strokePaint(1.5f, BLACK)
        canvas.drawLine(cx - radius * 0.12f, cy - radius * 0.12f, cx + radius * 0.12f, cy - radius * 0.12f, crossPaint)
        canvas.drawLine(cx, cy - radius * 0.24f, cx, cy, crossPaint)

        // Bottom motto ribbon arc
        val mottoPaint = serifPaint((radius * 0.20f).coerceAtLeast(4.5f), bold = true)
        drawCenteredText(canvas, "GGMC • JJ", cx, cy + radius * 0.52f, mottoPaint)
    }

    fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) {
            end--
        }
        return text.substring(0, end) + "…"
    }
}
