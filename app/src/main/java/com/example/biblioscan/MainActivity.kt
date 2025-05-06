package com.example.biblioscan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.biblioscan.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        checkCameraPermission()
        binding.captureButton.setOnClickListener {
            Log.d("CameraXDebug", "Capture bouton cliqué")
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("CameraXDebug", "Permission caméra accordée")
            startCamera()
        } else {
            Log.d("CameraXDebug", "Permission caméra refusée, demande permission")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        }
    }

    private fun startCamera() {
        Log.d("CameraXDebug", "Démarrage de la caméra")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.previewView.surfaceProvider
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraOptions = listOf(
                CameraSelector.DEFAULT_BACK_CAMERA,
                CameraSelector.DEFAULT_FRONT_CAMERA
            )

            var success = false
            for (selector in cameraOptions) {
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(this, selector, preview, imageCapture)
                    Log.d("CameraXDebug", "Caméra liée avec succès : ${if (selector == CameraSelector.DEFAULT_BACK_CAMERA) "arrière" else "avant"}")
                    success = true
                    break
                } catch (e: Exception) {
                    Log.w("CameraXDebug", "Échec liaison ${if (selector == CameraSelector.DEFAULT_BACK_CAMERA) "arrière" else "avant"}: ${e.message}")
                }
            }

            if (!success) {
                binding.ocrTextView.text = "Aucune caméra disponible."
            }

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("CameraXDebug", "Permission caméra accordée")
            startCamera()
        } else {
            Log.d("CameraXDebug", "Permission caméra refusée")
            binding.ocrTextView.text = "Permission caméra refusée."
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
