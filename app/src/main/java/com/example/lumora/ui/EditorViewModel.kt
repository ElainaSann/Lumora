package com.example.lumora.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lumora.data.local.ProjectEntity
import com.example.lumora.data.model.EditingParams
import com.example.lumora.data.model.Preset
import com.example.lumora.data.repository.ProjectRepository
import com.example.lumora.engine.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class EditorToolTab(val title: String) {
    LIGHT("Light"),
    COLOR("Color"),
    GRADING("Grading"),
    HSL("HSL"),
    CURVES("Curves"),
    DETAIL("Detail"),
    EFFECTS("Effects"),
    CROP("Crop"),
    TEXT("Text"),
    PRESETS("Presets")
}

enum class SaveStatus {
    IDLE, SAVING, SAVED
}

data class EditorUiState(
    val project: ProjectEntity? = null,
    val originalBitmap: Bitmap? = null,
    val proxyOriginalBitmap: Bitmap? = null,
    val renderedBitmap: Bitmap? = null,
    val params: EditingParams = EditingParams(),
    val activeTab: EditorToolTab = EditorToolTab.LIGHT,
    val isComparing: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isProcessing: Boolean = false,
    val saveStatus: SaveStatus = SaveStatus.IDLE,
    val exportDialogVisible: Boolean = false,
    val isExporting: Boolean = false,
    val exportSuccessUri: Uri? = null,
    val notificationMessage: String? = null
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProjectRepository(application)

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<EditingParams>()
    private val redoStack = mutableListOf<EditingParams>()

    private var renderJob: Job? = null
    private var autoSaveJob: Job? = null

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            val project = repository.getProject(projectId)
            if (project != null) {
                val orig = repository.loadOriginalBitmap(project.originalImagePath)
                if (orig != null) {
                    val proxy = ImageProcessor.createProxyBitmap(orig, 900)
                    val params = EditingParams.fromJson(project.editingParamsJson)

                    undoStack.clear()
                    redoStack.clear()

                    _uiState.value = _uiState.value.copy(
                        project = project,
                        originalBitmap = orig,
                        proxyOriginalBitmap = proxy,
                        params = params,
                        isProcessing = false,
                        canUndo = false,
                        canRedo = false
                    )

                    renderPreview(proxy, params)
                    return@launch
                }
            }
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                notificationMessage = "Failed to load project photo"
            )
        }
    }

    fun updateParams(newParams: EditingParams, recordHistory: Boolean = true) {
        val current = _uiState.value.params
        if (current == newParams) return

        if (recordHistory) {
            undoStack.add(current)
            if (undoStack.size > 30) undoStack.removeAt(0)
            redoStack.clear()
        }

        _uiState.value = _uiState.value.copy(
            params = newParams,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )

        val proxy = _uiState.value.proxyOriginalBitmap ?: return
        triggerRender(proxy, newParams)
        scheduleAutoSave(newParams)
    }

    private fun triggerRender(proxy: Bitmap, params: EditingParams) {
        renderJob?.cancel()
        renderJob = viewModelScope.launch(Dispatchers.Default) {
            val rendered = ImageProcessor.processBitmap(proxy, params)
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(renderedBitmap = rendered)
            }
        }
    }

    private fun renderPreview(proxy: Bitmap, params: EditingParams) {
        viewModelScope.launch(Dispatchers.Default) {
            val rendered = ImageProcessor.processBitmap(proxy, params)
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(renderedBitmap = rendered)
            }
        }
    }

    private fun scheduleAutoSave(params: EditingParams) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saveStatus = SaveStatus.SAVING)
            delay(750)
            val proj = _uiState.value.project ?: return@launch
            repository.saveEdits(proj.id, params, _uiState.value.renderedBitmap)
            _uiState.value = _uiState.value.copy(saveStatus = SaveStatus.SAVED)
            delay(1500)
            if (_uiState.value.saveStatus == SaveStatus.SAVED) {
                _uiState.value = _uiState.value.copy(saveStatus = SaveStatus.IDLE)
            }
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val previous = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(_uiState.value.params)

        _uiState.value = _uiState.value.copy(
            params = previous,
            canUndo = undoStack.isNotEmpty(),
            canRedo = true
        )

        val proxy = _uiState.value.proxyOriginalBitmap ?: return
        triggerRender(proxy, previous)
        scheduleAutoSave(previous)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val next = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(_uiState.value.params)

        _uiState.value = _uiState.value.copy(
            params = next,
            canUndo = true,
            canRedo = redoStack.isNotEmpty()
        )

        val proxy = _uiState.value.proxyOriginalBitmap ?: return
        triggerRender(proxy, next)
        scheduleAutoSave(next)
    }

    fun resetAll() {
        val current = _uiState.value.params
        val defaultParams = EditingParams()
        if (current == defaultParams) return

        undoStack.add(current)
        redoStack.clear()

        _uiState.value = _uiState.value.copy(
            params = defaultParams,
            canUndo = true,
            canRedo = false
        )

        val proxy = _uiState.value.proxyOriginalBitmap ?: return
        triggerRender(proxy, defaultParams)
        scheduleAutoSave(defaultParams)
    }

    fun setComparing(isComparing: Boolean) {
        _uiState.value = _uiState.value.copy(isComparing = isComparing)
    }

    fun selectToolTab(tab: EditorToolTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
    }

    fun applyPreset(preset: Preset) {
        val updated = preset.params(_uiState.value.params)
        updateParams(updated, recordHistory = true)
    }

    fun updateProjectTitle(newTitle: String) {
        val proj = _uiState.value.project ?: return
        viewModelScope.launch {
            repository.updateTitle(proj.id, newTitle)
            _uiState.value = _uiState.value.copy(project = proj.copy(title = newTitle))
        }
    }

    fun showExportDialog(visible: Boolean) {
        _uiState.value = _uiState.value.copy(exportDialogVisible = visible)
    }

    fun exportPhoto(
        format: String,
        quality: Int,
        resolutionScale: Float, // 1.0f (Original), 0.75f, 0.5f
        fileName: String,
        onComplete: (Uri?) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            val orig = _uiState.value.originalBitmap
            if (orig == null) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    notificationMessage = "Export failed: No original image"
                )
                onComplete(null)
                return@launch
            }

            try {
                // High-resolution processing
                val base = if (resolutionScale < 1.0f) {
                    val w = (orig.width * resolutionScale).toInt().coerceAtLeast(100)
                    val h = (orig.height * resolutionScale).toInt().coerceAtLeast(100)
                    Bitmap.createScaledBitmap(orig, w, h, true)
                } else orig

                val fullRendered = withContext(Dispatchers.Default) {
                    ImageProcessor.processBitmap(base, _uiState.value.params)
                }

                val uri = repository.exportImage(fullRendered, format, quality, fileName)
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportDialogVisible = false,
                    exportSuccessUri = uri,
                    notificationMessage = if (uri != null) "Photo exported to Gallery!" else "Export failed"
                )
                onComplete(uri)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    notificationMessage = "Export failed: ${e.message}"
                )
                onComplete(null)
            }
        }
    }

    fun clearNotification() {
        _uiState.value = _uiState.value.copy(notificationMessage = null)
    }
}
