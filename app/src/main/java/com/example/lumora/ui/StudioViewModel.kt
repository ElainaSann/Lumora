package com.example.lumora.ui

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lumora.data.local.LumoraDatabase
import com.example.lumora.data.local.ProjectEntity
import com.example.lumora.data.model.EditingParams
import com.example.lumora.data.repository.ProjectRepository
import com.example.lumora.engine.ImageProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class StudioFilter {
    ALL, RECENT, FAVORITES
}

data class StudioUiState(
    val projects: List<ProjectEntity> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: StudioFilter = StudioFilter.ALL,
    val isLoading: Boolean = true,
    val userNotification: String? = null
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    val repository = ProjectRepository(application)

    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow(StudioFilter.ALL)
    private val _isLoading = MutableStateFlow(true)
    private val _userNotification = MutableStateFlow<String?>(null)

    val uiState: StateFlow<StudioUiState> = combine(
        repository.projectsFlow,
        _searchQuery,
        _activeFilter,
        _isLoading,
        _userNotification
    ) { projects, query, filter, loading, notification ->
        val filtered = projects.filter { proj ->
            val matchesQuery = query.isBlank() || proj.title.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                StudioFilter.ALL -> true
                StudioFilter.RECENT -> System.currentTimeMillis() - proj.updatedAt < 7 * 24 * 3600 * 1000L
                StudioFilter.FAVORITES -> proj.isFavorite
            }
            matchesQuery && matchesFilter
        }
        StudioUiState(
            projects = filtered,
            searchQuery = query,
            activeFilter = filter,
            isLoading = loading,
            userNotification = notification
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudioUiState())

    init {
        viewModelScope.launch {
            repository.initializeSampleProjectsIfEmpty()
            _isLoading.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: StudioFilter) {
        _activeFilter.value = filter
    }

    fun importFromUri(uri: Uri, onProjectCreated: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val proj = repository.createProjectFromUri(uri)
                _userNotification.value = "Photo imported successfully"
                _isLoading.value = false
                onProjectCreated(proj.id)
            } catch (e: Exception) {
                _isLoading.value = false
                _userNotification.value = "Failed to import photo: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun importSample(resId: Int, sampleTitle: String, onProjectCreated: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val context = getApplication<Application>()
                val bmp = BitmapFactory.decodeResource(context.resources, resId)
                if (bmp != null) {
                    val id = UUID.randomUUID().toString()
                    val projDir = File(context.filesDir, "projects/$id").apply { mkdirs() }
                    val origFile = File(projDir, "original.jpg")
                    val fos = FileOutputStream(origFile)
                    bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, fos)
                    fos.close()

                    val thumbBmp = ImageProcessor.createProxyBitmap(bmp, 360)
                    val thumbFile = File(projDir, "thumb.jpg")
                    val tfos = FileOutputStream(thumbFile)
                    thumbBmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, tfos)
                    tfos.close()

                    val proj = ProjectEntity(
                        id = id,
                        title = sampleTitle,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        isFavorite = false,
                        originalImagePath = origFile.absolutePath,
                        thumbnailPath = thumbFile.absolutePath,
                        editingParamsJson = EditingParams().toJson()
                    )
                    LumoraDatabase.getDatabase(context).projectDao().insertOrUpdate(proj)
                    _userNotification.value = "Sample project created"
                    _isLoading.value = false
                    onProjectCreated(proj.id)
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _userNotification.value = "Failed to create sample: ${e.message}"
            }
        }
    }

    fun toggleFavorite(projectId: String, currentIsFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(projectId, !currentIsFav)
        }
    }

    fun renameProject(projectId: String, newTitle: String) {
        viewModelScope.launch {
            if (newTitle.isNotBlank()) {
                repository.updateTitle(projectId, newTitle.trim())
                _userNotification.value = "Project renamed"
            }
        }
    }

    fun duplicateProject(projectId: String) {
        viewModelScope.launch {
            val dup = repository.duplicateProject(projectId)
            if (dup != null) {
                _userNotification.value = "Project duplicated"
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            _userNotification.value = "Project deleted"
        }
    }

    fun clearNotification() {
        _userNotification.value = null
    }
}
