package com.example.biblioscan.fragments

import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
            imagePath = it.getString("capturedImagePath")
            detectionResults = it.getParcelableArrayList("detectionResults") ?: arrayListOf()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
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

                binding.detectionInfo.text = "📚 ${detectionResults.size} livre(s) détecté(s)"
                detectionResults.forEachIndexed { index, result ->
                    val labelView = TextView(requireContext()).apply {
                        text = "Livre ${index + 1} : ${result.label}"
                        textSize = 14f
                        setTextColor(Color.WHITE)
                        setPadding(0, 8, 0, 8)
                    }
                    binding.labelsContainer.addView(labelView)
                }

            } else {
                binding.imageView.setImageResource(android.R.color.darker_gray)
                binding.detectionInfo.text = "Erreur de chargement de l’image."
            }
        } else {
            binding.imageView.setImageResource(android.R.color.darker_gray)
            binding.detectionInfo.text = "Aucune image fournie."
        }

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun drawBoundingBoxes(bitmap: Bitmap, results: List<DetectionResult>): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        val textPaint = Paint().apply {
            color = Color.YELLOW
            textSize = 36f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }

        for ((index, result) in results.withIndex()) {
            canvas.drawRect(result.boundingBox, paint)
            val label = "Livre ${index + 1}"
            val textX = result.boundingBox.left
            val textY = (result.boundingBox.top - 10).coerceAtLeast(40f)
            canvas.drawText(label, textX, textY, textPaint)
        }

        return mutableBitmap
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
