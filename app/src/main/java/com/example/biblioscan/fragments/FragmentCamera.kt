package com.example.biblioscan.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.biblioscan.R
import com.example.biblioscan.databinding.FragmentCameraBinding
import com.example.biblioscan.imageProcessing.DetectionResult
import com.example.biblioscan.imageProcessing.YoloBookDetector
import com.example.biblioscan.imageProcessing.extractTextFromBoundingBoxes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FragmentCamera : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else Log.e("CameraXApp", "Permission caméra refusée.")
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        cameraExecutor = Executors.newSingleThreadExecutor()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkCameraPermission()
        binding.captureButton.setOnClickListener { takePhoto() }
        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_camera_to_accueil)
        }
    }

    private fun checkCameraPermission() {
        val hasPermission = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) startCamera()
        else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA,
                    preview, imageCapture
                )
            } catch (e: Exception) {
                Log.e("CameraXApp", "Erreur lors de l'initialisation de la caméra", e)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageDir = File(requireContext().filesDir, "images").apply { mkdirs() }
        val photoFile = File(imageDir, "original_$timeStamp.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                    lifecycleScope.launch {
                        val bitmap = withContext(Dispatchers.IO) {
                            BitmapFactory.decodeFile(photoFile.absolutePath)
                        }
                        // demarrer compteur pour calculer le temps d'un traitement
                        val startTime = System.currentTimeMillis()
                        val yoloStart = System.currentTimeMillis()

                        val detector = YoloBookDetector(requireContext())
                        val detections = detector.detect(bitmap)
                        val yoloEnd = System.currentTimeMillis()
                        val yoloDuration = yoloEnd - yoloStart

                        if (detections.isEmpty()) {
                            Log.d("CameraXApp", "Aucun livre détecté")
                            return@launch
                        }

                        val ocrStart = System.currentTimeMillis()
                        val detectionResults = extractTextFromBoundingBoxes(bitmap, detections)
                        val ocrEnd = System.currentTimeMillis()
                        val ocrDuration = ocrEnd - ocrStart
                        val annotated = drawBoundingBoxes(bitmap, detectionResults)

                        val processedFile = File(imageDir, "processed_$timeStamp.jpg")
                        withContext(Dispatchers.IO) {
                            FileOutputStream(processedFile).use { out ->
                                annotated.compress(Bitmap.CompressFormat.JPEG, 100, out)
                            }
                        }

                        val bundle = Bundle().apply {
                            putString("capturedImagePath", processedFile.absolutePath)
                            putParcelableArrayList("detectionResults", ArrayList(detectionResults))
                            putStringArrayList(
                                "detectedTexts",
                                ArrayList(detectionResults.map { it.label })

                            )
                            putLong("processingStartTime", startTime)
                            putLong("yoloDuration", yoloDuration)
                            putLong("ocrDuration", ocrDuration)

                        }

                        findNavController().navigate(R.id.action_camera_to_liste, bundle)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraXApp", "Erreur capture : ${exception.message}", exception)
                }
            }
        )
    }

    private fun drawBoundingBoxes(bitmap: Bitmap, results: List<DetectionResult>): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val boxPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        val textPaint = Paint().apply {
            color = Color.RED
            textSize = 36f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }

        for (result in results) {
            canvas.drawRect(result.boundingBox, boxPaint)
            canvas.drawText(result.label.take(20), result.boundingBox.left, result.boundingBox.top - 8, textPaint)
        }

        return mutableBitmap
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        cameraExecutor.shutdown()
    }
}
