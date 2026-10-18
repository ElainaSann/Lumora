package com.example.lumora.ui.screens.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lumora.data.model.EditingParams

@Composable
fun CropControl(
    params: EditingParams,
    onParamsChange: (EditingParams) -> Unit,
    modifier: Modifier = Modifier
) {
    val aspectRatios = listOf("ORIGINAL", "1:1", "4:5", "3:2", "16:9")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(14.dp)
            .testTag("crop_panel")
    ) {
        Text(
            text = "Aspect Ratio",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            aspectRatios.forEach { ratio ->
                val isSelected = params.cropAspectRatio == ratio
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val updated = when (ratio) {
                            "1:1" -> params.copy(
                                cropAspectRatio = "1:1",
                                cropLeft = 0.125f,
                                cropTop = 0f,
                                cropRight = 0.875f,
                                cropBottom = 1f
                            )
                            "4:5" -> params.copy(
                                cropAspectRatio = "4:5",
                                cropLeft = 0.1f,
                                cropTop = 0f,
                                cropRight = 0.9f,
                                cropBottom = 1f
                            )
                            "16:9" -> params.copy(
                                cropAspectRatio = "16:9",
                                cropLeft = 0f,
                                cropTop = 0.15f,
                                cropRight = 1f,
                                cropBottom = 0.85f
                            )
                            "3:2" -> params.copy(
                                cropAspectRatio = "3:2",
                                cropLeft = 0f,
                                cropTop = 0.1f,
                                cropRight = 1f,
                                cropBottom = 0.9f
                            )
                            else -> params.copy(
                                cropAspectRatio = "ORIGINAL",
                                cropLeft = 0f,
                                cropTop = 0f,
                                cropRight = 1f,
                                cropBottom = 1f
                            )
                        }
                        onParamsChange(updated)
                    },
                    label = { Text(ratio, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("ratio_$ratio")
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Transform",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val nextRotation = (params.rotationDegrees + 90) % 360
                    onParamsChange(params.copy(rotationDegrees = nextRotation))
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_rotate_90"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Rotate90DegreesCw, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Rotate", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = {
                    onParamsChange(params.copy(flipHorizontal = !params.flipHorizontal))
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_flip_h"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Flip H", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = {
                    onParamsChange(params.copy(flipVertical = !params.flipVertical))
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_flip_v"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Flip V", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = {
                onParamsChange(
                    params.copy(
                        cropAspectRatio = "ORIGINAL",
                        cropLeft = 0f,
                        cropTop = 0f,
                        cropRight = 1f,
                        cropBottom = 1f,
                        rotationDegrees = 0,
                        flipHorizontal = false,
                        flipVertical = false
                    )
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_reset_crop")
        ) {
            Icon(Icons.Default.RotateLeft, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset Crop & Transform", fontSize = 13.sp)
        }
    }
}
