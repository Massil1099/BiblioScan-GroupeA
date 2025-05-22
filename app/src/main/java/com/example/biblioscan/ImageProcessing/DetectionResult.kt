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
    var label: String = ""
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

// Fonction suspendue avec coroutine pour le traitement OCR
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

        val image = InputImage.fromBitmap(cropped, 0)
        try {
            val result = recognizer.process(image).await() // Utilisation de extension `await()`
            detection.label = result.text
        } catch (e: Exception) {
            Log.e("OCR", "Erreur OCR", e)
            detection.label = "Erreur OCR"
        }

        detection
    }
}
