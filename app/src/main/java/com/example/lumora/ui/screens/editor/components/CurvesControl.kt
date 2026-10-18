package com.example.lumora.ui.screens.editor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lumora.data.model.CurvePoint
import com.example.lumora.data.model.EditingParams
import kotlin.math.sqrt

enum class CurveChannel(val label: String, val color: Color) {
    RGB("RGB", Color(0xFF14B8A6)),
    RED("Red", Color(0xFFEF4444)),
    GREEN("Green", Color(0xFF22C55E)),
    BLUE("Blue", Color(0xFF3B82F6))
}

data class BezierPreset(
    val id: String,
    val name: String,
    val p0: CurvePoint = CurvePoint(0f, 0f),
    val p1: CurvePoint,
    val p2: CurvePoint,
    val p3: CurvePoint = CurvePoint(1f, 1f)
)

val BEZIER_PRESETS = listOf(
    BezierPreset(
        id = "linear",
        name = "LINEAR",
        p1 = CurvePoint(0.33f, 0.33f),
        p2 = CurvePoint(0.67f, 0.67f)
    ),
    BezierPreset(
        id = "s_curve",
        name = "CUBIC BEZIER EASING",
        p1 = CurvePoint(0.42f, 0.08f),
        p2 = CurvePoint(0.58f, 0.92f)
    ),
    BezierPreset(
        id = "ease_in",
        name = "EASE IN / SHADOWS",
        p1 = CurvePoint(0.45f, 0.05f),
        p2 = CurvePoint(0.95f, 0.75f)
    ),
    BezierPreset(
        id = "ease_out",
        name = "EASE OUT / HIGHLIGHTS",
        p1 = CurvePoint(0.05f, 0.25f),
        p2 = CurvePoint(0.55f, 0.95f)
    ),
    BezierPreset(
        id = "film_matte",
        name = "FILM MATTE / FADE",
        p0 = CurvePoint(0f, 0.12f),
        p1 = CurvePoint(0.35f, 0.22f),
        p2 = CurvePoint(0.65f, 0.88f),
        p3 = CurvePoint(1f, 0.95f)
    )
)

@Composable
fun CurvesControl(
    params: EditingParams,
    onParamsChange: (EditingParams) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedChannel by remember { mutableStateOf(CurveChannel.RGB) }
    var selectedPresetIndex by remember { mutableIntStateOf(1) } // default: Cubic Bezier S-Curve

    // Current curve for selected channel
    val rawPoints = when (selectedChannel) {
        CurveChannel.RGB -> params.rgbCurve
        CurveChannel.RED -> params.redCurve
        CurveChannel.GREEN -> params.greenCurve
        CurveChannel.BLUE -> params.blueCurve
    }

    // Ensure 4 Bezier control points [p0, p1, p2, p3]
    val bezierPoints = remember(rawPoints) {
        if (rawPoints.size == 4) {
            rawPoints
        } else if (rawPoints.size == 2) {
            listOf(
                rawPoints[0],
                CurvePoint(0.33f, 0.33f),
                CurvePoint(0.67f, 0.67f),
                rawPoints[1]
            )
        } else {
            listOf(
                CurvePoint(0f, 0f),
                CurvePoint(0.42f, 0.08f),
                CurvePoint(0.58f, 0.92f),
                CurvePoint(1f, 1f)
            )
        }
    }

    var p0 = bezierPoints[0]
    var p1 = bezierPoints[1]
    var p2 = bezierPoints[2]
    var p3 = bezierPoints[3]

    // Active dragged handle index: 0=p0, 1=p1, 2=p2, 3=p3, -1=none
    var draggedIndex by remember { mutableIntStateOf(-1) }

    fun updatePoints(newP0: CurvePoint, newP1: CurvePoint, newP2: CurvePoint, newP3: CurvePoint) {
        val list = listOf(newP0, newP1, newP2, newP3)
        val updatedParams = when (selectedChannel) {
            CurveChannel.RGB -> params.copy(rgbCurve = list)
            CurveChannel.RED -> params.copy(redCurve = list)
            CurveChannel.GREEN -> params.copy(greenCurve = list)
            CurveChannel.BLUE -> params.copy(blueCurve = list)
        }
        onParamsChange(updatedParams)
    }

    fun applyPreset(preset: BezierPreset) {
        updatePoints(preset.p0, preset.p1, preset.p2, preset.p3)
    }

    // App signature colors
    val curveColor = when (selectedChannel) {
        CurveChannel.RGB -> Color(0xFF14B8A6) // Lumora Teal
        CurveChannel.RED -> Color(0xFFEF4444)
        CurveChannel.GREEN -> Color(0xFF22C55E)
        CurveChannel.BLUE -> Color(0xFF3B82F6)
    }

    // Main Curve Container - Exactly matching user screenshot styling
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF141923)) // Luxury dark studio canvas matching Alight Motion screenshot
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("curves_panel")
    ) {
        // Channel Tabs Header & Reset
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CurveChannel.entries.forEach { ch ->
                    val isSelected = selectedChannel == ch
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) ch.color else Color(0xFF222B3D))
                            .border(
                                width = if (isSelected) 1.dp else 0.5.dp,
                                color = if (isSelected) Color.White.copy(alpha = 0.3f) else Color(0xFF2E394E),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable { selectedChannel = ch }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                            .testTag("curve_channel_${ch.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ch.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            IconButton(
                onClick = {
                    val defPreset = BEZIER_PRESETS[0]
                    selectedPresetIndex = 0
                    applyPreset(defPreset)
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Reset Curve",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Central Graph + Right Presets Column (Exact Layout from screenshot)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Main Bezier Graph Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF181F2C))
                    .border(1.dp, Color(0xFF263248), RoundedCornerShape(8.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(p0, p1, p2, p3) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val w = size.width
                                    val h = size.height
                                    val touchX = offset.x
                                    val touchY = offset.y

                                    // Screen positions of handles
                                    val s0 = Offset(p0.x * w, h - p0.y * h)
                                    val s1 = Offset(p1.x * w, h - p1.y * h)
                                    val s2 = Offset(p2.x * w, h - p2.y * h)
                                    val s3 = Offset(p3.x * w, h - p3.y * h)

                                    val threshold = 40.dp.toPx()

                                    fun dist(a: Offset, b: Offset) =
                                        sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y))

                                    val d1 = dist(offset, s1)
                                    val d2 = dist(offset, s2)
                                    val d0 = dist(offset, s0)
                                    val d3 = dist(offset, s3)

                                    draggedIndex = when {
                                        d1 < threshold && d1 <= d2 -> 1
                                        d2 < threshold -> 2
                                        d0 < threshold && d0 <= d3 -> 0
                                        d3 < threshold -> 3
                                        else -> if (offset.x < w * 0.5f) 1 else 2
                                    }
                                }
                            )
                        }
                        .pointerInput(p0, p1, p2, p3) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val w = size.width
                                    val h = size.height

                                    val s1 = Offset(p1.x * w, h - p1.y * h)
                                    val s2 = Offset(p2.x * w, h - p2.y * h)
                                    val s0 = Offset(p0.x * w, h - p0.y * h)
                                    val s3 = Offset(p3.x * w, h - p3.y * h)

                                    val threshold = 44.dp.toPx()
                                    fun dist(a: Offset, b: Offset) =
                                        sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y))

                                    val d1 = dist(offset, s1)
                                    val d2 = dist(offset, s2)
                                    val d0 = dist(offset, s0)
                                    val d3 = dist(offset, s3)

                                    draggedIndex = when {
                                        d1 < threshold && d1 <= d2 -> 1
                                        d2 < threshold -> 2
                                        d0 < threshold && d0 <= d3 -> 0
                                        d3 < threshold -> 3
                                        else -> if (offset.x < w * 0.5f) 1 else 2
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val w = size.width
                                    val h = size.height

                                    val normX = (change.position.x / w).coerceIn(0f, 1f)
                                    val normY = (1f - (change.position.y / h)).coerceIn(0f, 1f)

                                    when (draggedIndex) {
                                        1 -> {
                                            p1 = CurvePoint(normX, normY)
                                            updatePoints(p0, p1, p2, p3)
                                        }
                                        2 -> {
                                            p2 = CurvePoint(normX, normY)
                                            updatePoints(p0, p1, p2, p3)
                                        }
                                        0 -> {
                                            p0 = CurvePoint(0f, normY)
                                            updatePoints(p0, p1, p2, p3)
                                        }
                                        3 -> {
                                            p3 = CurvePoint(1f, normY)
                                            updatePoints(p0, p1, p2, p3)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggedIndex = -1
                                }
                            )
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // 1. Draw Fine Coordinate Grid (like in Alight Motion reference)
                    val gridColor = Color(0xFF26334D)
                    val cols = 12
                    val rows = 10
                    for (i in 1 until cols) {
                        val x = w * (i.toFloat() / cols)
                        drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 0.8f)
                    }
                    for (j in 1 until rows) {
                        val y = h * (j.toFloat() / rows)
                        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 0.8f)
                    }

                    // 2. Dashed Upper/Lower Limits and Center Divider (matching screenshot)
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    val dashedColor = Color(0xFF4A5568)

                    // Vertical center line (X = 0.5)
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(w * 0.5f, 0f),
                        end = Offset(w * 0.5f, h),
                        strokeWidth = 1.2f,
                        pathEffect = dashEffect
                    )

                    // Horizontal top dashed line (Y = 1.0 margin)
                    val topMargin = h * 0.15f
                    val bottomMargin = h * 0.85f
                    drawLine(
                        color = dashedColor,
                        start = Offset(0f, topMargin),
                        end = Offset(w, topMargin),
                        strokeWidth = 1f,
                        pathEffect = dashEffect
                    )
                    drawLine(
                        color = dashedColor,
                        start = Offset(0f, bottomMargin),
                        end = Offset(w, bottomMargin),
                        strokeWidth = 1f,
                        pathEffect = dashEffect
                    )

                    // 3. Screen coordinates for Bezier anchors & handles
                    val screenP0 = Offset(p0.x * w, h - p0.y * h)
                    val screenP1 = Offset(p1.x * w, h - p1.y * h)
                    val screenP2 = Offset(p2.x * w, h - p2.y * h)
                    val screenP3 = Offset(p3.x * w, h - p3.y * h)

                    // 4. Tangent arms (solid white lines from anchors to handles)
                    drawLine(
                        color = Color.White.copy(alpha = 0.85f),
                        start = screenP0,
                        end = screenP1,
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.85f),
                        start = screenP3,
                        end = screenP2,
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // 5. Draw Smooth Cubic Bezier Curve (Teal vibrant line)
                    val curvePath = Path()
                    curvePath.moveTo(screenP0.x, screenP0.y)
                    val steps = 60
                    for (i in 1..steps) {
                        val t = i.toFloat() / steps
                        val u = 1f - t
                        val bx = u * u * u * p0.x + 3f * u * u * t * p1.x + 3f * u * t * t * p2.x + t * t * t * p3.x
                        val by = u * u * u * p0.y + 3f * u * u * t * p1.y + 3f * u * t * t * p2.y + t * t * t * p3.y
                        curvePath.lineTo(bx * w, h - by * h)
                    }

                    drawPath(
                        path = curvePath,
                        color = curveColor,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 6. Anchor Dots (P0 and P3) in Teal
                    drawCircle(
                        color = curveColor,
                        radius = 5.dp.toPx(),
                        center = screenP0
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = screenP0
                    )

                    drawCircle(
                        color = curveColor,
                        radius = 5.dp.toPx(),
                        center = screenP3
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = screenP3
                    )

                    // 7. Large White Circular Control Handles (Handle 1 & Handle 2)
                    // Drop shadow
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.45f),
                        radius = 13.dp.toPx(),
                        center = Offset(screenP1.x, screenP1.y + 1.5.dp.toPx())
                    )
                    // Solid white handle 1
                    drawCircle(
                        color = Color.White,
                        radius = 12.dp.toPx(),
                        center = screenP1
                    )
                    // Inner ring indicator if being dragged
                    if (draggedIndex == 1) {
                        drawCircle(
                            color = curveColor,
                            radius = 6.dp.toPx(),
                            center = screenP1
                        )
                    }

                    // Handle 2
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.45f),
                        radius = 13.dp.toPx(),
                        center = Offset(screenP2.x, screenP2.y + 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 12.dp.toPx(),
                        center = screenP2
                    )
                    if (draggedIndex == 2) {
                        drawCircle(
                            color = curveColor,
                            radius = 6.dp.toPx(),
                            center = screenP2
                        )
                    }
                }
            }

            // Right Vertical Presets Sidebar (Exact thumbnail cards from reference image)
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BEZIER_PRESETS.forEachIndexed { idx, preset ->
                    val isPresetActive = idx == selectedPresetIndex
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isPresetActive) Color(0xFF1E293B) else Color(0xFF131824)
                            )
                            .border(
                                width = if (isPresetActive) 2.dp else 1.dp,
                                color = if (isPresetActive) curveColor else Color(0xFF2A364F),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedPresetIndex = idx
                                applyPreset(preset)
                            }
                            .testTag("bezier_preset_${preset.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Miniature Curve Thumbnail Preview
                        Canvas(modifier = Modifier.size(24.dp)) {
                            val pw = size.width
                            val ph = size.height

                            // Miniature dashed box
                            val dash = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f)
                            drawRect(
                                color = Color.White.copy(alpha = 0.25f),
                                size = size,
                                style = Stroke(width = 0.8f, pathEffect = dash)
                            )

                            // Miniature curve
                            val path = Path()
                            val sP0 = Offset(preset.p0.x * pw, ph - preset.p0.y * ph)
                            path.moveTo(sP0.x, sP0.y)
                            val nSteps = 15
                            for (s in 1..nSteps) {
                                val t = s.toFloat() / nSteps
                                val u = 1f - t
                                val bx = u * u * u * preset.p0.x + 3f * u * u * t * preset.p1.x + 3f * u * t * t * preset.p2.x + t * t * t * preset.p3.x
                                val by = u * u * u * preset.p0.y + 3f * u * u * t * preset.p1.y + 3f * u * t * t * preset.p2.y + t * t * t * preset.p3.y
                                path.lineTo(bx * pw, ph - by * ph)
                            }
                            drawPath(
                                path = path,
                                color = if (isPresetActive) curveColor else Color.White,
                                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Tiny end dots
                            drawCircle(
                                color = if (isPresetActive) curveColor else Color.White,
                                radius = 1.5.dp.toPx(),
                                center = sP0
                            )
                            drawCircle(
                                color = if (isPresetActive) curveColor else Color.White,
                                radius = 1.5.dp.toPx(),
                                center = Offset(preset.p3.x * pw, ph - preset.p3.y * ph)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bottom Navigation Bar matching "< CUBIC BEZIER EASING >"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(Color(0xFF101520), RoundedCornerShape(8.dp))
                .border(0.5.dp, Color(0xFF263248), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left invert / flip handles button
            IconButton(
                onClick = {
                    val invP1 = CurvePoint(1f - p1.x, 1f - p1.y)
                    val invP2 = CurvePoint(1f - p2.x, 1f - p2.y)
                    updatePoints(p0, invP1, invP2, p3)
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Invert Handles",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Arrow Left
            IconButton(
                onClick = {
                    selectedPresetIndex = if (selectedPresetIndex > 0) selectedPresetIndex - 1 else BEZIER_PRESETS.lastIndex
                    applyPreset(BEZIER_PRESETS[selectedPresetIndex])
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous preset",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Current preset label
            Text(
                text = BEZIER_PRESETS[selectedPresetIndex].name,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    fontSize = 11.sp
                ),
                color = Color.White
            )

            // Arrow Right
            IconButton(
                onClick = {
                    selectedPresetIndex = (selectedPresetIndex + 1) % BEZIER_PRESETS.size
                    applyPreset(BEZIER_PRESETS[selectedPresetIndex])
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next preset",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
