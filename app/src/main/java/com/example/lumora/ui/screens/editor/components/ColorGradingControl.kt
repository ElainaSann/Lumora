package com.example.lumora.ui.screens.editor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lumora.data.model.EditingParams
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class GradingTarget(val label: String) {
    SHADOWS("Shadows"),
    MIDTONES("Midtones"),
    HIGHLIGHTS("Highlights")
}

@Composable
fun ColorGradingControl(
    params: EditingParams,
    onParamsChange: (EditingParams) -> Unit,
    modifier: Modifier = Modifier
) {
    var target by remember { mutableStateOf(GradingTarget.SHADOWS) }

    val currentHue = when (target) {
        GradingTarget.SHADOWS -> params.shadowsHue
        GradingTarget.MIDTONES -> params.midtonesHue
        GradingTarget.HIGHLIGHTS -> params.highlightsHue
    }

    val currentSat = when (target) {
        GradingTarget.SHADOWS -> params.shadowsSat
        GradingTarget.MIDTONES -> params.midtonesSat
        GradingTarget.HIGHLIGHTS -> params.highlightsSat
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("color_grading_panel")
    ) {
        // Tonal Target selector chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GradingTarget.entries.forEach { t ->
                    FilterChip(
                        selected = target == t,
                        onClick = { target = t },
                        label = { Text(t.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            IconButton(
                onClick = {
                    val updated = when (target) {
                        GradingTarget.SHADOWS -> params.copy(shadowsSat = 0f)
                        GradingTarget.MIDTONES -> params.copy(midtonesSat = 0f)
                        GradingTarget.HIGHLIGHTS -> params.copy(highlightsSat = 0f)
                    }
                    onParamsChange(updated)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.RotateLeft,
                    contentDescription = "Reset tonal grading",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive Color Wheel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val wheelSize = 180.dp
            Canvas(
                modifier = Modifier
                    .size(wheelSize)
                    .clip(CircleShape)
                    .pointerInput(target, currentHue, currentSat) {
                        fun updatePosition(pos: Offset) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = pos.x - cx
                            val dy = pos.y - cy
                            val dist = sqrt(dx * dx + dy * dy)
                            val maxRadius = cx

                            val normSat = (dist / maxRadius).coerceIn(0f, 1f) * 100f
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) angle += 360f

                            val updated = when (target) {
                                GradingTarget.SHADOWS -> params.copy(shadowsHue = angle, shadowsSat = normSat)
                                GradingTarget.MIDTONES -> params.copy(midtonesHue = angle, midtonesSat = normSat)
                                GradingTarget.HIGHLIGHTS -> params.copy(highlightsHue = angle, highlightsSat = normSat)
                            }
                            onParamsChange(updated)
                        }

                        detectTapGestures { pos -> updatePosition(pos) }
                    }
                    .pointerInput(target, currentHue, currentSat) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = change.position.x - cx
                            val dy = change.position.y - cy
                            val dist = sqrt(dx * dx + dy * dy)
                            val maxRadius = cx

                            val normSat = (dist / maxRadius).coerceIn(0f, 1f) * 100f
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) angle += 360f

                            val updated = when (target) {
                                GradingTarget.SHADOWS -> params.copy(shadowsHue = angle, shadowsSat = normSat)
                                GradingTarget.MIDTONES -> params.copy(midtonesHue = angle, midtonesSat = normSat)
                                GradingTarget.HIGHLIGHTS -> params.copy(highlightsHue = angle, highlightsSat = normSat)
                            }
                            onParamsChange(updated)
                        }
                    }
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val radius = cx

                // Draw chromatic circle
                val sweepColors = listOf(
                    Color.Red,
                    Color.Yellow,
                    Color.Green,
                    Color.Cyan,
                    Color.Blue,
                    Color.Magenta,
                    Color.Red
                )
                drawCircle(
                    brush = Brush.sweepGradient(sweepColors, Offset(cx, cy)),
                    radius = radius
                )

                // Overlay desaturation towards center
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Gray, Color.Transparent),
                        center = Offset(cx, cy),
                        radius = radius
                    ),
                    radius = radius
                )

                // Outer border
                drawCircle(
                    color = Color.White.copy(alpha = 0.4f),
                    radius = radius,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Draw handle for currently selected (hue, sat)
                val rad = Math.toRadians(currentHue.toDouble())
                val handleDist = (currentSat / 100f) * radius
                val hx = cx + (handleDist * cos(rad)).toFloat()
                val hy = cy + (handleDist * sin(rad)).toFloat()

                drawCircle(
                    color = Color.Black.copy(alpha = 0.7f),
                    radius = 9.dp.toPx(),
                    center = Offset(hx, hy)
                )
                drawCircle(
                    color = Color.White,
                    radius = 7.dp.toPx(),
                    center = Offset(hx, hy)
                )
            }
        }

        // Numerical readout & balance slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Hue: ${currentHue.roundToInt()}°",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Sat: ${currentSat.roundToInt()}%",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        AdjustmentSlider(
            label = "Grading Balance",
            value = params.gradingBalance,
            onValueChange = { onParamsChange(params.copy(gradingBalance = it)) },
            range = -100f..100f,
            defaultValue = 0f
        )
    }
}
