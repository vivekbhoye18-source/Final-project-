package com.example.data

import com.example.data.model.FilledFieldEntry
import com.example.data.model.FilledFormRecord
import com.example.data.model.FormCategory
import com.example.data.model.FormTemplate
import com.example.data.model.PatientRecord
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class WardRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private val allowedGenders = setOf("Male", "Female", "Other", "Unknown")

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

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

    fun observePatients(userId: String): Flow<List<PatientRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$uid/patients"
        emitAll(
            db.collection("users").document(uid).collection("patients")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PatientRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun savePatient(patient: PatientRecord): Result<String> {
        val uidPreview = auth.currentUser?.uid ?: "unauthenticated"
        var docPath = "users/$uidPreview/patients"
        return try {
            val uid = requireUserId()
            val mrdNo = patient.mrdNo.trim().take(64)
            val fullName = patient.fullName.trim().take(160)
            require(mrdNo.isNotBlank()) { "MRD / UHID number is required." }
            require(fullName.isNotBlank()) { "Patient full name is required." }

            val col = db.collection("users").document(uid).collection("patients")
            val isUpdate = patient.id.isNotBlank()
            val docRef = if (isUpdate) col.document(patient.id) else col.document()
            docPath = docRef.path

            val baseFields = mutableMapOf<String, Any?>(
                "mrdNo" to mrdNo,
                "fullName" to fullName,
                "gender" to normalizeGender(patient.gender),
                "age" to patient.age.trim().take(40),
                "ipdId" to patient.ipdId.trim().take(64),
                "abhaId" to patient.abhaId.trim().take(64),
                "department" to patient.department.trim().take(100),
                "unit" to patient.unit.trim().take(60),
                "wardBed" to patient.wardBed.trim().take(100),
                "admittingDoctor" to patient.admittingDoctor.trim().take(120),
                "admissionDate" to patient.admissionDate.trim().take(64),
                "mobile" to patient.mobile.trim().take(32),
                "address" to patient.address.trim().take(400),
                "mlcNumber" to patient.mlcNumber.trim().take(80),
                "diagnosis" to patient.diagnosis.trim().take(500),
                "attendantName" to patient.attendantName.trim().take(120),
                "attendantRelation" to patient.attendantRelation.trim().take(60),
                "attendantMobile" to patient.attendantMobile.trim().take(32),
                "extraNotes" to patient.extraNotes.trim().take(1000),
                "ocrConfidence" to patient.ocrConfidence?.coerceIn(0L, 100L),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (isUpdate) {
                @Suppress("UNCHECKED_CAST")
                val updatePayload = baseFields.filterValues { it != null } as Map<String, Any>
                docRef.update(updatePayload).await()
            } else {
                baseFields["userId"] = uid
                baseFields["createdAt"] = FieldValue.serverTimestamp()
                @Suppress("UNCHECKED_CAST")
                val createPayload = baseFields.filterValues { it != null } as Map<String, Any>
                docRef.set(createPayload).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val op = if (patient.id.isNotBlank()) OperationType.UPDATE else OperationType.CREATE
            handleFirestoreError(e, op, docPath)
            Result.failure(e)
        }
    }

    suspend fun deletePatient(patientId: String): Result<Unit> {
        val uidPreview = auth.currentUser?.uid ?: "unauthenticated"
        val docPath = "users/$uidPreview/patients/$patientId"
        return try {
            val uid = requireUserId()
            require(patientId.isNotBlank()) { "Patient ID is required." }
            db.collection("users").document(uid).collection("patients")
                .document(patientId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- TEMPLATES ---

    fun observeTemplates(userId: String): Flow<List<FormTemplate>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$uid/templates"
        emitAll(
            db.collection("users").document(uid).collection("templates")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FormTemplate::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<FormTemplate> { it.builtIn }
                            .thenBy { it.name.lowercase() }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun seedDefaultTemplatesIfEmpty(userId: String): Result<Unit> {
        val path = "users/$userId/templates"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val col = db.collection("users").document(uid).collection("templates")
            val existing = col.limit(1).get().await()
            if (!existing.isEmpty) return Result.success(Unit)

            for (tpl in DefaultTemplates.builtInTemplates()) {
                val docId = "tpl_${tpl.templateKey}"
                val payload = mapOf(
                    "userId" to uid,
                    "templateKey" to tpl.templateKey.take(64),
                    "name" to tpl.name.take(120),
                    "category" to FormCategory.fromRaw(tpl.category).name,
                    "hospitalHeader" to tpl.hospitalHeader.take(240),
                    "departmentSubtitle" to tpl.departmentSubtitle.take(160),
                    "description" to tpl.description.take(300),
                    "footerText" to tpl.footerText.take(300),
                    "builtIn" to tpl.builtIn,
                    "fields" to tpl.fields.take(60).map { it.toFirestoreMap() },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                col.document(docId).set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveTemplate(template: FormTemplate): Result<String> {
        val uidPreview = auth.currentUser?.uid ?: "unauthenticated"
        var docPath = "users/$uidPreview/templates"
        return try {
            val uid = requireUserId()
            val name = template.name.trim().take(120)
            require(name.isNotBlank()) { "Template name is required." }

            val slug = template.templateKey.trim()
                .ifBlank { name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "custom_form" } }
                .take(64)

            val col = db.collection("users").document(uid).collection("templates")
            val isUpdate = template.id.isNotBlank()
            val docRef = if (isUpdate) col.document(template.id) else col.document()
            docPath = docRef.path

            val fieldMaps = template.fields.take(60).map { it.toFirestoreMap() }

            val mutableFields = mutableMapOf<String, Any>(
                "templateKey" to slug,
                "name" to name,
                "category" to FormCategory.fromRaw(template.category).name,
                "hospitalHeader" to template.hospitalHeader.trim().take(240),
                "departmentSubtitle" to template.departmentSubtitle.trim().take(160),
                "description" to template.description.trim().take(300),
                "footerText" to template.footerText.trim().take(300),
                "builtIn" to template.builtIn,
                "fields" to fieldMaps,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (isUpdate) {
                docRef.update(mutableFields).await()
            } else {
                mutableFields["userId"] = uid
                mutableFields["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(mutableFields).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val op = if (template.id.isNotBlank()) OperationType.UPDATE else OperationType.CREATE
            handleFirestoreError(e, op, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTemplate(templateId: String): Result<Unit> {
        val uidPreview = auth.currentUser?.uid ?: "unauthenticated"
        val docPath = "users/$uidPreview/templates/$templateId"
        return try {
            val uid = requireUserId()
            require(templateId.isNotBlank()) { "Template ID is required." }
            db.collection("users").document(uid).collection("templates")
                .document(templateId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- FILLED FORMS (RECORDS) ---

    fun observeFilledForms(userId: String): Flow<List<FilledFormRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$uid/filledForms"
        emitAll(
            db.collection("users").document(uid).collection("filledForms")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FilledFormRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveFilledForm(record: FilledFormRecord): Result<FilledFormRecord> {
        val uidPreview = auth.currentUser?.uid ?: "unauthenticated"
        var docPath = "users/$uidPreview/filledForms"
        return try {
            val uid = requireUserId()
            require(record.patientId.isNotBlank()) { "Patient selection is required." }
            require(record.patientName.isNotBlank()) { "Patient name is required." }
            require(record.patientMrdNo.isNotBlank()) { "Patient MRD number is required." }
            require(record.templateId.isNotBlank()) { "Template selection is required." }
            require(record.templateName.isNotBlank()) { "Template title is required." }

            val col = db.collection("users").document(uid).collection("filledForms")
            val isUpdate = record.id.isNotBlank()
            val docRef = if (isUpdate) col.document(record.id) else col.document()
            docPath = docRef.path

            val entryMaps = record.entries.take(60).map { it.toFirestoreMap() }

            val mutableFields = mutableMapOf<String, Any>(
                "patientId" to record.patientId.trim().take(128),
                "patientName" to record.patientName.trim().take(160),
                "patientMrdNo" to record.patientMrdNo.trim().take(64),
                "patientGender" to record.patientGender.trim().take(32),
                "patientAge" to record.patientAge.trim().take(40),
                "patientWardBed" to record.patientWardBed.trim().take(100),
                "patientDepartment" to record.patientDepartment.trim().take(100),
                "patientDoctor" to record.patientDoctor.trim().take(120),
                "patientIpdId" to record.patientIpdId.trim().take(64),
                "patientAdmissionDate" to record.patientAdmissionDate.trim().take(64),
                "patientMlcNumber" to record.patientMlcNumber.trim().take(80),
                "templateId" to record.templateId.trim().take(128),
                "templateName" to record.templateName.trim().take(120),
                "templateCategory" to FormCategory.fromRaw(record.templateCategory).name,
                "hospitalHeader" to record.hospitalHeader.trim().take(240),
                "departmentSubtitle" to record.departmentSubtitle.trim().take(160),
                "footerText" to record.footerText.trim().take(300),
                "entries" to entryMaps,
                "notes" to record.notes.trim().take(1000),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (isUpdate) {
                docRef.update(mutableFields).await()
            } else {
                mutableFields["userId"] = uid
                mutableFields["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(mutableFields).await()
            }
            Result.success(record.copy(id = docRef.id, userId = uid))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val op = if (record.id.isNotBlank()) OperationType.UPDATE else OperationType.CREATE
            handleFirestoreError(e, op, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteFilledForm(formId: String): Result<Unit> {
        val uidPreview = auth.currentUser?.uid ?: "unauthenticated"
        val docPath = "users/$uidPreview/filledForms/$formId"
        return try {
            val uid = requireUserId()
            require(formId.isNotBlank()) { "Form ID is required." }
            db.collection("users").document(uid).collection("filledForms")
                .document(formId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
