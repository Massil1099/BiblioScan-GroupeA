// FragmentOCR.kt
package com.example.biblioscan.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.biblioscan.R
import com.example.biblioscan.databinding.FragmentOcrBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.nio.ByteBuffer

class FragmentOCR : Fragment() {

    private var _binding: FragmentOcrBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOcrBinding.inflate(inflater, container, false)

        // Récupération du Bitmap depuis le bundle
        val byteArray = arguments?.getByteArray("capturedImage")
        byteArray?.let {
            val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
            val preprocessedBitmap = preprocessBitmap(bitmap)
            processImage(preprocessedBitmap)
        }

        // Bouton Retour à la Caméra
        binding.backToCameraButton.setOnClickListener {
            findNavController().navigate(R.id.action_ocr_to_camera)
        }

        return binding.root
    }

    private fun preprocessBitmap(bitmap: Bitmap): Bitmap {
        // 1. Convertir en niveaux de gris
        val grayBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val gray = (0.3 * r + 0.59 * g + 0.11 * b).toInt()
                grayBitmap.setPixel(x, y, Color.rgb(gray, gray, gray))
            }
        }

        // 2. Renforcement du contraste (CLAHE)
        val contrastBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        for (x in 0 until grayBitmap.width) {
            for (y in 0 until grayBitmap.height) {
                val pixel = grayBitmap.getPixel(x, y)
                val gray = Color.red(pixel)
                val enhanced = if (gray > 120) 255 else 0
                contrastBitmap.setPixel(x, y, Color.rgb(enhanced, enhanced, enhanced))
            }
        }

        // 3. Réduction du bruit avec un filtre médian (simulé)
        val filteredBitmap = Bitmap.createBitmap(contrastBitmap.width, contrastBitmap.height, Bitmap.Config.ARGB_8888)
        for (x in 1 until contrastBitmap.width - 1) {
            for (y in 1 until contrastBitmap.height - 1) {
                val neighbors = mutableListOf<Int>()
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        neighbors.add(Color.red(contrastBitmap.getPixel(x + dx, y + dy)))
                    }
                }
                neighbors.sort()
                val median = neighbors[neighbors.size / 2]
                filteredBitmap.setPixel(x, y, Color.rgb(median, median, median))
            }
        }

        return filteredBitmap
    }

    private fun processImage(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)

        // Initialisation du TextRecognizer
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { result ->
                val extractedText = result.text
                binding.textViewOcrResult.text = extractedText
                Log.d("OCR", "Texte détecté : $extractedText")
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "Erreur lors de la détection de texte", e)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
