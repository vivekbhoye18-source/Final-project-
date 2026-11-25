package com.example.pdf

import android.graphics.Canvas
import android.graphics.Paint
import com.example.data.model.FilledFormRecord
import com.example.pdf.PdfDrawUtils.BLACK
import com.example.pdf.PdfDrawUtils.INK_VALUE
import com.example.pdf.PdfDrawUtils.PAGE_WIDTH
import com.example.pdf.PdfDrawUtils.SHADE_LIGHT
import com.example.pdf.PdfDrawUtils.drawCellText
import com.example.pdf.PdfDrawUtils.drawCenteredText
import com.example.pdf.PdfDrawUtils.drawHospitalCrest
import com.example.pdf.PdfDrawUtils.drawLabelValueInline
import com.example.pdf.PdfDrawUtils.drawRightAlignedText
import com.example.pdf.PdfDrawUtils.drawWrappedBlock
import com.example.pdf.PdfDrawUtils.entryMap
import com.example.pdf.PdfDrawUtils.fillPaint
import com.example.pdf.PdfDrawUtils.formatAgeSex
import com.example.pdf.PdfDrawUtils.formatDeptUnit
import com.example.pdf.PdfDrawUtils.serifPaint
import com.example.pdf.PdfDrawUtils.strokePaint

/**
 * Exact 1-page A4 renderers matching Page 1 (Pathology Report) and Page 2 (Biochemistry Profile)
 * of the GGMC & Sir J. J. Group of Hospitals official hospital forms.
 */
internal object PathologyBiochemPdfRenderer {

    private sealed class BioRow {
        data class Banner(val title: String) : BioRow()
        data class Test(val label: String, val key: String, val unit: String, val normal: String) : BioRow()
    }

    // -------------------------------------------------------------------------
    // PAGE 1: DEPARTMENT OF PATHOLOGY — REPORT (CBC / SMEAR / COAG / URINE / STOOL)
    // -------------------------------------------------------------------------
    fun drawPathologyReportPdf(canvas: Canvas, record: FilledFormRecord) {
        val values = entryMap(record)
        fun v(key: String) = values[key.lowercase()].orEmpty()

        val marginL = 38f
        val marginR = 38f
        val contentW = PAGE_WIDTH - marginL - marginR
        val centerX = PAGE_WIDTH / 2f

        val headerBold = serifPaint(11.5f, bold = true)
        val reportBold = serifPaint(12f, bold = true)
        val labelPaint = serifPaint(9.0f, bold = false)
        val labelBold = serifPaint(9.0f, bold = true)
        val valuePaint = serifPaint(9.2f, bold = true, color = INK_VALUE)
        val tableHeaderPaint = serifPaint(9.5f, bold = true)
        val cellLabelPaint = serifPaint(8.8f, bold = false)
        val cellUnitPaint = serifPaint(8.5f, bold = false)
        val cellValPaint = serifPaint(9.0f, bold = true, color = INK_VALUE)
        val border = strokePaint(0.95f, BLACK)
        val thinBorder = strokePaint(0.75f, BLACK)

        // Official Header
        val deptTitle = record.departmentSubtitle.ifBlank { "DEPARTMENT OF PATHOLOGY" }
        val hospTitle = record.hospitalHeader.ifBlank { "GGMC AND SIR J. J. GROUP OF HOSPITALS, MUMBAI-08" }
        drawCenteredText(canvas, deptTitle, centerX, 54f, headerBold)
        drawCenteredText(canvas, hospTitle, centerX, 69f, headerBold)
        drawCenteredText(canvas, "REPORT", centerX, 86f, reportBold)
        val repW = reportBold.measureText("REPORT")
        canvas.drawLine(centerX - repW / 2f, 89f, centerX + repW / 2f, 89f, border)

        // Demographics Header (2 columns)
        val leftColX = marginL + 4f
        val rightColX = 330f
        val leftMaxW = 282f
        val rightMaxW = PAGE_WIDTH - marginR - rightColX

        drawLabelValueInline(canvas, "OPD/IPD :", record.patientIpdId, leftColX, 114f, leftMaxW, labelPaint, valuePaint)
        drawLabelValueInline(canvas, "SAMPLE ID :", v("sample_id"), rightColX, 114f, rightMaxW, labelPaint, valuePaint)

        drawLabelValueInline(canvas, "REGISTRATION No:", record.patientMrdNo, leftColX, 130f, leftMaxW, labelPaint, valuePaint)
        drawLabelValueInline(canvas, "PATIENT NAME :", record.patientName, rightColX, 130f, rightMaxW, labelPaint, valuePaint)

        drawLabelValueInline(canvas, "AGE/SEX :", formatAgeSex(record), leftColX, 146f, leftMaxW, labelPaint, valuePaint)
        drawLabelValueInline(canvas, "DEPT./UNIT :", formatDeptUnit(record), rightColX, 146f, rightMaxW, labelPaint, valuePaint)

        drawLabelValueInline(canvas, "CLINICAL DETAILS :", v("clinical_details"), leftColX, 162f, contentW - 8f, labelPaint, valuePaint)

        // ==================== TOP TABLE: CBC & PERIPHERAL SMEAR / COAG ====================
        val topTableY = 170f
        val rowH = 16.5f
        val cbcRows = listOf(
            Triple("RBC COUNT", "rbc_count", "X10^6/microL"),
            Triple("HAEMOGLOBIN", "haemoglobin", "g/dL"),
            Triple("HEMATOCRIT", "hematocrit", "%"),
            Triple("MCV", "mcv", "fL"),
            Triple("MCH", "mch", "Pg"),
            Triple("MCHC", "mchc", "g/dL"),
            Triple("RDW-CV", "rdw_cv", "%"),
            Triple("WBC COUNT", "wbc_count", "X10^3/microL"),
            Triple("NEUTROPHIL", "neutrophil", "%"),
            Triple("LYMPHOCYTE", "lymphocyte", "%"),
            Triple("MONOCYTE", "monocyte", "%"),
            Triple("EOSINOPHIL", "eosinophil", "%"),
            Triple("BASOPHIL", "basophil", "%"),
            Triple("PLATELETS", "platelets", "X10^3/microL"),
            Triple("MPV", "mpv", "fL"),
            Triple("PDW", "pdw", "fL"),
            Triple("PCT", "pct", "%")
        )
        val topHeaderH = 18f
        val topTableBottom = topTableY + topHeaderH + cbcRows.size * rowH
        val midX = marginL + contentW * 0.50f
        val cbcCol1W = (midX - marginL) * 0.42f
        val cbcCol2W = (midX - marginL) * 0.22f
        val cbcCol3W = (midX - marginL) - cbcCol1W - cbcCol2W

        // Outer rect + center split + header line
        canvas.drawRect(marginL, topTableY, marginL + contentW, topTableBottom, border)
        canvas.drawLine(midX, topTableY, midX, topTableBottom, border)
        canvas.drawLine(marginL, topTableY + topHeaderH, marginL + contentW, topTableY + topHeaderH, border)

        drawCellText(canvas, "CBC", marginL, topTableY, midX - marginL, topHeaderH, tableHeaderPaint, centered = true)
        drawCellText(canvas, "PERIPHERAL SMEAR", midX, topTableY, contentW - (midX - marginL), topHeaderH, tableHeaderPaint, centered = true)

        // CBC Vertical internal lines
        val cbcX2 = marginL + cbcCol1W
        val cbcX3 = cbcX2 + cbcCol2W
        canvas.drawLine(cbcX2, topTableY + topHeaderH, cbcX2, topTableBottom, thinBorder)
        canvas.drawLine(cbcX3, topTableY + topHeaderH, cbcX3, topTableBottom, thinBorder)

        // Draw 17 CBC rows
        cbcRows.forEachIndexed { idx, (label, key, unit) ->
            val ry = topTableY + topHeaderH + idx * rowH
            if (idx > 0) {
                canvas.drawLine(marginL, ry, midX, ry, thinBorder)
            }
            drawCellText(canvas, label, marginL, ry, cbcCol1W, rowH, cellLabelPaint)
            drawCellText(canvas, v(key), cbcX2, ry, cbcCol2W, rowH, cellValPaint, centered = true)
            drawCellText(canvas, unit, cbcX3, ry, cbcCol3W, rowH, cellUnitPaint)
        }

        // Right Half of Top Table:
        // Rows 0..9 -> Peripheral Smear text area
        val rightW = marginL + contentW - midX
        val smearText = v("peripheral_smear")
        if (smearText.isNotBlank()) {
            drawWrappedBlock(
                canvas = canvas,
                text = smearText,
                x = midX + 6f,
                y = topTableY + topHeaderH + 6f,
                width = rightW - 12f,
                paint = cellValPaint
            )
        }

        val rightSplitX = midX + rightW * 0.58f
        val rightRow10Y = topTableY + topHeaderH + 10 * rowH // PSMP
        val rightRow11Y = topTableY + topHeaderH + 11 * rowH // ESR
        val rightRow12Y = topTableY + topHeaderH + 12 * rowH // RETIC
        val rightRow13Y = topTableY + topHeaderH + 13 * rowH // COAGULATION PROFILE header
        val rightRow14Y = topTableY + topHeaderH + 14 * rowH // PT
        val rightRow15Y = topTableY + topHeaderH + 15 * rowH // INR
        val rightRow16Y = topTableY + topHeaderH + 16 * rowH // APTT

        // Horizontal lines on right half from row 10 downwards
        listOf(rightRow10Y, rightRow11Y, rightRow12Y, rightRow13Y, rightRow14Y, rightRow15Y, rightRow16Y).forEach { yLine ->
            canvas.drawLine(midX, yLine, marginL + contentW, yLine, thinBorder)
        }
        // Vertical split for ESR, RETIC (rows 11-12) and PT, INR, APTT (rows 14-16)
        canvas.drawLine(rightSplitX, rightRow11Y, rightSplitX, rightRow13Y, thinBorder)
        canvas.drawLine(rightSplitX, rightRow14Y, rightSplitX, topTableBottom, thinBorder)

        drawLabelValueInline(canvas, "PSMP :", v("psmp"), midX + 5f, rightRow10Y + 12f, rightW - 10f, labelBold, cellValPaint)

        drawCellText(canvas, "ESR (mm/hr)", midX, rightRow11Y, rightSplitX - midX, rowH, labelBold)
        drawCellText(canvas, v("esr"), rightSplitX, rightRow11Y, marginL + contentW - rightSplitX, rowH, cellValPaint, centered = true)

        drawCellText(canvas, "RETIC COUNT :", midX, rightRow12Y, rightSplitX - midX, rowH, labelBold)
        drawCellText(canvas, v("retic_count"), rightSplitX, rightRow12Y, marginL + contentW - rightSplitX, rowH, cellValPaint, centered = true)

        drawCellText(canvas, "COAGULATION PROFILE", midX, rightRow13Y, rightW, rowH, tableHeaderPaint, centered = true)

        drawCellText(canvas, "PT (sec)", midX, rightRow14Y, rightSplitX - midX, rowH, cellLabelPaint)
        drawCellText(canvas, v("pt_sec"), rightSplitX, rightRow14Y, marginL + contentW - rightSplitX, rowH, cellValPaint, centered = true)

        drawCellText(canvas, "INR", midX, rightRow15Y, rightSplitX - midX, rowH, cellLabelPaint)
        drawCellText(canvas, v("inr"), rightSplitX, rightRow15Y, marginL + contentW - rightSplitX, rowH, cellValPaint, centered = true)

        drawCellText(canvas, "APTT (sec)", midX, rightRow16Y, rightSplitX - midX, rowH, cellLabelPaint)
        drawCellText(canvas, v("aptt_sec"), rightSplitX, rightRow16Y, marginL + contentW - rightSplitX, rowH, cellValPaint, centered = true)

        // ==================== BOTTOM TABLE: URINE & STOOL ROUTINE MICROSCOPY ====================
        val botTableY = topTableBottom + 12f
        val botHeaderH = 22f
        val botRowH = 17.5f
        val botRowCount = 12
        val botTableBottom = botTableY + botHeaderH + botRowCount * botRowH
        val leftW = midX - marginL
        val leftSplitX = marginL + leftW * 0.50f
        val stoolSplitX = midX + rightW * 0.50f

        canvas.drawRect(marginL, botTableY, marginL + contentW, botTableBottom, border)
        canvas.drawLine(midX, botTableY, midX, botTableBottom, border)
        canvas.drawLine(marginL, botTableY + botHeaderH, marginL + contentW, botTableY + botHeaderH, border)

        drawCellText(canvas, "URINE ROUTINE MICROSCOPY", marginL, botTableY, leftW, botHeaderH, tableHeaderPaint, centered = true)
        drawCellText(canvas, "STOOL ROUTINE MICROSCOPY", midX, botTableY, rightW, botHeaderH, tableHeaderPaint, centered = true)

        // Draw horizontal grid lines across all 12 rows
        for (i in 1 until botRowCount) {
            val ry = botTableY + botHeaderH + i * botRowH
            canvas.drawLine(marginL, ry, marginL + contentW, ry, thinBorder)
        }

        // Urine internal vertical splits: rows 0..1 (VOLUME/COLOUR, SUGAR/PROTEIN) and rows 4..11 (Microscopy)
        val uRow0 = botTableY + botHeaderH
        val uRow2 = uRow0 + 2 * botRowH
        val uRow4 = uRow0 + 4 * botRowH
        canvas.drawLine(leftSplitX, uRow0, leftSplitX, uRow2, thinBorder)
        canvas.drawLine(leftSplitX, uRow4, leftSplitX, botTableBottom, thinBorder)

        // Stool internal vertical splits: rows 0..2 (VOL/COL/CONSIST) and rows 5..11 (Microscopy)
        val sRow3 = uRow0 + 3 * botRowH
        val sRow5 = uRow0 + 5 * botRowH
        canvas.drawLine(stoolSplitX, uRow0, stoolSplitX, sRow3, thinBorder)
        canvas.drawLine(stoolSplitX, sRow5, stoolSplitX, botTableBottom, thinBorder)

        // Urine rows 0..3
        drawLabelValueInline(canvas, "VOLUME:", v("urine_volume"), marginL + 5f, uRow0 + 12.5f, leftSplitX - marginL - 8f, cellLabelPaint, cellValPaint)
        drawLabelValueInline(canvas, "COLOUR :", v("urine_colour"), leftSplitX + 5f, uRow0 + 12.5f, midX - leftSplitX - 8f, cellLabelPaint, cellValPaint)

        drawLabelValueInline(canvas, "SUGAR :", v("urine_sugar"), marginL + 5f, uRow0 + botRowH + 12.5f, leftSplitX - marginL - 8f, cellLabelPaint, cellValPaint)
        drawLabelValueInline(canvas, "PROTEIN ;", v("urine_protein"), leftSplitX + 5f, uRow0 + botRowH + 12.5f, midX - leftSplitX - 8f, cellLabelPaint, cellValPaint)

        drawLabelValueInline(canvas, "OTHERS :", v("urine_others_macro"), marginL + 5f, uRow2 + 12.5f, leftW - 10f, cellLabelPaint, cellValPaint)
        drawCellText(canvas, "ON MICROSCOPY", marginL, uRow0 + 3 * botRowH, leftW, botRowH, cellLabelPaint)

        val urineMicroRows = listOf(
            Triple("BACTERIA", "urine_bacteria", ""),
            Triple("PARASITE", "urine_parasite", ""),
            Triple("CRYSTALS", "urine_crystals", ""),
            Triple("CAST", "urine_cast", ""),
            Triple("RBC", "urine_rbc", "/hpf"),
            Triple("EPITHELIAL CELLS", "urine_epithelial_cells", "/hpf"),
            Triple("PUS CELLS", "urine_pus_cells", "/hpf"),
            Triple("OTHERS", "urine_others_micro", "")
        )
        urineMicroRows.forEachIndexed { i, (lbl, key, suffix) ->
            val ry = uRow4 + i * botRowH
            drawCellText(canvas, lbl, marginL, ry, leftSplitX - marginL, botRowH, cellLabelPaint)
            val valStr = v(key)
            if (suffix.isNotBlank()) {
                drawRightAlignedText(canvas, suffix, midX - 24f, ry + 12.5f, cellUnitPaint)
                if (valStr.isNotBlank()) {
                    drawCellText(canvas, valStr, leftSplitX, ry, (midX - leftSplitX) - 42f, botRowH, cellValPaint)
                }
            } else {
                drawCellText(canvas, valStr, leftSplitX, ry, midX - leftSplitX, botRowH, cellValPaint)
            }
        }

        // Stool rows 0..2
        val stoolTopRows = listOf(
            "VOLUME" to "stool_volume",
            "COLOUR" to "stool_colour",
            "CONSISTENCY" to "stool_consistency"
        )
        stoolTopRows.forEachIndexed { i, (lbl, key) ->
            val ry = uRow0 + i * botRowH
            drawCellText(canvas, lbl, midX, ry, stoolSplitX - midX, botRowH, cellLabelPaint)
            drawCellText(canvas, v(key), stoolSplitX, ry, marginL + contentW - stoolSplitX, botRowH, cellValPaint)
        }
        // Stool row 4 -> ON MICROSCOPY :
        drawCellText(canvas, "ON MICROSCOPY :", midX, uRow0 + 4 * botRowH, rightW, botRowH, cellLabelPaint)

        val stoolMicroRows = listOf(
            Triple("BACTERIA", "stool_bacteria", ""),
            Triple("PARASITE", "stool_parasite", ""),
            Triple("RBC", "stool_rbc", "/hpf"),
            Triple("PUS CELLS", "stool_pus_cells", "/hpf"),
            Triple("OTHERS", "stool_others", ""),
            Triple("OCCULT BLOOD", "stool_occult_blood", "")
        )
        stoolMicroRows.forEachIndexed { i, (lbl, key, suffix) ->
            val ry = sRow5 + i * botRowH
            drawCellText(canvas, lbl, midX, ry, stoolSplitX - midX, botRowH, cellLabelPaint)
            val valStr = v(key)
            if (suffix.isNotBlank()) {
                drawRightAlignedText(canvas, suffix, marginL + contentW - 24f, ry + 12.5f, cellUnitPaint)
                if (valStr.isNotBlank()) {
                    drawCellText(canvas, valStr, stoolSplitX, ry, (marginL + contentW - stoolSplitX) - 42f, botRowH, cellValPaint)
                }
            } else {
                drawCellText(canvas, valStr, stoolSplitX, ry, marginL + contentW - stoolSplitX, botRowH, cellValPaint)
            }
        }

        // ==================== FOOTER ====================
        val footY = botTableBottom + 14f
        canvas.drawText("*Above findings are of sample received", marginL + 4f, footY, serifPaint(8.5f, bold = true))

        val commentY = footY + 20f
        val combinedComment = listOf(v("comment"), record.notes).filter { it.isNotBlank() }.joinToString(" | ")
        drawLabelValueInline(canvas, "COMMENT :", combinedComment, marginL + 4f, commentY, 320f, labelBold, cellValPaint)
        drawLabelValueInline(canvas, "CASE SEEN BY :", v("case_seen_by"), 384f, commentY, PAGE_WIDTH - marginR - 384f, labelBold, cellValPaint)
        drawLabelValueInline(canvas, "DATE OF REPORTING :", v("date_of_reporting"), 384f, commentY + 18f, PAGE_WIDTH - marginR - 384f, labelBold, cellValPaint)

        val codeText = record.footerText.ifBlank { "(G.C.P.) O 2663 (2,00,000-5-2025)" }
        canvas.drawText(codeText, marginL + 4f, 816f, serifPaint(8.2f))
    }

    // -------------------------------------------------------------------------
    // PAGE 2: DEPARTMENT OF BIOCHEMISTRY — BIOCHEMISTRY PROFILE
    // -------------------------------------------------------------------------
    fun drawBiochemistryProfilePdf(canvas: Canvas, record: FilledFormRecord) {
        val values = entryMap(record)
        fun v(key: String) = values[key.lowercase()].orEmpty()

        val marginL = 44f
        val marginR = 44f
        val contentW = PAGE_WIDTH - marginL - marginR
        val centerX = PAGE_WIDTH / 2f

        val border = strokePaint(0.95f, BLACK)
        val thinBorder = strokePaint(0.75f, BLACK)
        val shadeFill = fillPaint(SHADE_LIGHT)

        // Top-left GGMC Crest
        drawHospitalCrest(canvas, marginL + 40f, 74f, 28f)

        // Centered Header to right of crest
        val headerCenterX = 335f
        val deptTitle = record.departmentSubtitle.ifBlank { "DEPARTMENT OF BIOCHEMISTRY" }
        val hospTitle = record.hospitalHeader.ifBlank { "GRANT GOVT. MEDICAL COLLEGE & SIR J. J. GROUP OF HOSPITALS, MUMBAI" }
        drawCenteredText(canvas, deptTitle, headerCenterX, 66f, serifPaint(13f, bold = true))
        drawCenteredText(canvas, hospTitle, headerCenterX, 82f, serifPaint(9.2f, bold = true))
        drawCenteredText(
            canvas,
            "Tel: 022-2373 55 55, 2373 11 44 | Fax: 022-2373 55 99 | E-mail: gmcjjh@bom7.vsnl.net.in",
            headerCenterX,
            100f,
            serifPaint(7.8f)
        )

        // Horizontal divider
        canvas.drawLine(marginL, 120f, PAGE_WIDTH - marginR, 120f, border)

        // Right-aligned Date line
        val dateText = v("report_date").ifBlank { "____ / ____ / ________" }
        drawRightAlignedText(canvas, "Date: $dateText", PAGE_WIDTH - marginR, 136f, serifPaint(9.2f))

        // 5-Column Patient Strip Box
        val ptTableY = 144f
        val ptHeadH = 16f
        val ptValH = 26f
        val ptCols = listOf(0.18f, 0.25f, 0.20f, 0.20f, 0.17f)
        val ptHeaders = listOf("MRD No.", "Name", "Age / Gender", "Ward / OPD", "Lab. No.")
        val ptValues = listOf(
            record.patientMrdNo,
            record.patientName,
            formatAgeSex(record),
            record.patientWardBed,
            v("lab_no")
        )

        canvas.drawRect(marginL, ptTableY, marginL + contentW, ptTableY + ptHeadH + ptValH, border)
        canvas.drawLine(marginL, ptTableY + ptHeadH, marginL + contentW, ptTableY + ptHeadH, border)

        val ptHeadPaint = serifPaint(9.0f, bold = true)
        val ptValPaint = serifPaint(8.8f, bold = true, color = INK_VALUE)
        var curX = marginL
        ptCols.forEachIndexed { idx, ratio ->
            val cw = contentW * ratio
            if (idx > 0) {
                canvas.drawLine(curX, ptTableY, curX, ptTableY + ptHeadH + ptValH, thinBorder)
            }
            drawCellText(canvas, ptHeaders[idx], curX, ptTableY, cw, ptHeadH, ptHeadPaint, centered = true)
            drawCellText(canvas, ptValues[idx], curX, ptTableY + ptHeadH, cw, ptValH, ptValPaint, centered = true)
            curX += cw
        }

        // Centered Title: BIOCHEMISTRY PROFILE
        drawCenteredText(canvas, "BIOCHEMISTRY PROFILE", centerX, 208f, serifPaint(11f, bold = true))

        // Main 4-Column Table (Header + 3 section banners + 26 tests = 30 rows)
        val tableY = 216f
        val rowH = 18.0f
        val col1W = contentW * 0.29f
        val col2W = contentW * 0.24f
        val col3W = contentW * 0.22f
        val col4W = contentW - col1W - col2W - col3W
        val x1 = marginL
        val x2 = x1 + col1W
        val x3 = x2 + col2W
        val x4 = x3 + col3W

        val rows = listOf(
            BioRow.Banner("LFT / RFT"),
            BioRow.Test("Urea", "urea", "mg/dL", "15.0 – 40.0 mg/dL"),
            BioRow.Test("Creatinine", "creatinine", "mg/dL", "0.70 – 1.5 mg/dL"),
            BioRow.Test("SGOT (AST)", "sgot_ast", "IU/L", "8 – 40 IU/L"),
            BioRow.Test("SGPT (ALT)", "sgpt_alt", "IU/L", "5 – 35 IU/L"),
            BioRow.Test("Total Bilirubin", "total_bilirubin", "mg/dL", "0.2 – 0.8 mg/dL"),
            BioRow.Test("Total Protein", "total_protein", "g/dL", "6.0 – 8.0 g/dL"),
            BioRow.Test("Albumin", "albumin", "g/dL", "3.5 – 5.0 g/dL"),
            BioRow.Test("Globulin", "globulin", "g/dL", "2.5 – 3.0 g/dL"),
            BioRow.Test("Alkaline Phosphatase", "alkaline_phosphatase", "U/L", "80 – 306 U/L"),
            BioRow.Test("Calcium", "calcium", "mg/dL", "9.0 – 11.0 mg/dL"),
            BioRow.Test("Phosphorus (Inorganic)", "phosphorus", "mg/dL", "2.0 – 4.5 mg/dL"),
            BioRow.Test("Uric Acid", "uric_acid", "mg/dL", "2.0 – 6.5 mg/dL"),
            BioRow.Test("Amylase", "amylase", "U/L", "Up to 90 U/L"),
            BioRow.Test("LDH", "ldh", "IU/L", "235 – 470 IU/L"),
            BioRow.Test("Sodium", "sodium", "mEq/L", "135 – 145 mEq/L"),
            BioRow.Test("Potassium", "potassium", "mEq/L", "3.5 – 5.0 mEq/L"),
            BioRow.Test("Chloride", "chloride", "mEq/L", "95 – 105 mEq/L"),
            BioRow.Banner("LIPID PROFILE"),
            BioRow.Test("Triglycerides", "triglycerides", "mg/dL", "3 – 150 mg/dL"),
            BioRow.Test("Total Cholesterol", "total_cholesterol", "mg/dL", "130 – 230 mg/dL"),
            BioRow.Test("HDL Cholesterol", "hdl_cholesterol", "mg/dL", "40 – 60 mg/dL"),
            BioRow.Test("VLDL Cholesterol", "vldl_cholesterol", "mg/dL", "12 – 35 mg/dL"),
            BioRow.Test("LDL Cholesterol", "ldl_cholesterol", "mg/dL", "Up to 150 mg/dL"),
            BioRow.Banner("DIABETIC PROFILE"),
            BioRow.Test("Fasting Blood Sugar", "fasting_blood_sugar", "mg/dL", "70.0 – 110.0 mg/dL"),
            BioRow.Test("Postprandial Blood Sugar", "postprandial_blood_sugar", "mg/dL", "Up to 140 mg/dL"),
            BioRow.Test("Random Blood Sugar", "random_blood_sugar", "mg/dL", "Up to 160 mg/dL"),
            BioRow.Test("HbA1c (Glycosylated Hb)", "hba1c", "%", "4 – 6 %")
        )

        val headPaint = serifPaint(9.0f, bold = true)
        val bannerPaint = serifPaint(9.2f, bold = true)
        val testPaint = serifPaint(8.9f)
        val valPaint = serifPaint(9.2f, bold = true, color = INK_VALUE)

        // Header Row
        canvas.drawRect(marginL, tableY, marginL + contentW, tableY + rowH, shadeFill)
        canvas.drawRect(marginL, tableY, marginL + contentW, tableY + rowH, border)
        canvas.drawLine(x2, tableY, x2, tableY + rowH, thinBorder)
        canvas.drawLine(x3, tableY, x3, tableY + rowH, thinBorder)
        canvas.drawLine(x4, tableY, x4, tableY + rowH, thinBorder)
        drawCellText(canvas, "TEST", x1, tableY, col1W, rowH, headPaint, centered = true)
        drawCellText(canvas, "PATIENT'S VALUE", x2, tableY, col2W, rowH, headPaint, centered = true)
        drawCellText(canvas, "UNITS", x3, tableY, col3W, rowH, headPaint, centered = true)
        drawCellText(canvas, "NORMAL RANGE", x4, tableY, col4W, rowH, headPaint, centered = true)

        var y = tableY + rowH
        rows.forEach { row ->
            when (row) {
                is BioRow.Banner -> {
                    canvas.drawRect(marginL, y, marginL + contentW, y + rowH, shadeFill)
                    canvas.drawRect(marginL, y, marginL + contentW, y + rowH, thinBorder)
                    drawCellText(canvas, row.title, marginL, y, contentW, rowH, bannerPaint, centered = true)
                }

                is BioRow.Test -> {
                    canvas.drawRect(marginL, y, marginL + contentW, y + rowH, thinBorder)
                    canvas.drawLine(x2, y, x2, y + rowH, thinBorder)
                    canvas.drawLine(x3, y, x3, y + rowH, thinBorder)
                    canvas.drawLine(x4, y, x4, y + rowH, thinBorder)

                    drawCellText(canvas, row.label, x1, y, col1W, rowH, testPaint)
                    drawCellText(canvas, v(row.key), x2, y, col2W, rowH, valPaint, centered = true)
                    drawCellText(canvas, row.unit, x3, y, col3W, rowH, testPaint, centered = true)
                    drawCellText(canvas, row.normal, x4, y, col4W, rowH, testPaint)
                }
            }
            y += rowH
        }

        // Outer crisp frame around full table
        canvas.drawRect(marginL, tableY, marginL + contentW, y, border)

        // Optional clinical notes if entered
        if (record.notes.isNotBlank()) {
            drawLabelValueInline(canvas, "Remarks:", record.notes, marginL, y + 13f, contentW, serifPaint(8.3f, bold = true), valPaint)
        }

        // Footer: Please correlate clinically. + rule + Signature
        val footY = y + 26f
        val corrPaint = serifPaint(9.5f, italic = true)
        val corrText = record.footerText.ifBlank { "Please correlate clinically." }
        canvas.drawText(corrText, marginL, footY, corrPaint)
        val corrW = corrPaint.measureText(corrText)
        canvas.drawLine(marginL, footY + 2f, marginL + corrW, footY + 2f, thinBorder)
        canvas.drawLine(marginL, footY + 4f, PAGE_WIDTH - marginR, footY + 4f, border)

        drawRightAlignedText(canvas, "Signature", PAGE_WIDTH - marginR, footY + 18f, serifPaint(9.8f, bold = true))
    }
}
