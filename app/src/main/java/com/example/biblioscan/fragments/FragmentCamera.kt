// FragmentCamera.kt
package com.example.biblioscan.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.biblioscan.R
import com.example.biblioscan.databinding.FragmentCameraBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


import android.graphics.BitmapFactory
import com.example.biblioscan.ImageProcessing.YoloBookDetector
import com.example.biblioscan.ImageProcessing.extractTextFromBoundingBoxes


class FragmentCamera : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                Log.e("CameraXApp", "Permission non accordée")
            }
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

        // Vérification des permissions
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // Bouton de capture
        binding.captureButton.setOnClickListener {
            takePhoto()
        }

        // Bouton de retour
        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_camera_to_accueil)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Configuration de la Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

            // Configuration pour la capture d'image
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // Sélection de la caméra arrière
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Démarrer la caméra
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        // Création du fichier pour sauvegarder l'image
        val photoFile = File(
            requireContext().getExternalFilesDir(null),
            "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Capture de l'image
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = photoFile.absolutePath
                    Log.d("CameraXApp", "Image sauvegardée : $savedUri")

                    // Étape 1 : Charger le bitmap
                    val bitmap = BitmapFactory.decodeFile(savedUri)

                    // Étape 2 : Détecter les livres avec YOLO
                    val detector = YoloBookDetector(requireContext())
                    val results = detector.detect(bitmap)

                    if (results.isEmpty()) {
                        Log.d("CameraXApp", "Aucun livre détecté.")
                        return
                    }

                    // Étape 3 : Appliquer OCR (extraction du texte sur les bounding boxes)
                    extractTextFromBoundingBoxes(bitmap, results) { texts ->
                        texts.forEachIndexed { index, text ->
                            Log.d("OCR", "Livre ${index + 1} : $text")
                        }

                        // Exemple :on pourra maintenant naviguer vers un fragment avec les textes en paramètre
                        val bundle = Bundle().apply {
                            putString("capturedImagePath", savedUri)
                            putStringArrayList("detectedTexts", ArrayList(texts))
                        }
                        findNavController().navigate(R.id.action_camera_to_liste, bundle)
                    }
                }


                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraXApp", "Erreur lors de la capture : ${exception.message}", exception)
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        cameraExecutor.shutdown()
    }
}
