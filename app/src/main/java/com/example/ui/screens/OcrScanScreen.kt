package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.TextSnippet
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.model.OcrExtractionResult
import com.example.data.model.PatientRecord
import com.example.ui.components.ClinicalSectionHeader
import com.example.ui.components.OcrConfidenceChip
import com.example.ui.components.confidenceTint
import com.example.ui.components.loadBitmapFromUri
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnAmberContainer
import com.example.viewmodel.OcrProcessState
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OcrScanScreen(
    ocrState: OcrProcessState,
    isSaving: Boolean,
    onScanImage: (Bitmap) -> Unit,
    onParseText: (String) -> Unit,
    onResetOcr: () -> Unit,
    onSavePatient: (PatientRecord, Boolean) -> Unit
) {
    val context = LocalContext.current
    var inputMode by remember { mutableIntStateOf(0) } // 0 = Image OCR, 1 = Smart Text Feed
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rawTextInput by remember { mutableStateOf("") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Editable patient draft populated when OCR succeeds
    var draftMrd by remember { mutableStateOf("") }
    var draftName by remember { mutableStateOf("") }
    var draftGender by remember { mutableStateOf("Male") }
    var draftAge by remember { mutableStateOf("") }
    var draftIpd by remember { mutableStateOf("") }
    var draftAbha by remember { mutableStateOf("") }
    var draftDepartment by remember { mutableStateOf("") }
    var draftUnit by remember { mutableStateOf("") }
    var draftWardBed by remember { mutableStateOf("") }
    var draftDoctor by remember { mutableStateOf("") }
    var draftAdmissionDate by remember { mutableStateOf("") }
    var draftMobile by remember { mutableStateOf("") }
    var draftAddress by remember { mutableStateOf("") }
    var draftMlc by remember { mutableStateOf("") }
    var draftDiagnosis by remember { mutableStateOf("") }
    var draftAttendantName by remember { mutableStateOf("") }
    var draftAttendantRelation by remember { mutableStateOf("") }
    var draftAttendantMobile by remember { mutableStateOf("") }
    var draftNotes by remember { mutableStateOf("") }
    var draftConfidence by remember { mutableStateOf<Long?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(ocrState) {
        if (ocrState is OcrProcessState.Success) {
            val p = ocrState.result.draftPatient
            draftMrd = p.mrdNo
            draftName = p.fullName
            draftGender = p.gender.ifBlank { "Male" }
            draftAge = p.age
            draftIpd = p.ipdId
            draftAbha = p.abhaId
            draftDepartment = p.department
            draftUnit = p.unit
            draftWardBed = p.wardBed
            draftDoctor = p.admittingDoctor
            draftAdmissionDate = p.admissionDate
            draftMobile = p.mobile
            draftAddress = p.address
            draftMlc = p.mlcNumber
            draftDiagnosis = p.diagnosis
            draftAttendantName = p.attendantName
            draftAttendantRelation = p.attendantRelation
            draftAttendantMobile = p.attendantMobile
            draftNotes = p.extraNotes
            draftConfidence = ocrState.result.overallAccuracy.toLong()
            validationError = null
        }
    }

    // Zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bmp = loadBitmapFromUri(context, uri)
            if (bmp != null) {
                previewBitmap = bmp
                onScanImage(bmp)
            }
        }
    }

    // Standard Camera Intent via FileProvider (no broad storage permission required)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            val bmp = loadBitmapFromUri(context, uri)
            if (bmp != null) {
                previewBitmap = bmp
                onScanImage(bmp)
            }
        }
    }

    fun launchCameraCapture() {
        val dir = File(context.cacheDir, "scans").apply { mkdirs() }
        val file = File(dir, "scan_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }

    fun buildPatientFromDraft(): PatientRecord? {
        if (draftMrd.isBlank()) {
            validationError = "MRD / UHID number is required before saving."
            return null
        }
        if (draftName.isBlank()) {
            validationError = "Patient full name is required before saving."
            return null
        }
        validationError = null
        return PatientRecord(
            mrdNo = draftMrd.trim(),
            fullName = draftName.trim(),
            gender = draftGender,
            age = draftAge.trim(),
            ipdId = draftIpd.trim(),
            abhaId = draftAbha.trim(),
            department = draftDepartment.trim(),
            unit = draftUnit.trim(),
            wardBed = draftWardBed.trim(),
            admittingDoctor = draftDoctor.trim(),
            admissionDate = draftAdmissionDate.trim(),
            mobile = draftMobile.trim(),
            address = draftAddress.trim(),
            mlcNumber = draftMlc.trim(),
            diagnosis = draftDiagnosis.trim(),
            attendantName = draftAttendantName.trim(),
            attendantRelation = draftAttendantRelation.trim(),
            attendantMobile = draftAttendantMobile.trim(),
            extraNotes = draftNotes.trim(),
            ocrConfidence = draftConfidence
        )
    }

    val fieldScoreMap = remember(ocrState) {
        if (ocrState is OcrProcessState.Success) {
            ocrState.result.fieldAccuracies.associateBy { it.fieldName.lowercase() }
        } else emptyMap()
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "header") {
            ClinicalSectionHeader(
                title = "OCR Document Scan & Smart Feed",
                subtitle = "Upload an admission ticket, lab slip, or paste ward notes to auto-populate the patient database with accuracy verification."
            )
        }

        // Input Mode Switcher
        item(key = "mode_switch") {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = inputMode == 0,
                    onClick = { inputMode = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    icon = { Icon(Icons.Rounded.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("ocr_mode_image_tab")
                ) {
                    Text("Image OCR Scan")
                }
                SegmentedButton(
                    selected = inputMode == 1,
                    onClick = { inputMode = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    icon = { Icon(Icons.Rounded.TextSnippet, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("ocr_mode_text_tab")
                ) {
                    Text("Feed via Text")
                }
            }
        }

        // Source Card: Image Upload OR Text Feed
        item(key = "source_card") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (inputMode == 0) {
                        if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap!!.asImageBitmap(),
                                contentDescription = "Uploaded patient document preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                            Spacer(Modifier.height(12.dp))
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Rounded.DocumentScanner,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Admission Ticket, OPD Slip, or Case Sheet",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Reads both printed tables & handwritten ward notes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = ocrState !is OcrProcessState.Scanning,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("upload_document_photo_button")
                            ) {
                                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Upload Photo")
                            }

                            OutlinedButton(
                                onClick = { launchCameraCapture() },
                                enabled = ocrState !is OcrProcessState.Scanning,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("capture_camera_photo_button")
                            ) {
                                Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Camera")
                            }
                        }

                        if (previewBitmap != null && ocrState !is OcrProcessState.Scanning) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = { onScanImage(previewBitmap!!) },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Re-run OCR Scan")
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = rawTextInput,
                            onValueChange = { rawTextInput = it },
                            label = { Text("Paste or type patient summary / HMIS text") },
                            placeholder = {
                                Text("e.g., UHID 20260173023 Mr. Jalil Shaikh 40Y Male Gen Surgery Unit 6 Emerg Ward 4 Dr. Waqar Ansari MLC EPR 20262/SJP Acute Abdomen")
                            },
                            minLines = 4,
                            maxLines = 8,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ocr_text_feed_input")
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onParseText(rawTextInput) },
                            enabled = rawTextInput.isNotBlank() && ocrState !is OcrProcessState.Scanning,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("extract_from_text_button")
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Extract Patient Details")
                        }
                    }
                }
            }
        }

        // Scanning Progress
        if (ocrState is OcrProcessState.Scanning) {
            item(key = "scanning") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 2.8.dp
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = ocrState.modeLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Extracting MRD/UHID, Name, Gender, Ward, Diagnosis & confidence ratings...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Error Card
        if (ocrState is OcrProcessState.Error) {
            item(key = "error") {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "OCR Extraction Failed",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(text = ocrState.message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Accuracy Feedback Card + Editable Verified Form
        if (ocrState is OcrProcessState.Success) {
            item(key = "accuracy_feedback") {
                OcrAccuracyFeedbackCard(result = ocrState.result)
            }

            item(key = "review_form") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Verify & Edit Extracted Fields",
                                style = MaterialTheme.typography.titleMedium
                            )
                            TextButton(onClick = {
                                previewBitmap = null
                                onResetOcr()
                            }) {
                                Text("Clear")
                            }
                        }

                        if (validationError != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = validationError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        LabeledConfidenceField(
                            label = "MRD / UHID / MRN No *",
                            value = draftMrd,
                            onValueChange = { draftMrd = it },
                            confidence = fieldScoreMap["mrdno"]?.confidence,
                            tag = "ocr_field_mrd"
                        )
                        LabeledConfidenceField(
                            label = "Patient Full Name *",
                            value = draftName,
                            onValueChange = { draftName = it },
                            confidence = fieldScoreMap["fullname"]?.confidence,
                            tag = "ocr_field_name"
                        )

                        // Gender Selector with confidence pill
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Gender *",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                fieldScoreMap["gender"]?.confidence?.let {
                                    OcrConfidenceChip(confidence = it, labelPrefix = "Conf")
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Male", "Female", "Other", "Unknown").forEach { option ->
                                    FilterChip(
                                        selected = draftGender == option,
                                        onClick = { draftGender = option },
                                        label = { Text(option) }
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            LabeledConfidenceField(
                                label = "Age",
                                value = draftAge,
                                onValueChange = { draftAge = it },
                                confidence = fieldScoreMap["age"]?.confidence,
                                modifier = Modifier.weight(1f)
                            )
                            LabeledConfidenceField(
                                label = "IPD ID",
                                value = draftIpd,
                                onValueChange = { draftIpd = it },
                                confidence = fieldScoreMap["ipdid"]?.confidence,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            LabeledConfidenceField(
                                label = "Department",
                                value = draftDepartment,
                                onValueChange = { draftDepartment = it },
                                confidence = fieldScoreMap["department"]?.confidence,
                                modifier = Modifier.weight(1f)
                            )
                            LabeledConfidenceField(
                                label = "Unit",
                                value = draftUnit,
                                onValueChange = { draftUnit = it },
                                confidence = fieldScoreMap["unit"]?.confidence,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        LabeledConfidenceField(
                            label = "Ward / Bed No",
                            value = draftWardBed,
                            onValueChange = { draftWardBed = it },
                            confidence = fieldScoreMap["wardbed"]?.confidence
                        )
                        LabeledConfidenceField(
                            label = "Admitting Doctor",
                            value = draftDoctor,
                            onValueChange = { draftDoctor = it },
                            confidence = fieldScoreMap["admittingdoctor"]?.confidence
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            LabeledConfidenceField(
                                label = "Admission Date",
                                value = draftAdmissionDate,
                                onValueChange = { draftAdmissionDate = it },
                                confidence = fieldScoreMap["admissiondate"]?.confidence,
                                modifier = Modifier.weight(1f)
                            )
                            LabeledConfidenceField(
                                label = "MLC / EPR No",
                                value = draftMlc,
                                onValueChange = { draftMlc = it },
                                confidence = fieldScoreMap["mlcnumber"]?.confidence,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        LabeledConfidenceField(
                            label = "Diagnosis",
                            value = draftDiagnosis,
                            onValueChange = { draftDiagnosis = it },
                            confidence = fieldScoreMap["diagnosis"]?.confidence,
                            singleLine = false
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            LabeledConfidenceField(
                                label = "Mobile",
                                value = draftMobile,
                                onValueChange = { draftMobile = it },
                                confidence = fieldScoreMap["mobile"]?.confidence,
                                modifier = Modifier.weight(1f)
                            )
                            LabeledConfidenceField(
                                label = "ABHA ID",
                                value = draftAbha,
                                onValueChange = { draftAbha = it },
                                confidence = fieldScoreMap["abhaid"]?.confidence,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        LabeledConfidenceField(
                            label = "Address",
                            value = draftAddress,
                            onValueChange = { draftAddress = it },
                            confidence = fieldScoreMap["address"]?.confidence,
                            singleLine = false
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            LabeledConfidenceField(
                                label = "Attendant Name",
                                value = draftAttendantName,
                                onValueChange = { draftAttendantName = it },
                                modifier = Modifier.weight(1f)
                            )
                            LabeledConfidenceField(
                                label = "Relation",
                                value = draftAttendantRelation,
                                onValueChange = { draftAttendantRelation = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        LabeledConfidenceField(
                            label = "Attendant Mobile",
                            value = draftAttendantMobile,
                            onValueChange = { draftAttendantMobile = it }
                        )
                        LabeledConfidenceField(
                            label = "Extra Ward Notes / Handwritten Stamps",
                            value = draftNotes,
                            onValueChange = { draftNotes = it },
                            confidence = fieldScoreMap["extranotes"]?.confidence,
                            singleLine = false
                        )

                        Spacer(Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    buildPatientFromDraft()?.let { onSavePatient(it, false) }
                                },
                                enabled = !isSaving,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("save_ocr_patient_button")
                            ) {
                                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Save Patient")
                            }

                            Button(
                                onClick = {
                                    buildPatientFromDraft()?.let { onSavePatient(it, true) }
                                },
                                enabled = !isSaving,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("save_and_fill_form_button")
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Save & Fill Form")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OcrAccuracyFeedbackCard(result: OcrExtractionResult) {
    val barColor = confidenceTint(result.overallAccuracy)
    val accuracyTier = when {
        result.overallAccuracy >= 85 -> "High Extraction Accuracy"
        result.overallAccuracy >= 60 -> "Moderate Accuracy — Review Highlighted Fields"
        else -> "Low Accuracy — Manual Verification Recommended"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, barColor.copy(alpha = 0.55f)),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ocr_accuracy_feedback_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = accuracyTier,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "AI OCR Accuracy Report",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${result.overallAccuracy}%",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = barColor
                )
            }

            Spacer(Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { result.overallAccuracy / 100f },
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = result.qualitySummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (result.fieldAccuracies.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Field Confidence Breakdown",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    result.fieldAccuracies.take(10).forEach { fa ->
                        OcrConfidenceChip(
                            confidence = fa.confidence,
                            labelPrefix = fa.label.take(14)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = result.warnings.isNotEmpty()) {
                Surface(
                    color = AmberContainer,
                    contentColor = OnAmberContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Verification Notes",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        result.warnings.forEach { warning ->
                            Text(
                                text = "• $warning",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledConfidenceField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    confidence: Int? = null,
    singleLine: Boolean = true,
    tag: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (confidence != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OcrConfidenceChip(confidence = confidence, labelPrefix = "")
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = if (confidence == null) ({ Text(label) }) else null,
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else 4,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (tag != null) Modifier.testTag(tag) else Modifier)
        )
    }
}
