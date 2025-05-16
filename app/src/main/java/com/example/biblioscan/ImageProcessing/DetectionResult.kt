package com.example.biblioscan.ImageProcessing

import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

// Classe contenant les résultats de détection, sérialisable entre fragments
data class DetectionResult(
    val boundingBox: RectF,
    val confidence: Float
) : Parcelable {
    constructor(parcel: Parcel) : this(
        RectF(
            parcel.readFloat(),
            parcel.readFloat(),
            parcel.readFloat(),
            parcel.readFloat()
        ),
        parcel.readFloat()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeFloat(boundingBox.left)
        parcel.writeFloat(boundingBox.top)
        parcel.writeFloat(boundingBox.right)
        parcel.writeFloat(boundingBox.bottom)
        parcel.writeFloat(confidence)
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

// Fonction pour extraire le texte des zones détectées
fun extractTextFromBoundingBoxes(
    bitmap: Bitmap,
    results: List<DetectionResult>,
    onResult: (List<String>) -> Unit
) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val texts = mutableListOf<String>()

    for (detection in results) {
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
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                texts.add(visionText.text)
                if (texts.size == results.size) {
                    onResult(texts)
                }
            }
            .addOnFailureListener {
                texts.add("")
                if (texts.size == results.size) {
                    onResult(texts)
                }
            }
    }
}
