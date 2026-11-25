package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

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

@Entity(tableName = "patients")
data class PatientRecord(
    @PrimaryKey val id: String = "",
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
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
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

data class TemplateField(
    val key: String = "",
    val label: String = "",
    val fieldType: String = FieldType.TEXT.name,
    val unit: String = "",
    val defaultValue: String = "",
    val section: String = "General",
    val patientMapping: String = ""
)

@Entity(tableName = "form_templates")
data class FormTemplate(
    @PrimaryKey val id: String = "",
    val templateKey: String = "",
    val name: String = "",
    val category: String = FormCategory.CUSTOM.name,
    val hospitalHeader: String = "",
    val departmentSubtitle: String = "",
    val description: String = "",
    val footerText: String = "",
    val builtIn: Boolean = false,
    val fields: List<TemplateField> = emptyList(),
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

data class FilledFieldEntry(
    val key: String = "",
    val label: String = "",
    val value: String = "",
    val unit: String = "",
    val section: String = "General",
    val fieldType: String = FieldType.TEXT.name
)

@Entity(tableName = "filled_forms")
data class FilledFormRecord(
    @PrimaryKey val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val patientMrdNo: String = "",
    val patientGender: String = "",
    val patientAge: String = "",
    val patientWardBed: String = "",
    val patientDepartment: String = "",
    val patientUnit: String = "",
    val patientDoctor: String = "",
    val patientIpdId: String = "",
    val patientAdmissionDate: String = "",
    val patientMlcNumber: String = "",
    val patientAddress: String = "",
    val patientMobile: String = "",
    val templateId: String = "",
    val templateKey: String = "",
    val templateName: String = "",
    val templateCategory: String = FormCategory.CUSTOM.name,
    val hospitalHeader: String = "",
    val departmentSubtitle: String = "",
    val footerText: String = "",
    val entries: List<FilledFieldEntry> = emptyList(),
    val notes: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
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
