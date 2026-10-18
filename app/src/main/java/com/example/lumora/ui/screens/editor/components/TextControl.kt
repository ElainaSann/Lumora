package com.example.lumora.ui.screens.editor.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.SouthEast
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lumora.data.fonts.FontItem
import com.example.lumora.data.fonts.FontManager
import com.example.lumora.data.model.EditingParams
import kotlin.math.roundToInt

@Composable
fun TextControl(
    params: EditingParams,
    onParamsChange: (EditingParams) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var allFonts by remember { mutableStateOf(FontManager.getAllFonts(context)) }

    // Launcher for importing .otf / .ttf font files
    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val imported = FontManager.importFontFromUri(context, uri)
            if (imported != null) {
                allFonts = FontManager.getAllFonts(context)
                onParamsChange(
                    params.copy(
                        textFontId = imported.id,
                        textCustomFontPath = imported.filePath
                    )
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(14.dp)
            .testTag("text_panel")
    ) {
        // Text Input Field
        OutlinedTextField(
            value = params.textOverlay,
            onValueChange = { onParamsChange(params.copy(textOverlay = it)) },
            label = { Text("Overlay Text") },
            placeholder = { Text("Type something to add on photo...") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_text_overlay")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Text Controls (Size, Opacity, Align, Colors)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Size: ${params.textSize.roundToInt()}sp",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Alignment buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "LEFT" to Icons.Default.FormatAlignLeft,
                    "CENTER" to Icons.Default.FormatAlignCenter,
                    "RIGHT" to Icons.Default.FormatAlignRight
                ).forEach { (align, icon) ->
                    val isSelected = params.textAlignment.equals(align, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onParamsChange(params.copy(textAlignment = align)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = align,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Slider(
            value = params.textSize,
            onValueChange = { onParamsChange(params.copy(textSize = it)) },
            valueRange = 16f..80f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Color Palette Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Text Color",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    0xFFFFFFFF to "White",
                    0xFF000000 to "Black",
                    0xFF14B8A6 to "Teal",
                    0xFFF59E0B to "Amber",
                    0xFFEF4444 to "Red",
                    0xFF3B82F6 to "Blue",
                    0xFFA855F7 to "Purple"
                ).forEach { (colorHex, _) ->
                    val isSelected = params.textColorHex == colorHex
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(colorHex))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                            .clickable { onParamsChange(params.copy(textColorHex = colorHex)) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Header for Font Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Fonts (${allFonts.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2-Column Grid of Font Preview Cards + Flat Accent "+ Import .otf files" Card
        // As requested by user and matching Alight Motion screenshot!
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Pair items in rows of 2
            val combinedCards = mutableListOf<FontCardType>()
            // First item: The "+ Import .otf files" card
            combinedCards.add(FontCardType.ImportAction)
            // Subsequent items: The font cards
            allFonts.forEach { font ->
                combinedCards.add(FontCardType.FontDisplay(font))
            }

            val chunked = combinedCards.chunked(2)
            chunked.forEach { rowCards ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowCards.forEach { card ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.6f)
                        ) {
                            when (card) {
                                is FontCardType.ImportAction -> {
                                    // The requested flat app accent color card with "+" and "Import .otf files"
                                    Card(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                fontPickerLauncher.launch(
                                                    arrayOf(
                                                        "font/*",
                                                        "font/otf",
                                                        "font/ttf",
                                                        "application/x-font-opentype",
                                                        "application/x-font-truetype",
                                                        "application/font-sfnt",
                                                        "application/octet-stream",
                                                        "*/*"
                                                    )
                                                )
                                            }
                                            .testTag("btn_import_font"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primary // Flat app accent color
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Import font",
                                                tint = Color.White,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Import .otf files",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                ),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                                is FontCardType.FontDisplay -> {
                                    val font = card.font
                                    val isSelected = (params.textFontId == font.id) ||
                                            (font.filePath != null && params.textCustomFontPath == font.filePath)

                                    val fontFamily = remember(font) { FontManager.getComposeFontFamily(font) }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF2E394E),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                onParamsChange(
                                                    params.copy(
                                                        textFontId = font.id,
                                                        textCustomFontPath = font.filePath
                                                    )
                                                )
                                            }
                                            .testTag("font_card_${font.id}"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFF161E2E) // Dark slate container matching screenshot
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            // Top-left icon (from screenshot)
                                            Icon(
                                                imageVector = Icons.Outlined.SouthEast,
                                                contentDescription = null,
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .size(14.dp)
                                            )

                                            // Center Sample Text rendered in the actual font
                                            Text(
                                                text = font.previewText,
                                                fontFamily = fontFamily,
                                                fontSize = 20.sp,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.align(Alignment.Center)
                                            )

                                            // Bottom Row: Font Name on left, Star icon on right
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .align(Alignment.BottomCenter),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = font.name,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                Icon(
                                                    imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                                                    contentDescription = "Favorite",
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF64748B),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // If row has 1 card, fill empty space
                    if (rowCards.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private sealed class FontCardType {
    data object ImportAction : FontCardType()
    data class FontDisplay(val font: FontItem) : FontCardType()
}
