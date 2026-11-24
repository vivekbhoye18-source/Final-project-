package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.OcrAiService
import com.example.data.WardRepository
import com.example.data.model.FilledFormRecord
import com.example.data.model.FormTemplate
import com.example.data.model.OcrExtractionResult
import com.example.data.model.PatientRecord
import com.example.data.model.TemplateImportResult
import com.example.data.model.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface OcrProcessState {
    data object Idle : OcrProcessState
    data class Scanning(val modeLabel: String) : OcrProcessState
    data class Success(val result: OcrExtractionResult) : OcrProcessState
    data class Error(val message: String) : OcrProcessState
}

sealed interface TemplateImportState {
    data object Idle : TemplateImportState
    data object Importing : TemplateImportState
    data class Success(val result: TemplateImportResult) : TemplateImportState
    data class Error(val message: String) : TemplateImportState
}

class WardViewModel(
    private val repository: WardRepository,
    private val ocrService: OcrAiService
) : ViewModel() {

    val patientsState: StateFlow<UiState<List<PatientRecord>>> = repository.observePatients()
        .map<List<PatientRecord>, UiState<List<PatientRecord>>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing patients", error)
            emit(UiState.Error(error.message ?: "Failed to load patient database."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val templatesState: StateFlow<UiState<List<FormTemplate>>> = repository.observeTemplates()
        .map<List<FormTemplate>, UiState<List<FormTemplate>>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing templates", error)
            emit(UiState.Error(error.message ?: "Failed to load form templates."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val filledFormsState: StateFlow<UiState<List<FilledFormRecord>>> = repository.observeFilledForms()
        .map<List<FilledFormRecord>, UiState<List<FilledFormRecord>>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing filled forms", error)
            emit(UiState.Error(error.message ?: "Failed to load saved form records."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    private val _ocrState = MutableStateFlow<OcrProcessState>(OcrProcessState.Idle)
    val ocrState: StateFlow<OcrProcessState> = _ocrState.asStateFlow()

    private val _templateImportState = MutableStateFlow<TemplateImportState>(TemplateImportState.Idle)
    val templateImportState: StateFlow<TemplateImportState> = _templateImportState.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.seedDefaultTemplatesIfEmpty()
        }
    }

    fun scanPatientImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _ocrState.value = OcrProcessState.Scanning("Scanning document image with Gemini OCR...")
            val result = ocrService.extractPatientFromImage(bitmap)
            result.fold(
                onSuccess = { extracted ->
                    _ocrState.value = OcrProcessState.Success(extracted)
                },
                onFailure = { err ->
                    _ocrState.value = OcrProcessState.Error(
                        err.localizedMessage ?: "OCR extraction failed."
                    )
                }
            )
        }
    }

    fun parsePatientText(rawText: String) {
        viewModelScope.launch {
            _ocrState.value = OcrProcessState.Scanning("Extracting patient details from text...")
            val result = ocrService.extractPatientFromText(rawText)
            result.fold(
                onSuccess = { extracted ->
                    _ocrState.value = OcrProcessState.Success(extracted)
                },
                onFailure = { err ->
                    _ocrState.value = OcrProcessState.Error(
                        err.localizedMessage ?: "Text parsing failed."
                    )
                }
            )
        }
    }

    fun clearOcrState() {
        _ocrState.value = OcrProcessState.Idle
    }

    fun importTemplateFromImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _templateImportState.value = TemplateImportState.Importing
            val result = ocrService.importTemplateFromImage(bitmap)
            result.fold(
                onSuccess = { imported ->
                    _templateImportState.value = TemplateImportState.Success(imported)
                },
                onFailure = { err ->
                    _templateImportState.value = TemplateImportState.Error(
                        err.localizedMessage ?: "Template scan failed."
                    )
                }
            )
        }
    }

    fun importTemplateFromText(rawText: String) {
        viewModelScope.launch {
            _templateImportState.value = TemplateImportState.Importing
            val result = ocrService.importTemplateFromText(rawText)
            result.fold(
                onSuccess = { imported ->
                    _templateImportState.value = TemplateImportState.Success(imported)
                },
                onFailure = { err ->
                    _templateImportState.value = TemplateImportState.Error(
                        err.localizedMessage ?: "Template import failed."
                    )
                }
            )
        }
    }

    fun clearTemplateImportState() {
        _templateImportState.value = TemplateImportState.Idle
    }

    fun savePatient(patient: PatientRecord, onSaved: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.savePatient(patient)
            _isSaving.value = false
            result.fold(
                onSuccess = { id ->
                    _events.tryEmit(if (patient.id.isBlank()) "Patient added to database." else "Patient details updated.")
                    onSaved(id)
                },
                onFailure = { err ->
                    _events.tryEmit(err.localizedMessage ?: "Failed to save patient.")
                }
            )
        }
    }

    fun deletePatient(patientId: String) {
        viewModelScope.launch {
            val result = repository.deletePatient(patientId)
            result.fold(
                onSuccess = { _events.tryEmit("Patient removed from registry.") },
                onFailure = { err -> _events.tryEmit(err.localizedMessage ?: "Failed to delete patient.") }
            )
        }
    }

    fun saveTemplate(template: FormTemplate, onSaved: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.saveTemplate(template)
            _isSaving.value = false
            result.fold(
                onSuccess = { id ->
                    _events.tryEmit(if (template.id.isBlank()) "Form template saved." else "Form template updated.")
                    onSaved(id)
                },
                onFailure = { err ->
                    _events.tryEmit(err.localizedMessage ?: "Failed to save template.")
                }
            )
        }
    }

    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            val result = repository.deleteTemplate(templateId)
            result.fold(
                onSuccess = { _events.tryEmit("Template deleted.") },
                onFailure = { err -> _events.tryEmit(err.localizedMessage ?: "Failed to delete template.") }
            )
        }
    }

    fun restoreDefaultTemplates() {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.restoreDefaultTemplates()
            _isSaving.value = false
            result.fold(
                onSuccess = { _events.tryEmit("Standard hospital templates restored.") },
                onFailure = { err -> _events.tryEmit(err.localizedMessage ?: "Failed to restore templates.") }
            )
        }
    }

    fun saveFilledForm(record: FilledFormRecord, onSaved: (FilledFormRecord) -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.saveFilledForm(record)
            _isSaving.value = false
            result.fold(
                onSuccess = { saved ->
                    _events.tryEmit("Form record saved.")
                    onSaved(saved)
                },
                onFailure = { err ->
                    _events.tryEmit(err.localizedMessage ?: "Failed to save form record.")
                }
            )
        }
    }

    fun deleteFilledForm(formId: String) {
        viewModelScope.launch {
            val result = repository.deleteFilledForm(formId)
            result.fold(
                onSuccess = { _events.tryEmit("Form record deleted.") },
                onFailure = { err -> _events.tryEmit(err.localizedMessage ?: "Failed to delete record.") }
            )
        }
    }

    fun emitMessage(message: String) {
        _events.tryEmit(message)
    }

    private companion object {
        const val TAG = "WardViewModel"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
