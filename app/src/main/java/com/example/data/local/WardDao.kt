package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.FilledFormRecord
import com.example.data.model.FormTemplate
import com.example.data.model.PatientRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface WardDao {

    // Patients
    @Query("SELECT * FROM patients ORDER BY updatedAtMillis DESC, createdAtMillis DESC")
    fun observePatients(): Flow<List<PatientRecord>>

    @Query("SELECT * FROM patients WHERE id = :id LIMIT 1")
    suspend fun getPatientById(id: String): PatientRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPatient(patient: PatientRecord)

    @Query("DELETE FROM patients WHERE id = :id")
    suspend fun deletePatientById(id: String)

    // Form Templates
    @Query("SELECT * FROM form_templates ORDER BY builtIn DESC, name COLLATE NOCASE ASC")
    fun observeTemplates(): Flow<List<FormTemplate>>

    @Query("SELECT COUNT(*) FROM form_templates")
    suspend fun countTemplates(): Int

    @Query("SELECT * FROM form_templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateById(id: String): FormTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplate(template: FormTemplate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplates(templates: List<FormTemplate>)

    @Query("DELETE FROM form_templates WHERE id = :id")
    suspend fun deleteTemplateById(id: String)

    // Filled Form Records
    @Query("SELECT * FROM filled_forms ORDER BY updatedAtMillis DESC, createdAtMillis DESC")
    fun observeFilledForms(): Flow<List<FilledFormRecord>>

    @Query("SELECT * FROM filled_forms WHERE id = :id LIMIT 1")
    suspend fun getFilledFormById(id: String): FilledFormRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFilledForm(record: FilledFormRecord)

    @Query("DELETE FROM filled_forms WHERE id = :id")
    suspend fun deleteFilledFormById(id: String)
}
