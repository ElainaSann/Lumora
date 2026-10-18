package com.example.lumora.data.model

import org.json.JSONArray
import org.json.JSONObject

enum class HslColor(val label: String, val baseColorHex: Long) {
    RED("Red", 0xFFEF4444),
    ORANGE("Orange", 0xFFF97316),
    YELLOW("Yellow", 0xFFEAB308),
    GREEN("Green", 0xFF22C55E),
    AQUA("Aqua", 0xFF06B6D4),
    BLUE("Blue", 0xFF3B82F6),
    PURPLE("Purple", 0xFFA855F7),
    MAGENTA("Magenta", 0xFFEC4899)
}

data class HslAdjustment(
    val hue: Float = 0f,        // -100 to +100
    val sat: Float = 0f,        // -100 to +100
    val lum: Float = 0f         // -100 to +100
) {
    fun isDefault(): Boolean = hue == 0f && sat == 0f && lum == 0f
}

data class CurvePoint(val x: Float, val y: Float)

data class EditingParams(
    // Light
    val exposure: Float = 0f,       // -100 to +100
    val brightness: Float = 0f,     // -100 to +100
    val contrast: Float = 0f,       // -100 to +100
    val highlights: Float = 0f,     // -100 to +100
    val shadows: Float = 0f,        // -100 to +100
    val whites: Float = 0f,         // -100 to +100
    val blacks: Float = 0f,         // -100 to +100
    val gamma: Float = 1.0f,        // 0.5 to 2.0 (1.0 = neutral)

    // Color
    val temperature: Float = 0f,    // -100 (cool/blue) to +100 (warm/amber)
    val tint: Float = 0f,           // -100 (green) to +100 (magenta)
    val saturation: Float = 0f,     // -100 to +100
    val vibrance: Float = 0f,       // -100 to +100
    val hue: Float = 0f,            // -180 to +180

    // Color Grading
    val shadowsHue: Float = 210f,   // 0 to 360
    val shadowsSat: Float = 0f,     // 0 to 100
    val midtonesHue: Float = 35f,   // 0 to 360
    val midtonesSat: Float = 0f,    // 0 to 100
    val highlightsHue: Float = 45f, // 0 to 360
    val highlightsSat: Float = 0f,  // 0 to 100
    val gradingBalance: Float = 0f, // -100 to +100

    // HSL Adjustments per Color Channel
    val hslAdjustments: Map<HslColor, HslAdjustment> = HslColor.entries.associateWith { HslAdjustment() },

    // Detail
    val sharpening: Float = 0f,     // 0 to 100
    val noiseReduction: Float = 0f, // 0 to 100
    val clarity: Float = 0f,        // -100 to +100
    val texture: Float = 0f,        // -100 to +100
    val dehaze: Float = 0f,         // -100 to +100

    // Effects
    val vignette: Float = 0f,       // -100 to +100
    val grain: Float = 0f,          // 0 to 100
    val fade: Float = 0f,           // 0 to 100
    val blur: Float = 0f,           // 0 to 100

    // Curves (Tone Curves)
    val rgbCurve: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
    val redCurve: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
    val greenCurve: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
    val blueCurve: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),

    // Crop & Transform
    val cropAspectRatio: String = "ORIGINAL", // FREE, 1:1, 4:5, 3:2, 16:9, ORIGINAL
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f,
    val rotationDegrees: Int = 0,    // 0, 90, 180, 270
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,

    // Preset reference
    val appliedPresetName: String? = null,

    // Text Overlay
    val textOverlay: String = "",
    val textFontId: String = "bebas_neue",
    val textCustomFontPath: String? = null,
    val textSize: Float = 36f,
    val textColorHex: Long = 0xFFFFFFFF,
    val textAlignment: String = "CENTER",
    val textOpacity: Float = 1.0f
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("exposure", exposure.toDouble())
        root.put("brightness", brightness.toDouble())
        root.put("contrast", contrast.toDouble())
        root.put("highlights", highlights.toDouble())
        root.put("shadows", shadows.toDouble())
        root.put("whites", whites.toDouble())
        root.put("blacks", blacks.toDouble())
        root.put("gamma", gamma.toDouble())

        root.put("temperature", temperature.toDouble())
        root.put("tint", tint.toDouble())
        root.put("saturation", saturation.toDouble())
        root.put("vibrance", vibrance.toDouble())
        root.put("hue", hue.toDouble())

        root.put("shadowsHue", shadowsHue.toDouble())
        root.put("shadowsSat", shadowsSat.toDouble())
        root.put("midtonesHue", midtonesHue.toDouble())
        root.put("midtonesSat", midtonesSat.toDouble())
        root.put("highlightsHue", highlightsHue.toDouble())
        root.put("highlightsSat", highlightsSat.toDouble())
        root.put("gradingBalance", gradingBalance.toDouble())

        val hslJson = JSONObject()
        for ((color, adj) in hslAdjustments) {
            val obj = JSONObject()
            obj.put("h", adj.hue.toDouble())
            obj.put("s", adj.sat.toDouble())
            obj.put("l", adj.lum.toDouble())
            hslJson.put(color.name, obj)
        }
        root.put("hsl", hslJson)

        root.put("sharpening", sharpening.toDouble())
        root.put("noiseReduction", noiseReduction.toDouble())
        root.put("clarity", clarity.toDouble())
        root.put("texture", texture.toDouble())
        root.put("dehaze", dehaze.toDouble())

        root.put("vignette", vignette.toDouble())
        root.put("grain", grain.toDouble())
        root.put("fade", fade.toDouble())
        root.put("blur", blur.toDouble())

        fun serializeCurve(points: List<CurvePoint>): JSONArray {
            val arr = JSONArray()
            points.forEach {
                val pt = JSONObject()
                pt.put("x", it.x.toDouble())
                pt.put("y", it.y.toDouble())
                arr.put(pt)
            }
            return arr
        }
        root.put("rgbCurve", serializeCurve(rgbCurve))
        root.put("redCurve", serializeCurve(redCurve))
        root.put("greenCurve", serializeCurve(greenCurve))
        root.put("blueCurve", serializeCurve(blueCurve))

        root.put("cropAspectRatio", cropAspectRatio)
        root.put("cropLeft", cropLeft.toDouble())
        root.put("cropTop", cropTop.toDouble())
        root.put("cropRight", cropRight.toDouble())
        root.put("cropBottom", cropBottom.toDouble())
        root.put("rotationDegrees", rotationDegrees)
        root.put("flipHorizontal", flipHorizontal)
        root.put("flipVertical", flipVertical)
        root.put("appliedPresetName", appliedPresetName ?: "")

        root.put("textOverlay", textOverlay)
        root.put("textFontId", textFontId)
        root.put("textCustomFontPath", textCustomFontPath ?: "")
        root.put("textSize", textSize.toDouble())
        root.put("textColorHex", textColorHex)
        root.put("textAlignment", textAlignment)
        root.put("textOpacity", textOpacity.toDouble())

        return root.toString()
    }

    companion object {
        fun fromJson(jsonStr: String?): EditingParams {
            if (jsonStr.isNullOrBlank()) return EditingParams()
            return try {
                val obj = JSONObject(jsonStr)
                val hslMap = mutableMapOf<HslColor, HslAdjustment>()
                val hslJson = obj.optJSONObject("hsl")
                HslColor.entries.forEach { color ->
                    val colorObj = hslJson?.optJSONObject(color.name)
                    if (colorObj != null) {
                        hslMap[color] = HslAdjustment(
                            hue = colorObj.optDouble("h", 0.0).toFloat(),
                            sat = colorObj.optDouble("s", 0.0).toFloat(),
                            lum = colorObj.optDouble("l", 0.0).toFloat()
                        )
                    } else {
                        hslMap[color] = HslAdjustment()
                    }
                }

                fun deserializeCurve(arr: JSONArray?): List<CurvePoint> {
                    if (arr == null || arr.length() == 0) return listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))
                    val pts = mutableListOf<CurvePoint>()
                    for (i in 0 until arr.length()) {
                        val pt = arr.getJSONObject(i)
                        pts.add(CurvePoint(pt.getDouble("x").toFloat(), pt.getDouble("y").toFloat()))
                    }
                    return pts.sortedBy { it.x }
                }

                EditingParams(
                    exposure = obj.optDouble("exposure", 0.0).toFloat(),
                    brightness = obj.optDouble("brightness", 0.0).toFloat(),
                    contrast = obj.optDouble("contrast", 0.0).toFloat(),
                    highlights = obj.optDouble("highlights", 0.0).toFloat(),
                    shadows = obj.optDouble("shadows", 0.0).toFloat(),
                    whites = obj.optDouble("whites", 0.0).toFloat(),
                    blacks = obj.optDouble("blacks", 0.0).toFloat(),
                    gamma = obj.optDouble("gamma", 1.0).toFloat(),

                    temperature = obj.optDouble("temperature", 0.0).toFloat(),
                    tint = obj.optDouble("tint", 0.0).toFloat(),
                    saturation = obj.optDouble("saturation", 0.0).toFloat(),
                    vibrance = obj.optDouble("vibrance", 0.0).toFloat(),
                    hue = obj.optDouble("hue", 0.0).toFloat(),

                    shadowsHue = obj.optDouble("shadowsHue", 210.0).toFloat(),
                    shadowsSat = obj.optDouble("shadowsSat", 0.0).toFloat(),
                    midtonesHue = obj.optDouble("midtonesHue", 35.0).toFloat(),
                    midtonesSat = obj.optDouble("midtonesSat", 0.0).toFloat(),
                    highlightsHue = obj.optDouble("highlightsHue", 45.0).toFloat(),
                    highlightsSat = obj.optDouble("highlightsSat", 0.0).toFloat(),
                    gradingBalance = obj.optDouble("gradingBalance", 0.0).toFloat(),

                    hslAdjustments = hslMap,

                    sharpening = obj.optDouble("sharpening", 0.0).toFloat(),
                    noiseReduction = obj.optDouble("noiseReduction", 0.0).toFloat(),
                    clarity = obj.optDouble("clarity", 0.0).toFloat(),
                    texture = obj.optDouble("texture", 0.0).toFloat(),
                    dehaze = obj.optDouble("dehaze", 0.0).toFloat(),

                    vignette = obj.optDouble("vignette", 0.0).toFloat(),
                    grain = obj.optDouble("grain", 0.0).toFloat(),
                    fade = obj.optDouble("fade", 0.0).toFloat(),
                    blur = obj.optDouble("blur", 0.0).toFloat(),

                    rgbCurve = deserializeCurve(obj.optJSONArray("rgbCurve")),
                    redCurve = deserializeCurve(obj.optJSONArray("redCurve")),
                    greenCurve = deserializeCurve(obj.optJSONArray("greenCurve")),
                    blueCurve = deserializeCurve(obj.optJSONArray("blueCurve")),

                    cropAspectRatio = obj.optString("cropAspectRatio", "ORIGINAL"),
                    cropLeft = obj.optDouble("cropLeft", 0.0).toFloat(),
                    cropTop = obj.optDouble("cropTop", 0.0).toFloat(),
                    cropRight = obj.optDouble("cropRight", 1.0).toFloat(),
                    cropBottom = obj.optDouble("cropBottom", 1.0).toFloat(),
                    rotationDegrees = obj.optInt("rotationDegrees", 0),
                    flipHorizontal = obj.optBoolean("flipHorizontal", false),
                    flipVertical = obj.optBoolean("flipVertical", false),
                    appliedPresetName = obj.optString("appliedPresetName", "").ifBlank { null },

                    textOverlay = obj.optString("textOverlay", ""),
                    textFontId = obj.optString("textFontId", "bebas_neue"),
                    textCustomFontPath = obj.optString("textCustomFontPath", "").ifBlank { null },
                    textSize = obj.optDouble("textSize", 36.0).toFloat(),
                    textColorHex = obj.optLong("textColorHex", 0xFFFFFFFF),
                    textAlignment = obj.optString("textAlignment", "CENTER"),
                    textOpacity = obj.optDouble("textOpacity", 1.0).toFloat()
                )
            } catch (e: Exception) {
                EditingParams()
            }
        }
    }
}
