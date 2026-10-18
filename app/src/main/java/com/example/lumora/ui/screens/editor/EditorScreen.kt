package com.example.lumora.ui.screens.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lumora.ui.EditorToolTab
import com.example.lumora.ui.EditorViewModel
import com.example.lumora.ui.SaveStatus
import com.example.lumora.ui.screens.editor.components.ColorControl
import com.example.lumora.ui.screens.editor.components.ColorGradingControl
import com.example.lumora.ui.screens.editor.components.CropControl
import com.example.lumora.ui.screens.editor.components.CurvesControl
import com.example.lumora.ui.screens.editor.components.DetailControl
import com.example.lumora.ui.screens.editor.components.EffectsControl
import com.example.lumora.ui.screens.editor.components.ExportDialog
import com.example.lumora.ui.screens.editor.components.HslControl
import com.example.lumora.ui.screens.editor.components.ImageCanvas
import com.example.lumora.ui.screens.editor.components.LightControl
import androidx.compose.material.icons.filled.TextFields
import com.example.lumora.ui.screens.editor.components.PresetsControl
import com.example.lumora.ui.screens.editor.components.TextControl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectId: String,
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var renameDialogVisible by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }
    var resetConfirmVisible by remember { mutableStateOf(false) }

    BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    LaunchedEffect(uiState.notificationMessage) {
        uiState.notificationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("editor_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isTablet = maxWidth > 650.dp

            Column(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                EditorTopBar(
                    title = uiState.project?.title ?: "Untitled",
                    saveStatus = uiState.saveStatus,
                    canUndo = uiState.canUndo,
                    canRedo = uiState.canRedo,
                    isComparing = uiState.isComparing,
                    onBackClick = onNavigateBack,
                    onTitleClick = {
                        renameInput = uiState.project?.title ?: ""
                        renameDialogVisible = true
                    },
                    onUndoClick = { viewModel.undo() },
                    onRedoClick = { viewModel.redo() },
                    onResetClick = { resetConfirmVisible = true },
                    onCompareStart = { viewModel.setComparing(true) },
                    onCompareEnd = { viewModel.setComparing(false) },
                    onExportClick = { viewModel.showExportDialog(true) }
                )

                if (isTablet) {
                    // Tablet Side-by-Side Layout
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        // Canvas Area (60% width)
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .fillMaxHeight()
                        ) {
                            ImageCanvas(
                                renderedBitmap = uiState.renderedBitmap,
                                originalBitmap = uiState.proxyOriginalBitmap ?: uiState.originalBitmap,
                                isComparing = uiState.isComparing
                            )
                        }

                        // Editing Controls Docked Right (40% width)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                        ) {
                            EditorTabs(
                                activeTab = uiState.activeTab,
                                onTabSelect = { viewModel.selectToolTab(it) }
                            )

                            if (uiState.activeTab == EditorToolTab.CURVES) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                ) {
                                    ActivePanelContent(
                                        activeTab = uiState.activeTab,
                                        viewModel = viewModel,
                                        uiState = uiState
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    ActivePanelContent(
                                        activeTab = uiState.activeTab,
                                        viewModel = viewModel,
                                        uiState = uiState
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Mobile Vertical Stack Layout
                    val isCurves = uiState.activeTab == EditorToolTab.CURVES
                    val panelHeight = if (isCurves) 290.dp else if (uiState.activeTab == EditorToolTab.TEXT) 340.dp else 270.dp

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        // Image Canvas (Flex Weight)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            ImageCanvas(
                                renderedBitmap = uiState.renderedBitmap,
                                originalBitmap = uiState.proxyOriginalBitmap ?: uiState.originalBitmap,
                                isComparing = uiState.isComparing
                            )
                        }

                        // Bottom Control Panel
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(panelHeight)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                )
                        ) {
                            EditorTabs(
                                activeTab = uiState.activeTab,
                                onTabSelect = { viewModel.selectToolTab(it) }
                            )

                            if (isCurves) {
                                // Strictly non-scrollable container for Curves so handles drag smoothly
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    ActivePanelContent(
                                        activeTab = uiState.activeTab,
                                        viewModel = viewModel,
                                        uiState = uiState
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    ActivePanelContent(
                                        activeTab = uiState.activeTab,
                                        viewModel = viewModel,
                                        uiState = uiState
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (renameDialogVisible) {
        AlertDialog(
            onDismissRequest = { renameDialogVisible = false },
            title = { Text("Rename Project") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProjectTitle(renameInput)
                        renameDialogVisible = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameDialogVisible = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset Confirmation Dialog
    if (resetConfirmVisible) {
        AlertDialog(
            onDismissRequest = { resetConfirmVisible = false },
            title = { Text("Reset All Adjustments?") },
            text = { Text("This will revert all sliders, color adjustments, and crop to original state.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAll()
                        resetConfirmVisible = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset All")
                }
            },
            dismissButton = {
                TextButton(onClick = { resetConfirmVisible = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Dialog
    if (uiState.exportDialogVisible) {
        ExportDialog(
            previewBitmap = uiState.renderedBitmap ?: uiState.proxyOriginalBitmap,
            defaultName = uiState.project?.title ?: "Photo",
            isExporting = uiState.isExporting,
            exportSuccessUri = uiState.exportSuccessUri,
            onDismiss = { viewModel.showExportDialog(false) },
            onExport = { format, quality, scale, fileName ->
                viewModel.exportPhoto(format, quality, scale, fileName) {}
            }
        )
    }
}

@Composable
private fun ActivePanelContent(
    activeTab: EditorToolTab,
    viewModel: EditorViewModel,
    uiState: com.example.lumora.ui.EditorUiState
) {
    when (activeTab) {
        EditorToolTab.LIGHT -> LightControl(
            params = uiState.params,
            onParamsChange = { viewModel.updateParams(it) }
        )
        EditorToolTab.COLOR -> ColorControl(
            params = uiState.params,
            onParamsChange = { viewModel.updateParams(it) }
        )
        EditorToolTab.GRADING -> ColorGradingControl(
            params = uiState.params,
            onParamsChange = { viewModel.updateParams(it) }
        )
        EditorToolTab.HSL -> HslControl(
            params = uiState.params,
            onParamsChange = { viewModel.updateParams(it) }
        )
        EditorToolTab.CURVES -> CurvesControl(
            params = uiState.params,
            onParamsChange = { viewModel.updateParams(it) }
        )
        EditorToolTab.DETAIL -> DetailControl(
            params = uiState.params,
            onParamsChange = { viewModel.updateParams(it) }
        )
        EditorToolTab.EFFECTS -> EffectsControl(
            params = uiState.params,
            onParamsChange = { viewModel.updateParams(it) }
        )
        EditorToolTab.CROP -> CropControl(
            params = uiState.params,
            onParamsChange = { viewModel.updateParams(it) }
        )
        EditorToolTab.TEXT -> TextControl(
            params = uiState.params,
            onParamsChange = { viewModel.updateParams(it) }
        )
        EditorToolTab.PRESETS -> PresetsControl(
            appliedPresetName = uiState.params.appliedPresetName,
            onApplyPreset = { viewModel.applyPreset(it) }
        )
    }
}

@Composable
private fun EditorTopBar(
    title: String,
    saveStatus: SaveStatus,
    canUndo: Boolean,
    canRedo: Boolean,
    isComparing: Boolean,
    onBackClick: () -> Unit,
    onTitleClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onResetClick: () -> Unit,
    onCompareStart: () -> Unit,
    onCompareEnd: () -> Unit,
    onExportClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.surface)
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("btn_back_studio")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Studio"
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onTitleClick() })
                    }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.DriveFileRenameOutline,
                        contentDescription = "Rename",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                if (saveStatus == SaveStatus.SAVING) {
                    Text(
                        text = "Saving...",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (saveStatus == SaveStatus.SAVED) {
                    Text(
                        text = "Saved",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action Buttons Row
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Undo
            IconButton(
                onClick = onUndoClick,
                enabled = canUndo,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("btn_undo")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Redo
            IconButton(
                onClick = onRedoClick,
                enabled = canRedo,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("btn_redo")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Before / After Compare (Press and Hold)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isComparing) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onCompareStart()
                                tryAwaitRelease()
                                onCompareEnd()
                            }
                        )
                    }
                    .testTag("btn_compare"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Compare,
                    contentDescription = "Hold for Before / After",
                    tint = if (isComparing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Reset
            IconButton(
                onClick = onResetClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("btn_reset_all")
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Reset All",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Export Button
            Button(
                onClick = onExportClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("btn_export_top")
            ) {
                Text(
                    text = "Export",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTabs(
    activeTab: EditorToolTab,
    onTabSelect: (EditorToolTab) -> Unit
) {
    val tabs = EditorToolTab.entries
    val activeIndex = tabs.indexOf(activeTab)

    PrimaryScrollableTabRow(
        selectedTabIndex = activeIndex,
        edgePadding = 8.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        tabs.forEachIndexed { idx, tab ->
            val isSelected = activeIndex == idx
            Tab(
                selected = isSelected,
                onClick = { onTabSelect(tab) },
                text = {
                    Text(
                        text = tab.title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                },
                icon = {
                    Icon(
                        imageVector = getTabIcon(tab),
                        contentDescription = tab.title,
                        modifier = Modifier.size(18.dp)
                    )
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
            )
        }
    }
}

private fun getTabIcon(tab: EditorToolTab): ImageVector = when (tab) {
    EditorToolTab.LIGHT -> Icons.Default.LightMode
    EditorToolTab.COLOR -> Icons.Default.Palette
    EditorToolTab.GRADING -> Icons.Default.Gradient
    EditorToolTab.HSL -> Icons.Default.Tune
    EditorToolTab.CURVES -> Icons.Default.ShowChart
    EditorToolTab.DETAIL -> Icons.Default.AutoAwesome
    EditorToolTab.EFFECTS -> Icons.Default.Flare
    EditorToolTab.CROP -> Icons.Default.Crop
    EditorToolTab.TEXT -> Icons.Default.TextFields
    EditorToolTab.PRESETS -> Icons.Default.Style
}
