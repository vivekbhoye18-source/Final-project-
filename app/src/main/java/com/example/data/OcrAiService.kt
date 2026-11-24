package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.FieldAccuracy
import com.example.data.model.FieldType
import com.example.data.model.FormCategory
import com.example.data.model.FormTemplate
import com.example.data.model.OcrExtractionResult
import com.example.data.model.PatientRecord
import com.example.data.model.TemplateField
import com.example.data.model.TemplateImportResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "OcrAiService"
private const val MODEL_NAME = "gemini-3.8-flash"
private const val AI_TIMEOUT_MS = 60_000L

class OcrAiService {

    private val patientOcrSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "mrdNo" to Schema.string(description = "Patient MRD / UHID / MRN number. Empty string if not found."),
                "fullName" to Schema.string(description = "Patient full name including honorific if printed."),
                "gender" to Schema.enumeration(
                    values = listOf("Male", "Female", "Other", "Unknown"),
                    description = "Normalized patient gender."
                ),
                "age" to Schema.string(description = "Patient age (e.g. 40Y 0M 1D or 40 Yrs)."),
                "ipdId" to Schema.string(description = "IPD / Inpatient Admission ID if present."),
                "abhaId" to Schema.string(description = "ABHA Health ID if present."),
                "department" to Schema.string(description = "Hospital department (e.g. General Surgery)."),
                "unit" to Schema.string(description = "Clinical unit (e.g. UNIT 6)."),
                "wardBed" to Schema.string(description = "Ward and bed number (e.g. EMERG WARD 4 / Bed 2)."),
                "admittingDoctor" to Schema.string(description = "Admitting doctor / consultant name."),
                "admissionDate" to Schema.string(description = "Date and time of admission."),
                "mobile" to Schema.string(description = "Patient mobile phone number."),
                "address" to Schema.string(description = "Patient residential address."),
                "mlcNumber" to Schema.string(description = "MLC / Police Case / EPR number if present."),
                "diagnosis" to Schema.string(description = "Provisional, working, or handwritten diagnosis."),
                "attendantName" to Schema.string(description = "Attendant or relative name."),
                "attendantRelation" to Schema.string(description = "Relationship of attendant to patient."),
                "attendantMobile" to Schema.string(description = "Attendant phone number."),
                "extraNotes" to Schema.string(description = "Handwritten ward instructions, stamps, shift notes, or billing/scheme info."),
                "overallAccuracy" to Schema.integer(description = "Overall OCR extraction accuracy score from 0 to 100."),
                "qualitySummary" to Schema.string(description = "Concise clinical OCR accuracy assessment noting printed vs handwritten clarity and fields to double-check."),
                "fieldAccuracies" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "fieldName" to Schema.string(description = "Key name matching patient field (e.g. mrdNo, fullName, gender, age, ipdId, wardBed, diagnosis, mlcNumber)."),
                            "label" to Schema.string(description = "Human readable label (e.g. MRD / UHID No, Patient Name, Gender)."),
                            "value" to Schema.string(description = "Extracted value."),
                            "confidence" to Schema.integer(description = "Accuracy percentage 0-100 for this specific field."),
                            "note" to Schema.string(description = "Short reason if below 90% (e.g. handwritten, partially blurred, inferred).")
                        ),
                        optionalProperties = listOf("note")
                    ),
                    description = "Per-field accuracy ratings for all key extracted fields."
                ),
                "warnings" to Schema.array(
                    items = Schema.string(),
                    description = "Actionable warnings for fields that are handwritten, smudged, missing, or need human verification."
                )
            ),
            optionalProperties = listOf(
                "age", "ipdId", "abhaId", "department", "unit", "wardBed",
                "admittingDoctor", "admissionDate", "mobile", "address",
                "mlcNumber", "diagnosis", "attendantName", "attendantRelation",
                "attendantMobile", "extraNotes", "warnings"
            )
        )
    }

    private val templateImportSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "name" to Schema.string(description = "Clear hospital form title (e.g. Biochemistry LFT Form, Histopathology Request)."),
                "category" to Schema.enumeration(
                    values = listOf("LAB", "SURGERY", "DISCHARGE", "CUSTOM"),
                    description = "Best matching form category."
                ),
                "hospitalHeader" to Schema.string(description = "Hospital name printed on the top header if visible."),
                "departmentSubtitle" to Schema.string(description = "Department or form subtitle."),
                "description" to Schema.string(description = "Brief one-line description of this form."),
                "footerText" to Schema.string(description = "Signature or footer lines at the bottom of the form."),
                "overallAccuracy" to Schema.integer(description = "Confidence score 0-100 for template structure extraction."),
                "summary" to Schema.string(description = "Brief summary of imported sections and fields."),
                "fields" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "key" to Schema.string(description = "snake_case unique field identifier."),
                            "label" to Schema.string(description = "Display field label."),
                            "fieldType" to Schema.enumeration(
                                values = listOf("TEXT", "MULTILINE", "NUMBER", "CHECKBOX", "DATE"),
                                description = "Input widget type."
                            ),
                            "unit" to Schema.string(description = "Unit or normal reference range if shown (e.g. mg/dL (0.2-1.2))."),
                            "defaultValue" to Schema.string(description = "Pre-filled default value if any."),
                            "section" to Schema.string(description = "Section heading grouping this field."),
                            "patientMapping" to Schema.string(description = "Optional patient field key to auto-fill: diagnosis, admittingDoctor, wardBed, admissionDate, mlcNumber, extraNotes, or empty string.")
                        ),
                        optionalProperties = listOf("unit", "defaultValue", "patientMapping")
                    ),
                    description = "Ordered list of form fields excluding standard header demographics like Patient Name/MRD/Gender."
                )
            ),
            optionalProperties = listOf("hospitalHeader", "departmentSubtitle", "description", "footerText")
        )
    }

    private val patientOcrModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = patientOcrSchema
            }
        )
    }

    private val templateImportModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = templateImportSchema
            }
        )
    }

    suspend fun extractPatientFromImage(bitmap: Bitmap): Result<OcrExtractionResult> {
        return try {
            val scaled = downscaleForInference(bitmap)
            val prompt = """
                You are a clinical medical records OCR engine specializing in hospital admission tickets, HMIS slips, case sheets, lab slips, and discharge cards.
                Carefully read BOTH printed text AND handwritten notes/stamps from this patient document image.
                Extract:
                - MRD / UHID / MRN number into mrdNo (if both UHID and IPD exist, put UHID/MRD in mrdNo and IPD in ipdId).
                - Full patient name into fullName.
                - Normalized gender (Male, Female, Other, or Unknown).
                - Age, department, unit, ward/bed, admitting doctor, admission date/time, mobile, address, MLC/Police-Case/EPR number, diagnosis, attendant details, and any handwritten notes/stamps in extraNotes.
                - Provide honest per-field accuracy percentages (0-100) in fieldAccuracies, an overallAccuracy score (0-100), a clear qualitySummary explaining extraction accuracy, and actionable warnings for any uncertain or handwritten fields.
            """.trimIndent()

            val response = withTimeout(AI_TIMEOUT_MS) {
                patientOcrModel.generateContent(
                    content {
                        image(scaled)
                        text(prompt)
                    }
                )
            }
            val jsonText = response.text ?: throw IllegalStateException("OCR model returned an empty response.")
            Result.success(parsePatientOcrJson(jsonText))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image OCR timed out", e)
            Result.failure(Exception("OCR scan timed out after 60 seconds. Please retry with a clearer photo."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image OCR failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to scan patient document."))
        }
    }

    suspend fun extractPatientFromText(rawText: String): Result<OcrExtractionResult> {
        return try {
            require(rawText.isNotBlank()) { "Please enter or paste patient text to extract." }
            val prompt = """
                You are a clinical medical records parser.
                Extract structured hospital patient details from the following raw ward note, handover message, or HMIS text.
                Rate confidence (0-100) honestly for each extracted field and provide an overallAccuracy score, qualitySummary, and warnings for missing or ambiguous required fields (especially MRD/UHID, Patient Name, and Gender).

                INPUT TEXT:
                $rawText
            """.trimIndent()

            val response = withTimeout(AI_TIMEOUT_MS) {
                patientOcrModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("AI text parser returned an empty response.")
            Result.success(parsePatientOcrJson(jsonText))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Text extraction timed out", e)
            Result.failure(Exception("Text extraction timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Text extraction failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to parse patient text."))
        }
    }

    suspend fun importTemplateFromImage(bitmap: Bitmap): Result<TemplateImportResult> {
        return try {
            val scaled = downscaleForInference(bitmap)
            val prompt = """
                You are a hospital clinical form template designer.
                Analyze this photograph of a medical/hospital form (such as LFT, RFT, CBC, Surgery Specimen, Consent, or HMIS Discharge Card).
                Convert its structure into a clean, reusable form template with hospitalHeader, departmentSubtitle, form name, category, footerText, and ordered fields grouped by section.
                Do NOT duplicate standard patient identification fields (Name, MRD/UHID, Age, Sex, Ward) since WardForms automatically renders a Patient Demographics Header Box on every printed PDF.
                Include units and normal reference ranges in the 'unit' property for lab parameters.
            """.trimIndent()

            val response = withTimeout(AI_TIMEOUT_MS) {
                templateImportModel.generateContent(
                    content {
                        image(scaled)
                        text(prompt)
                    }
                )
            }
            val jsonText = response.text ?: throw IllegalStateException("Template scanner returned an empty response.")
            Result.success(parseTemplateImportJson(jsonText))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Template image import timed out", e)
            Result.failure(Exception("Template import timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Template image import failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to import form template from image."))
        }
    }

    suspend fun importTemplateFromText(rawTemplateText: String): Result<TemplateImportResult> {
        return try {
            require(rawTemplateText.isNotBlank()) { "Please paste or load form template text first." }
            val prompt = """
                You are a hospital clinical form template parser.
                Convert the following pasted form outline, CSV/JSON specification, or clinical checklist into a clean reusable WardForms template.
                Extract the form title, category (LAB, SURGERY, DISCHARGE, or CUSTOM), hospital header if mentioned, footerText, and ordered fields with appropriate fieldTypes (TEXT, MULTILINE, NUMBER, CHECKBOX, DATE) and units/reference ranges.
                Exclude standard patient demographics (MRD, Name, Age, Sex) since WardForms prints those automatically in the header block.

                TEMPLATE INPUT:
                $rawTemplateText
            """.trimIndent()

            val response = withTimeout(AI_TIMEOUT_MS) {
                templateImportModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Template parser returned an empty response.")
            Result.success(parseTemplateImportJson(jsonText))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Template text import timed out", e)
            Result.failure(Exception("Template import timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Template text import failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to parse form template."))
        }
    }

    private fun parsePatientOcrJson(jsonString: String): OcrExtractionResult {
        val json = JSONObject(jsonString)
        val overallAccuracy = json.optInt("overallAccuracy", 75).coerceIn(0, 100)

        val draft = PatientRecord(
            mrdNo = json.optString("mrdNo", "").trim(),
            fullName = json.optString("fullName", "").trim(),
            gender = json.optString("gender", "Unknown").trim().ifBlank { "Unknown" },
            age = json.optString("age", "").trim(),
            ipdId = json.optString("ipdId", "").trim(),
            abhaId = json.optString("abhaId", "").trim(),
            department = json.optString("department", "").trim(),
            unit = json.optString("unit", "").trim(),
            wardBed = json.optString("wardBed", "").trim(),
            admittingDoctor = json.optString("admittingDoctor", "").trim(),
            admissionDate = json.optString("admissionDate", "").trim(),
            mobile = json.optString("mobile", "").trim(),
            address = json.optString("address", "").trim(),
            mlcNumber = json.optString("mlcNumber", "").trim(),
            diagnosis = json.optString("diagnosis", "").trim(),
            attendantName = json.optString("attendantName", "").trim(),
            attendantRelation = json.optString("attendantRelation", "").trim(),
            attendantMobile = json.optString("attendantMobile", "").trim(),
            extraNotes = json.optString("extraNotes", "").trim(),
            ocrConfidence = overallAccuracy.toLong()
        )

        val fieldAccuracies = mutableListOf<FieldAccuracy>()
        val faArray = json.optJSONArray("fieldAccuracies")
        if (faArray != null) {
            for (i in 0 until faArray.length()) {
                val item = faArray.optJSONObject(i) ?: continue
                val value = item.optString("value", "").trim()
                val label = item.optString("label", item.optString("fieldName", "Field")).trim()
                fieldAccuracies.add(
                    FieldAccuracy(
                        fieldName = item.optString("fieldName", ""),
                        label = label,
                        value = value,
                        confidence = item.optInt("confidence", overallAccuracy).coerceIn(0, 100),
                        note = item.optString("note", "").trim()
                    )
                )
            }
        }

        val warnings = mutableListOf<String>()
        val warnArray = json.optJSONArray("warnings")
        if (warnArray != null) {
            for (i in 0 until warnArray.length()) {
                val w = warnArray.optString(i, "").trim()
                if (w.isNotBlank()) warnings.add(w)
            }
        }
        if (draft.mrdNo.isBlank()) warnings.add("MRD / UHID number was not detected — please enter manually.")
        if (draft.fullName.isBlank()) warnings.add("Patient name was not detected — please enter manually.")

        val summary = json.optString("qualitySummary", "").trim().ifBlank {
            "Extraction completed with $overallAccuracy% estimated accuracy. Review highlighted fields before saving."
        }

        return OcrExtractionResult(
            draftPatient = draft,
            overallAccuracy = overallAccuracy,
            qualitySummary = summary,
            fieldAccuracies = fieldAccuracies,
            warnings = warnings.distinct()
        )
    }

    private fun parseTemplateImportJson(jsonString: String): TemplateImportResult {
        val json = JSONObject(jsonString)
        val name = json.optString("name", "Imported Clinical Form").trim().ifBlank { "Imported Clinical Form" }
        val category = FormCategory.fromRaw(json.optString("category", "CUSTOM")).name
        val slug = name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "imported_form" }.take(56)

        val fields = mutableListOf<TemplateField>()
        val arr = json.optJSONArray("fields")
        if (arr != null) {
            for (i in 0 until minOf(arr.length(), 60)) {
                val f = arr.optJSONObject(i) ?: continue
                val label = f.optString("label", "").trim()
                if (label.isBlank()) continue
                val key = f.optString("key", "")
                    .trim()
                    .ifBlank { label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_') }
                    .ifBlank { "field_${i + 1}" }
                fields.add(
                    TemplateField(
                        key = key.take(40),
                        label = label.take(100),
                        fieldType = FieldType.fromRaw(f.optString("fieldType", "TEXT")).name,
                        unit = f.optString("unit", "").trim().take(80),
                        defaultValue = f.optString("defaultValue", "").trim().take(300),
                        section = f.optString("section", "Clinical Fields").trim().ifBlank { "Clinical Fields" }.take(80),
                        patientMapping = f.optString("patientMapping", "").trim().take(40)
                    )
                )
            }
        }

        val draftTemplate = FormTemplate(
            templateKey = slug,
            name = name.take(120),
            category = category,
            hospitalHeader = json.optString("hospitalHeader", DefaultTemplates.DEFAULT_HOSPITAL_HEADER)
                .trim()
                .ifBlank { DefaultTemplates.DEFAULT_HOSPITAL_HEADER }
                .take(240),
            departmentSubtitle = json.optString("departmentSubtitle", "").trim().take(160),
            description = json.optString("description", "Imported clinical form template").trim().take(300),
            footerText = json.optString("footerText", "Prepared By: ________________   Authorized Sign: ________________")
                .trim()
                .take(300),
            builtIn = false,
            fields = fields
        )

        val accuracy = json.optInt("overallAccuracy", 85).coerceIn(0, 100)
        val summary = json.optString("summary", "Imported ${fields.size} fields into $name.")
        return TemplateImportResult(draftTemplate, accuracy, summary)
    }

    private fun downscaleForInference(bitmap: Bitmap, maxDim: Int = 1600): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / longest.toFloat()
        val targetW = (bitmap.width * ratio).roundToInt().coerceAtLeast(1)
        val targetH = (bitmap.height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}
