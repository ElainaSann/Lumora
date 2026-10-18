package com.example.lumora.data.fonts

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.res.ResourcesCompat
import com.example.R
import java.io.File
import java.io.FileOutputStream

data class FontItem(
    val id: String,
    val name: String,
    val previewText: String = "Abcdefg",
    val resId: Int? = null,
    val filePath: String? = null,
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false
)

object FontManager {

    val builtInFonts = listOf(
        FontItem(
            id = "arcade_classic",
            name = "ARCADECLASSIC",
            previewText = "ABCDEFG",
            resId = R.font.press_start_2p
        ),
        FontItem(
            id = "alchosti",
            name = "Alchosti",
            previewText = "Abcdefg",
            resId = R.font.pacifico
        ),
        FontItem(
            id = "goodfather",
            name = "The Goodfather DEMO",
            previewText = "Abcdefg",
            resId = R.font.playfair_display
        ),
        FontItem(
            id = "nasional_sans",
            name = "NasionalSans-Black",
            previewText = "Abcdefg",
            resId = R.font.bebas_neue
        )
    )

    fun getImportedFonts(context: Context): List<FontItem> {
        val fontsDir = File(context.filesDir, "imported_fonts")
        if (!fontsDir.exists()) return emptyList()

        val files = fontsDir.listFiles { file ->
            val ext = file.extension.lowercase()
            ext == "otf" || ext == "ttf"
        } ?: return emptyList()

        return files.map { file ->
            val cleanName = file.nameWithoutExtension.replace("_", " ").replace("-", " ")
            FontItem(
                id = "custom_${file.name}",
                name = cleanName,
                previewText = "Abcdefg",
                filePath = file.absolutePath,
                isCustom = true
            )
        }
    }

    fun getAllFonts(context: Context): List<FontItem> {
        return builtInFonts + getImportedFonts(context)
    }

    fun importFontFromUri(context: Context, uri: Uri): FontItem? {
        return try {
            val fontsDir = File(context.filesDir, "imported_fonts").apply { mkdirs() }
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            var fileName = "imported_font_${System.currentTimeMillis()}.otf"
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = it.getString(nameIndex)
                    }
                }
            }

            val destFile = File(fontsDir, fileName)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val outputStream = FileOutputStream(destFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.flush()
            outputStream.close()

            val cleanName = destFile.nameWithoutExtension.replace("_", " ").replace("-", " ")
            FontItem(
                id = "custom_${destFile.name}",
                name = cleanName,
                previewText = "Abcdefg",
                filePath = destFile.absolutePath,
                isCustom = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getComposeFontFamily(fontItem: FontItem): FontFamily {
        if (fontItem.filePath != null) {
            val file = File(fontItem.filePath)
            if (file.exists()) {
                return try {
                    val typeface = Typeface.createFromFile(file)
                    FontFamily(typeface)
                } catch (e: Exception) {
                    FontFamily.Default
                }
            }
        }
        if (fontItem.resId != null) {
            return FontFamily(Font(fontItem.resId, FontWeight.Normal))
        }
        return FontFamily.Default
    }

    fun getAndroidTypeface(context: Context, fontItem: FontItem): Typeface {
        if (fontItem.filePath != null) {
            val file = File(fontItem.filePath)
            if (file.exists()) {
                return try {
                    Typeface.createFromFile(file)
                } catch (e: Exception) {
                    Typeface.DEFAULT
                }
            }
        }
        if (fontItem.resId != null) {
            return try {
                ResourcesCompat.getFont(context, fontItem.resId) ?: Typeface.DEFAULT
            } catch (e: Exception) {
                Typeface.DEFAULT
            }
        }
        return Typeface.DEFAULT
    }
}
