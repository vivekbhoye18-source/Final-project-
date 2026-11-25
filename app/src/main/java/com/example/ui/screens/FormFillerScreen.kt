package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DefaultTemplates
import com.example.data.model.FieldType
import com.example.data.model.FilledFieldEntry
import com.example.data.model.FilledFormRecord
import com.example.data.model.FormTemplate
import com.example.data.model.PatientRecord
import com.example.pdf.PdfPrintAndShareHelper
import com.example.ui.components.CategoryBadge
import com.example.ui.components.ClinicalSectionHeader
import com.example.ui.components.PatientDemographicsBanner
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.OnEmeraldContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormFillerScreen(
    initialPatient: PatientRecord?,
    initialTemplate: FormTemplate?,
    existingRecord: FilledFormRecord?,
    allPatients: List<PatientRecord>,
    allTemplates: List<FormTemplate>,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSaveRecord: (FilledFormRecord, (FilledFormRecord) -> Unit) -> Unit,
    onShowMessage: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current

    var selectedPatient by remember(initialPatient, existingRecord, allPatients) {
        mutableStateOf(
            initialPatient
                ?: existingRecord?.let { rec -> allPatients.firstOrNull { it.id == rec.patientId } }
                ?: allPatients.firstOrNull()
        )
    }

    var selectedTemplate by remember(initialTemplate, existingRecord, allTemplates) {
        mutableStateOf(
            initialTemplate
                ?: existingRecord?.let { rec -> allTemplates.firstOrNull { it.id == rec.templateId } }
                ?: allTemplates.firstOrNull()
        )
    }

    var savedRecordId by remember(existingRecord) { mutableStateOf(existingRecord?.id.orEmpty()) }
    var clinicalNotes by remember(existingRecord) { mutableStateOf(existingRecord?.notes.orEmpty()) }
    val fieldValues = remember { mutableStateMapOf<String, String>() }

    var showPatientPicker by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }
    var pendingPdfRecord by remember { mutableStateOf<FilledFormRecord?>(null) }

    // Populate fields whenever patient, template, or existing record changes
    LaunchedEffect(selectedPatient?.id, selectedTemplate?.id, existingRecord?.id) {
        fieldValues.clear()
        if (existingRecord != null &&
            (selectedTemplate == null || selectedTemplate?.id == existingRecord.templateId)
        ) {
            existingRecord.entries.forEach { entry ->
                fieldValues[entry.key] = entry.value
            }
        } else {
            val tpl = selectedTemplate
            val pt = selectedPatient
            tpl?.fields?.forEach { field ->
                val mapped = if (pt != null && field.patientMapping.isNotBlank()) {
                    pt.resolveMapping(field.patientMapping)
                } else ""
                fieldValues[field.key] = mapped.ifBlank { field.defaultValue }
            }
        }
    }

    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val rec = pendingPdfRecord
        if (uri != null && rec != null) {
            val res = PdfPrintAndShareHelper.writePdfToUri(context, rec, uri)
            res.fold(
                onSuccess = { onShowMessage("PDF saved successfully.") },
                onFailure = { onShowMessage(it.localizedMessage ?: "Failed to save PDF.") }
            )
        }
        pendingPdfRecord = null
    }

    fun buildCurrentRecord(): FilledFormRecord? {
        val pt = selectedPatient
        val tpl = selectedTemplate
        if (existingRecord == null && (pt == null || tpl == null)) {
            onShowMessage("Please select both a Patient and a Form Template.")
            return null
        }

        val entries = if (tpl != null) {
            tpl.fields.map { f ->
                FilledFieldEntry(
                    key = f.key,
                    label = f.label,
                    value = fieldValues[f.key].orEmpty().trim(),
                    unit = f.unit,
                    section = f.section,
                    fieldType = f.fieldType
                )
            }
        } else {
            existingRecord!!.entries.map { e ->
                e.copy(value = fieldValues[e.key].orEmpty().trim())
            }
        }

        return FilledFormRecord(
            id = savedRecordId,
            patientId = pt?.id ?: existingRecord?.patientId.orEmpty(),
            patientName = pt?.fullName ?: existingRecord?.patientName.orEmpty(),
            patientMrdNo = pt?.mrdNo ?: existingRecord?.patientMrdNo.orEmpty(),
            patientGender = pt?.gender ?: existingRecord?.patientGender.orEmpty(),
            patientAge = pt?.age ?: existingRecord?.patientAge.orEmpty(),
            patientWardBed = pt?.wardBed ?: existingRecord?.patientWardBed.orEmpty(),
            patientDepartment = pt?.department ?: existingRecord?.patientDepartment.orEmpty(),
            patientUnit = pt?.unit ?: existingRecord?.patientUnit.orEmpty(),
            patientDoctor = pt?.admittingDoctor ?: existingRecord?.patientDoctor.orEmpty(),
            patientIpdId = pt?.ipdId ?: existingRecord?.patientIpdId.orEmpty(),
            patientAdmissionDate = pt?.admissionDate ?: existingRecord?.patientAdmissionDate.orEmpty(),
            patientMlcNumber = pt?.mlcNumber ?: existingRecord?.patientMlcNumber.orEmpty(),
            patientAddress = pt?.address ?: existingRecord?.patientAddress.orEmpty(),
            patientMobile = pt?.mobile ?: existingRecord?.patientMobile.orEmpty(),
            templateId = tpl?.id ?: existingRecord?.templateId.orEmpty(),
            templateKey = tpl?.templateKey ?: existingRecord?.templateKey.orEmpty(),
            templateName = tpl?.name ?: existingRecord?.templateName.orEmpty(),
            templateCategory = tpl?.category ?: existingRecord?.templateCategory.orEmpty(),
            hospitalHeader = tpl?.hospitalHeader ?: existingRecord?.hospitalHeader.orEmpty(),
            departmentSubtitle = tpl?.departmentSubtitle ?: existingRecord?.departmentSubtitle.orEmpty(),
            footerText = tpl?.footerText ?: existingRecord?.footerText.orEmpty(),
            entries = entries,
            notes = clinicalNotes.trim()
        )
    }

    val sections = remember(selectedTemplate, existingRecord) {
        val sourceFields = selectedTemplate?.fields ?: existingRecord?.entries?.map {
            com.example.data.model.TemplateField(
                key = it.key,
                label = it.label,
                fieldType = it.fieldType,
                unit = it.unit,
                section = it.section
            )
        }.orEmpty()
        sourceFields.groupBy { it.section.ifBlank { "General" } }.entries.toList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selectedTemplate?.name ?: existingRecord?.templateName ?: "Fill Clinical Form",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Patient Selector Card
            item(key = "patient_selector") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ClinicalSectionHeader("Patient Details", modifier = Modifier.weight(1f))
                        if (existingRecord == null && allPatients.isNotEmpty()) {
                            TextButton(
                                onClick = { showPatientPicker = true },
                                modifier = Modifier.testTag("change_patient_button")
                            ) {
                                Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Change")
                            }
                        }
                    }

                    if (selectedPatient != null) {
                        PatientDemographicsBanner(patient = selectedPatient!!)
                    } else if (existingRecord != null) {
                        PatientDemographicsBanner(
                            patient = PatientRecord(
                                mrdNo = existingRecord.patientMrdNo,
                                fullName = existingRecord.patientName,
                                gender = existingRecord.patientGender,
                                age = existingRecord.patientAge,
                                wardBed = existingRecord.patientWardBed,
                                department = existingRecord.patientDepartment,
                                ipdId = existingRecord.patientIpdId
                            )
                        )
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No patients in database yet. Add or scan a patient first to auto-fill demographics.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // Template Selector Card
            item(key = "template_selector") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedTemplate?.name ?: existingRecord?.templateName ?: "Select Form Template",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            val cat = selectedTemplate?.category ?: existingRecord?.templateCategory
                            val activeKey = selectedTemplate?.templateKey ?: existingRecord?.templateKey.orEmpty()
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                if (!cat.isNullOrBlank()) {
                                    CategoryBadge(categoryRaw = cat)
                                }
                                if (activeKey in DefaultTemplates.EXACT_HOSPITAL_LAYOUT_KEYS) {
                                    Surface(
                                        color = EmeraldContainer,
                                        contentColor = OnEmeraldContainer,
                                        shape = MaterialTheme.shapes.extraSmall
                                    ) {
                                        Text(
                                            text = "Exact A4 Print Form",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (existingRecord == null && allTemplates.isNotEmpty()) {
                            TextButton(
                                onClick = { showTemplatePicker = true },
                                modifier = Modifier.testTag("change_template_button")
                            ) {
                                Text("Switch Form")
                            }
                        }
                    }
                }
            }

            // Grouped Form Inputs
            sections.forEach { (sectionTitle, sectionFields) ->
                item(key = "section_$sectionTitle") {
                    Spacer(Modifier.height(4.dp))
                    ClinicalSectionHeader(title = sectionTitle)
                }

                items(sectionFields, key = { "${sectionTitle}_${it.key}" }) { field ->
                    val currentVal = fieldValues[field.key].orEmpty()
                    val type = FieldType.fromRaw(field.fieldType)
                    when (type) {
                        FieldType.CHECKBOX -> {
                            val checked = currentVal.equals("Yes", ignoreCase = true) ||
                                currentVal.equals("true", ignoreCase = true)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { fieldValues[field.key] = if (checked) "No" else "Yes" }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { isChecked ->
                                            fieldValues[field.key] = if (isChecked) "Yes" else "No"
                                        }
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Column {
                                        Text(field.label, style = MaterialTheme.typography.bodyMedium)
                                        if (field.unit.isNotBlank()) {
                                            Text(
                                                field.unit,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        FieldType.MULTILINE -> {
                            OutlinedTextField(
                                value = currentVal,
                                onValueChange = { fieldValues[field.key] = it },
                                label = { Text(field.label) },
                                supportingText = if (field.unit.isNotBlank()) ({ Text(field.unit) }) else null,
                                minLines = 3,
                                maxLines = 6,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        else -> {
                            OutlinedTextField(
                                value = currentVal,
                                onValueChange = { fieldValues[field.key] = it },
                                label = { Text(field.label) },
                                supportingText = if (field.unit.isNotBlank()) ({ Text("Ref / Unit: ${field.unit}") }) else null,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Clinical Notes
            item(key = "notes") {
                Spacer(Modifier.height(4.dp))
                ClinicalSectionHeader("Additional Remarks")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = clinicalNotes,
                    onValueChange = { clinicalNotes = it },
                    label = { Text("Clinical Notes / Special Instructions") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Action Buttons: Save, Print, Export PDF, Share PDF
            item(key = "actions") {
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            buildCurrentRecord()?.let { record ->
                                onSaveRecord(record) { saved ->
                                    savedRecordId = saved.id
                                }
                            }
                        },
                        enabled = !isSaving,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("form_save_record_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (savedRecordId.isBlank()) "Save Clinical Form Record" else "Update Saved Record")
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilledTonalButton(
                            onClick = {
                                buildCurrentRecord()?.let { rec ->
                                    PdfPrintAndShareHelper.printForm(context, rec)
                                }
                            },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("form_print_button")
                        ) {
                            Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Print", maxLines = 1)
                        }

                        FilledTonalButton(
                            onClick = {
                                buildCurrentRecord()?.let { rec ->
                                    pendingPdfRecord = rec
                                    exportPdfLauncher.launch(PdfPrintAndShareHelper.suggestedPdfFilename(rec))
                                }
                            },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("form_export_pdf_button")
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export PDF", maxLines = 1)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            buildCurrentRecord()?.let { rec ->
                                PdfPrintAndShareHelper.sharePdf(context, rec)
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("form_share_pdf_button")
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share A4 Clinical PDF")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showPatientPicker) {
        AlertDialog(
            onDismissRequest = { showPatientPicker = false },
            title = { Text("Select Patient") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(allPatients, key = { it.id }) { patient ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPatient = patient
                                    showPatientPicker = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(patient.fullName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "MRD ${patient.mrdNo}  •  ${patient.age} ${patient.gender}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPatientPicker = false }) { Text("Close") }
            }
        )
    }

    if (showTemplatePicker) {
        AlertDialog(
            onDismissRequest = { showTemplatePicker = false },
            title = { Text("Select Clinical Form Template") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(allTemplates, key = { it.id }) { tpl ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTemplate = tpl
                                    showTemplatePicker = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(tpl.name, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${tpl.fields.size} fields  •  ${tpl.hospitalHeader}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplatePicker = false }) { Text("Close") }
            }
        )
    }
}
