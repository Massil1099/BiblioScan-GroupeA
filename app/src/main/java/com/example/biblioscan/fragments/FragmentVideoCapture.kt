package com.example.biblioscan.fragments

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.biblioscan.databinding.FragmentVideoCaptureBinding
import java.util.concurrent.Executors
import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.biblioscan.ImageProcessing.DetectionResult
import com.example.biblioscan.ImageProcessing.YoloBookDetector
import com.example.biblioscan.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class FragmentVideoCapture : Fragment() {

    private var _binding: FragmentVideoCaptureBinding? = null
    private val binding get() = _binding!!

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private val cameraExecutor by lazy { Executors.newSingleThreadExecutor() }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) startCamera()
        else {
            Toast.makeText(requireContext(), "Permissions manquantes", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoCaptureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnStartRecording.setOnClickListener { startRecording() }
        binding.btnStopRecording.setOnClickListener { stopRecording() }
        checkPermissions()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()

            videoCapture = VideoCapture.withOutput(recorder)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, videoCapture)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur caméra : ${e.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun startRecording() {
        val videoCapture = this.videoCapture ?: return

        val name = "video_${System.currentTimeMillis()}.mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/BiblioScan")
            }
        }

        val outputOptions = MediaStoreOutputOptions.Builder(
            requireContext().contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(requireContext(), "Permission AUDIO manquante", Toast.LENGTH_SHORT).show()
            return
        }

        recording = videoCapture.output
            .prepareRecording(requireContext(), outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(requireContext())) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        binding.btnStartRecording.isEnabled = false
                        binding.btnStopRecording.isEnabled = true
                        Toast.makeText(requireContext(), "Enregistrement démarré", Toast.LENGTH_SHORT).show()
                    }
                    is VideoRecordEvent.Finalize -> {
                        binding.btnStopRecording.isEnabled = false
                        binding.btnStartRecording.isEnabled = true

                        if (!event.hasError()) {
                            val videoUri = event.outputResults.outputUri
                            Toast.makeText(requireContext(), "Vidéo enregistrée", Toast.LENGTH_SHORT).show()

                            val videoFile = getFileFromUri(videoUri)
                            if (videoFile != null) {
                                lifecycleScope.launch {
                                    withContext(Dispatchers.Main) {
                                        binding.loadingOverlay.visibility = View.VISIBLE                                     }
                                    processVideoFrames(videoFile)

                                    withContext(Dispatchers.Main) {
                                        binding.loadingOverlay.visibility = View.GONE

                                        AlertDialog.Builder(requireContext())
                                            .setTitle("Vidéo enregistrée")
                                            .setMessage("Que souhaitez-vous faire ?")
                                            .setPositiveButton("Voir la vidéo") { _, _ ->
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(videoUri, "video/mp4")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                startActivity(intent)
                                            }
                                            .setNegativeButton("Voir les bounding boxes") { _, _ ->
                                                findNavController().navigate(R.id.action_fragmentVideoCapture_to_imageGalleryFragment)
                                            }
                                            .setCancelable(false)
                                            .show()
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(requireContext(), "Erreur d'enregistrement : ${event.error}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
    }

    private fun getFileFromUri(uri: Uri): File? {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        return if (cursor != null && cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
            val path = if (index != -1) cursor.getString(index) else null
            cursor.close()
            path?.let { File(it) }
        } else {
            null
        }
    }

    private suspend fun processVideoFrames(videoFile: File) = withContext(Dispatchers.IO) {
        val frames = extractFramesFromVideo(videoFile)
        val detector = YoloBookDetector(requireContext())
        val dir = File(requireContext().filesDir, "video_frames")
        if (!dir.exists()) dir.mkdirs()

        clearDirectory(dir)
        frames.forEachIndexed { index, bitmap ->
            val results = detector.detect(bitmap)
            val annotated = drawBoundingBoxes(bitmap, results)
            val frameFile = File(dir, "frame_${index}.jpg")
            FileOutputStream(frameFile).use { fos ->
                annotated.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            }
        }
        Log.d("VideoProcessing", "Frames annotées sauvegardées dans ${dir.absolutePath}")
    }

    private fun extractFramesFromVideo(videoFile: File): List<Bitmap> {
        val retriever = MediaMetadataRetriever()
        val frameList = mutableListOf<Bitmap>()
        try {
            retriever.setDataSource(videoFile.absolutePath)
            val duration =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L

            val intervalMs = 500L // Extraire une frame toutes les 500ms
            for (timeMs in 0 until duration step intervalMs) {
                val frame = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                frame?.let { frameList.add(it) }
            }
        } catch (e: Exception) {
            Log.e("VideoProcessing", "Erreur extraction des frames : ${e.message}", e)
        } finally {
            retriever.release()
        }
        return frameList
    }

    private fun drawBoundingBoxes(bitmap: Bitmap, results: List<DetectionResult>): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val textPaint = Paint().apply {
            color = Color.YELLOW
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
        }

        for (result in results) {
            canvas.drawRect(result.boundingBox, paint)
            canvas.drawText(result.label.take(20), result.boundingBox.left, result.boundingBox.top - 10, textPaint)
        }

        return mutableBitmap
    }


    private fun clearDirectory(dir: File) {
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.forEach { it.delete() }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        cameraExecutor.shutdown()
    }
}

