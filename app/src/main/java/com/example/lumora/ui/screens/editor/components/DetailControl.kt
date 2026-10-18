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
fun DetailControl(
    params: EditingParams,
    onParamsChange: (EditingParams) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("detail_panel")
    ) {
        AdjustmentSlider(
            label = "Sharpening",
            value = params.sharpening,
            onValueChange = { onParamsChange(params.copy(sharpening = it)) },
            range = 0f..100f,
            defaultValue = 0f
        )
        AdjustmentSlider(
            label = "Clarity",
            value = params.clarity,
            onValueChange = { onParamsChange(params.copy(clarity = it)) },
            range = -100f..100f,
            defaultValue = 0f
        )
        AdjustmentSlider(
            label = "Texture",
            value = params.texture,
            onValueChange = { onParamsChange(params.copy(texture = it)) },
            range = -100f..100f,
            defaultValue = 0f
        )
        AdjustmentSlider(
            label = "Dehaze",
            value = params.dehaze,
            onValueChange = { onParamsChange(params.copy(dehaze = it)) },
            range = -100f..100f,
            defaultValue = 0f
        )
        AdjustmentSlider(
            label = "Noise Reduction",
            value = params.noiseReduction,
            onValueChange = { onParamsChange(params.copy(noiseReduction = it)) },
            range = 0f..100f,
            defaultValue = 0f
        )
    }
}
