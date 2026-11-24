package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.FilledFormRecord
import com.example.data.model.FormCategory
import com.example.data.model.UiState
import com.example.pdf.PdfPrintAndShareHelper
import com.example.ui.components.CategoryBadge
import com.example.ui.components.ClinicalSectionHeader
import com.example.ui.components.WardEmptyState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordsScreen(
    recordsState: UiState<List<FilledFormRecord>>,
    onOpenRecord: (FilledFormRecord) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<FormCategory?>(null) }
    var recordPendingDelete by remember { mutableStateOf<FilledFormRecord?>(null) }
    var recordPendingExport by remember { mutableStateOf<FilledFormRecord?>(null) }

    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val rec = recordPendingExport
        if (uri != null && rec != null) {
            val res = PdfPrintAndShareHelper.writePdfToUri(context, rec, uri)
            res.fold(
                onSuccess = { onShowMessage("Clinical PDF exported successfully.") },
                onFailure = { onShowMessage(it.localizedMessage ?: "PDF export failed.") }
            )
        }
        recordPendingExport = null
    }

    val allRecords = (recordsState as? UiState.Success)?.data.orEmpty()
    val filteredRecords = remember(allRecords, searchQuery, selectedCategory) {
        allRecords.filter { rec ->
            val matchesCategory = selectedCategory == null || FormCategory.fromRaw(rec.templateCategory) == selectedCategory
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                rec.patientName.lowercase().contains(q) ||
                rec.patientMrdNo.lowercase().contains(q) ||
                rec.templateName.lowercase().contains(q) ||
                rec.patientWardBed.lowercase().contains(q)
            matchesCategory && matchesQuery
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "header") {
            ClinicalSectionHeader(
                title = "Saved Forms & PDF Archive",
                subtitle = "Reopen, print, export A4 PDFs, or share completed lab slips, surgery forms, and HMIS discharge cards."
            )
        }

        item(key = "search") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by patient name, MRD, form type, or ward...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search records") },
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
                    .testTag("records_search_input")
            )
        }

        item(key = "categories") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All (${allRecords.size})") }
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

        when (recordsState) {
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
                            text = recordsState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            is UiState.Success -> {
                if (filteredRecords.isEmpty()) {
                    item(key = "empty") {
                        WardEmptyState(
                            icon = Icons.Rounded.FolderOpen,
                            title = if (allRecords.isEmpty()) "No saved forms yet" else "No matching form records",
                            subtitle = if (allRecords.isEmpty()) {
                                "Select a patient or template and tap Save to build your searchable on-device archive of LFT, RFT, CBC, Surgery Specimen, and HMIS Discharge PDFs."
                            } else {
                                "Try clearing your search or category filter."
                            }
                        )
                    }
                } else {
                    items(filteredRecords, key = { it.id }) { record ->
                        RecordCard(
                            record = record,
                            onOpen = { onOpenRecord(record) },
                            onPrint = { PdfPrintAndShareHelper.printForm(context, record) },
                            onExportPdf = {
                                recordPendingExport = record
                                exportPdfLauncher.launch(PdfPrintAndShareHelper.suggestedPdfFilename(record))
                            },
                            onShare = { PdfPrintAndShareHelper.sharePdf(context, record) },
                            onDelete = { recordPendingDelete = record }
                        )
                    }
                }
            }
        }
    }

    if (recordPendingDelete != null) {
        AlertDialog(
            onDismissRequest = { recordPendingDelete = null },
            title = { Text("Delete Form Record?") },
            text = {
                Text("Remove \"${recordPendingDelete!!.templateName}\" for ${recordPendingDelete!!.patientName} from your archive?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteRecord(recordPendingDelete!!.id)
                        recordPendingDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RecordCard(
    record: FilledFormRecord,
    onOpen: () -> Unit,
    onPrint: () -> Unit,
    onExportPdf: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val dateLabel = remember(record.updatedAtMillis, record.createdAtMillis) {
        val ts = record.updatedAtMillis.takeIf { it > 0L } ?: record.createdAtMillis
        if (ts > 0L) {
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(ts))
        } else "Just now"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("record_card_${record.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                CategoryBadge(categoryRaw = record.templateCategory)
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = record.templateName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "${record.patientName}  •  MRD ${record.patientMrdNo}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary
            )

            val subMeta = listOfNotNull(
                listOf(record.patientAge, record.patientGender).filter { it.isNotBlank() }.joinToString(" / ").takeIf { it.isNotBlank() },
                record.patientWardBed.takeIf { it.isNotBlank() },
                "${record.entries.count { it.value.isNotBlank() }}/${record.entries.size} filled"
            ).joinToString("  •  ")

            if (subMeta.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subMeta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledTonalButton(
                    onClick = onOpen,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open", maxLines = 1)
                }

                IconButton(onClick = onPrint, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Print, contentDescription = "Print clinical form", tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(onClick = onExportPdf, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = "Export A4 PDF", tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(onClick = onShare, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Share, contentDescription = "Share PDF", tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete record", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
