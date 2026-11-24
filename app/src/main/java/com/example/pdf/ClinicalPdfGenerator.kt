package com.example.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.DefaultTemplates
import com.example.data.model.FieldType
import com.example.data.model.FilledFieldEntry
import com.example.data.model.FilledFormRecord
import com.example.data.model.FormCategory
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ClinicalPdfGenerator {
    // Standard A4 size in PostScript points (72 dpi)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_H = 36f
    private const val MARGIN_TOP = 34f
    private const val MARGIN_BOTTOM = 44f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_H * 2)

    private val TEAL_DARK = Color.parseColor("#0B4F5C")
    private val TEAL_TINT = Color.parseColor("#EAF4F4")
    private val ROW_ALT = Color.parseColor("#F7FAFA")
    private val BORDER_COLOR = Color.parseColor("#9FB8BC")
    private val INK_DARK = Color.parseColor("#0F1F24")
    private val INK_MUTED = Color.parseColor("#475E66")

    fun generatePdfFile(context: Context, record: FilledFormRecord): File {
        val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val safeMrd = record.patientMrdNo.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(24)
        val safeTpl = record.templateName.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(32)
        val fileName = "${safeTpl}_${safeMrd}_${System.currentTimeMillis() % 100000}.pdf"
        val outFile = File(dir, fileName)

        val pdf = PdfDocument()
        try {
            var pageNum = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
            var page = pdf.startPage(pageInfo)
            var canvas = page.canvas
            var y = MARGIN_TOP

            fun startNextPage() {
                drawPageFooter(canvas, pageNum, record.footerText)
                pdf.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
                page = pdf.startPage(pageInfo)
                canvas = page.canvas
                y = MARGIN_TOP
                // Compact continuation header
                val contPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = INK_MUTED
                    textSize = 8.5f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }
                canvas.drawText(
                    "${record.templateName.uppercase(Locale.US)}  |  ${record.patientName} (MRD: ${record.patientMrdNo}) — Contd.",
                    MARGIN_H,
                    y + 10f,
                    contPaint
                )
                y += 18f
                val linePaint = Paint().apply { color = BORDER_COLOR; strokeWidth = 0.8f }
                canvas.drawLine(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y, linePaint)
                y += 12f
            }

            fun ensureSpace(neededHeight: Float) {
                if (y + neededHeight > PAGE_HEIGHT - MARGIN_BOTTOM - 32f) {
                    startNextPage()
                }
            }

            // 1. Official Hospital Header Box
            y = drawHospitalHeader(canvas, record, y)
            y += 10f

            // 2. Patient Demographics 2-Column Clinical Grid
            y = drawPatientDemographicsTable(canvas, record, y)
            y += 14f

            // 3. Grouped Clinical Form Entries
            val isLabTable = FormCategory.fromRaw(record.templateCategory) == FormCategory.LAB
            val grouped = record.entries.groupBy { it.section.ifBlank { "Clinical Details" } }

            for ((sectionName, sectionEntries) in grouped) {
                ensureSpace(56f)
                y = drawSectionBanner(canvas, sectionName, y)

                if (isLabTable) {
                    y = drawLabTableHeader(canvas, y)
                    sectionEntries.forEachIndexed { idx, entry ->
                        val rowH = measureLabRowHeight(entry)
                        ensureSpace(rowH + 6f)
                        y = drawLabTableRow(canvas, entry, y, rowH, idx % 2 == 1)
                    }
                    y += 10f
                } else {
                    sectionEntries.forEach { entry ->
                        val blockH = measureClinicalBlockHeight(entry)
                        ensureSpace(blockH + 8f)
                        y = drawClinicalFieldBlock(canvas, entry, y, blockH)
                    }
                    y += 8f
                }
            }

            // 4. Additional Form Notes
            if (record.notes.isNotBlank()) {
                val noteHeight = measureWrappedText(record.notes, CONTENT_WIDTH - 16f, 9.5f) + 26f
                ensureSpace(noteHeight + 12f)
                y = drawSectionBanner(canvas, "Clinical Remarks / Additional Notes", y)
                val boxPaint = Paint().apply {
                    color = BORDER_COLOR
                    style = Paint.Style.STROKE
                    strokeWidth = 0.8f
                }
                canvas.drawRect(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + noteHeight, boxPaint)
                drawWrappedText(
                    canvas = canvas,
                    text = record.notes,
                    x = MARGIN_H + 8f,
                    y = y + 8f,
                    width = CONTENT_WIDTH - 16f,
                    textSize = 9.5f,
                    color = INK_DARK
                )
                y += noteHeight + 12f
            }

            // 5. Signature & Verification Box
            ensureSpace(58f)
            y = drawSignatureBlock(canvas, record, y)

            drawPageFooter(canvas, pageNum, record.footerText)
            pdf.finishPage(page)

            FileOutputStream(outFile).use { fos ->
                pdf.writeTo(fos)
            }
        } finally {
            pdf.close()
        }
        return outFile
    }

    private fun drawHospitalHeader(canvas: Canvas, record: FilledFormRecord, startY: Float): Float {
        var y = startY
        val header = record.hospitalHeader.ifBlank { DefaultTemplates.DEFAULT_HOSPITAL_HEADER }
        val subtitle = record.departmentSubtitle.ifBlank { record.patientDepartment }

        val borderPaint = Paint().apply {
            color = TEAL_DARK
            style = Paint.Style.STROKE
            strokeWidth = 1.4f
        }
        val titleBgPaint = Paint().apply {
            color = TEAL_DARK
            style = Paint.Style.FILL
        }

        val headerHeight = measureWrappedText(header, CONTENT_WIDTH - 24f, 12.5f, bold = true)
        val subHeight = if (subtitle.isNotBlank()) measureWrappedText(subtitle, CONTENT_WIDTH - 24f, 9f) + 4f else 0f
        val totalHeaderH = headerHeight + subHeight + 44f

        canvas.drawRect(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + totalHeaderH, borderPaint)

        // Hospital Name Centered
        val hospPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK_DARK
            textSize = 12.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val hospLayout = StaticLayout.Builder.obtain(
            header, 0, header.length, hospPaint, (CONTENT_WIDTH - 24f).toInt()
        ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()
        canvas.save()
        canvas.translate(MARGIN_H + 12f, y + 8f)
        hospLayout.draw(canvas)
        canvas.restore()

        var innerY = y + 8f + hospLayout.height + 3f

        if (subtitle.isNotBlank()) {
            val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = INK_MUTED
                textSize = 9f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            val subLayout = StaticLayout.Builder.obtain(
                subtitle, 0, subtitle.length, subPaint, (CONTENT_WIDTH - 24f).toInt()
            ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()
            canvas.save()
            canvas.translate(MARGIN_H + 12f, innerY)
            subLayout.draw(canvas)
            canvas.restore()
            innerY += subLayout.height + 6f
        } else {
            innerY += 6f
        }

        // Solid Teal Form Title Strip
        val bannerTop = y + totalHeaderH - 24f
        canvas.drawRect(MARGIN_H, bannerTop, PAGE_WIDTH - MARGIN_H, y + totalHeaderH, titleBgPaint)
        val formTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 11f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(
            record.templateName.uppercase(Locale.US),
            PAGE_WIDTH / 2f,
            bannerTop + 16f,
            formTitlePaint
        )

        return y + totalHeaderH
    }

    private fun drawPatientDemographicsTable(
        canvas: Canvas,
        record: FilledFormRecord,
        startY: Float
    ): Float {
        val dateStamp = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US).format(Date())
        val rows = listOf(
            Pair("Patient Name" to record.patientName.ifBlank { "—" }, "MRD / UHID No" to record.patientMrdNo.ifBlank { "—" }),
            Pair("Age / Sex" to listOf(record.patientAge, record.patientGender).filter { it.isNotBlank() }.joinToString(" / ").ifBlank { "—" }, "IPD ID" to record.patientIpdId.ifBlank { "—" }),
            Pair("Department" to record.patientDepartment.ifBlank { "—" }, "Ward / Bed No" to record.patientWardBed.ifBlank { "—" }),
            Pair("Admitting Doctor" to record.patientDoctor.ifBlank { "—" }, "Admission Date" to record.patientAdmissionDate.ifBlank { "—" }),
            Pair("MLC / EPR No" to record.patientMlcNumber.ifBlank { "N/A" }, "Form Printed" to dateStamp)
        )

        val colW = CONTENT_WIDTH / 2f
        val rowH = 20f
        var y = startY
        val borderPaint = Paint().apply {
            color = BORDER_COLOR
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        val bgPaint = Paint().apply {
            color = TEAL_TINT
            style = Paint.Style.FILL
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEAL_DARK
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK_DARK
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        rows.forEachIndexed { idx, (left, right) ->
            if (idx % 2 == 0) {
                canvas.drawRect(MARGIN_H, y, PAGE_WIDTH - MARGIN_H, y + rowH, bgPaint)
            }
            canvas.drawRect(MARGIN_H, y, MARGIN_H + colW, y + rowH, borderPaint)
            canvas.drawRect(MARGIN_H + colW, y, PAGE_WIDTH - MARGIN_H, y + rowH, borderPaint)

            val textY = y + 13.5f
            canvas.drawText("${left.first}:", MARGIN_H + 6f, textY, labelPaint)
            canvas.drawText(truncateText(left.second, valPaint, colW - 88f), MARGIN_H + 82f, textY, valPaint)

            canvas.drawText("${right.first}:", MARGIN_H + colW + 6f, textY, labelPaint)
            canvas.drawText(truncateText(right.second, valPaint, colW - 88f), MARGIN_H + colW + 82f, textY, valPaint)

            y += rowH
        }
        return y
    }

    private fun drawSectionBanner(canvas: Canvas, title: String, startY: Float): Float {
        val h = 19f
        val bgPaint = Paint().apply { color = TEAL_TINT; style = Paint.Style.FILL }
        val accentPaint = Paint().apply { color = TEAL_DARK; style = Paint.Style.FILL }
        val borderPaint = Paint().apply { color = BORDER_COLOR; style = Paint.Style.STROKE; strokeWidth = 0.7f }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEAL_DARK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        canvas.drawRect(MARGIN_H, startY, PAGE_WIDTH - MARGIN_H, startY + h, bgPaint)
        canvas.drawRect(MARGIN_H, startY, MARGIN_H + 3.5f, startY + h, accentPaint)
        canvas.drawRect(MARGIN_H, startY, PAGE_WIDTH - MARGIN_H, startY + h, borderPaint)
        canvas.drawText(title.uppercase(Locale.US), MARGIN_H + 10f, startY + 13f, textPaint)
        return startY + h
    }

    private fun drawLabTableHeader(canvas: Canvas, startY: Float): Float {
        val h = 18f
        val col1 = CONTENT_WIDTH * 0.44f
        val col2 = CONTENT_WIDTH * 0.24f
        val bgPaint = Paint().apply { color = Color.parseColor("#E3ECEE"); style = Paint.Style.FILL }
        val borderPaint = Paint().apply { color = BORDER_COLOR; style = Paint.Style.STROKE; strokeWidth = 0.7f }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK_DARK
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        canvas.drawRect(MARGIN_H, startY, PAGE_WIDTH - MARGIN_H, startY + h, bgPaint)
        canvas.drawRect(MARGIN_H, startY, MARGIN_H + col1, startY + h, borderPaint)
        canvas.drawRect(MARGIN_H + col1, startY, MARGIN_H + col1 + col2, startY + h, borderPaint)
        canvas.drawRect(MARGIN_H + col1 + col2, startY, PAGE_WIDTH - MARGIN_H, startY + h, borderPaint)

        canvas.drawText("TEST / PARAMETER", MARGIN_H + 6f, startY + 12.5f, textPaint)
        canvas.drawText("RESULT / VALUE", MARGIN_H + col1 + 6f, startY + 12.5f, textPaint)
        canvas.drawText("UNITS & REFERENCE RANGE", MARGIN_H + col1 + col2 + 6f, startY + 12.5f, textPaint)
        return startY + h
    }

    private fun measureLabRowHeight(entry: FilledFieldEntry): Float {
        val col1 = CONTENT_WIDTH * 0.44f - 12f
        val col2 = CONTENT_WIDTH * 0.24f - 12f
        val col3 = CONTENT_WIDTH * 0.32f - 12f
        val h1 = measureWrappedText(entry.label, col1, 9f)
        val valDisplay = formatFieldValue(entry)
        val h2 = measureWrappedText(valDisplay, col2, 9.5f, bold = true)
        val h3 = measureWrappedText(entry.unit.ifBlank { "—" }, col3, 8.5f)
        return maxOf(20f, maxOf(h1, maxOf(h2, h3)) + 9f)
    }

    private fun drawLabTableRow(
        canvas: Canvas,
        entry: FilledFieldEntry,
        startY: Float,
        rowH: Float,
        shaded: Boolean
    ): Float {
        val col1 = CONTENT_WIDTH * 0.44f
        val col2 = CONTENT_WIDTH * 0.24f
        if (shaded) {
            val bg = Paint().apply { color = ROW_ALT; style = Paint.Style.FILL }
            canvas.drawRect(MARGIN_H, startY, PAGE_WIDTH - MARGIN_H, startY + rowH, bg)
        }
        val border = Paint().apply { color = BORDER_COLOR; style = Paint.Style.STROKE; strokeWidth = 0.6f }
        canvas.drawRect(MARGIN_H, startY, MARGIN_H + col1, startY + rowH, border)
        canvas.drawRect(MARGIN_H + col1, startY, MARGIN_H + col1 + col2, startY + rowH, border)
        canvas.drawRect(MARGIN_H + col1 + col2, startY, PAGE_WIDTH - MARGIN_H, startY + rowH, border)

        drawWrappedText(canvas, entry.label, MARGIN_H + 6f, startY + 5f, col1 - 12f, 9f, INK_DARK)
        drawWrappedText(canvas, formatFieldValue(entry), MARGIN_H + col1 + 6f, startY + 5f, col2 - 12f, 9.5f, INK_DARK, bold = true)
        drawWrappedText(canvas, entry.unit.ifBlank { "—" }, MARGIN_H + col1 + col2 + 6f, startY + 5f, CONTENT_WIDTH - col1 - col2 - 12f, 8.5f, INK_MUTED)
        return startY + rowH
    }

    private fun measureClinicalBlockHeight(entry: FilledFieldEntry): Float {
        val labelH = measureWrappedText(entry.label, CONTENT_WIDTH - 16f, 8.5f, bold = true)
        val valueText = formatFieldValue(entry).let { v ->
            if (entry.unit.isNotBlank() && v != "—") "$v  (${entry.unit})" else v
        }
        val valH = measureWrappedText(valueText, CONTENT_WIDTH - 16f, 9.5f)
        val minValH = if (FieldType.fromRaw(entry.fieldType) == FieldType.MULTILINE) 22f else 14f
        return labelH + maxOf(valH, minValH) + 12f
    }

    private fun drawClinicalFieldBlock(
        canvas: Canvas,
        entry: FilledFieldEntry,
        startY: Float,
        blockH: Float
    ): Float {
        val border = Paint().apply { color = BORDER_COLOR; style = Paint.Style.STROKE; strokeWidth = 0.6f }
        canvas.drawRect(MARGIN_H, startY, PAGE_WIDTH - MARGIN_H, startY + blockH, border)

        val labelH = drawWrappedText(
            canvas,
            entry.label,
            MARGIN_H + 8f,
            startY + 4.5f,
            CONTENT_WIDTH - 16f,
            8.5f,
            TEAL_DARK,
            bold = true
        )
        val valueText = formatFieldValue(entry).let { v ->
            if (entry.unit.isNotBlank() && v != "—") "$v  (${entry.unit})" else v
        }
        drawWrappedText(
            canvas,
            valueText,
            MARGIN_H + 8f,
            startY + 6.5f + labelH,
            CONTENT_WIDTH - 16f,
            9.5f,
            INK_DARK
        )
        return startY + blockH
    }

    private fun drawSignatureBlock(canvas: Canvas, record: FilledFormRecord, startY: Float): Float {
        val h = 48f
        val border = Paint().apply { color = BORDER_COLOR; style = Paint.Style.STROKE; strokeWidth = 0.8f }
        canvas.drawRect(MARGIN_H, startY, PAGE_WIDTH - MARGIN_H, startY + h, border)

        val footer = record.footerText.ifBlank {
            "Prepared By: ________________      Resident / Consultant Signature & Stamp: ________________"
        }
        drawWrappedText(
            canvas,
            footer,
            MARGIN_H + 10f,
            startY + 24f,
            CONTENT_WIDTH - 20f,
            8.5f,
            INK_DARK,
            bold = true
        )
        return startY + h
    }

    private fun drawPageFooter(canvas: Canvas, pageNum: Int, footerText: String) {
        val lineY = PAGE_HEIGHT - 28f
        val linePaint = Paint().apply { color = BORDER_COLOR; strokeWidth = 0.6f }
        canvas.drawLine(MARGIN_H, lineY, PAGE_WIDTH - MARGIN_H, lineY, linePaint)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK_MUTED
            textSize = 7.5f
        }
        canvas.drawText("Generated by WardForms Clinical Registry", MARGIN_H, lineY + 12f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Page $pageNum", PAGE_WIDTH - MARGIN_H, lineY + 12f, paint)
    }

    private fun formatFieldValue(entry: FilledFieldEntry): String {
        val raw = entry.value.trim()
        if (FieldType.fromRaw(entry.fieldType) == FieldType.CHECKBOX) {
            return if (raw.equals("true", true) || raw.equals("yes", true)) "[ X ] Yes" else "[   ] No"
        }
        return raw.ifBlank { "—" }
    }

    private fun measureWrappedText(text: String, width: Float, textSize: Float, bold: Boolean = false): Float {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            this.typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
        val safeText = text.ifEmpty { " " }
        val layout = StaticLayout.Builder.obtain(
            safeText, 0, safeText.length, paint, width.toInt().coerceAtLeast(24)
        ).build()
        return layout.height.toFloat()
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        textSize: Float,
        color: Int,
        bold: Boolean = false
    ): Float {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.textSize = textSize
            this.typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
        val safeText = text.ifEmpty { " " }
        val layout = StaticLayout.Builder.obtain(
            safeText, 0, safeText.length, paint, width.toInt().coerceAtLeast(24)
        ).build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
        return layout.height.toFloat()
    }

    private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "...") > maxWidth) {
            end--
        }
        return text.substring(0, end) + "..."
    }
}
