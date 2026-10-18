package com.example.lumora.ui.screens.editor.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.lumora.data.model.EditingParams

@Composable
fun LightControl(
    params: EditingParams,
    onParamsChange: (EditingParams) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("light_panel")
    ) {
        AdjustmentSlider(
            label = "Exposure",
            value = params.exposure,
            onValueChange = { onParamsChange(params.copy(exposure = it)) },
            range = -100f..100f,
            defaultValue = 0f
        )
        AdjustmentSlider(
            label = "Brightness",
            value = params.brightness,
            onValueChange = { onParamsChange(params.copy(brightness = it)) },
            range = -100f..100f,
            defaultValue = 0f
        )
        AdjustmentSlider(
            label = "Contrast",
            value = params.contrast,
            onValueChange = { onParamsChange(params.copy(contrast = it)) },
            range = -100f..100f,
            defaultValue = 0f
        )
        AdjustmentSlider(
            label = "Highlights",
            value = params.highlights,
            onValueChange = { onParamsChange(params.copy(highlights = it)) },
            range = -100f..100f,
            defaultValue = 0f
        )
        AdjustmentSlider(
            label = "Shadows",
            value = params.shadows,
            onValueChange = { onParamsChange(params.copy(shadows = it)) },
            range = -100f..100f,
            defaultValue = 0f
        )
        AdjustmentSlider(
            label = "Whites",
            value = params.whites,
            onValueChange = { onParamsChange(params.copy(whites = it)) },
            range = -100f..100f,
            defaultValue = 0f
        )
        AdjustmentSlider(
            label = "Blacks",
            value = params.blacks,
            onValueChange = { onParamsChange(params.copy(blacks = it)) },
            range = -100f..100f,
            defaultValue = 0f
        )
        AdjustmentSlider(
            label = "Gamma",
            value = params.gamma,
            onValueChange = { onParamsChange(params.copy(gamma = it)) },
            range = 0.5f..2.0f,
            defaultValue = 1.0f,
            formatAsInteger = false
        )
    }
}
