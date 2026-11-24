package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class FormCategory(val label: String) {
    LAB("Lab Forms"),
    SURGERY("Surgery / Specimen"),
    DISCHARGE("HMIS Discharge"),
    CUSTOM("Custom Forms");

    companion object {
        fun fromRaw(raw: String?): FormCategory =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: CUSTOM
    }
}

enum class FieldType(val label: String) {
    TEXT("Single Line"),
    MULTILINE("Paragraph"),
    NUMBER("Lab Value / Number"),
    CHECKBOX("Checkbox"),
    DATE("Date / Time");

    companion object {
        fun fromRaw(raw: String?): FieldType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: TEXT
    }
}

@Keep
@IgnoreExtraProperties
data class PatientRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mrdNo: String = "",
    val fullName: String = "",
    val gender: String = "Male",
    val age: String = "",
    val ipdId: String = "",
    val abhaId: String = "",
    val department: String = "",
    val unit: String = "",
    val wardBed: String = "",
    val admittingDoctor: String = "",
    val admissionDate: String = "",
    val mobile: String = "",
    val address: String = "",
    val mlcNumber: String = "",
    val diagnosis: String = "",
    val attendantName: String = "",
    val attendantRelation: String = "",
    val attendantMobile: String = "",
    val extraNotes: String = "",
    val ocrConfidence: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolveMapping(mappingKey: String): String = when (mappingKey.lowercase()) {
        "mrdno", "mrd", "uhid" -> mrdNo
        "fullname", "name" -> fullName
        "gender", "sex" -> gender
        "age" -> age
        "ipdid", "ipd" -> ipdId
        "abhaid", "abha" -> abhaId
        "department", "dept" -> department
        "unit" -> unit
        "wardbed", "ward" -> wardBed
        "admittingdoctor", "doctor" -> admittingDoctor
        "admissiondate" -> admissionDate
        "mobile", "phone" -> mobile
        "address" -> address
        "mlcnumber", "mlc" -> mlcNumber
        "diagnosis" -> diagnosis
        "attendantname" -> attendantName
        "attendantrelation" -> attendantRelation
        "attendantmobile" -> attendantMobile
        "extranotes", "notes" -> extraNotes
        else -> ""
    }
}

@Keep
@IgnoreExtraProperties
data class TemplateField(
    val key: String = "",
    val label: String = "",
    val fieldType: String = FieldType.TEXT.name,
    val unit: String = "",
    val defaultValue: String = "",
    val section: String = "General",
    val patientMapping: String = ""
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "key" to key.trim().ifBlank { label.lowercase().replace(Regex("[^a-z0-9]+"), "_").take(40) },
        "label" to label.trim().take(100),
        "fieldType" to FieldType.fromRaw(fieldType).name,
        "unit" to unit.trim().take(80),
        "defaultValue" to defaultValue.trim().take(300),
        "section" to section.trim().ifBlank { "General" }.take(80),
        "patientMapping" to patientMapping.trim().take(40)
    )
}

@Keep
@IgnoreExtraProperties
data class FormTemplate(
    @DocumentId val id: String = "",
    val userId: String = "",
    val templateKey: String = "",
    val name: String = "",
    val category: String = FormCategory.CUSTOM.name,
    val hospitalHeader: String = "",
    val departmentSubtitle: String = "",
    val description: String = "",
    val footerText: String = "",
    val builtIn: Boolean = false,
    val fields: List<TemplateField> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FilledFieldEntry(
    val key: String = "",
    val label: String = "",
    val value: String = "",
    val unit: String = "",
    val section: String = "General",
    val fieldType: String = FieldType.TEXT.name
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "key" to key.take(64),
        "label" to label.take(100),
        "value" to value.take(1000),
        "unit" to unit.take(80),
        "section" to section.take(80),
        "fieldType" to FieldType.fromRaw(fieldType).name
    )
}

@Keep
@IgnoreExtraProperties
data class FilledFormRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val patientMrdNo: String = "",
    val patientGender: String = "",
    val patientAge: String = "",
    val patientWardBed: String = "",
    val patientDepartment: String = "",
    val patientDoctor: String = "",
    val patientIpdId: String = "",
    val patientAdmissionDate: String = "",
    val patientMlcNumber: String = "",
    val templateId: String = "",
    val templateName: String = "",
    val templateCategory: String = FormCategory.CUSTOM.name,
    val hospitalHeader: String = "",
    val departmentSubtitle: String = "",
    val footerText: String = "",
    val entries: List<FilledFieldEntry> = emptyList(),
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class FieldAccuracy(
    val fieldName: String,
    val label: String,
    val value: String,
    val confidence: Int,
    val note: String = ""
)

data class OcrExtractionResult(
    val draftPatient: PatientRecord,
    val overallAccuracy: Int,
    val qualitySummary: String,
    val fieldAccuracies: List<FieldAccuracy>,
    val warnings: List<String>
)

data class TemplateImportResult(
    val draftTemplate: FormTemplate,
    val overallAccuracy: Int,
    val summary: String
)
