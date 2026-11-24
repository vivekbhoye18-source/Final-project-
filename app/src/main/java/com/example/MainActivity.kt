package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.OcrAiService
import com.example.data.WardRepository
import com.example.data.local.WardDatabase
import com.example.data.model.FilledFormRecord
import com.example.data.model.FormTemplate
import com.example.data.model.PatientRecord
import com.example.data.model.UiState
import com.example.firebase.configureAppCheck
import com.example.ui.screens.FormFillerScreen
import com.example.ui.screens.OcrScanScreen
import com.example.ui.screens.PatientEditorScreen
import com.example.ui.screens.PatientsScreen
import com.example.ui.screens.RecordsScreen
import com.example.ui.screens.TemplateEditorScreen
import com.example.ui.screens.TemplatesScreen
import com.example.ui.theme.WardFormsTheme
import com.example.viewmodel.WardViewModel

private enum class WardTab(val titleRes: Int, val icon: ImageVector, val tag: String) {
    PATIENTS(R.string.nav_patients, Icons.Rounded.People, "tab_patients"),
    OCR_SCAN(R.string.nav_scan, Icons.Rounded.DocumentScanner, "tab_ocr_scan"),
    TEMPLATES(R.string.nav_templates, Icons.Rounded.Description, "tab_templates"),
    RECORDS(R.string.nav_records, Icons.Rounded.FolderCopy, "tab_records")
}

private sealed interface WardSubScreen {
    data class PatientEditor(val patient: PatientRecord?) : WardSubScreen
    data class TemplateEditor(val template: FormTemplate?, val importBanner: String? = null) : WardSubScreen
    data class FormFiller(
        val patient: PatientRecord? = null,
        val template: FormTemplate? = null,
        val existingRecord: FilledFormRecord? = null
    ) : WardSubScreen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Initialize Firebase App Check for Gemini AI Logic (no user login required)
        configureAppCheck(this, intent)

        setContent {
            WardFormsTheme {
                WardFormsRoot()
            }
        }
    }
}

@Composable
private fun WardFormsRoot() {
    val viewModel: WardViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val db = WardDatabase.getInstance(app)
                val repository = WardRepository(dao = db.wardDao())
                val ocrService = OcrAiService()
                WardViewModel(
                    repository = repository,
                    ocrService = ocrService
                )
            }
        }
    )

    WardFormsWorkspace(viewModel = viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WardFormsWorkspace(
    viewModel: WardViewModel
) {
    val patientsState by viewModel.patientsState.collectAsStateWithLifecycle()
    val templatesState by viewModel.templatesState.collectAsStateWithLifecycle()
    val filledFormsState by viewModel.filledFormsState.collectAsStateWithLifecycle()
    val ocrState by viewModel.ocrState.collectAsStateWithLifecycle()
    val templateImportState by viewModel.templateImportState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(WardTab.PATIENTS) }
    var activeSubScreen by remember { mutableStateOf<WardSubScreen?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val allPatients = (patientsState as? UiState.Success)?.data.orEmpty()
    val allTemplates = (templatesState as? UiState.Success)?.data.orEmpty()
    val allForms = (filledFormsState as? UiState.Success)?.data.orEmpty()

    when (val sub = activeSubScreen) {
        is WardSubScreen.PatientEditor -> {
            PatientEditorScreen(
                initialPatient = sub.patient,
                isSaving = isSaving,
                onBack = { activeSubScreen = null },
                onSave = { updated ->
                    viewModel.savePatient(updated) {
                        activeSubScreen = null
                    }
                }
            )
            return
        }

        is WardSubScreen.TemplateEditor -> {
            TemplateEditorScreen(
                initialTemplate = sub.template,
                importBannerMessage = sub.importBanner,
                isSaving = isSaving,
                onBack = { activeSubScreen = null },
                onSave = { updated ->
                    viewModel.saveTemplate(updated) {
                        activeSubScreen = null
                    }
                }
            )
            return
        }

        is WardSubScreen.FormFiller -> {
            FormFillerScreen(
                initialPatient = sub.patient,
                initialTemplate = sub.template,
                existingRecord = sub.existingRecord,
                allPatients = allPatients,
                allTemplates = allTemplates,
                isSaving = isSaving,
                onBack = { activeSubScreen = null },
                onSaveRecord = { record, onSaved ->
                    viewModel.saveFilledForm(record, onSaved)
                },
                onShowMessage = { viewModel.emitMessage(it) }
            )
            return
        }

        null -> Unit
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 700.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.LocalHospital,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        WardTab.entries.forEach { tab ->
                            val label = stringResource(tab.titleRes)
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(tab.icon, contentDescription = label) },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        WardTab.entries.forEach { tab ->
                            val label = stringResource(tab.titleRes)
                            NavigationRailItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(tab.icon, contentDescription = label) },
                                label = { Text(label) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }

                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxSize()) {
                        when (currentTab) {
                            WardTab.PATIENTS -> {
                                PatientsScreen(
                                    patientsState = patientsState,
                                    totalTemplates = allTemplates.size,
                                    totalForms = allForms.size,
                                    onOpenOcrScan = { currentTab = WardTab.OCR_SCAN },
                                    onAddManualPatient = {
                                        activeSubScreen = WardSubScreen.PatientEditor(null)
                                    },
                                    onEditPatient = { patient ->
                                        activeSubScreen = WardSubScreen.PatientEditor(patient)
                                    },
                                    onFillFormForPatient = { patient ->
                                        activeSubScreen = WardSubScreen.FormFiller(
                                            patient = patient,
                                            template = allTemplates.firstOrNull()
                                        )
                                    },
                                    onDeletePatient = { viewModel.deletePatient(it) }
                                )
                            }

                            WardTab.OCR_SCAN -> {
                                OcrScanScreen(
                                    ocrState = ocrState,
                                    isSaving = isSaving,
                                    onScanImage = { viewModel.scanPatientImage(it) },
                                    onParseText = { viewModel.parsePatientText(it) },
                                    onResetOcr = { viewModel.clearOcrState() },
                                    onSavePatient = { draftPatient, openFormAfter ->
                                        viewModel.savePatient(draftPatient) { savedId ->
                                            viewModel.clearOcrState()
                                            if (openFormAfter) {
                                                activeSubScreen = WardSubScreen.FormFiller(
                                                    patient = draftPatient.copy(id = savedId),
                                                    template = allTemplates.firstOrNull()
                                                )
                                            } else {
                                                currentTab = WardTab.PATIENTS
                                            }
                                        }
                                    }
                                )
                            }

                            WardTab.TEMPLATES -> {
                                TemplatesScreen(
                                    templatesState = templatesState,
                                    importState = templateImportState,
                                    isSaving = isSaving,
                                    onUseTemplate = { template ->
                                        activeSubScreen = WardSubScreen.FormFiller(
                                            patient = allPatients.firstOrNull(),
                                            template = template
                                        )
                                    },
                                    onCreateTemplate = {
                                        activeSubScreen = WardSubScreen.TemplateEditor(null)
                                    },
                                    onEditTemplate = { template ->
                                        activeSubScreen = WardSubScreen.TemplateEditor(template)
                                    },
                                    onDeleteTemplate = { viewModel.deleteTemplate(it) },
                                    onRestoreDefaults = { viewModel.restoreDefaultTemplates() },
                                    onImportTemplateImage = { viewModel.importTemplateFromImage(it) },
                                    onImportTemplateText = { viewModel.importTemplateFromText(it) },
                                    onTemplateImported = { importResult ->
                                        activeSubScreen = WardSubScreen.TemplateEditor(
                                            template = importResult.draftTemplate,
                                            importBanner = "AI Template Import (${importResult.overallAccuracy}% confidence): ${importResult.summary}"
                                        )
                                    },
                                    onClearImportState = { viewModel.clearTemplateImportState() },
                                    onShowMessage = { viewModel.emitMessage(it) }
                                )
                            }

                            WardTab.RECORDS -> {
                                RecordsScreen(
                                    recordsState = filledFormsState,
                                    onOpenRecord = { record ->
                                        activeSubScreen = WardSubScreen.FormFiller(existingRecord = record)
                                    },
                                    onDeleteRecord = { viewModel.deleteFilledForm(it) },
                                    onShowMessage = { viewModel.emitMessage(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
