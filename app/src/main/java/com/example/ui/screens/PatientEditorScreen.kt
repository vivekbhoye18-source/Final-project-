package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.PatientRecord
import com.example.ui.components.ClinicalSectionHeader

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PatientEditorScreen(
    initialPatient: PatientRecord?,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: (PatientRecord) -> Unit
) {
    BackHandler(onBack = onBack)

    var mrdNo by remember(initialPatient) { mutableStateOf(initialPatient?.mrdNo.orEmpty()) }
    var fullName by remember(initialPatient) { mutableStateOf(initialPatient?.fullName.orEmpty()) }
    var gender by remember(initialPatient) { mutableStateOf(initialPatient?.gender?.ifBlank { "Male" } ?: "Male") }
    var age by remember(initialPatient) { mutableStateOf(initialPatient?.age.orEmpty()) }
    var ipdId by remember(initialPatient) { mutableStateOf(initialPatient?.ipdId.orEmpty()) }
    var abhaId by remember(initialPatient) { mutableStateOf(initialPatient?.abhaId.orEmpty()) }
    var department by remember(initialPatient) { mutableStateOf(initialPatient?.department.orEmpty()) }
    var unit by remember(initialPatient) { mutableStateOf(initialPatient?.unit.orEmpty()) }
    var wardBed by remember(initialPatient) { mutableStateOf(initialPatient?.wardBed.orEmpty()) }
    var admittingDoctor by remember(initialPatient) { mutableStateOf(initialPatient?.admittingDoctor.orEmpty()) }
    var admissionDate by remember(initialPatient) { mutableStateOf(initialPatient?.admissionDate.orEmpty()) }
    var mobile by remember(initialPatient) { mutableStateOf(initialPatient?.mobile.orEmpty()) }
    var address by remember(initialPatient) { mutableStateOf(initialPatient?.address.orEmpty()) }
    var mlcNumber by remember(initialPatient) { mutableStateOf(initialPatient?.mlcNumber.orEmpty()) }
    var diagnosis by remember(initialPatient) { mutableStateOf(initialPatient?.diagnosis.orEmpty()) }
    var attendantName by remember(initialPatient) { mutableStateOf(initialPatient?.attendantName.orEmpty()) }
    var attendantRelation by remember(initialPatient) { mutableStateOf(initialPatient?.attendantRelation.orEmpty()) }
    var attendantMobile by remember(initialPatient) { mutableStateOf(initialPatient?.attendantMobile.orEmpty()) }
    var extraNotes by remember(initialPatient) { mutableStateOf(initialPatient?.extraNotes.orEmpty()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialPatient == null) "Add Patient" else "Edit Patient Details") },
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
            if (errorMsg != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                    }
                }
            }

            item { ClinicalSectionHeader("Identity & Hospital IDs") }
            item {
                OutlinedTextField(
                    value = mrdNo,
                    onValueChange = { mrdNo = it },
                    label = { Text("MRD / UHID / MRN Number *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("patient_editor_mrd")
                )
            }
            item {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Patient Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("patient_editor_name")
                )
            }
            item {
                Column {
                    Text("Gender *", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Male", "Female", "Other", "Unknown").forEach { option ->
                            FilterChip(
                                selected = gender == option,
                                onClick = { gender = option },
                                label = { Text(option) }
                            )
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("Age (e.g. 40Y)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ipdId,
                        onValueChange = { ipdId = it },
                        label = { Text("IPD ID") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = abhaId,
                    onValueChange = { abhaId = it },
                    label = { Text("ABHA ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { ClinicalSectionHeader("Ward, Department & Admission") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = department,
                        onValueChange = { department = it },
                        label = { Text("Department") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = wardBed,
                    onValueChange = { wardBed = it },
                    label = { Text("Ward / Bed No.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = admittingDoctor,
                    onValueChange = { admittingDoctor = it },
                    label = { Text("Admitting Doctor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = admissionDate,
                        onValueChange = { admissionDate = it },
                        label = { Text("Admission Date") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = mlcNumber,
                        onValueChange = { mlcNumber = it },
                        label = { Text("MLC / EPR No") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item { ClinicalSectionHeader("Clinical & Contact Details") }
            item {
                OutlinedTextField(
                    value = diagnosis,
                    onValueChange = { diagnosis = it },
                    label = { Text("Provisional / Working Diagnosis") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Patient Mobile") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Residential Address") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = attendantName,
                        onValueChange = { attendantName = it },
                        label = { Text("Attendant Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = attendantRelation,
                        onValueChange = { attendantRelation = it },
                        label = { Text("Relation") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = attendantMobile,
                    onValueChange = { attendantMobile = it },
                    label = { Text("Attendant Mobile") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = extraNotes,
                    onValueChange = { extraNotes = it },
                    label = { Text("Extra Notes / Ward Remarks") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = {
                        if (mrdNo.isBlank()) {
                            errorMsg = "Please enter the patient's MRD / UHID number."
                            return@Button
                        }
                        if (fullName.isBlank()) {
                            errorMsg = "Please enter the patient's full name."
                            return@Button
                        }
                        errorMsg = null
                        onSave(
                            (initialPatient ?: PatientRecord()).copy(
                                mrdNo = mrdNo.trim(),
                                fullName = fullName.trim(),
                                gender = gender,
                                age = age.trim(),
                                ipdId = ipdId.trim(),
                                abhaId = abhaId.trim(),
                                department = department.trim(),
                                unit = unit.trim(),
                                wardBed = wardBed.trim(),
                                admittingDoctor = admittingDoctor.trim(),
                                admissionDate = admissionDate.trim(),
                                mobile = mobile.trim(),
                                address = address.trim(),
                                mlcNumber = mlcNumber.trim(),
                                diagnosis = diagnosis.trim(),
                                attendantName = attendantName.trim(),
                                attendantRelation = attendantRelation.trim(),
                                attendantMobile = attendantMobile.trim(),
                                extraNotes = extraNotes.trim()
                            )
                        )
                    },
                    enabled = !isSaving,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("patient_editor_save_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (initialPatient == null) "Save Patient to Database" else "Update Patient Record")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
