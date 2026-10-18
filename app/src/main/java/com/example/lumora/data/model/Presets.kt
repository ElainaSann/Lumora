package com.example.lumora.data.model

data class Preset(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val params: (EditingParams) -> EditingParams
)

object PresetRegistry {
    val presets = listOf(
        Preset(
            id = "natural",
            name = "Natural",
            category = "General",
            description = "Subtle clarity and balanced vibrancy for everyday shots",
            params = { current ->
                current.copy(
                    contrast = 10f,
                    vibrance = 15f,
                    highlights = -8f,
                    shadows = 12f,
                    clarity = 12f,
                    appliedPresetName = "Natural"
                )
            }
        ),
        Preset(
            id = "warm",
            name = "Warm Sunset",
            category = "Atmosphere",
            description = "Rich golden hour radiance with amber highlights",
            params = { current ->
                current.copy(
                    temperature = 28f,
                    tint = 6f,
                    vibrance = 18f,
                    highlights = -15f,
                    shadows = 10f,
                    highlightsHue = 42f,
                    highlightsSat = 26f,
                    shadowsHue = 210f,
                    shadowsSat = 12f,
                    appliedPresetName = "Warm Sunset"
                )
            }
        ),
        Preset(
            id = "cool",
            name = "Cool Nordic",
            category = "Atmosphere",
            description = "Crisp scandinavian aesthetic with clean cool tones",
            params = { current ->
                current.copy(
                    temperature = -25f,
                    tint = -5f,
                    contrast = 14f,
                    highlights = -10f,
                    shadows = 8f,
                    shadowsHue = 205f,
                    shadowsSat = 22f,
                    clarity = 15f,
                    appliedPresetName = "Cool Nordic"
                )
            }
        ),
        Preset(
            id = "cinematic",
            name = "Cinematic",
            category = "Stylized",
            description = "Blockbuster teal & orange grading with deep shadows",
            params = { current ->
                current.copy(
                    contrast = 22f,
                    shadowsHue = 195f,
                    shadowsSat = 35f,
                    highlightsHue = 36f,
                    highlightsSat = 28f,
                    vignette = -20f,
                    clarity = 18f,
                    fade = 12f,
                    appliedPresetName = "Cinematic"
                )
            }
        ),
        Preset(
            id = "moody",
            name = "Moody",
            category = "Stylized",
            description = "Dark moody tones with crushed blacks and muted greens",
            params = { current ->
                current.copy(
                    exposure = -8f,
                    contrast = 25f,
                    highlights = -25f,
                    shadows = -15f,
                    blacks = -18f,
                    saturation = -15f,
                    vignette = -30f,
                    fade = 16f,
                    clarity = 20f,
                    appliedPresetName = "Moody"
                )
            }
        ),
        Preset(
            id = "vintage",
            name = "Vintage 70s",
            category = "Retro",
            description = "Nostalgic analog film character with soft faded blacks",
            params = { current ->
                current.copy(
                    temperature = 18f,
                    tint = 12f,
                    contrast = -8f,
                    fade = 28f,
                    grain = 32f,
                    vignette = -22f,
                    saturation = -10f,
                    shadowsHue = 50f,
                    shadowsSat = 18f,
                    appliedPresetName = "Vintage 70s"
                )
            }
        ),
        Preset(
            id = "bw",
            name = "Classic B&W",
            category = "Monochrome",
            description = "Timeless black and white with punchy dynamic range",
            params = { current ->
                current.copy(
                    saturation = -100f,
                    contrast = 30f,
                    whites = 20f,
                    blacks = -15f,
                    clarity = 24f,
                    grain = 15f,
                    appliedPresetName = "Classic B&W"
                )
            }
        ),
        Preset(
            id = "portrait",
            name = "Portrait Glow",
            category = "People",
            description = "Soft skin illumination and gentle tonal compression",
            params = { current ->
                current.copy(
                    exposure = 4f,
                    temperature = 10f,
                    highlights = -18f,
                    shadows = 20f,
                    texture = -10f,
                    clarity = -6f,
                    vibrance = 12f,
                    appliedPresetName = "Portrait Glow"
                )
            }
        ),
        Preset(
            id = "film",
            name = "Film Look",
            category = "Retro",
            description = "Organic 35mm film grain, matte highlights, and rich midtones",
            params = { current ->
                current.copy(
                    contrast = 15f,
                    highlights = -20f,
                    shadows = 15f,
                    grain = 38f,
                    fade = 18f,
                    vignette = -15f,
                    appliedPresetName = "Film Look"
                )
            }
        )
    )
}
