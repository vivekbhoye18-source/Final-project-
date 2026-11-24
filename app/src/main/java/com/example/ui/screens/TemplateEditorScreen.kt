package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FieldType
import com.example.data.model.FormCategory
import com.example.data.model.FormTemplate
import com.example.data.model.TemplateField
import com.example.ui.components.ClinicalSectionHeader
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.OnEmeraldContainer

private val PATIENT_MAPPING_OPTIONS = listOf(
    "" to "None (Manual entry)",
    "diagnosis" to "Diagnosis",
    "wardBed" to "Ward / Bed",
    "department" to "Department",
    "unit" to "Unit",
    "admittingDoctor" to "Admitting Doctor",
    "admissionDate" to "Admission Date",
    "mlcNumber" to "MLC / EPR No",
    "ipdId" to "IPD ID",
    "abhaId" to "ABHA ID",
    "mobile" to "Patient Mobile",
    "address" to "Address",
    "attendantName" to "Attendant Name",
    "extraNotes" to "Extra Ward Notes"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TemplateEditorScreen(
    initialTemplate: FormTemplate?,
    importBannerMessage: String?,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: (FormTemplate) -> Unit
) {
    BackHandler(onBack = onBack)

    var name by remember(initialTemplate) { mutableStateOf(initialTemplate?.name.orEmpty()) }
    var category by remember(initialTemplate) {
        mutableStateOf(FormCategory.fromRaw(initialTemplate?.category))
    }
    var hospitalHeader by remember(initialTemplate) {
        mutableStateOf(initialTemplate?.hospitalHeader.orEmpty().ifBlank { "HOSPITAL CLINICAL SERVICES" })
    }
    var departmentSubtitle by remember(initialTemplate) {
        mutableStateOf(initialTemplate?.departmentSubtitle.orEmpty())
    }
    var description by remember(initialTemplate) {
        mutableStateOf(initialTemplate?.description.orEmpty())
    }
    var footerText by remember(initialTemplate) {
        mutableStateOf(
            initialTemplate?.footerText.orEmpty()
                .ifBlank { "Doctor / Signatory: _______________________     Date & Stamp: _______________" }
        )
    }
    var fields by remember(initialTemplate) {
        mutableStateOf(initialTemplate?.fields.orEmpty())
    }

    var errorMsg by remember { mutableStateOf<String?>(null) }
    var editingFieldIndex by remember { mutableStateOf<Int?>(null) } // null = closed, -1 = new, >=0 = edit

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (initialTemplate?.id.isNullOrBlank()) "Create / Import Format" else "Edit Form Format")
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
            if (!importBannerMessage.isNullOrBlank()) {
                item(key = "import_banner") {
                    Surface(
                        color = EmeraldContainer,
                        contentColor = OnEmeraldContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = importBannerMessage,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            if (errorMsg != null) {
                item(key = "error") {
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

            item(key = "meta_header") {
                ClinicalSectionHeader("Form Header & Print Layout")
            }

            item(key = "name") {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Form Title *") },
                    placeholder = { Text("e.g., Liver Function Test (LFT) Requisition") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("template_editor_name")
                )
            }

            item(key = "category") {
                Column {
                    Text("Form Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FormCategory.entries.forEach { option ->
                            FilterChip(
                                selected = category == option,
                                onClick = { category = option },
                                label = { Text(option.label) }
                            )
                        }
                    }
                }
            }

            item(key = "hospital") {
                OutlinedTextField(
                    value = hospitalHeader,
                    onValueChange = { hospitalHeader = it },
                    label = { Text("Hospital / Institution Print Header") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item(key = "subtitle") {
                OutlinedTextField(
                    value = departmentSubtitle,
                    onValueChange = { departmentSubtitle = it },
                    label = { Text("Department / Lab Subtitle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item(key = "desc") {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Short Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item(key = "footer") {
                OutlinedTextField(
                    value = footerText,
                    onValueChange = { footerText = it },
                    label = { Text("PDF Footer / Signature Line") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item(key = "fields_header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ClinicalSectionHeader(
                        title = "Form Fields (${fields.size})",
                        subtitle = "Reorder, customize parameters, units, or patient auto-fill links.",
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(
                        onClick = { editingFieldIndex = -1 },
                        modifier = Modifier.testTag("add_template_field_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Field")
                    }
                }
            }

            itemsIndexed(fields, key = { index, field -> "${index}_${field.key}_${field.label}" }) { index, field ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = field.label,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val meta = listOfNotNull(
                                field.section.takeIf { it.isNotBlank() },
                                FieldType.fromRaw(field.fieldType).label,
                                field.unit.takeIf { it.isNotBlank() }?.let { "Unit: $it" },
                                field.patientMapping.takeIf { it.isNotBlank() }?.let { "Auto-fill: $it" }
                            ).joinToString("  •  ")
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                if (index > 0) {
                                    val mutable = fields.toMutableList()
                                    val moved = mutable.removeAt(index)
                                    mutable.add(index - 1, moved)
                                    fields = mutable
                                }
                            },
                            enabled = index > 0,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move field up", modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = {
                                if (index < fields.lastIndex) {
                                    val mutable = fields.toMutableList()
                                    val moved = mutable.removeAt(index)
                                    mutable.add(index + 1, moved)
                                    fields = mutable
                                }
                            },
                            enabled = index < fields.lastIndex,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move field down", modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { editingFieldIndex = index },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit field", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = {
                                fields = fields.toMutableList().apply { removeAt(index) }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove field", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item(key = "save_btn") {
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            errorMsg = "Please enter a Form Title."
                            return@Button
                        }
                        if (fields.isEmpty()) {
                            errorMsg = "Add at least one field to this form template."
                            return@Button
                        }
                        errorMsg = null
                        val cleanKey = initialTemplate?.templateKey?.ifBlank {
                            name.lowercase().replace(Regex("[^a-z0-9]+"), "_").take(36)
                        } ?: name.lowercase().replace(Regex("[^a-z0-9]+"), "_").take(36)

                        onSave(
                            (initialTemplate ?: FormTemplate()).copy(
                                templateKey = cleanKey,
                                name = name.trim(),
                                category = category.name,
                                hospitalHeader = hospitalHeader.trim(),
                                departmentSubtitle = departmentSubtitle.trim(),
                                description = description.trim(),
                                footerText = footerText.trim(),
                                fields = fields
                            )
                        )
                    },
                    enabled = !isSaving,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("template_editor_save_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save Form Template")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    val activeIdx = editingFieldIndex
    if (activeIdx != null) {
        val existingField = if (activeIdx >= 0 && activeIdx < fields.size) fields[activeIdx] else null
        TemplateFieldEditorDialog(
            initialField = existingField,
            onDismiss = { editingFieldIndex = null },
            onConfirm = { updated ->
                fields = if (activeIdx >= 0 && activeIdx < fields.size) {
                    fields.toMutableList().apply { set(activeIdx, updated) }
                } else {
                    fields + updated
                }
                editingFieldIndex = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateFieldEditorDialog(
    initialField: TemplateField?,
    onDismiss: () -> Unit,
    onConfirm: (TemplateField) -> Unit
) {
    var label by remember(initialField) { mutableStateOf(initialField?.label.orEmpty()) }
    var fieldType by remember(initialField) { mutableStateOf(FieldType.fromRaw(initialField?.fieldType)) }
    var unit by remember(initialField) { mutableStateOf(initialField?.unit.orEmpty()) }
    var defaultValue by remember(initialField) { mutableStateOf(initialField?.defaultValue.orEmpty()) }
    var section by remember(initialField) { mutableStateOf(initialField?.section.orEmpty().ifBlank { "General" }) }
    var patientMapping by remember(initialField) { mutableStateOf(initialField?.patientMapping.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialField == null) "Add Form Field" else "Edit Form Field") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Field Label *") },
                    placeholder = { Text("e.g., Total Bilirubin / Specimen Site") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it },
                    label = { Text("Section Heading") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Field Input Type", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldType.entries.forEach { ft ->
                        FilterChip(
                            selected = fieldType == ft,
                            onClick = { fieldType = ft },
                            label = { Text(ft.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit / Reference Range (optional)") },
                    placeholder = { Text("e.g., mg/dL (0.2-1.2)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = defaultValue,
                    onValueChange = { defaultValue = it },
                    label = { Text("Default Value (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Auto-Fill from Patient Database", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PATIENT_MAPPING_OPTIONS.forEach { (key, title) ->
                        FilterChip(
                            selected = patientMapping.equals(key, ignoreCase = true),
                            onClick = { patientMapping = key },
                            label = { Text(title) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedLabel = label.trim()
                    if (trimmedLabel.isNotBlank()) {
                        val safeKey = initialField?.key?.ifBlank {
                            trimmedLabel.lowercase().replace(Regex("[^a-z0-9]+"), "_").take(36)
                        } ?: trimmedLabel.lowercase().replace(Regex("[^a-z0-9]+"), "_").take(36)
                        onConfirm(
                            TemplateField(
                                key = safeKey,
                                label = trimmedLabel,
                                fieldType = fieldType.name,
                                unit = unit.trim(),
                                defaultValue = defaultValue.trim(),
                                section = section.trim().ifBlank { "General" },
                                patientMapping = patientMapping.trim()
                            )
                        )
                    }
                },
                enabled = label.isNotBlank()
            ) {
                Text("Save Field")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
