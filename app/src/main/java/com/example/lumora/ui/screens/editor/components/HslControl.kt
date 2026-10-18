package com.example.lumora.ui.screens.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lumora.data.model.EditingParams
import com.example.lumora.data.model.HslAdjustment
import com.example.lumora.data.model.HslColor

@Composable
fun HslControl(
    params: EditingParams,
    onParamsChange: (EditingParams) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedColor by remember { mutableStateOf(HslColor.RED) }
    val currentAdj = params.hslAdjustments[selectedColor] ?: HslAdjustment()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("hsl_panel")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Color Mixer • ${selectedColor.label}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(
                onClick = {
                    val map = params.hslAdjustments.toMutableMap()
                    map[selectedColor] = HslAdjustment()
                    onParamsChange(params.copy(hslAdjustments = map))
                }
            ) {
                Icon(
                    imageVector = Icons.Default.RotateLeft,
                    contentDescription = "Reset ${selectedColor.label}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 8 Color Selector Circles Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HslColor.entries.forEach { color ->
                val isSelected = color == selectedColor
                val adj = params.hslAdjustments[color] ?: HslAdjustment()
                val isModified = !adj.isDefault()

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(color.baseColorHex))
                        .border(
                            width = if (isSelected) 3.dp else if (isModified) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else if (isModified) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { selectedColor = color }
                        .testTag("hsl_color_${color.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isModified && !isSelected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hue Slider
        AdjustmentSlider(
            label = "Hue",
            value = currentAdj.hue,
            onValueChange = { newHue ->
                val map = params.hslAdjustments.toMutableMap()
                map[selectedColor] = currentAdj.copy(hue = newHue)
                onParamsChange(params.copy(hslAdjustments = map))
            },
            range = -100f..100f,
            defaultValue = 0f,
            activeColor = Color(selectedColor.baseColorHex)
        )

        // Saturation Slider
        AdjustmentSlider(
            label = "Saturation",
            value = currentAdj.sat,
            onValueChange = { newSat ->
                val map = params.hslAdjustments.toMutableMap()
                map[selectedColor] = currentAdj.copy(sat = newSat)
                onParamsChange(params.copy(hslAdjustments = map))
            },
            range = -100f..100f,
            defaultValue = 0f,
            activeColor = Color(selectedColor.baseColorHex)
        )

        // Luminance Slider
        AdjustmentSlider(
            label = "Luminance",
            value = currentAdj.lum,
            onValueChange = { newLum ->
                val map = params.hslAdjustments.toMutableMap()
                map[selectedColor] = currentAdj.copy(lum = newLum)
                onParamsChange(params.copy(hslAdjustments = map))
            },
            range = -100f..100f,
            defaultValue = 0f,
            activeColor = Color(selectedColor.baseColorHex)
        )
    }
}
