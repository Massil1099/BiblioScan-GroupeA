package com.example.biblioscan.ImageProcessing

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import androidx.core.graphics.createBitmap

object ImagePreprocessor {

    // Convertit l’image en niveaux de gris
    fun toGrayscale(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val grayBitmap = createBitmap(width, height)

        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val color = pixels[i]
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            val gray = (0.3 * r + 0.59 * g + 0.11 * b).toInt()
            pixels[i] = Color.rgb(gray, gray, gray)
        }

        grayBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return grayBitmap
    }

    // Binarisation adaptative simple avec moyenne globale
    fun binarizeBitmap(src: Bitmap): Bitmap {
        val gray = toGrayscale(src)
        val width = gray.width
        val height = gray.height
        val pixels = IntArray(width * height)
        gray.getPixels(pixels, 0, width, 0, 0, width, height)

        // Calcule de la moyenne globale de luminosité
        val avgGray = pixels.map { Color.red(it) }.average()

        for (i in pixels.indices) {
            val grayValue = Color.red(pixels[i])
            pixels[i] = if (grayValue < avgGray) Color.BLACK else Color.WHITE
        }

        val binarized = createBitmap(width, height)
        binarized.setPixels(pixels, 0, width, 0, 0, width, height)
        return binarized
    }

    // Rotation simple
    fun rotateBitmap(bitmap: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(angle) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }


}
