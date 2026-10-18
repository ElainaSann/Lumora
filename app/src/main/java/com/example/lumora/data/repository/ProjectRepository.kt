package com.example.lumora.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.R
import com.example.lumora.data.local.LumoraDatabase
import com.example.lumora.data.local.ProjectEntity
import com.example.lumora.data.model.EditingParams
import com.example.lumora.data.model.PresetRegistry
import com.example.lumora.engine.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.UUID

class ProjectRepository(private val context: Context) {
    private val db = LumoraDatabase.getDatabase(context)
    private val dao = db.projectDao()

    val projectsFlow: Flow<List<ProjectEntity>> = dao.getAllProjectsFlow()

    suspend fun initializeSampleProjectsIfEmpty() = withContext(Dispatchers.IO) {
        val count = dao.getProjectCount()
        if (count > 0) return@withContext

        // Seed 1: Portrait Project with Warm Sunset preset
        val portraitResId = R.drawable.lumora_sample_portrait
        val portraitBmp = BitmapFactory.decodeResource(context.resources, portraitResId)
        if (portraitBmp != null) {
            val portraitId = UUID.randomUUID().toString()
            val origFile = saveBitmapToInternal(portraitId, "original.jpg", portraitBmp)
            val warmParams = PresetRegistry.presets.firstOrNull { it.id == "warm" }?.params?.invoke(EditingParams()) ?: EditingParams()
            val thumbBmp = ImageProcessor.processBitmap(ImageProcessor.createProxyBitmap(portraitBmp, 320), warmParams)
            val thumbFile = saveBitmapToInternal(portraitId, "thumb.jpg", thumbBmp)

            val p1 = ProjectEntity(
                id = portraitId,
                title = "Golden Hour Portrait",
                createdAt = System.currentTimeMillis() - 86400000,
                updatedAt = System.currentTimeMillis() - 86400000,
                isFavorite = true,
                originalImagePath = origFile.absolutePath,
                thumbnailPath = thumbFile.absolutePath,
                editingParamsJson = warmParams.toJson()
            )
            dao.insertOrUpdate(p1)
        }

        // Seed 2: Scenery Project with Cinematic preset
        val sceneryResId = R.drawable.lumora_sample_scenery
        val sceneryBmp = BitmapFactory.decodeResource(context.resources, sceneryResId)
        if (sceneryBmp != null) {
            val sceneryId = UUID.randomUUID().toString()
            val origFile = saveBitmapToInternal(sceneryId, "original.jpg", sceneryBmp)
            val cinParams = PresetRegistry.presets.firstOrNull { it.id == "cinematic" }?.params?.invoke(EditingParams()) ?: EditingParams()
            val thumbBmp = ImageProcessor.processBitmap(ImageProcessor.createProxyBitmap(sceneryBmp, 320), cinParams)
            val thumbFile = saveBitmapToInternal(sceneryId, "thumb.jpg", thumbBmp)

            val p2 = ProjectEntity(
                id = sceneryId,
                title = "Alpine Lake Dawn",
                createdAt = System.currentTimeMillis() - 172800000,
                updatedAt = System.currentTimeMillis() - 172800000,
                isFavorite = false,
                originalImagePath = origFile.absolutePath,
                thumbnailPath = thumbFile.absolutePath,
                editingParamsJson = cinParams.toJson()
            )
            dao.insertOrUpdate(p2)
        }
    }

    suspend fun createProjectFromUri(uri: Uri, title: String? = null): ProjectEntity = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Could not open image uri")
        val bitmap = BitmapFactory.decodeStream(inputStream)
            ?: throw IllegalArgumentException("Failed to decode image bitmap")
        inputStream.close()

        val id = UUID.randomUUID().toString()
        val origFile = saveBitmapToInternal(id, "original.jpg", bitmap)

        val defaultParams = EditingParams()
        val thumbBmp = ImageProcessor.createProxyBitmap(bitmap, 360)
        val thumbFile = saveBitmapToInternal(id, "thumb.jpg", thumbBmp)

        val project = ProjectEntity(
            id = id,
            title = title ?: "Project #${(dao.getProjectCount() + 1)}",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isFavorite = false,
            originalImagePath = origFile.absolutePath,
            thumbnailPath = thumbFile.absolutePath,
            editingParamsJson = defaultParams.toJson()
        )
        dao.insertOrUpdate(project)
        project
    }

    suspend fun getProject(id: String): ProjectEntity? = withContext(Dispatchers.IO) {
        dao.getProjectById(id)
    }

    suspend fun loadOriginalBitmap(path: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveEdits(projectId: String, params: EditingParams, previewBitmap: Bitmap?) = withContext(Dispatchers.IO) {
        val project = dao.getProjectById(projectId) ?: return@withContext
        val now = System.currentTimeMillis()

        var thumbPath = project.thumbnailPath
        if (previewBitmap != null) {
            val thumbBmp = ImageProcessor.createProxyBitmap(previewBitmap, 360)
            val thumbFile = saveBitmapToInternal(projectId, "thumb.jpg", thumbBmp)
            thumbPath = thumbFile.absolutePath
        }

        val updated = project.copy(
            updatedAt = now,
            thumbnailPath = thumbPath,
            editingParamsJson = params.toJson()
        )
        dao.insertOrUpdate(updated)
    }

    suspend fun updateTitle(projectId: String, newTitle: String) = withContext(Dispatchers.IO) {
        dao.updateTitle(projectId, newTitle, System.currentTimeMillis())
    }

    suspend fun toggleFavorite(projectId: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        dao.updateFavorite(projectId, isFavorite, System.currentTimeMillis())
    }

    suspend fun duplicateProject(projectId: String): ProjectEntity? = withContext(Dispatchers.IO) {
        val orig = dao.getProjectById(projectId) ?: return@withContext null
        val newId = UUID.randomUUID().toString()

        val origFile = File(orig.originalImagePath)
        var newOrigPath = orig.originalImagePath
        if (origFile.exists()) {
            val bmp = BitmapFactory.decodeFile(origFile.absolutePath)
            if (bmp != null) {
                newOrigPath = saveBitmapToInternal(newId, "original.jpg", bmp).absolutePath
            }
        }

        var newThumbPath: String? = null
        if (orig.thumbnailPath != null) {
            val thumbFile = File(orig.thumbnailPath)
            if (thumbFile.exists()) {
                val tbmp = BitmapFactory.decodeFile(thumbFile.absolutePath)
                if (tbmp != null) {
                    newThumbPath = saveBitmapToInternal(newId, "thumb.jpg", tbmp).absolutePath
                }
            }
        }

        val duplicated = ProjectEntity(
            id = newId,
            title = "${orig.title} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isFavorite = orig.isFavorite,
            originalImagePath = newOrigPath,
            thumbnailPath = newThumbPath,
            editingParamsJson = orig.editingParamsJson
        )
        dao.insertOrUpdate(duplicated)
        duplicated
    }

    suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
        val project = dao.getProjectById(projectId)
        if (project != null) {
            try {
                val projDir = File(context.filesDir, "projects/$projectId")
                if (projDir.exists()) projDir.deleteRecursively()
            } catch (_: Exception) {}
        }
        dao.deleteById(projectId)
    }

    suspend fun exportImage(
        bitmap: Bitmap,
        format: String, // "JPEG", "PNG", "WEBP"
        quality: Int,   // 1 to 100
        fileName: String
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val mimeType = when (format.uppercase()) {
                "PNG" -> "image/png"
                "WEBP" -> "image/webp"
                else -> "image/jpeg"
            }
            val extension = when (format.uppercase()) {
                "PNG" -> ".png"
                "WEBP" -> ".webp"
                else -> ".jpg"
            }
            val finalName = if (fileName.endsWith(extension, ignoreCase = true)) fileName else "$fileName$extension"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, finalName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Lumora")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext null

            val outputStream: OutputStream? = context.contentResolver.openOutputStream(uri)
            if (outputStream != null) {
                val compressFormat = when (format.uppercase()) {
                    "PNG" -> Bitmap.CompressFormat.PNG
                    "WEBP" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                    else -> Bitmap.CompressFormat.JPEG
                }
                bitmap.compress(compressFormat, quality.coerceIn(1, 100), outputStream)
                outputStream.flush()
                outputStream.close()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveBitmapToInternal(projectId: String, fileName: String, bitmap: Bitmap): File {
        val projDir = File(context.filesDir, "projects/$projectId")
        if (!projDir.exists()) projDir.mkdirs()
        val file = File(projDir, fileName)
        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
        fos.flush()
        fos.close()
        return file
    }
}
