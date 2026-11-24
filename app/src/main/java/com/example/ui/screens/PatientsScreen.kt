package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PatientRecord
import com.example.data.model.UiState
import com.example.ui.components.OcrConfidenceChip
import com.example.ui.components.WardEmptyState
import com.example.ui.theme.AlertCoralContainer
import com.example.ui.theme.DeepClinicalTeal
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OnAlertCoralContainer
import com.example.ui.theme.TealBright

@Composable
fun PatientsScreen(
    patientsState: UiState<List<PatientRecord>>,
    totalTemplates: Int,
    totalForms: Int,
    onOpenOcrScan: () -> Unit,
    onAddManualPatient: () -> Unit,
    onEditPatient: (PatientRecord) -> Unit,
    onFillFormForPatient: (PatientRecord) -> Unit,
    onDeletePatient: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var patientPendingDelete by remember { mutableStateOf<PatientRecord?>(null) }

    val allPatients = (patientsState as? UiState.Success)?.data.orEmpty()
    val filteredPatients = remember(allPatients, searchQuery) {
        if (searchQuery.isBlank()) {
            allPatients
        } else {
            val q = searchQuery.trim().lowercase()
            allPatients.filter { p ->
                p.fullName.lowercase().contains(q) ||
                    p.mrdNo.lowercase().contains(q) ||
                    p.ipdId.lowercase().contains(q) ||
                    p.wardBed.lowercase().contains(q) ||
                    p.diagnosis.lowercase().contains(q) ||
                    p.mlcNumber.lowercase().contains(q)
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Clinical Command Hero Banner
        item(key = "hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(Brush.linearGradient(listOf(DeepClinicalTeal, TealBright)))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Patient Database",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Scan slips, manage records & print forms",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalHospital,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(10.dp).size(24.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Stats Strip
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HeroStatPill("Patients", "${allPatients.size}", Modifier.weight(1f))
                        HeroStatPill("Templates", "$totalTemplates", Modifier.weight(1f))
                        HeroStatPill("Saved PDFs", "$totalForms", Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onOpenOcrScan,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = DeepClinicalTeal
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("quick_ocr_scan_button")
                        ) {
                            Icon(Icons.Rounded.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Scan / OCR", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onAddManualPatient,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.65f)),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("add_patient_manual_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Manual Entry", maxLines = 1)
                        }
                    }
                }
            }
        }

        // Search Bar
        item(key = "search") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search MRD/UHID, Name, Ward, IPD, or Diagnosis...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search patients") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("patient_search_input")
            )
        }

        when (patientsState) {
            is UiState.Loading -> {
                item(key = "loading") {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp)
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            is UiState.Error -> {
                item(key = "error") {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = patientsState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            is UiState.Success -> {
                if (filteredPatients.isEmpty()) {
                    item(key = "empty") {
                        WardEmptyState(
                            icon = Icons.Rounded.PersonSearch,
                            title = if (allPatients.isEmpty()) "No patients registered yet" else "No matching patients",
                            subtitle = if (allPatients.isEmpty()) {
                                "Upload an admission ticket photo with AI OCR, paste ward text, or add a patient manually to start printing LFT, RFT, CBC, Surgery, and HMIS Discharge forms."
                            } else {
                                "Try searching by MRD number, patient name, ward, or diagnosis."
                            },
                            action = if (allPatients.isEmpty()) {
                                {
                                    Button(onClick = onOpenOcrScan, modifier = Modifier.testTag("empty_scan_button")) {
                                        Icon(Icons.Rounded.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Scan Admission Ticket")
                                    }
                                }
                            } else null
                        )
                    }
                } else {
                    items(filteredPatients, key = { it.id }) { patient ->
                        PatientCard(
                            patient = patient,
                            onFillForm = { onFillFormForPatient(patient) },
                            onEdit = { onEditPatient(patient) },
                            onDelete = { patientPendingDelete = patient }
                        )
                    }
                }
            }
        }
    }

    if (patientPendingDelete != null) {
        AlertDialog(
            onDismissRequest = { patientPendingDelete = null },
            title = { Text("Delete Patient Record?") },
            text = {
                Text("Remove ${patientPendingDelete!!.fullName} (MRD: ${patientPendingDelete!!.mrdNo}) from your patient database? Previously saved PDFs in Records remain intact.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePatient(patientPendingDelete!!.id)
                        patientPendingDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { patientPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HeroStatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
private fun PatientCard(
    patient: PatientRecord,
    onFillForm: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("patient_card_${patient.mrdNo}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top badges row: MRD + OCR + MLC
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "MRD ${patient.mrdNo}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                if (patient.mlcNumber.isNotBlank()) {
                    Surface(
                        color = AlertCoralContainer,
                        contentColor = OnAlertCoralContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "MLC",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                val conf = patient.ocrConfidence?.toInt()
                if (conf != null && conf > 0) {
                    OcrConfidenceChip(confidence = conf)
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = patient.fullName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            val demographics = listOfNotNull(
                listOf(patient.age, patient.gender).filter { it.isNotBlank() }.joinToString(" • ").takeIf { it.isNotBlank() },
                patient.wardBed.takeIf { it.isNotBlank() },
                patient.department.takeIf { it.isNotBlank() }
            ).joinToString("  |  ")

            if (demographics.isNotBlank()) {
                Text(
                    text = demographics,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (patient.admittingDoctor.isNotBlank() || patient.ipdId.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                val sub = listOfNotNull(
                    patient.admittingDoctor.takeIf { it.isNotBlank() },
                    patient.ipdId.takeIf { it.isNotBlank() }?.let { "IPD $it" }
                ).joinToString("  •  ")
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (patient.diagnosis.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Diagnosis: ${patient.diagnosis}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledTonalButton(
                    onClick = onFillForm,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("fill_form_button_${patient.mrdNo}")
                ) {
                    Icon(Icons.Rounded.Assignment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Fill & Print Form")
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit patient details",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete patient",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
