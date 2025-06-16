package com.example.biblioscan.imageProcessing

import android.graphics.*
import androidx.core.graphics.createBitmap
import kotlin.math.*
import androidx.core.graphics.get
import androidx.core.graphics.set

object ImagePreprocessor {

    /**
     * Effectue un prétraitement complet de l'image :
     * - conversion en niveaux de gris
     * - amélioration du contraste
     * - accentuation de la netteté
     */
    fun preprocess(original: Bitmap): Bitmap {
        val gray = toGrayscale(original)
        val contrast = enhanceContrast(gray)
        return sharpenBitmap(contrast)
    }

    /**
     * Convertit une image couleur en niveaux de gris en supprimant la saturation.
     */
    fun toGrayscale(bitmap: Bitmap): Bitmap {
        val grayscale = createBitmap(bitmap.width, bitmap.height)
        val canvas = Canvas(grayscale)
        val paint = Paint()

        // Réduction de la saturation à 0 pour obtenir du niveau de gris
        val colorMatrix = ColorMatrix().apply {
            setSaturation(0f)
        }

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return grayscale
    }

    /**
     * Améliore le contraste et ajuste la luminosité.
     * Plus le contraste est haut, plus les zones sombres et claires sont accentuées.
     */
    fun enhanceContrast(bitmap: Bitmap): Bitmap {
        val contrast = 1.8f    // Facteur de contraste (1.0 = inchangé)
        val brightness = -20f  // Décalage de luminosité (valeurs négatives assombrissent)

        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }

        val result = createBitmap(bitmap.width, bitmap.height)
        val canvas = Canvas(result)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }

    /**
     * Applique un filtre de netteté (sharpen) via un noyau de convolution.
     * Renforce les contours pour rendre le texte ou les détails plus lisibles.
     */
    fun sharpenBitmap(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height

        val result = createBitmap(width, height, src.config ?: Bitmap.Config.ARGB_8888)

        // Noyau de netteté classique (filtre de convolution)
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

                // Application du noyau 3x3
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixel = src[x + kx, y + ky]
                        val factor = kernel[ky + 1][kx + 1]
                        r += ((pixel shr 16) and 0xFF) * factor
                        g += ((pixel shr 8) and 0xFF) * factor
                        b += (pixel and 0xFF) * factor
                    }
                }

                // Clamping entre 0 et 255 pour éviter les débordements
                r = r.coerceIn(0, 255)
                g = g.coerceIn(0, 255)
                b = b.coerceIn(0, 255)

                result[x, y] = Color.rgb(r, g, b)
            }
        }

        return result
    }

    /**
     * (Optionnel) Corrige l'orientation si nécessaire à partir des EXIF ou de la logique de rotation.
     * À intégrer si tu charges des images depuis l'appareil photo, en fonction de leur EXIF.
     */
    // fun correctOrientationIfNeeded(bitmap: Bitmap): Bitmap {
    //     // Exemple à ajouter si tu traites des images tournées
    //     return bitmap
    // }
}
