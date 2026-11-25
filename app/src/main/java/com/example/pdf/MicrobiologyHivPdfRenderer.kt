package com.example.pdf

import android.graphics.Canvas
import android.text.Layout
import com.example.data.model.FilledFormRecord
import com.example.pdf.PdfDrawUtils.BLACK
import com.example.pdf.PdfDrawUtils.INK_VALUE
import com.example.pdf.PdfDrawUtils.PAGE_WIDTH
import com.example.pdf.PdfDrawUtils.drawCenteredText
import com.example.pdf.PdfDrawUtils.drawCheckBox
import com.example.pdf.PdfDrawUtils.drawHospitalCrest
import com.example.pdf.PdfDrawUtils.drawLabelValueInline
import com.example.pdf.PdfDrawUtils.drawRightAlignedText
import com.example.pdf.PdfDrawUtils.drawUnderlinedField
import com.example.pdf.PdfDrawUtils.drawWrappedBlock
import com.example.pdf.PdfDrawUtils.entryMap
import com.example.pdf.PdfDrawUtils.formatAgeSex
import com.example.pdf.PdfDrawUtils.isChecked
import com.example.pdf.PdfDrawUtils.sansPaint
import com.example.pdf.PdfDrawUtils.serifPaint
import com.example.pdf.PdfDrawUtils.strokePaint
import com.example.pdf.PdfDrawUtils.truncateText

/**
 * Exact 1-page A4 renderers matching Page 3 (Bacteriology Test Request),
 * Page 4 (Serology Test Request with 3-column checkbox table), and Page 5 (Consent Form for HIV Testing)
 * of the GGMC & Sir J. J. Group of Hospitals official hospital forms.
 */
internal object MicrobiologyHivPdfRenderer {

    // -------------------------------------------------------------------------
    // PAGE 3: DEPARTMENT OF MICROBIOLOGY — Bacteriology Laboratory (Test Request Form)
    // -------------------------------------------------------------------------
    fun drawBacteriologyRequestPdf(canvas: Canvas, record: FilledFormRecord) {
        val values = entryMap(record)
        fun v(key: String) = values[key.lowercase()].orEmpty()

        val marginL = 72f
        val marginR = 54f
        val contentW = PAGE_WIDTH - marginL - marginR
        val centerX = PAGE_WIDTH / 2f

        // Left & Right crests flanking bilingual header
        drawHospitalCrest(canvas, 96f, 84f, 26f)
        drawHospitalCrest(canvas, PAGE_WIDTH - 96f, 84f, 26f)

        drawCenteredText(canvas, "DEPARTMENT OF MICROBIOLOGY", centerX, 62f, sansPaint(9.5f, bold = true))
        drawCenteredText(canvas, "GRANT GOVERNMENT MEDICAL COLLEGE AND SIR J. J.", centerX, 77f, serifPaint(10.5f, bold = true))
        drawCenteredText(canvas, "GROUP OF HOSPITALS, BYCULLA, MUMBAI - 08", centerX, 92f, serifPaint(10.5f, bold = true))
        drawCenteredText(canvas, "सूक्ष्मजीवशास्त्र विभाग", centerX, 110f, sansPaint(10.5f, bold = true))
        drawCenteredText(
            canvas,
            "ग्रॅन्ट शासकीय वैद्यकीय महाविद्यालय व सर ज. जी. समूह रुग्णालये, भायखळा, मुंबई - ०८",
            centerX,
            125f,
            sansPaint(8.8f)
        )

        // Form Title
        drawCenteredText(canvas, "Bacteriology Laboratory", centerX, 174f, serifPaint(15f, bold = true))
        drawCenteredText(canvas, "(Test Request Form)", centerX, 198f, serifPaint(11.5f, bold = true))

        val labelPaint = serifPaint(10.2f)
        val valPaint = serifPaint(10.4f, bold = true, color = INK_VALUE)
        val col2X = 326f
        val col1W = col2X - marginL - 10f
        val col2W = PAGE_WIDTH - marginR - col2X

        var y = 242f
        val step = 38f

        // 1. Patient's Name | Reg. No.-
        drawLabelValueInline(canvas, "Patient's Name :", record.patientName, marginL, y, col1W, labelPaint, valPaint)
        drawLabelValueInline(canvas, "Reg. No.–", record.patientMrdNo, col2X, y, col2W, labelPaint, valPaint)
        y += step

        // 2. Age/Sex
        drawLabelValueInline(canvas, "Age/Sex :", formatAgeSex(record), marginL, y, contentW, labelPaint, valPaint)
        y += step

        // 3. Ward No | Unit | Department
        val wardX = marginL
        val unitX = 216f
        val deptX = 326f
        drawLabelValueInline(canvas, "Ward No :", record.patientWardBed, wardX, y, unitX - wardX - 8f, labelPaint, valPaint)
        drawLabelValueInline(canvas, "Unit :", record.patientUnit, unitX, y, deptX - unitX - 8f, labelPaint, valPaint)
        drawLabelValueInline(canvas, "Department :", record.patientDepartment, deptX, y, PAGE_WIDTH - marginR - deptX, labelPaint, valPaint)
        y += step

        // 4. Date of Admission
        val doa = v("date_of_admission").ifBlank { record.patientAdmissionDate }
        drawLabelValueInline(canvas, "Date of Admission :", doa, marginL, y, contentW, labelPaint, valPaint)
        y += step

        // 5. Date of sample collection | Time of Collection
        drawLabelValueInline(canvas, "Date of sample collection :", v("sample_collection_date"), marginL, y, col1W, labelPaint, valPaint)
        drawLabelValueInline(canvas, "Time of Collection :", v("sample_collection_time"), col2X, y, col2W, labelPaint, valPaint)
        y += step

        // 6. Clinical diagnosis
        drawLabelValueInline(canvas, "Clinical diagnosis :", v("clinical_diagnosis"), marginL, y, contentW, labelPaint, valPaint)
        y += step + 14f

        // 7. Antibiotics started if any
        drawLabelValueInline(canvas, "Antibiotics started if any :", v("antibiotics_started"), marginL, y, contentW, labelPaint, valPaint)
        y += step + 14f

        // 8. Specimen type
        drawLabelValueInline(canvas, "Specimen type :", v("specimen_type"), marginL, y, contentW, labelPaint, valPaint)
        y += step

        // 9. Site of specimen collection
        drawLabelValueInline(canvas, "Site of specimen collection : (specify please)", v("specimen_site"), marginL, y, contentW, labelPaint, valPaint)
        y += step

        // 10. Case of SSI | Date of Surgery
        drawLabelValueInline(canvas, "Case of SSI : Yes/No", v("case_of_ssi"), marginL, y, col1W, labelPaint, valPaint)
        drawLabelValueInline(canvas, "Date of Surgery :", v("date_of_surgery"), col2X, y, col2W, labelPaint, valPaint)
        y += step

        // 11. Investigation Required
        canvas.drawText("Investigation Required :", marginL, y, labelPaint)
        val invText = listOf(v("investigation_required"), record.notes).filter { it.isNotBlank() }.joinToString("\n")
        if (invText.isNotBlank()) {
            drawWrappedBlock(
                canvas = canvas,
                text = invText,
                x = marginL + labelPaint.measureText("Investigation Required : "),
                y = y - 10f,
                width = contentW - labelPaint.measureText("Investigation Required : "),
                paint = valPaint
            )
        }

        // Footer
        val footY = 760f
        drawLabelValueInline(canvas, "Date", v("request_date"), marginL, footY, 200f, serifPaint(11f), valPaint)
        val docName = v("doctor_name")
        if (docName.isNotBlank()) {
            drawRightAlignedText(canvas, docName, PAGE_WIDTH - marginR - 10f, footY - 14f, valPaint)
        }
        drawRightAlignedText(canvas, "Doctor's Signature.", PAGE_WIDTH - marginR - 10f, footY, serifPaint(11f))

        val gcp = record.footerText.ifBlank { "(G.C.P.) O 1180 ( 1,00,000–12-2024)" }
        canvas.drawText(gcp, marginL, 786f, serifPaint(8f))
    }

    // -------------------------------------------------------------------------
    // PAGE 4: DEPARTMENT OF MICROBIOLOGY — Serology Laboratory (Test Request Form)
    // -------------------------------------------------------------------------
    fun drawSerologyRequestPdf(canvas: Canvas, record: FilledFormRecord) {
        val values = entryMap(record)
        fun v(key: String) = values[key.lowercase()].orEmpty()

        val marginL = 56f
        val marginR = 46f
        val contentW = PAGE_WIDTH - marginL - marginR
        val centerX = PAGE_WIDTH / 2f
        val border = strokePaint(0.95f, BLACK)
        val thinBorder = strokePaint(0.75f, BLACK)

        // Boxed 3-panel Header matching Page 4
        val headTop = 44f
        val headH = 88f
        val boxL = 34f
        val boxR = PAGE_WIDTH - 28f
        val leftPanelW = 86f
        val rightPanelW = 92f

        canvas.drawRect(boxL, headTop, boxR, headTop + headH, thinBorder)
        canvas.drawLine(boxL + leftPanelW, headTop, boxL + leftPanelW, headTop + headH, thinBorder)
        canvas.drawLine(boxR - rightPanelW, headTop, boxR - rightPanelW, headTop + headH, thinBorder)

        drawHospitalCrest(canvas, boxL + leftPanelW / 2f, headTop + headH / 2f, 28f)
        drawHospitalCrest(canvas, boxR - rightPanelW / 2f, headTop + headH / 2f, 28f)

        val midHeadCenter = (boxL + leftPanelW + (boxR - rightPanelW)) / 2f
        drawCenteredText(canvas, "DEPARTMENT OF MICROBIOLOGY", midHeadCenter, headTop + 20f, sansPaint(9.8f, bold = true))
        drawCenteredText(canvas, "GRANT GOVT. MEDICAL COLLEGE & SIR J. J. GROUP OF", midHeadCenter, headTop + 34f, sansPaint(9.5f))
        drawCenteredText(canvas, "HOSPITALS, BYCULLA, MUMBAI 8", midHeadCenter, headTop + 47f, sansPaint(9.5f))
        drawCenteredText(canvas, "सूक्ष्मजीवशास्त्रविभाग", midHeadCenter, headTop + 62f, sansPaint(9.0f, bold = true))
        drawCenteredText(
            canvas,
            "ग्रॅन्ट शासकीय वैद्यकीय महाविद्यालय व सर ज. जी. समूहरुग्णालये, भायखळा, मुंबई ८",
            midHeadCenter,
            headTop + 76f,
            sansPaint(7.6f)
        )

        // Form Title
        drawCenteredText(canvas, "Serology Laboratory", centerX, 166f, serifPaint(14.5f, bold = true))
        drawCenteredText(canvas, "(Test Request Form)", centerX, 188f, serifPaint(11.5f, bold = true))

        // Demographics lines
        val lblBold = serifPaint(10.0f, bold = true)
        val valPaint = serifPaint(10.0f, bold = true, color = INK_VALUE)
        val col2X = 338f
        val col1W = col2X - marginL - 10f
        val col2W = PAGE_WIDTH - marginR - col2X

        var y = 216f
        val step = 25f

        drawLabelValueInline(canvas, "Patient's Name:", record.patientName, marginL, y, col1W, lblBold, valPaint)
        drawLabelValueInline(canvas, "Reg. No –", record.patientMrdNo, col2X, y, col2W, lblBold, valPaint)
        y += step

        drawLabelValueInline(canvas, "Age/Sex:", formatAgeSex(record), marginL, y, col1W, lblBold, valPaint)
        val mlcVal = v("mlc_prisoner_bloodbank").ifBlank { record.patientMlcNumber }
        drawLabelValueInline(canvas, "MLC No./ Prisoner/Blood bank:", mlcVal, col2X, y, col2W, lblBold, valPaint)
        y += step

        val unitX = 224f
        drawLabelValueInline(canvas, "Ward :", record.patientWardBed, marginL, y, unitX - marginL - 8f, lblBold, valPaint)
        drawLabelValueInline(canvas, "Unit:", record.patientUnit, unitX, y, col2X - unitX - 8f, lblBold, valPaint)
        drawLabelValueInline(canvas, "Department:", record.patientDepartment, col2X, y, col2W, lblBold, valPaint)
        y += step

        val doa = v("date_of_admission").ifBlank { record.patientAdmissionDate }
        drawLabelValueInline(canvas, "Date of Admission:", doa, marginL, y, contentW, lblBold, valPaint)
        y += step

        drawLabelValueInline(canvas, "Date of sample collection:", v("sample_collection_date"), marginL, y, col1W, lblBold, valPaint)
        drawLabelValueInline(canvas, "Time of Collection:", v("sample_collection_time"), col2X, y, col2W, lblBold, valPaint)
        y += step

        drawLabelValueInline(canvas, "Clinical diagnosis:", v("clinical_diagnosis"), marginL, y, contentW, lblBold, valPaint)
        y += step

        drawLabelValueInline(canvas, "Clinical History:", v("clinical_history"), marginL, y, contentW, lblBold, valPaint)
        y += step

        drawLabelValueInline(canvas, "Specimen type:", v("specimen_type"), marginL, y, contentW, lblBold, valPaint)
        y += step

        canvas.drawText("Investigation Required:  Tick the required test", marginL, y, lblBold)

        // ==================== 3-COLUMN CHECKBOX MATRIX TABLE ====================
        val tableY = y + 14f
        val tableW = contentW + 6f
        val col1TW = tableW * 0.25f
        val col2TW = tableW * 0.41f
        val col3TW = tableW - col1TW - col2TW
        val cx1 = marginL
        val cx2 = cx1 + col1TW
        val cx3 = cx2 + col2TW

        val headerH = 54f
        val rowH = 27.5f
        val rowCount = 8
        val tableBottom = tableY + headerH + rowCount * rowH

        canvas.drawRect(cx1, tableY, cx1 + tableW, tableBottom, border)
        canvas.drawLine(cx2, tableY, cx2, tableBottom, thinBorder)
        canvas.drawLine(cx3, tableY, cx3, tableBottom, thinBorder)
        canvas.drawLine(cx1, tableY + headerH, cx1 + tableW, tableY + headerH, border)

        val tblHeadPaint = serifPaint(9.8f, bold = true)
        drawWrappedBlock(canvas, "Latex agglutination/\nFloculation/ Tube\nagglutination", cx1 + 6f, tableY + 6f, col1TW - 12f, tblHeadPaint)
        drawWrappedBlock(canvas, "Rapid Immunochromatography", cx2 + 6f, tableY + 6f, col2TW - 12f, tblHeadPaint)
        drawWrappedBlock(canvas, "ELISA", cx3 + 6f, tableY + 6f, col3TW - 12f, tblHeadPaint)

        val col1Items = listOf(
            "RA" to "latex_ra",
            "ASO" to "latex_aso",
            "CRP" to "latex_crp",
            "RPR" to "latex_rpr",
            "Tube WIDAL" to "latex_tube_widal",
            null,
            null,
            null
        )
        val col2Items = listOf(
            "HBsAg" to "rapid_hbsag",
            "HCV IgM" to "rapid_hcv_igm",
            "HAV IgM" to "rapid_hav_igm",
            "HEV IgM" to "rapid_hev_igm",
            "Dengue NS1Ag IgM/IgG" to "rapid_dengue_ns1_igm_igg",
            "Leptospira IgM" to "rapid_leptospira_igm",
            "Chikungunya IgM" to "rapid_chikungunya_igm",
            "Malaria Pf/Pv Antigen" to "rapid_malaria_pf_pv"
        )
        val col3Items = listOf(
            "HBsAg" to "elisa_hbsag",
            "HCV IgM" to "elisa_hcv_igm",
            "HAV IgM" to "elisa_hav_igm",
            "HEV IgM" to "elisa_hev_igm",
            "Dengue NS1Ag IgM" to "elisa_dengue_ns1_igm",
            "Dengue IgM" to "elisa_dengue_igm",
            "Leptospira IgM" to "elisa_leptospira_igm",
            null
        )

        val itemPaint = serifPaint(9.8f, bold = true)
        val boxSize = 11.5f

        for (i in 0 until rowCount) {
            val ry = tableY + headerH + i * rowH
            if (i > 0) {
                canvas.drawLine(cx1, ry, cx1 + tableW, ry, thinBorder)
            }
            val textBaseY = ry + 17.5f
            val boxY = ry + (rowH - boxSize) / 2f

            col1Items[i]?.let { (label, key) ->
                canvas.drawText(truncateText(label, itemPaint, col1TW - 32f), cx1 + 6f, textBaseY, itemPaint)
                drawCheckBox(canvas, cx2 - boxSize - 10f, boxY, boxSize, isChecked(values, key))
            }
            col2Items[i]?.let { (label, key) ->
                canvas.drawText(truncateText(label, itemPaint, col2TW - 32f), cx2 + 6f, textBaseY, itemPaint)
                drawCheckBox(canvas, cx3 - boxSize - 14f, boxY, boxSize, isChecked(values, key))
            }
            col3Items[i]?.let { (label, key) ->
                canvas.drawText(truncateText(label, itemPaint, col3TW - 32f), cx3 + 6f, textBaseY, itemPaint)
                drawCheckBox(canvas, cx1 + tableW - boxSize - 12f, boxY, boxSize, isChecked(values, key))
            }
        }

        if (record.notes.isNotBlank()) {
            drawLabelValueInline(canvas, "Notes:", record.notes, marginL, tableBottom + 18f, contentW, serifPaint(9f, bold = true), valPaint)
        }

        // Footer: Date | Sign
        val footY = 786f
        drawLabelValueInline(canvas, "Date", v("request_date"), marginL + 4f, footY, 220f, serifPaint(10.5f, bold = true), valPaint)
        drawRightAlignedText(canvas, "Sign", PAGE_WIDTH - marginR - 46f, footY, serifPaint(10.5f, bold = true))
    }

    // -------------------------------------------------------------------------
    // PAGE 5: CONSENT FORM FOR HIV TESTING
    // -------------------------------------------------------------------------
    fun drawHivConsentPdf(canvas: Canvas, record: FilledFormRecord) {
        val values = entryMap(record)
        fun v(key: String) = values[key.lowercase()].orEmpty()

        val marginL = 62f
        val marginR = 54f
        val contentW = PAGE_WIDTH - marginL - marginR
        val centerX = PAGE_WIDTH / 2f
        val linePaint = strokePaint(0.8f, BLACK)

        // Top-left GGMC Crest
        drawHospitalCrest(canvas, 102f, 76f, 26f)

        // Centered Bilingual Government of Maharashtra Header
        val headCenterX = 328f
        drawCenteredText(canvas, "महाराष्ट्र शासन", headCenterX, 52f, sansPaint(8.5f))
        drawCenteredText(canvas, "सूक्ष्मजीवशास्त्र विभाग", headCenterX, 70f, sansPaint(13f, bold = true))
        drawCenteredText(canvas, "ग्रॅन्ट वैद्यकीय महाविद्यालय व", headCenterX, 87f, sansPaint(10.5f))
        drawCenteredText(canvas, "सर ज. जी. समूह रुग्णालय, मुंबई ४०० ००८.", headCenterX, 103f, sansPaint(10.5f))
        drawCenteredText(canvas, "Government of Maharashtra", headCenterX, 118f, serifPaint(8.5f))
        drawCenteredText(canvas, "Department of Microbiology", headCenterX, 134f, serifPaint(12.2f, bold = true))
        drawCenteredText(
            canvas,
            "Grant Medical college & Sir J.J. Group of Hospitals, Mumbai 400 008",
            headCenterX,
            149f,
            serifPaint(10.2f)
        )

        // Centered Title
        drawCenteredText(canvas, "CONSENT FORM FOR HIV TESTING", centerX, 198f, serifPaint(12.2f, bold = true))

        // Underlined Patient Details
        val lblPaint = serifPaint(11.0f)
        val valPaint = serifPaint(11.0f, bold = true, color = INK_VALUE)
        var y = 242f

        // Row 1: Name: ________   Age/ Sex: _____ / _____
        drawUnderlinedField(canvas, "Name:", record.patientName, marginL, y, 270f, lblPaint, valPaint, linePaint)
        val ageX = marginL + 286f
        canvas.drawText("Age/ Sex:", ageX, y, lblPaint)
        val ageStart = ageX + lblPaint.measureText("Age/ Sex:") + 6f
        val slashX = ageStart + 64f
        val sexEnd = marginL + contentW
        canvas.drawLine(ageStart, y + 2f, slashX - 5f, y + 2f, linePaint)
        canvas.drawText("/", slashX - 2f, y, lblPaint)
        canvas.drawLine(slashX + 6f, y + 2f, sexEnd, y + 2f, linePaint)
        if (record.patientAge.isNotBlank()) {
            canvas.drawText(truncateText(record.patientAge, valPaint, 56f), ageStart + 4f, y - 1f, valPaint)
        }
        if (record.patientGender.isNotBlank()) {
            canvas.drawText(truncateText(record.patientGender, valPaint, sexEnd - slashX - 10f), slashX + 10f, y - 1f, valPaint)
        }

        // Row 2: MRD No.: ________   Doctor/Ward: ________
        y += 40f
        val docWard = v("doctor_ward").ifBlank {
            listOf(record.patientDoctor, record.patientWardBed).filter { it.isNotBlank() }.joinToString(" / ")
        }
        drawUnderlinedField(canvas, "MRD No.:", record.patientMrdNo, marginL, y, 256f, lblPaint, valPaint, linePaint)
        drawUnderlinedField(canvas, "Doctor/Ward:", docWard, marginL + 272f, y, contentW - 272f, lblPaint, valPaint, linePaint)

        // Row 3: Address (2 ruled lines)
        y += 32f
        val addressVal = v("patient_address").ifBlank { record.patientAddress }
        canvas.drawText("Address:", marginL, y, lblPaint)
        val addrStartX = marginL + 68f
        canvas.drawLine(addrStartX, y + 2f, marginL + contentW - 44f, y + 2f, linePaint)
        canvas.drawLine(addrStartX, y + 28f, marginL + contentW - 44f, y + 28f, linePaint)
        if (addressVal.isNotBlank()) {
            val availLineW = (marginL + contentW - 44f) - addrStartX - 6f
            if (valPaint.measureText(addressVal) <= availLineW) {
                canvas.drawText(addressVal, addrStartX + 4f, y - 1f, valPaint)
            } else {
                // Split cleanly across the two ruled address lines
                var splitIdx = addressVal.length
                while (splitIdx > 1 && valPaint.measureText(addressVal.substring(0, splitIdx)) > availLineW) {
                    splitIdx--
                }
                val lastSpace = addressVal.lastIndexOf(' ', splitIdx).takeIf { it > 10 } ?: splitIdx
                val line1 = addressVal.substring(0, lastSpace).trim()
                val line2 = addressVal.substring(lastSpace).trim()
                canvas.drawText(line1, addrStartX + 4f, y - 1f, valPaint)
                canvas.drawText(truncateText(line2, valPaint, availLineW), addrStartX + 4f, y + 25f, valPaint)
            }
        }

        // Row 4: Phone No.
        y += 58f
        val phoneVal = v("patient_phone").ifBlank { record.patientMobile }
        drawUnderlinedField(canvas, "Phone No.:", phoneVal, marginL, y, contentW - 44f, lblPaint, valPaint, linePaint)

        // Verbatim Consent Declaration Paragraphs
        y += 22f
        val bodyPaint = serifPaint(11.0f)
        val para1 = "This is to state that I, the above, have been counselled about the HIV test to be conducted on my blood sample. I have also been explained about the implications of the test result. All the details pertaining to HIV, its transmission, testing procedures, their limitations, and interpretation of test results have been explained to me in a manner that I can understand."
        val h1 = drawWrappedBlock(canvas, para1, marginL, y, contentW, bodyPaint, Layout.Alignment.ALIGN_NORMAL, 1.20f)
        y += h1 + 8f

        val para2 = "I hereby give my consent for the HIV test to be conducted on my blood sample in order to ascertain my HIV status."
        val h2 = drawWrappedBlock(canvas, para2, marginL, y, contentW, bodyPaint, Layout.Alignment.ALIGN_NORMAL, 1.20f)
        y += h2 + 18f

        // Verbatim Statutory Notes Section
        val notePaint = serifPaint(9.0f)
        canvas.drawText("Note:", marginL, y, notePaint)
        y += 12f

        val notes = listOf(
            "1." to "It may be noted that the general consent obtained for carrying out procedures in a hospital does not include HIV consent.",
            "1." to "In case of patients who are minor, mental illness or unconscious, the consent form shall be acquired from parents/ guardian/ closest relative available at the time.",
            "2." to "In case no relative is available, the test may be carried out on the recommendation of two attending doctors.",
            "3." to "In cases of occupational exposure (needle stick injury etc.) of health care worker, even if the source patient has refused for HIV counselling and testing, it is the right of the exposed person to ask that blood to be taken for testing and the source patient may decline to be informed of the result."
        )
        notes.forEach { (num, text) ->
            canvas.drawText(num, marginL + 16f, y + 9f, notePaint)
            val nh = drawWrappedBlock(canvas, text, marginL + 34f, y, contentW - 34f, notePaint, Layout.Alignment.ALIGN_NORMAL, 1.15f)
            y += nh + 5f
        }

        // Bottom Signature & Date/Time Block
        val sigLabelY = 678f
        val sigLineY = 720f
        val dtY = 752f
        val rightSigX = 368f

        canvas.drawText("Signature/Thumb impression:", marginL, sigLabelY, lblPaint)
        canvas.drawLine(marginL, sigLineY, marginL + 182f, sigLineY, linePaint)

        canvas.drawText("Doctors Signature:", rightSigX, sigLabelY, lblPaint)
        canvas.drawLine(rightSigX, sigLineY, PAGE_WIDTH - marginR, sigLineY, linePaint)

        drawUnderlinedField(canvas, "Date:", v("consent_date"), marginL, dtY, 182f, lblPaint, valPaint, linePaint)
        drawUnderlinedField(canvas, "Time:", v("consent_time"), rightSigX, dtY, PAGE_WIDTH - marginR - rightSigX, lblPaint, valPaint, linePaint)
    }
}
