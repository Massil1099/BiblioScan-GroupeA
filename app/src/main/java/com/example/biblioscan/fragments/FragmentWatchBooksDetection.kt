package com.example.biblioscan.fragments

import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.biblioscan.ImageProcessing.DetectionResult
import com.example.biblioscan.databinding.FragmentWatchBooksDetectionBinding

class FragmentWatchBooksDetection : Fragment() {

    private var _binding: FragmentWatchBooksDetectionBinding? = null
    private val binding get() = _binding!!

    private var imagePath: String? = null
    private var detectionResults: ArrayList<DetectionResult> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imagePath = it.getString("imagePath")
            detectionResults = it.getParcelableArrayList("detectionResults") ?: arrayListOf()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWatchBooksDetectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!imagePath.isNullOrEmpty()) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap != null) {
                val bitmapWithBoxes = drawBoundingBoxes(bitmap, detectionResults)
                binding.imageView.setImageBitmap(bitmapWithBoxes)
            } else {
                // Image non chargée : afficher un placeholder
                binding.imageView.setImageResource(android.R.color.darker_gray)
                // Tu peux aussi afficher un message d’erreur ici si tu veux
            }
        } else {
            // Pas de chemin image : afficher un placeholder
            binding.imageView.setImageResource(android.R.color.darker_gray)
        }

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun drawBoundingBoxes(
        bitmap: Bitmap,
        results: List<DetectionResult>
    ): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        val textPaint = Paint().apply {
            color = Color.RED
            textSize = 40f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        for (result in results) {
            canvas.drawRect(result.boundingBox, paint)
            // Calculer la position du texte avec marge pour ne pas sortir de l'image
            val textX = result.boundingBox.left.toFloat()
            val textY = (result.boundingBox.top - 10).coerceAtLeast(40f).toFloat() // 40 pour ne pas être hors écran
            canvas.drawText(
                "${(result.confidence * 100).toInt()}%",
                textX,
                textY,
                textPaint
            )
        }
        return mutableBitmap
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
