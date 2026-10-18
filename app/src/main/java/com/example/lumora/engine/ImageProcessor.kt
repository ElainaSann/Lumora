package com.example.lumora.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.lumora.data.model.CurvePoint
import com.example.lumora.data.model.EditingParams
import com.example.lumora.data.model.HslColor
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object ImageProcessor {

    /**
     * Applies the complete non-destructive editing pipeline to a source Bitmap.
     */
    fun processBitmap(source: Bitmap, params: EditingParams): Bitmap {
        // Step 1: Handle Crop, Rotation & Flips
        val transformed = applyTransform(source, params)

        // Step 2: Check if any color/light/effect adjustments are needed
        val result = transformed.copy(Bitmap.Config.ARGB_8888, true)
        if (transformed != source) {
            // If transformed is an intermediate bitmap, we can recycle it later
        }

        // Build 256-entry Look-Up Tables (LUTs) for fast channel operations
        val masterLut = buildCurveLut(params.rgbCurve)
        val redLut = buildCurveLut(params.redCurve)
        val greenLut = buildCurveLut(params.greenCurve)
        val blueLut = buildCurveLut(params.blueCurve)

        val width = result.width
        val height = result.height
        val pixels = IntArray(width * height)
        result.getPixels(pixels, 0, width, 0, 0, width, height)

        val exposureMultiplier = 2.0.pow((params.exposure / 35.0).toDouble()).toFloat()
        val contrastFactor = (1.0f + params.contrast / 100.0f).coerceAtLeast(0.1f)
        val brightnessShift = (params.brightness / 100.0f) * 128f
        val highlightsShift = params.highlights / 100.0f
        val shadowsShift = params.shadows / 100.0f
        val whitesShift = params.whites / 100.0f
        val blacksShift = params.blacks / 100.0f
        val invGamma = 1.0f / params.gamma.coerceIn(0.4f, 2.5f)

        // Temperature & Tint
        val tempR = if (params.temperature > 0) 1f + (params.temperature / 100f) * 0.28f else 1f
        val tempB = if (params.temperature < 0) 1f + (-params.temperature / 100f) * 0.32f else 1f - (params.temperature / 100f) * 0.18f
        val tintG = if (params.tint < 0) 1f + (-params.tint / 100f) * 0.22f else 1f - (params.tint / 100f) * 0.12f
        val tintM = if (params.tint > 0) 1f + (params.tint / 100f) * 0.22f else 1f

        val globalSatFactor = 1.0f + (params.saturation / 100.0f)
        val vibranceFactor = params.vibrance / 100.0f
        val globalHueShift = params.hue

        val hasColorGrading = params.shadowsSat > 0 || params.midtonesSat > 0 || params.highlightsSat > 0
        val hasHsl = params.hslAdjustments.values.any { !it.isDefault() }
        val hasFade = params.fade > 0f
        val fadeAmount = (params.fade / 100.0f) * 45f

        val hsvTemp = FloatArray(3)

        // Process all pixels
        for (i in pixels.indices) {
            val color = pixels[i]
            var r = (color shr 16) and 0xFF
            var g = (color shr 8) and 0xFF
            var b = color and 0xFF

            // 1. Exposure & Brightness
            var rf = (r * exposureMultiplier) + brightnessShift
            var gf = (g * exposureMultiplier) + brightnessShift
            var bf = (b * exposureMultiplier) + brightnessShift

            // 2. Temperature & Tint
            rf *= tempR * tintM
            gf *= tintG
            bf *= tempB

            // Clamp into 0..255 for tone evaluation
            rf = rf.coerceIn(0f, 255f)
            gf = gf.coerceIn(0f, 255f)
            bf = bf.coerceIn(0f, 255f)

            // 3. Luminance calculation
            var luma = 0.299f * rf + 0.587f * gf + 0.114f * bf
            val normLuma = (luma / 255.0f).coerceIn(0f, 1f)

            // 4. Highlights, Shadows, Whites, Blacks
            if (highlightsShift != 0f && normLuma > 0.5f) {
                val weight = (normLuma - 0.5f) * 2f
                val delta = highlightsShift * 50f * weight
                rf += delta
                gf += delta
                bf += delta
            }
            if (shadowsShift != 0f && normLuma < 0.5f) {
                val weight = (0.5f - normLuma) * 2f
                val delta = shadowsShift * 50f * weight
                rf += delta
                gf += delta
                bf += delta
            }
            if (whitesShift != 0f) {
                val weight = normLuma * normLuma
                val delta = whitesShift * 40f * weight
                rf += delta; gf += delta; bf += delta
            }
            if (blacksShift != 0f) {
                val weight = (1f - normLuma) * (1f - normLuma)
                val delta = blacksShift * 40f * weight
                rf += delta; gf += delta; bf += delta
            }

            // 5. Contrast
            rf = ((rf - 128f) * contrastFactor + 128f)
            gf = ((gf - 128f) * contrastFactor + 128f)
            bf = ((bf - 128f) * contrastFactor + 128f)

            // 6. Gamma
            if (params.gamma != 1.0f) {
                rf = (rf / 255f).coerceIn(0f, 1f).pow(invGamma) * 255f
                gf = (gf / 255f).coerceIn(0f, 1f).pow(invGamma) * 255f
                bf = (bf / 255f).coerceIn(0f, 1f).pow(invGamma) * 255f
            }

            // 7. Tone Curves (Master + Individual RGB channels)
            val clampR = rf.toInt().coerceIn(0, 255)
            val clampG = gf.toInt().coerceIn(0, 255)
            val clampB = bf.toInt().coerceIn(0, 255)

            var curR = masterLut[redLut[clampR]].toFloat()
            var curG = masterLut[greenLut[clampG]].toFloat()
            var curB = masterLut[blueLut[clampB]].toFloat()

            // 8. Fade effect (lifts black level)
            if (hasFade) {
                curR = curR * (1f - fadeAmount / 255f) + fadeAmount
                curG = curG * (1f - fadeAmount / 255f) + fadeAmount
                curB = curB * (1f - fadeAmount / 255f) + fadeAmount
            }

            // 9. Saturation & Vibrance & Global Hue
            luma = 0.299f * curR + 0.587f * curG + 0.114f * curB
            val currentSat = (max(curR, max(curG, curB)) - min(curR, min(curG, curB))) / 255f

            // Vibrance gives higher boost to less-saturated colors
            val vibMultiplier = 1f + vibranceFactor * (1f - currentSat)
            val totalSat = (globalSatFactor * vibMultiplier).coerceAtLeast(0f)

            curR = luma + (curR - luma) * totalSat
            curG = luma + (curG - luma) * totalSat
            curB = luma + (curB - luma) * totalSat

            // 10. Color Grading (Split Toning)
            if (hasColorGrading) {
                val nl = (luma / 255f).coerceIn(0f, 1f)
                val balanceOffset = params.gradingBalance / 200f
                val shadowWeight = ((0.5f + balanceOffset - nl) * 2f).coerceIn(0f, 1f)
                val highlightWeight = ((nl - (0.5f + balanceOffset)) * 2f).coerceIn(0f, 1f)
                val midtoneWeight = (1f - abs(nl - (0.5f + balanceOffset)) * 2f).coerceIn(0f, 1f)

                if (params.shadowsSat > 0 && shadowWeight > 0f) {
                    val sc = hueToRgb(params.shadowsHue, params.shadowsSat / 100f)
                    curR += (sc[0] - 128f) * (shadowWeight * 0.4f)
                    curG += (sc[1] - 128f) * (shadowWeight * 0.4f)
                    curB += (sc[2] - 128f) * (shadowWeight * 0.4f)
                }
                if (params.midtonesSat > 0 && midtoneWeight > 0f) {
                    val mc = hueToRgb(params.midtonesHue, params.midtonesSat / 100f)
                    curR += (mc[0] - 128f) * (midtoneWeight * 0.35f)
                    curG += (mc[1] - 128f) * (midtoneWeight * 0.35f)
                    curB += (mc[2] - 128f) * (midtoneWeight * 0.35f)
                }
                if (params.highlightsSat > 0 && highlightWeight > 0f) {
                    val hc = hueToRgb(params.highlightsHue, params.highlightsSat / 100f)
                    curR += (hc[0] - 128f) * (highlightWeight * 0.4f)
                    curG += (hc[1] - 128f) * (highlightWeight * 0.4f)
                    curB += (hc[2] - 128f) * (highlightWeight * 0.4f)
                }
            }

            // 11. HSL Sector Adjustment
            if (hasHsl || globalHueShift != 0f) {
                Color.RGBToHSV(
                    curR.toInt().coerceIn(0, 255),
                    curG.toInt().coerceIn(0, 255),
                    curB.toInt().coerceIn(0, 255),
                    hsvTemp
                )

                if (globalHueShift != 0f) {
                    hsvTemp[0] = (hsvTemp[0] + globalHueShift + 360f) % 360f
                }

                if (hasHsl) {
                    val h = hsvTemp[0]
                    for ((hslColor, adj) in params.hslAdjustments) {
                        if (adj.isDefault()) continue
                        val centerHue = getCenterHue(hslColor)
                        var diff = abs(h - centerHue)
                        if (diff > 180f) diff = 360f - diff

                        // Influence within 35 degrees
                        if (diff < 35f) {
                            val weight = (1f - (diff / 35f)).coerceIn(0f, 1f)
                            hsvTemp[0] = (hsvTemp[0] + (adj.hue * 0.35f * weight) + 360f) % 360f
                            hsvTemp[1] = (hsvTemp[1] * (1f + (adj.sat / 100f) * weight)).coerceIn(0f, 1f)
                            hsvTemp[2] = (hsvTemp[2] * (1f + (adj.lum / 100f) * weight)).coerceIn(0f, 1f)
                        }
                    }
                }
                val newColor = Color.HSVToColor(hsvTemp)
                curR = Color.red(newColor).toFloat()
                curG = Color.green(newColor).toFloat()
                curB = Color.blue(newColor).toFloat()
            }

            pixels[i] = (0xFF shl 24) or
                    (curR.toInt().coerceIn(0, 255) shl 16) or
                    (curG.toInt().coerceIn(0, 255) shl 8) or
                    curB.toInt().coerceIn(0, 255)
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height)

        // Step 3: Vignette effect
        if (params.vignette != 0f) {
            applyVignette(result, params.vignette)
        }

        // Step 4: Grain effect
        if (params.grain > 0f) {
            applyGrain(result, params.grain)
        }

        // Step 5: Clarity / Sharpening if needed
        if (params.sharpening > 0f || params.clarity > 0f) {
            applySharpenClarity(result, params.sharpening, params.clarity)
        }

        // Step 6: Blur effect
        if (params.blur > 0f) {
            applyBlur(result, params.blur)
        }

        // Step 7: Text Overlay
        if (params.textOverlay.isNotBlank()) {
            applyTextOverlay(result, params)
        }

        return result
    }

    private fun applyTextOverlay(bitmap: Bitmap, params: EditingParams) {
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = params.textColorHex.toInt()
            alpha = (params.textOpacity.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
            val baseScale = bitmap.width.coerceAtLeast(200) / 450f
            textSize = (params.textSize * baseScale).coerceAtLeast(16f)
            textAlign = when (params.textAlignment.uppercase()) {
                "LEFT" -> Paint.Align.LEFT
                "RIGHT" -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            if (params.textCustomFontPath != null) {
                val file = java.io.File(params.textCustomFontPath)
                if (file.exists()) {
                    try {
                        typeface = android.graphics.Typeface.createFromFile(file)
                    } catch (_: Exception) {}
                }
            }
        }
        val x = when (params.textAlignment.uppercase()) {
            "LEFT" -> bitmap.width * 0.1f
            "RIGHT" -> bitmap.width * 0.9f
            else -> bitmap.width * 0.5f
        }
        val y = bitmap.height * 0.5f
        canvas.drawText(params.textOverlay, x, y, paint)
    }

    private fun getCenterHue(color: HslColor): Float = when (color) {
        HslColor.RED -> 0f
        HslColor.ORANGE -> 30f
        HslColor.YELLOW -> 60f
        HslColor.GREEN -> 120f
        HslColor.AQUA -> 180f
        HslColor.BLUE -> 240f
        HslColor.PURPLE -> 280f
        HslColor.MAGENTA -> 320f
    }

    private fun hueToRgb(hue: Float, sat: Float): FloatArray {
        val hsv = floatArrayOf(hue % 360f, sat.coerceIn(0f, 1f), 0.5f)
        val rgbInt = Color.HSVToColor(hsv)
        return floatArrayOf(
            Color.red(rgbInt).toFloat(),
            Color.green(rgbInt).toFloat(),
            Color.blue(rgbInt).toFloat()
        )
    }

    /**
     * Builds a monotonic cubic spline 256-entry lookup table for tone curve points.
     */
    fun buildCurveLut(points: List<CurvePoint>): IntArray {
        val lut = IntArray(256)
        if (points.isEmpty()) {
            for (i in 0..255) lut[i] = i
            return lut
        }

        // Support 4-point Cubic Bezier curves (p0 = start, p1 = handle1, p2 = handle2, p3 = end)
        if (points.size == 4) {
            val p0 = points[0]
            val p1 = points[1]
            val p2 = points[2]
            val p3 = points[3]

            for (i in 0..255) {
                val targetX = i / 255.0f
                var low = 0.0f
                var high = 1.0f
                var t = targetX
                for (iter in 0 until 14) {
                    t = (low + high) * 0.5f
                    val u = 1.0f - t
                    val curX = u * u * u * p0.x + 3.0f * u * u * t * p1.x + 3.0f * u * t * t * p2.x + t * t * t * p3.x
                    if (curX < targetX) {
                        low = t
                    } else {
                        high = t
                    }
                }
                if (i == 0) {
                    lut[0] = Math.round(p0.y * 255.0f).toInt().coerceIn(0, 255)
                    continue
                }
                if (i == 255) {
                    lut[255] = Math.round(p3.y * 255.0f).toInt().coerceIn(0, 255)
                    continue
                }

                val u = 1.0f - t
                val y = u * u * u * p0.y + 3.0f * u * u * t * p1.y + 3.0f * u * t * t * p2.y + t * t * t * p3.y
                lut[i] = Math.round(y * 255.0f).toInt().coerceIn(0, 255)
            }
            return lut
        }

        val sorted = points.sortedBy { it.x }
        if (sorted.size == 1) {
            val yVal = (sorted[0].y * 255f).toInt().coerceIn(0, 255)
            for (i in 0..255) lut[i] = yVal
            return lut
        }

        for (i in 0..255) {
            val xNorm = i / 255.0f
            var segIdx = 0
            while (segIdx < sorted.size - 2 && sorted[segIdx + 1].x < xNorm) {
                segIdx++
            }
            val p0 = sorted[segIdx]
            val p1 = sorted[segIdx + 1]

            val t = if (p1.x > p0.x) ((xNorm - p0.x) / (p1.x - p0.x)).coerceIn(0f, 1f) else 0f
            val smoothT = t * t * (3f - 2f * t)
            val yVal = (p0.y + (p1.y - p0.y) * smoothT) * 255.0f
            lut[i] = yVal.toInt().coerceIn(0, 255)
        }
        return lut
    }

    /**
     * Applies crop, rotation (90, 180, 270), and horizontal/vertical flips.
     */
    private fun applyTransform(source: Bitmap, params: EditingParams): Bitmap {
        var bmp = source

        // Check if rotation or flips are required
        if (params.rotationDegrees != 0 || params.flipHorizontal || params.flipVertical) {
            val matrix = Matrix()
            if (params.rotationDegrees != 0) {
                matrix.postRotate(params.rotationDegrees.toFloat())
            }
            val scaleX = if (params.flipHorizontal) -1f else 1f
            val scaleY = if (params.flipVertical) -1f else 1f
            if (scaleX != 1f || scaleY != 1f) {
                matrix.postScale(scaleX, scaleY)
            }
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        }

        // Apply crop if bounds are customized
        if (params.cropLeft > 0f || params.cropTop > 0f || params.cropRight < 1f || params.cropBottom < 1f) {
            val startX = (params.cropLeft * bmp.width).toInt().coerceIn(0, bmp.width - 1)
            val startY = (params.cropTop * bmp.height).toInt().coerceIn(0, bmp.height - 1)
            val cropW = ((params.cropRight - params.cropLeft) * bmp.width).toInt().coerceIn(1, bmp.width - startX)
            val cropH = ((params.cropBottom - params.cropTop) * bmp.height).toInt().coerceIn(1, bmp.height - startY)

            bmp = Bitmap.createBitmap(bmp, startX, startY, cropW, cropH)
        }

        return bmp
    }

    private fun applyVignette(bitmap: Bitmap, vignetteStrength: Float) {
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = max(width, height) * 0.72f

        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val alpha = (abs(vignetteStrength) / 100f * 200).toInt().coerceIn(0, 255)
        val color = if (vignetteStrength < 0) Color.BLACK else Color.WHITE

        val edgeColor = (alpha shl 24) or (color and 0x00FFFFFF)
        val centerColor = 0x00000000

        paint.shader = RadialGradient(
            centerX, centerY, radius,
            intArrayOf(centerColor, edgeColor),
            floatArrayOf(0.45f, 1.0f),
            Shader.TileMode.CLAMP
        )

        canvas.drawRect(0f, 0f, width, height, paint)
    }

    private fun applyGrain(bitmap: Bitmap, grainStrength: Float) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val intensity = (grainStrength / 100f) * 40f
        var seed = 123456789L

        for (i in pixels.indices) {
            seed = (seed * 1103515245L + 12345L) and 0x7FFFFFFF
            val rand = ((seed % 1000) / 500f) - 1.0f // -1.0 to 1.0
            val noise = (rand * intensity).toInt()

            val c = pixels[i]
            val r = (((c shr 16) and 0xFF) + noise).coerceIn(0, 255)
            val g = (((c shr 8) and 0xFF) + noise).coerceIn(0, 255)
            val b = ((c and 0xFF) + noise).coerceIn(0, 255)

            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    private fun applySharpenClarity(bitmap: Bitmap, sharpenVal: Float, clarityVal: Float) {
        val total = (sharpenVal * 0.015f + clarityVal * 0.012f).coerceIn(0f, 1.5f)
        if (total <= 0.05f) return

        val width = bitmap.width
        val height = bitmap.height
        val src = IntArray(width * height)
        val dst = IntArray(width * height)
        bitmap.getPixels(src, 0, width, 0, 0, width, height)

        for (y in 1 until height - 1) {
            val yOffset = y * width
            for (x in 1 until width - 1) {
                val idx = yOffset + x
                val center = src[idx]
                val left = src[idx - 1]
                val right = src[idx + 1]
                val top = src[idx - width]
                val bottom = src[idx + width]

                val cr = (center shr 16) and 0xFF
                val cg = (center shr 8) and 0xFF
                val cb = center and 0xFF

                val lr = (left shr 16) and 0xFF
                val rr = (right shr 16) and 0xFF
                val tr = (top shr 16) and 0xFF
                val br = (bottom shr 16) and 0xFF

                val lg = (left shr 8) and 0xFF
                val rg = (right shr 8) and 0xFF
                val tg = (top shr 8) and 0xFF
                val bg = (bottom shr 8) and 0xFF

                val lb = left and 0xFF
                val rb = right and 0xFF
                val tb = top and 0xFF
                val bb = bottom and 0xFF

                // Laplacien high pass
                val diffR = (4 * cr - (lr + rr + tr + br))
                val diffG = (4 * cg - (lg + rg + tg + bg))
                val diffB = (4 * cb - (lb + rb + tb + bb))

                val outR = (cr + diffR * total).toInt().coerceIn(0, 255)
                val outG = (cg + diffG * total).toInt().coerceIn(0, 255)
                val outB = (cb + diffB * total).toInt().coerceIn(0, 255)

                dst[idx] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
        }
        bitmap.setPixels(dst, 0, width, 0, 0, width, height)
    }

    private fun applyBlur(bitmap: Bitmap, blurPercent: Float) {
        val radius = ((blurPercent / 100f) * 16).toInt().coerceIn(1, 25)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val temp = IntArray(width * height)

        // Horizontal pass
        for (y in 0 until height) {
            val yOffset = y * width
            for (x in 0 until width) {
                var rSum = 0; var gSum = 0; var bSum = 0; var count = 0
                for (k in -radius..radius) {
                    val nx = (x + k).coerceIn(0, width - 1)
                    val c = pixels[yOffset + nx]
                    rSum += (c shr 16) and 0xFF
                    gSum += (c shr 8) and 0xFF
                    bSum += c and 0xFF
                    count++
                }
                temp[yOffset + x] = (0xFF shl 24) or ((rSum / count) shl 16) or ((gSum / count) shl 8) or (bSum / count)
            }
        }

        // Vertical pass
        for (x in 0 until width) {
            for (y in 0 until height) {
                var rSum = 0; var gSum = 0; var bSum = 0; var count = 0
                for (k in -radius..radius) {
                    val ny = (y + k).coerceIn(0, height - 1)
                    val c = temp[ny * width + x]
                    rSum += (c shr 16) and 0xFF
                    gSum += (c shr 8) and 0xFF
                    bSum += c and 0xFF
                    count++
                }
                pixels[y * width + x] = (0xFF shl 24) or ((rSum / count) shl 16) or ((gSum / count) shl 8) or (bSum / count)
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    /**
     * Scale a bitmap to a max dimension for lightning-fast proxy editing preview.
     */
    fun createProxyBitmap(source: Bitmap, maxDim: Int = 900): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= maxDim && h <= maxDim) return source

        val scale = if (w > h) maxDim.toFloat() / w else maxDim.toFloat() / h
        val dstW = (w * scale).toInt().coerceAtLeast(1)
        val dstH = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, dstW, dstH, true)
    }
}
