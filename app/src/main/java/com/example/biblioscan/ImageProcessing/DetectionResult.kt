package com.example.biblioscan.ImageProcessing

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.example.biblioscan.ImageProcessing.ImagePreprocessor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/******************************************************************************************************************************************/
// Classe pour représenter un objet détecté avec une boîte englobante
/******************************************************************************************************************************************/

data class DetectionResult(
    val boundingBox: RectF,
    val confidence: Float,
    var label: String = "", // texte brut OCR reconnu
    var title: String? = null,
    var author: String? = null,
    var status : String = "ok"
) : Parcelable {
    constructor(parcel: Parcel) : this(
        RectF(
            parcel.readFloat(),
            parcel.readFloat(),
            parcel.readFloat(),
            parcel.readFloat()
        ),
        parcel.readFloat(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeFloat(boundingBox.left)
        parcel.writeFloat(boundingBox.top)
        parcel.writeFloat(boundingBox.right)
        parcel.writeFloat(boundingBox.bottom)
        parcel.writeFloat(confidence)
        parcel.writeString(label)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<DetectionResult> {
        override fun createFromParcel(parcel: Parcel): DetectionResult {
            return DetectionResult(parcel)
        }

        override fun newArray(size: Int): Array<DetectionResult?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * Tente de classifier une ligne de texte extraite de la tranche d’un livre
 * en deux parties : titre et auteur, à l’aide de règles heuristiques simples.
 *
 * @param line Ligne de texte brute extraite de l'image.
 * @return Une paire (titre, auteur), où l’un des deux peut être null si incertain.
 */
fun classifyBookText(line: String): Pair<String?, String?> {
    if (line.isBlank()) return null to null

    // Étape 1 : normalisation avancée
    val normalizedLine = line
        .replace(Regex("""\s*[-–•/|:,]\s*"""), " | ") // Unifie plus de séparateurs
        .replace(Regex("""\s*&\s*"""), " et ")        // Normalise "&" en "et"
        .replace(Regex("""\s+"""), " ")
        .trim()

    // Découpe en segments
    val parts = normalizedLine.split("|").map { it.trim() }

    // Cas avec séparateur explicite
    if (parts.size >= 2) {
        val (first, second) = parts.take(2)

        return when {
            isProbablyTitle(first) && isProbablyAuthor(second) -> first to second
            isProbablyAuthor(first) && isProbablyTitle(second) -> second to first
            first.length > second.length -> first to second // Heuristique de longueur
            else -> second to first
        }
    }

    // Cas avec virgule (Auteur, Titre)
    if (line.contains(",")) {
        val splitByComma = line.split(",").map { it.trim() }
        if (splitByComma.size >= 2) {
            val (first, second) = splitByComma.take(2)
            return if (isProbablyAuthor(first)) second to first else first to second
        }
    }


    // Fallback pour un seul segment
    val singleSegment = parts[0] // On utilise parts[0] au lieu de only
    return when {
        isProbablyTitle(singleSegment) -> singleSegment to null
        isProbablyAuthor(singleSegment) -> null to singleSegment
        singleSegment.length > 30 -> singleSegment to null // Long texte probablement un titre
        else -> null to singleSegment
    }
}

fun isProbablyTitle(text: String): Boolean {
    if (text.isEmpty()) return false

    // Vérifie les motifs typiques des titres
    return when {
        text == text.uppercase() -> true
        text.split(" ").count { it.firstOrNull()?.isUpperCase() == true } > text.split(" ").size / 2 -> true
        text.split(" ").size > 3 -> true // Plus de 3 mots → probablement un titre
        text.any { it.isLowerCase() } && text.split(" ").any { it.all { c -> c.isUpperCase() } } -> true
        else -> false
    }
}

fun isProbablyAuthor(text: String): Boolean {
    if (text.isEmpty()) return false

    // Vérifie les motifs typiques des noms d'auteurs
    return when {
        text.contains(".") -> true // Initiales comme J.K. Rowling
        text.split(" ").all { it.firstOrNull()?.isUpperCase() == true } -> true // Prénom + Nom
        text.split(" ").size == 2 && text.split(" ").all { it.length > 2 } -> true // Deux mots longs
        text.split(", ").size == 2 -> true // Format Nom, Prénom
        else -> false
    }
}

/******************************************************************************************************************************************/
// Fonctions utilitaires pour le deskewing (redressement automatique de texte)
/******************************************************************************************************************************************/

fun deskew(bitmap: Bitmap): Bitmap {
    val angles = (-10..10 step 1).map { it.toFloat() }
    val binarized = ImagePreprocessor.binarizeBitmap(bitmap)

    val bestAngle = angles.maxByOrNull { angle ->
        val rotated = ImagePreprocessor.rotateBitmap(binarized, angle)
        computeHorizontalProjectionVariance(rotated)
    } ?: 0f

    return if (bestAngle != 0f) ImagePreprocessor.rotateBitmap(bitmap, bestAngle) else bitmap
}

// Calcule la variance de la projection horizontale pour estimer l'alignement du texte
private fun computeHorizontalProjectionVariance(bitmap: Bitmap): Double {
    val width = bitmap.width
    val height = bitmap.height
    val projection = IntArray(height)

    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    for (y in 0 until height) {
        var blackCount = 0
        for (x in 0 until width) {
            val color = pixels[y * width + x]
            if (Color.red(color) < 128) blackCount++
        }
        projection[y] = blackCount
    }

    val mean = projection.average()
    return projection.map { (it - mean).let { d -> d * d } }.average()
}

/******************************************************************************************************************************************/
// Fonction suspendue qui applique l'OCR sur chaque détection avec prétraitement
/******************************************************************************************************************************************/
suspend fun extractTextFromBoundingBoxes(
    bitmap: Bitmap,
    results: List<DetectionResult>,
): List<DetectionResult> = withContext(Dispatchers.IO) {

    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    results.map { detection ->
        val left = detection.boundingBox.left.toInt().coerceAtLeast(0)
        val top = detection.boundingBox.top.toInt().coerceAtLeast(0)
        val right = detection.boundingBox.right.toInt().coerceAtMost(bitmap.width)
        val bottom = detection.boundingBox.bottom.toInt().coerceAtMost(bitmap.height)

        var cropped: Bitmap? = null
        var binarized: Bitmap? = null
        var deskewed: Bitmap? = null
        var rotated: Bitmap? = null

        try {
            // Étape 0 : Extraction du sous-bitmap
            cropped = Bitmap.createBitmap(
                bitmap,
                left,
                top,
                (right - left).coerceAtLeast(1),
                (bottom - top).coerceAtLeast(1)
            )

            // Étape 1 : Binarisation
            binarized = ImagePreprocessor.binarizeBitmap(cropped)
            cropped.recycle()
            cropped = null

            // Étape 2 : Deskew
            deskewed = deskew(binarized)
            binarized.recycle()
            binarized = null

            // Étape 3 : Rotation si besoin
            var finalBitmap = deskewed
            if (deskewed.height > deskewed.width * 1.2) {
                rotated = ImagePreprocessor.rotateBitmap(deskewed, 90f)
                deskewed.recycle()
                deskewed = null
                finalBitmap = rotated
            }

            // Étape 4 : OCR
            val image = InputImage.fromBitmap(finalBitmap, 0)
            val result = recognizer.process(image).await()
            val lines = result.text.lines().filter { it.isNotBlank() }

            val (title, author) = classifyBookText(lines.joinToString(" ") { it.trim() })
            detection.title = title ?: "Titre inconnu"
            detection.author = author ?: "Auteur inconnu"
            detection.label = "Titre : ${detection.title}\nAuteur(s) : ${detection.author}"

            finalBitmap.recycle()

        } catch (e: Exception) {
            Log.e("OCR", "Erreur OCR", e)
            detection.label = "Erreur OCR"

            // Recyclage en cas d'erreur
            cropped?.recycle()
            binarized?.recycle()
            deskewed?.recycle()
            rotated?.recycle()
        }

        detection
    }
}

