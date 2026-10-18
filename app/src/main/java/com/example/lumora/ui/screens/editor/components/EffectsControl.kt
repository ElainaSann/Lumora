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
fun EffectsControl(
    params: EditingParams,
    onParamsChange: (EditingParams) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("effects_panel")
    ) {
        AdjustmentSlider(
            label = "Vignette",
            value = params.vignette,
            onValueChange = { onParamsChange(params.copy(vignette = it)) },
            range = -100f..100f,
            defaultValue = 0f
        )
        AdjustmentSlider(
            label = "Film Grain",
            value = params.grain,
            onValueChange = { onParamsChange(params.copy(grain = it)) },
            range = 0f..100f,
            defaultValue = 0f
        )
        AdjustmentSlider(
            label = "Matte Fade",
            value = params.fade,
            onValueChange = { onParamsChange(params.copy(fade = it)) },
            range = 0f..100f,
            defaultValue = 0f
        )
        AdjustmentSlider(
            label = "Atmospheric Blur",
            value = params.blur,
            onValueChange = { onParamsChange(params.copy(blur = it)) },
            range = 0f..100f,
            defaultValue = 0f
        )
    }
}
