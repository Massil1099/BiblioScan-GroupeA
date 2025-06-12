package com.example.biblioscan.imageProcessing

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
    var status: String = "ok",
    var frameIndex: Int? = null // Rendons-le nullable pour les photos
) : Parcelable {
    constructor(parcel: Parcel) : this(
        RectF(parcel.readFloat(), parcel.readFloat(), parcel.readFloat(), parcel.readFloat()),
        parcel.readFloat(),
        parcel.readString() ?: "",
        parcel.readString() ?: "ok",
        parcel.readInt().takeIf { it != -1 } // Gestion nullable

    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeFloat(boundingBox.left)
        parcel.writeFloat(boundingBox.top)
        parcel.writeFloat(boundingBox.right)
        parcel.writeFloat(boundingBox.bottom)
        parcel.writeFloat(confidence)
        parcel.writeString(label)
        parcel.writeString(status)
        parcel.writeInt(frameIndex ?: -1) // -1 pour null
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<DetectionResult> {
        override fun createFromParcel(parcel: Parcel): DetectionResult = DetectionResult(parcel)
        override fun newArray(size: Int): Array<DetectionResult?> = arrayOfNulls(size)
    }
}

suspend fun extractTextFromBoundingBoxes(
    bitmap: Bitmap,
    results: List<DetectionResult>
): List<DetectionResult> = withContext(Dispatchers.IO) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    results.map { detection ->
        val box = detection.boundingBox
        val left = box.left.toInt().coerceIn(0, bitmap.width - 1)
        val top = box.top.toInt().coerceIn(0, bitmap.height - 1)
        val right = box.right.toInt().coerceIn(left + 1, bitmap.width)
        val bottom = box.bottom.toInt().coerceIn(top + 1, bitmap.height)

        val cropped = try {
            Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
        } catch (e: Exception) {
            detection.status = "error_crop"
            null
        }

        if (cropped != null) {
            val preprocessed = ImagePreprocessor.preprocess(cropped)

            val image = InputImage.fromBitmap(preprocessed, 0)
            try {
                val result = recognizer.process(image).await()
                val rawText = result.text.trim()
                val cleaned = cleanText(rawText)
                detection.label = cleaned
                detection.status = if (cleaned.length < 5) "no_text" else "ok"
            } catch (e: Exception) {
                detection.label = "Erreur OCR"
                detection.status = "ocr_failed"
            }
        }

        detection
    }
}

// Nettoyage + suppression de caractères inutiles
private fun cleanText(text: String): String {
    return text
        .replace("\n", " ")
        .replace(Regex("\\s+"), " ")
        .replace(Regex("[^\\p{L}\\p{N} .,'’:\\-]"), "")
        .trim()
}
