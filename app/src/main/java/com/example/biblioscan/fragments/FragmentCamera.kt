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
import com.example.biblioscan.ImageProcessing.DetectionResult
import com.example.biblioscan.ImageProcessing.YoloBookDetector
import com.example.biblioscan.ImageProcessing.extractTextFromBoundingBoxes
import com.example.biblioscan.R
import com.example.biblioscan.databinding.FragmentCameraBinding
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

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) startCamera()
            else Log.e("CameraXApp", "Permission non accordée")
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

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.captureButton.setOnClickListener { takePhoto() }
        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_camera_to_accueil)
        }
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

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dir = File(requireContext().filesDir, "images")
        if (!dir.exists()) dir.mkdirs()

        val imageFile = File(dir, "original_$timeStamp.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedPath = imageFile.absolutePath
                    lifecycleScope.launch {
                        val bitmap = withContext(Dispatchers.IO) {
                            BitmapFactory.decodeFile(savedPath)
                        }

                        val detector = YoloBookDetector(requireContext())
                        val results = detector.detect(bitmap)

                        if (results.isEmpty()) {
                            Log.d("CameraXApp", "Aucun livre détecté.")
                            return@launch
                        }

                        val detectionResults = extractTextFromBoundingBoxes(bitmap, results)
                        val annotatedBitmap = drawBoundingBoxes(bitmap, detectionResults)

                        val processedFile = File(dir, "processed_$timeStamp.jpg")
                        withContext(Dispatchers.IO) {
                            FileOutputStream(processedFile).use { fos ->
                                annotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                            }
                        }

                        val bundle = Bundle().apply {
                            putString("capturedImagePath", processedFile.absolutePath)
                            putParcelableArrayList("detectionResults", ArrayList(detectionResults))
                            putStringArrayList("detectedTexts", ArrayList(detectionResults.map { it.label }))
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
            canvas.drawText(result.label.take(20), result.boundingBox.left, result.boundingBox.top - 10, textPaint)
        }
        return mutableBitmap
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        cameraExecutor.shutdown()
    }
}
