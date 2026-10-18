package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.lumora.data.model.CurvePoint
import com.example.lumora.data.model.EditingParams
import com.example.lumora.data.model.HslAdjustment
import com.example.lumora.data.model.HslColor
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LUMORA", appName)
    }

    @Test
    fun testEditingParamsJsonRoundTrip() {
        val original = EditingParams(
            exposure = 15f,
            contrast = 25f,
            temperature = -10f,
            tint = 5f,
            vignette = -20f,
            grain = 15f,
            shadowsHue = 180f,
            shadowsSat = 30f,
            hslAdjustments = mapOf(
                HslColor.RED to HslAdjustment(hue = 10f, sat = -15f, lum = 5f)
            ),
            rgbCurve = listOf(
                CurvePoint(0f, 0f),
                CurvePoint(0.5f, 0.6f),
                CurvePoint(1f, 1f)
            ),
            cropAspectRatio = "16:9",
            textOverlay = "LUMORA STUDIO",
            textFontId = "arcade_classic",
            textSize = 42f
        )

        val json = original.toJson()
        val restored = EditingParams.fromJson(json)

        assertEquals(original.exposure, restored.exposure, 0.01f)
        assertEquals(original.contrast, restored.contrast, 0.01f)
        assertEquals(original.temperature, restored.temperature, 0.01f)
        assertEquals(original.vignette, restored.vignette, 0.01f)
        assertEquals(original.cropAspectRatio, restored.cropAspectRatio)
        assertEquals(original.textOverlay, restored.textOverlay)
        assertEquals(original.textFontId, restored.textFontId)
        assertEquals(original.textSize, restored.textSize, 0.01f)
        assertEquals(10f, restored.hslAdjustments[HslColor.RED]?.hue ?: 0f, 0.01f)
        assertEquals(3, restored.rgbCurve.size)
        assertEquals(0.6f, restored.rgbCurve[1].y, 0.01f)
    }
}
