package com.example.biblioscan.imageProcessing

import android.graphics.*
import androidx.core.graphics.createBitmap
import kotlin.math.*
object ImagePreprocessor {

    fun preprocess(original: Bitmap): Bitmap {
        val gray = toGrayscale(original)
        val contrast = enhanceContrast(gray)
        return sharpenBitmap(contrast)
    }

    fun toGrayscale(bitmap: Bitmap): Bitmap {
        val grayscale = createBitmap(bitmap.width, bitmap.height)
        val canvas = Canvas(grayscale)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        val filter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = filter
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return grayscale
    }

    fun enhanceContrast(bitmap: Bitmap): Bitmap {
        val contrast = 1.5f
        val brightness = -30f
        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(cm)
        val result = createBitmap(bitmap.width, bitmap.height)
        val canvas = Canvas(result)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    fun sharpenBitmap(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val result = Bitmap.createBitmap(width, height, src.config ?: Bitmap.Config.ARGB_8888)

        val kernel = arrayOf(
            intArrayOf(0, -1, 0),
            intArrayOf(-1, 5, -1),
            intArrayOf(0, -1, 0)
        )

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var r = 0
                var g = 0
                var b = 0

                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixel = src.getPixel(x + kx, y + ky)
                        val factor = kernel[ky + 1][kx + 1]
                        r += ((pixel shr 16) and 0xFF) * factor
                        g += ((pixel shr 8) and 0xFF) * factor
                        b += (pixel and 0xFF) * factor
                    }
                }

                // Clamping des valeurs entre 0 et 255
                r = r.coerceIn(0, 255)
                g = g.coerceIn(0, 255)
                b = b.coerceIn(0, 255)

                result.setPixel(x, y, (0xFF shl 24) or (r shl 16) or (g shl 8) or b)
            }
        }

        return result
    }
}

