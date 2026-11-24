package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.data.model.FormCategory
import com.example.data.model.FormTemplate
import com.example.data.model.TemplateImportResult
import com.example.data.model.UiState
import com.example.ui.components.CategoryBadge
import com.example.ui.components.ClinicalSectionHeader
import com.example.ui.components.WardEmptyState
import com.example.ui.components.loadBitmapFromUri
import com.example.viewmodel.TemplateImportState

@Composable
fun TemplatesScreen(
    templatesState: UiState<List<FormTemplate>>,
    importState: TemplateImportState,
    isSaving: Boolean,
    onUseTemplate: (FormTemplate) -> Unit,
    onCreateTemplate: () -> Unit,
    onEditTemplate: (FormTemplate) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onRestoreDefaults: () -> Unit,
    onImportTemplateImage: (Bitmap) -> Unit,
    onImportTemplateText: (String) -> Unit,
    onTemplateImported: (TemplateImportResult) -> Unit,
    onClearImportState: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf<FormCategory?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var pastedTemplateText by remember { mutableStateOf("") }
    var templatePendingDelete by remember { mutableStateOf<FormTemplate?>(null) }

    LaunchedEffect(importState) {
        if (importState is TemplateImportState.Success) {
            showImportDialog = false
            pastedTemplateText = ""
            onTemplateImported(importState.result)
            onClearImportState()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bitmap = loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                showImportDialog = false
                onImportTemplateImage(bitmap)
            } else {
                onShowMessage("Could not read selected template image.")
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (text.isNotBlank()) {
                    showImportDialog = false
                    onImportTemplateText(text)
                } else {
                    onShowMessage("Selected file is empty.")
                }
            } catch (_: Exception) {
                onShowMessage("Unable to read template file.")
            }
        }
    }

    val allTemplates = (templatesState as? UiState.Success)?.data.orEmpty()
    val filteredTemplates = remember(allTemplates, selectedCategory) {
        if (selectedCategory == null) {
            allTemplates
        } else {
            allTemplates.filter { FormCategory.fromRaw(it.category) == selectedCategory }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "header") {
            ClinicalSectionHeader(
                title = "Clinical Form Templates",
                subtitle = "Edit LFT, RFT, CBC, Surgery Specimen & HMIS Discharge formats or import custom hospital forms."
            )
        }

        // Action Bar
        item(key = "actions") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onCreateTemplate,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("new_template_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New Format", maxLines = 1)
                }
                FilledTonalButton(
                    onClick = { showImportDialog = true },
                    enabled = importState !is TemplateImportState.Importing,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("import_template_button")
                ) {
                    Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import Form", maxLines = 1)
                }
            }
        }

        // Import Status Banner
        if (importState is TemplateImportState.Importing) {
            item(key = "importing") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Analyzing Template Layout with AI...",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Detecting hospital headers, lab parameters, units & patient auto-fill mappings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        if (importState is TemplateImportState.Error) {
            item(key = "import_error") {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = importState.message,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onClearImportState) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }

        // Category Filter Row + Restore Action
        item(key = "categories") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All (${allTemplates.size})") }
                        )
                    }
                    items(FormCategory.entries) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = if (selectedCategory == category) null else category },
                            label = { Text(category.label) }
                        )
                    }
                }
            }
        }

        item(key = "restore_row") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${filteredTemplates.size} form template(s)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onRestoreDefaults,
                    enabled = !isSaving,
                    modifier = Modifier.testTag("restore_default_templates_button")
                ) {
                    Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Restore Standard Forms")
                }
            }
        }

        when (templatesState) {
            is UiState.Loading -> {
                item(key = "loading") {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp)
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
                            text = templatesState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            is UiState.Success -> {
                if (filteredTemplates.isEmpty()) {
                    item(key = "empty") {
                        WardEmptyState(
                            icon = Icons.Rounded.Description,
                            title = "No templates in this category",
                            subtitle = "Create a custom hospital form layout, import one from a photo or file, or restore standard LFT/RFT/CBC/Surgery/HMIS forms.",
                            action = {
                                OutlinedButton(onClick = onRestoreDefaults) {
                                    Text("Restore Built-in Forms")
                                }
                            }
                        )
                    }
                } else {
                    items(filteredTemplates, key = { it.id }) { template ->
                        TemplateCard(
                            template = template,
                            onUse = { onUseTemplate(template) },
                            onEdit = { onEditTemplate(template) },
                            onDelete = { templatePendingDelete = template }
                        )
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Clinical Form Template") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Upload a photo of a hospital form, open a text/JSON template file, or paste fields below. AI will build an editable template automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilledTonalButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("import_template_photo_button")
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Photo", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("text/*", "application/json", "*/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("import_template_file_button")
                        ) {
                            Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("File", maxLines = 1)
                        }
                    }

                    OutlinedTextField(
                        value = pastedTemplateText,
                        onValueChange = { pastedTemplateText = it },
                        label = { Text("Or paste form layout / field list") },
                        placeholder = { Text("e.g., Serum Electrolytes: Na+ (135-145), K+ (3.5-5.1), Cl- (98-107), HCO3-...") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_template_text_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pastedTemplateText.isNotBlank()) {
                            onImportTemplateText(pastedTemplateText)
                        }
                    },
                    enabled = pastedTemplateText.isNotBlank(),
                    modifier = Modifier.testTag("import_template_parse_button")
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Parse Text")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (templatePendingDelete != null) {
        AlertDialog(
            onDismissRequest = { templatePendingDelete = null },
            title = { Text("Delete Form Template?") },
            text = {
                Text("Delete \"${templatePendingDelete!!.name}\"? You can restore standard built-in hospital forms anytime.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTemplate(templatePendingDelete!!.id)
                        templatePendingDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { templatePendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TemplateCard(
    template: FormTemplate,
    onUse: () -> Unit,
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
            .testTag("template_card_${template.templateKey.ifBlank { template.id }}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CategoryBadge(categoryRaw = template.category)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${template.fields.size} fields",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                if (template.builtIn) {
                    Text(
                        text = "STANDARD",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = template.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (template.hospitalHeader.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = template.hospitalHeader,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (template.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledTonalButton(
                    onClick = onUse,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("use_template_button_${template.templateKey.ifBlank { template.id }}")
                ) {
                    Icon(Icons.Rounded.Assignment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Fill & Print")
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("edit_template_button_${template.templateKey.ifBlank { template.id }}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit form template format",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete form template",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
