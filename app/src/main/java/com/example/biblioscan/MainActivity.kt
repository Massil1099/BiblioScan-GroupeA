package com.example.biblioscan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.biblioscan.databinding.ActivityMainBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var outputDirectory: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        checkCameraPermission()

        binding.captureButton.setOnClickListener {
            capturePhoto()
        }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, "BiblioScan").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    private fun capturePhoto() {
        val photoFile = File(
            outputDirectory,
            "BiblioScan-${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraX", "Erreur de capture : ${exc.message}", exc)
                    binding.ocrTextView.text = "Erreur lors de la capture."
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Image enregistrée : ${photoFile.absolutePath}"
                    Log.d("CameraX", msg)
                    binding.ocrTextView.text = msg

                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    binding.capturedImage.setImageBitmap(bitmap)
                }
            }
        )
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.previewView.surfaceProvider
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelectors = listOf(
                CameraSelector.DEFAULT_BACK_CAMERA,
                CameraSelector.DEFAULT_FRONT_CAMERA
            )

            var bound = false
            for (selector in cameraSelectors) {
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(this, selector, preview, imageCapture)
                    Log.d("CameraX", "Caméra liée : ${if (selector == CameraSelector.DEFAULT_BACK_CAMERA) "arrière" else "avant"}")
                    bound = true
                    break
                } catch (e: Exception) {
                    Log.w("CameraX", "Échec liaison caméra ${e.message}")
                }
            }

            if (!bound) {
                binding.ocrTextView.text = "Aucune caméra disponible."
                Log.e("CameraX", "Impossible de lier une caméra disponible")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            binding.ocrTextView.text = "Permission caméra refusée."
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
