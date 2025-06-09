package com.example.biblioscan.ImageProcessing

import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

data class DetectionResult(
    val boundingBox: RectF,
    val confidence: Float,
    var label: String = "",
    var status: String = "ok"
) : Parcelable {
    constructor(parcel: Parcel) : this(
        RectF(
            parcel.readFloat(),
            parcel.readFloat(),
            parcel.readFloat(),
            parcel.readFloat()
        ),
        parcel.readFloat(),
        parcel.readString() ?: "",
        parcel.readString() ?: "ok"
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeFloat(boundingBox.left)
        parcel.writeFloat(boundingBox.top)
        parcel.writeFloat(boundingBox.right)
        parcel.writeFloat(boundingBox.bottom)
        parcel.writeFloat(confidence)
        parcel.writeString(label)
        parcel.writeString(status)
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
///**
// * Tente de classifier une ligne de texte extraite de la tranche d’un livre
// * en deux parties : titre et auteur, à l’aide de règles heuristiques simples.
// *
// * @param line Ligne de texte brute extraite de l'image.
// * @return Une paire (titre, auteur), où l’un des deux peut être null si incertain.
// */
//fun classifyBookText(line: String): Pair<String?, String?> {
//    if (line.isBlank()) return null to null // Cas vide
//
//    // Étape 1 : normalisation des séparateurs typiques
//    val normalizedLine = line
//        .replace(Regex("""\s*[-–•/|]\s*"""), " | ") // Unifie les séparateurs typographiques
//        .replace(Regex("""\s+"""), " ")             // Réduit les espaces multiples
//        .trim()
//
//    // Découpe en segments sur le séparateur standardisé "|"
//    val parts = normalizedLine.split("|").map { it.trim() }
//
//    // Cas classique avec au moins deux segments détectés
//    if (parts.size >= 2) {
//        val (first, second) = parts.take(2)
//
//        // Heuristiques basées sur la casse et la taille du texte
//        return when {
//            isProbablyTitle(first) && isProbablyAuthor(second) -> first to second
//            isProbablyAuthor(first) && isProbablyTitle(second) -> second to first
//            else -> first to second // Si doute, on garde la séparation de base
//        }
//    }
//
//    // Cas alternatif : séparé par une virgule (ex: "Auteur, Titre")
//    if (parts.size == 1 && line.contains(",")) {
//        val splitByComma = parts[0].split(",").map { it.trim() }
//        if (splitByComma.size >= 2) {
//            val (first, second) = splitByComma.take(2)
//            return if (isProbablyAuthor(first)) second to first else first to second
//        }
//    }
//
//    // Fallback : si on a qu’un seul segment, on tente de deviner le rôle
//    val only = parts[0]
//    return if (isProbablyTitle(only)) only to null else null to only
//}


//
///**
// * Estime si une chaîne ressemble à un titre de livre
// * Basé sur l’usage des majuscules ou du nombre de mots en capitales
// */
//fun isProbablyTitle(text: String): Boolean {
//    return text == text.uppercase() || // Tout en majuscules
//            text.split(" ")
//                .count { it.firstOrNull()?.isUpperCase() == true } > 1 // Plusieurs mots capitalisés
//}
//
///**
// * Estime si une chaîne ressemble à un nom d’auteur
// * Basé sur la brièveté et la présence de lettres (et non de chiffres ou ponctuation)
// */
//fun isProbablyAuthor(text: String): Boolean {
//    return text.split(" ").size <= 3 && // Noms d’auteurs courts
//            text.any { it.isLetter() }   // Contient des lettres (évite les ISBN ou dates)
//}
//
///******************************************************************************************************************************************/
//// Fonctions utilitaires pour le deskewing (redressement automatique de texte)
///******************************************************************************************************************************************/
//
//fun deskew(bitmap: Bitmap): Bitmap {
//    val angles = (-10..10 step 1).map { it.toFloat() }
//    val binarized = ImagePreprocessor.binarizeBitmap(bitmap)
//
//    val bestAngle = angles.maxByOrNull { angle ->
//        val rotated = ImagePreprocessor.rotateBitmap(binarized, angle)
//        computeHorizontalProjectionVariance(rotated)
//    } ?: 0f
//
//    return if (bestAngle != 0f) ImagePreprocessor.rotateBitmap(bitmap, bestAngle) else bitmap
//}
//
//// Calcule la variance de la projection horizontale pour estimer l'alignement du texte
//private fun computeHorizontalProjectionVariance(bitmap: Bitmap): Double {
//    val width = bitmap.width
//    val height = bitmap.height
//    val projection = IntArray(height)
//
//    val pixels = IntArray(width * height)
//    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
//
//    for (y in 0 until height) {
//        var blackCount = 0
//        for (x in 0 until width) {
//            val color = pixels[y * width + x]
//            if (Color.red(color) < 128) blackCount++
//        }
//        projection[y] = blackCount
//    }
//
//    val mean = projection.average()
//    return projection.map { (it - mean).let { d -> d * d } }.average()
//}

/****************************************************************
// Fonction suspendue avec coroutine pour le traitement OCR
 **************************************************************/

suspend fun extractTextFromBoundingBoxes(
    bitmap: Bitmap,
    results: List<DetectionResult>
): List<DetectionResult> = withContext(Dispatchers.IO) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    results.map { detection ->
        val left = detection.boundingBox.left.toInt().coerceAtLeast(0)
        val top = detection.boundingBox.top.toInt().coerceAtLeast(0)
        val right = detection.boundingBox.right.toInt().coerceAtMost(bitmap.width)
        val bottom = detection.boundingBox.bottom.toInt().coerceAtMost(bitmap.height)

        val cropped = Bitmap.createBitmap(
            bitmap,
            left,
            top,
            (right - left).coerceAtLeast(1),
            (bottom - top).coerceAtLeast(1)
        )
        // Activation deu prétraitement de l'image

        /***********************************************
        // Étape 1 : Binarisation pour renforcer le contraste du texte
        cropped = ImagePreprocessor.binarizeBitmap(cropped)

        // Étape 2 : Deskew (correction automatique de l'inclinaison du texte)
        cropped = deskew(cropped)

        // Étape 3 : Rotation manuelle si l’image est très verticale (lecture latérale)
        if (cropped.height > cropped.width * 1.2) {
            cropped = ImagePreprocessor.rotateBitmap(cropped, 90f)
        }

         ********************************************************/

        val image = InputImage.fromBitmap(cropped, 0)
        try {
            val result = recognizer.process(image).await()

            detection.label = result.text
            detection.status = if (result.text.trim().length < 5) "no_text" else "ok"
        } catch (e: Exception) {
            Log.e("OCR", "Erreur OCR", e)
            detection.label = "Erreur OCR"
            detection.status = "ignored"
        }
        // Pour utiliser le post traitement de l'ocr
        /*******
        try {
            val result = recognizer.process(image).await()
            val lines = result.text.lines().filter { it.isNotBlank() }

            val (title, author) = classifyBookText(lines.joinToString(" ") { it.trim() })
            detection.title = title ?: "Titre inconnu"
            detection.author = author ?: "Auteur inconnu"
            detection.label = "Titre : ${detection.title}\nAuteur(s) : ${detection.author}"

        } catch (e: Exception) {
            Log.e("OCR", "Erreur OCR", e)
            detection.label = "Erreur OCR"
        }
        ***********/

        detection
    }
}
