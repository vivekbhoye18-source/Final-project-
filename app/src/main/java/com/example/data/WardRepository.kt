package com.example.data

import com.example.data.local.WardDao
import com.example.data.model.FilledFormRecord
import com.example.data.model.FormCategory
import com.example.data.model.FormTemplate
import com.example.data.model.PatientRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class WardRepository(
    private val dao: WardDao
) {
    private val allowedGenders = setOf("Male", "Female", "Other", "Unknown")

    private fun normalizeGender(raw: String): String {
        val cleaned = raw.trim()
        return when {
            cleaned.equals("male", ignoreCase = true) || cleaned.equals("m", ignoreCase = true) -> "Male"
            cleaned.equals("female", ignoreCase = true) || cleaned.equals("f", ignoreCase = true) -> "Female"
            cleaned.equals("other", ignoreCase = true) -> "Other"
            cleaned in allowedGenders -> cleaned
            else -> "Unknown"
        }
    }

    // --- PATIENTS ---

    fun observePatients(): Flow<List<PatientRecord>> = dao.observePatients()

    suspend fun savePatient(patient: PatientRecord): Result<String> {
        return try {
            val mrdNo = patient.mrdNo.trim().take(64)
            val fullName = patient.fullName.trim().take(160)
            require(mrdNo.isNotBlank()) { "MRD / UHID number is required." }
            require(fullName.isNotBlank()) { "Patient full name is required." }

            val now = System.currentTimeMillis()
            val id = patient.id.ifBlank { "pt_${UUID.randomUUID()}" }
            val existing = if (patient.id.isNotBlank()) dao.getPatientById(id) else null

            val cleanPatient = patient.copy(
                id = id,
                mrdNo = mrdNo,
                fullName = fullName,
                gender = normalizeGender(patient.gender),
                age = patient.age.trim().take(40),
                ipdId = patient.ipdId.trim().take(64),
                abhaId = patient.abhaId.trim().take(64),
                department = patient.department.trim().take(100),
                unit = patient.unit.trim().take(60),
                wardBed = patient.wardBed.trim().take(100),
                admittingDoctor = patient.admittingDoctor.trim().take(120),
                admissionDate = patient.admissionDate.trim().take(64),
                mobile = patient.mobile.trim().take(32),
                address = patient.address.trim().take(400),
                mlcNumber = patient.mlcNumber.trim().take(80),
                diagnosis = patient.diagnosis.trim().take(500),
                attendantName = patient.attendantName.trim().take(120),
                attendantRelation = patient.attendantRelation.trim().take(60),
                attendantMobile = patient.attendantMobile.trim().take(32),
                extraNotes = patient.extraNotes.trim().take(1000),
                ocrConfidence = patient.ocrConfidence?.coerceIn(0L, 100L),
                createdAtMillis = existing?.createdAtMillis?.takeIf { it > 0L } ?: now,
                updatedAtMillis = now
            )

            dao.upsertPatient(cleanPatient)
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePatient(patientId: String): Result<Unit> {
        return try {
            require(patientId.isNotBlank()) { "Patient ID is required." }
            dao.deletePatientById(patientId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- TEMPLATES ---

    fun observeTemplates(): Flow<List<FormTemplate>> = dao.observeTemplates()

    suspend fun seedDefaultTemplatesIfEmpty(): Result<Unit> {
        return try {
            val existingIds = dao.getAllTemplateIds().toSet()
            val now = System.currentTimeMillis()
            val missingBuiltIns = DefaultTemplates.builtInTemplates()
                .map { tpl ->
                    tpl.copy(
                        id = "tpl_${tpl.templateKey}",
                        category = FormCategory.fromRaw(tpl.category).name,
                        createdAtMillis = now,
                        updatedAtMillis = now
                    )
                }
                .filter { it.id !in existingIds }

            if (missingBuiltIns.isNotEmpty()) {
                dao.upsertTemplates(missingBuiltIns)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreDefaultTemplates(): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val builtIns = DefaultTemplates.builtInTemplates().map { tpl ->
                tpl.copy(
                    id = "tpl_${tpl.templateKey}",
                    category = FormCategory.fromRaw(tpl.category).name,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            }
            dao.upsertTemplates(builtIns)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveTemplate(template: FormTemplate): Result<String> {
        return try {
            val name = template.name.trim().take(120)
            require(name.isNotBlank()) { "Template name is required." }

            val slug = template.templateKey.trim()
                .ifBlank {
                    name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "custom_form" }
                }
                .take(64)

            val now = System.currentTimeMillis()
            val id = template.id.ifBlank { "tpl_${UUID.randomUUID()}" }
            val existing = if (template.id.isNotBlank()) dao.getTemplateById(id) else null

            val cleanTemplate = template.copy(
                id = id,
                templateKey = slug,
                name = name,
                category = FormCategory.fromRaw(template.category).name,
                hospitalHeader = template.hospitalHeader.trim().take(240),
                departmentSubtitle = template.departmentSubtitle.trim().take(160),
                description = template.description.trim().take(300),
                footerText = template.footerText.trim().take(300),
                fields = template.fields.take(100),
                createdAtMillis = existing?.createdAtMillis?.takeIf { it > 0L } ?: now,
                updatedAtMillis = now
            )

            dao.upsertTemplate(cleanTemplate)
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTemplate(templateId: String): Result<Unit> {
        return try {
            require(templateId.isNotBlank()) { "Template ID is required." }
            dao.deleteTemplateById(templateId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- FILLED FORMS (RECORDS) ---

    fun observeFilledForms(): Flow<List<FilledFormRecord>> = dao.observeFilledForms()

    suspend fun saveFilledForm(record: FilledFormRecord): Result<FilledFormRecord> {
        return try {
            require(record.patientName.isNotBlank()) { "Patient name is required." }
            require(record.patientMrdNo.isNotBlank()) { "Patient MRD number is required." }
            require(record.templateName.isNotBlank()) { "Template title is required." }

            val now = System.currentTimeMillis()
            val id = record.id.ifBlank { "form_${UUID.randomUUID()}" }
            val existing = if (record.id.isNotBlank()) dao.getFilledFormById(id) else null

            val cleanRecord = record.copy(
                id = id,
                patientId = record.patientId.trim().take(128),
                patientName = record.patientName.trim().take(160),
                patientMrdNo = record.patientMrdNo.trim().take(64),
                patientGender = record.patientGender.trim().take(32),
                patientAge = record.patientAge.trim().take(40),
                patientWardBed = record.patientWardBed.trim().take(100),
                patientDepartment = record.patientDepartment.trim().take(100),
                patientUnit = record.patientUnit.trim().take(60),
                patientDoctor = record.patientDoctor.trim().take(120),
                patientIpdId = record.patientIpdId.trim().take(64),
                patientAdmissionDate = record.patientAdmissionDate.trim().take(64),
                patientMlcNumber = record.patientMlcNumber.trim().take(80),
                patientAddress = record.patientAddress.trim().take(400),
                patientMobile = record.patientMobile.trim().take(32),
                templateId = record.templateId.trim().take(128),
                templateKey = record.templateKey.trim().take(64),
                templateName = record.templateName.trim().take(120),
                templateCategory = FormCategory.fromRaw(record.templateCategory).name,
                hospitalHeader = record.hospitalHeader.trim().take(240),
                departmentSubtitle = record.departmentSubtitle.trim().take(160),
                footerText = record.footerText.trim().take(300),
                entries = record.entries.take(100),
                notes = record.notes.trim().take(1000),
                createdAtMillis = existing?.createdAtMillis?.takeIf { it > 0L } ?: now,
                updatedAtMillis = now
            )

            dao.upsertFilledForm(cleanRecord)
            Result.success(cleanRecord)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFilledForm(formId: String): Result<Unit> {
        return try {
            require(formId.isNotBlank()) { "Form ID is required." }
            dao.deleteFilledFormById(formId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
