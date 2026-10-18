package com.example.lumora.ui.screens.editor.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.lumora.data.model.EditingParams

@Composable
fun ColorControl(
    params: EditingParams,
    onParamsChange: (EditingParams) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("color_panel")
    ) {
        AdjustmentSlider(
            label = "Temperature",
            value = params.temperature,
            onValueChange = { onParamsChange(params.copy(temperature = it)) },
            range = -100f..100f,
            defaultValue = 0f,
            activeColor = if (params.temperature >= 0) Color(0xFFF59E0B) else Color(0xFF38BDF8)
        )
        AdjustmentSlider(
            label = "Tint",
            value = params.tint,
            onValueChange = { onParamsChange(params.copy(tint = it)) },
            range = -100f..100f,
            defaultValue = 0f,
            activeColor = if (params.tint >= 0) Color(0xFFEC4899) else Color(0xFF22C55E)
        )
        AdjustmentSlider(
            label = "Vibrance",
            value = params.vibrance,
            onValueChange = { onParamsChange(params.copy(vibrance = it)) },
            range = -100f..100f,
            defaultValue = 0f
        )
        AdjustmentSlider(
            label = "Saturation",
            value = params.saturation,
            onValueChange = { onParamsChange(params.copy(saturation = it)) },
            range = -100f..100f,
            defaultValue = 0f
        )
        AdjustmentSlider(
            label = "Hue Shift",
            value = params.hue,
            onValueChange = { onParamsChange(params.copy(hue = it)) },
            range = -180f..180f,
            defaultValue = 0f
        )
    }
}
