package com.example

import com.example.lumora.data.model.CurvePoint
import com.example.lumora.data.model.EditingParams
import com.example.lumora.data.model.PresetRegistry
import com.example.lumora.engine.ImageProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testCurveLutEvaluationBounds() {
        val points = listOf(
            CurvePoint(0f, 0f),
            CurvePoint(0.5f, 0.7f),
            CurvePoint(1f, 1f)
        )
        val lut = ImageProcessor.buildCurveLut(points)
        assertEquals(256, lut.size)
        assertEquals(0, lut[0])
        assertEquals(255, lut[255])
        for (v in lut) {
            assertTrue(v in 0..255)
        }
    }

    @Test
    fun testCubicBezierLutEvaluation() {
        val bezierPoints = listOf(
            CurvePoint(0f, 0f),
            CurvePoint(0.42f, 0.08f),
            CurvePoint(0.58f, 0.92f),
            CurvePoint(1f, 1f)
        )
        val lut = ImageProcessor.buildCurveLut(bezierPoints)
        assertEquals(256, lut.size)
        assertEquals(0, lut[0])
        assertEquals(255, lut[255])
        // In an S-curve, midpoint (index 128) should be around 128
        assertTrue(lut[128] in 120..136)
        // Shadows (e.g. index 64) should be lower than linear 64
        assertTrue(lut[64] < 64)
        // Highlights (e.g. index 192) should be higher than linear 192
        assertTrue(lut[192] > 192)
    }

    @Test
    fun testPresetsRegistry() {
        val presets = PresetRegistry.presets
        assertTrue(presets.size >= 9)
        val cinematic = presets.firstOrNull { it.id == "cinematic" }
        assertNotNull(cinematic)
        val edited = cinematic!!.params(EditingParams())
        assertEquals("Cinematic", edited.appliedPresetName)
        assertTrue(edited.contrast > 0)
    }
}
