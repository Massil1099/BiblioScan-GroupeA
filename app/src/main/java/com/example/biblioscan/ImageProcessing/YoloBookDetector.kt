package com.example.biblioscan.ImageProcessing


import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

class YoloBookDetector(context: Context) {

    private val interpreter: Interpreter
    private val inputSize = 640  // a cause de notre modèle .tflite
    private val NUM_DETECTIONS = 10
    private val CONFIDENCE_THRESHOLD = 0.3f
    private val IOU_THRESHOLD = 0.5f

    init {
        val modelBuffer = FileUtil.loadMappedFile(context, "yolo_books.tflite")
        interpreter = Interpreter(modelBuffer)
    }

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, false)
        val input = bitmapToFloatBuffer(resizedBitmap)

        val output = Array(1) { Array(5) { FloatArray(8400) } }

        val outputShape = interpreter.getOutputTensor(0).shape()
        Log.d("YOLO", "Output shape: ${outputShape.joinToString()}")

        interpreter.run(input, output)

        val results = mutableListOf<DetectionResult>()
        val scaleX = bitmap.width.toFloat() / inputSize
        val scaleY = bitmap.height.toFloat() / inputSize

        for (i in 0 until 8400) {
            val x = output[0][0][i]
            val y = output[0][1][i]
            val w = output[0][2][i]
            val h = output[0][3][i]
            val confidence = output[0][4][i]

            if (confidence > CONFIDENCE_THRESHOLD) {
                val left = (x - w / 2) * scaleX
                val top = (y - h / 2) * scaleY
                val right = (x + w / 2) * scaleX
                val bottom = (y + h / 2) * scaleY
                results.add(DetectionResult(RectF(left, top, right, bottom), confidence))
            }
        }

        return applyNMS(results)
    }



    private fun bitmapToFloatBuffer(bitmap: Bitmap): ByteBuffer {
        val inputChannels = 3
        val byteBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * inputChannels * 4)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixelValue in intValues) {
            val r = (pixelValue shr 16 and 0xFF) / 255f
            val g = (pixelValue shr 8 and 0xFF) / 255f
            val b = (pixelValue and 0xFF) / 255f
            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }

        return byteBuffer
    }










    // NMS
    private fun applyNMS(
        detections: List<DetectionResult>,
        iouThreshold: Float = IOU_THRESHOLD
    ): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()
        val sortedDetections = detections.sortedByDescending { it.confidence }.toMutableList()

        while (sortedDetections.isNotEmpty()) {
            val best = sortedDetections.removeAt(0)
            results.add(best)

            val iterator = sortedDetections.iterator()
            while (iterator.hasNext()) {
                val other = iterator.next()
                val iou = computeIoU(best.boundingBox, other.boundingBox)
                if (iou > iouThreshold) {
                    iterator.remove()
                }
            }
        }

        return results
    }

    private fun computeIoU(box1: RectF, box2: RectF): Float {
        val intersectionLeft = maxOf(box1.left, box2.left)
        val intersectionTop = maxOf(box1.top, box2.top)
        val intersectionRight = minOf(box1.right, box2.right)
        val intersectionBottom = minOf(box1.bottom, box2.bottom)

        val intersectionArea = maxOf(0f, intersectionRight - intersectionLeft) *
                maxOf(0f, intersectionBottom - intersectionTop)

        val box1Area = (box1.right - box1.left) * (box1.bottom - box1.top)
        val box2Area = (box2.right - box2.left) * (box2.bottom - box2.top)

        val unionArea = box1Area + box2Area - intersectionArea

        return if (unionArea == 0f) 0f else intersectionArea / unionArea
    }

}
