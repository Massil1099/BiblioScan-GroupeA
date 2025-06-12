package com.example.biblioscan.special_video

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
import androidx.lifecycle.lifecycleScope
import areTextsSimilar
import com.example.biblioscan.imageProcessing.DetectionResult
import com.example.biblioscan.imageProcessing.YoloBookDetector
import com.example.biblioscan.imageProcessing.extractTextFromBoundingBoxes
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

    private val allResultsWithText = mutableListOf<DetectionResult>()
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    videoCapture
                )
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

                            val videoFile = uriToFile(videoUri)
                            if (videoFile != null) {
                                lifecycleScope.launch {
                                    withContext(Dispatchers.Main) {
                                        binding.loadingOverlay.visibility = View.VISIBLE
                                    }

                                    processVideoFrames(videoFile)

                                    withContext(Dispatchers.Main) {
                                        binding.loadingOverlay.visibility = View.GONE
                                        AlertDialog.Builder(requireContext())
                                            .setTitle("Vidéo enregistrée")
                                            .setMessage("Que souhaitez-vous faire ?")
                                            .setPositiveButton("Voir la vidéo") { _, _ ->
                                                startActivity(Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(videoUri, "video/mp4")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                })
                                            }
                                            .setNegativeButton("Voir les bounding boxes") { _, _ ->
                                                findNavController().navigate(R.id.action_fragmentVideoCapture_to_imageGalleryFragment)
                                            }
                                            .setNeutralButton("Voir la liste des livres") { _, _ ->
                                                val bundle = Bundle().apply {
                                                    putParcelableArrayList("detectionResultsFromVideo", ArrayList(allResultsWithText))
                                                    putStringArray("detectedTextsFromVideo", allResultsWithText.map { it.label }.toTypedArray())
                                                }
                                                findNavController().navigate(R.id.action_fragmentVideoCapture_to_fragmentListeVideo, bundle)
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

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
            inputStream?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            file
        } catch (e: Exception) {
            Log.e("uriToFile", "Erreur conversion Uri -> File : ${e.message}")
            null
        }
    }


    private suspend fun processVideoFrames(videoFile: File) = withContext(Dispatchers.IO) {
        val frames = extractFramesFromVideo(videoFile)
        val detector = YoloBookDetector(requireContext())
        val dir = File(requireContext().filesDir, "video_frames").apply { mkdirs() }

        clearDirectory(dir)
        allResultsWithText.clear()

        val alreadySeenTexts = mutableListOf<String>()

        frames.forEachIndexed { index, bitmap ->
            val results = detector.detect(bitmap)
            val resultsWithText = extractTextFromBoundingBoxes(bitmap, results)

            // Garde tous les résultats pour affichage
            val toDraw = resultsWithText.toMutableList()

            // Déduplication pour allResultsWithText
            val filteredResults = resultsWithText.filter { detection ->
                val text = detection.label.trim()
                if (text.length < 5) {
                    detection.status = "no_text"
                    return@filter false
                }

                val isDuplicate = alreadySeenTexts.any { seen ->
                    areTextsSimilar(seen, text)
                }

                if (isDuplicate) {
                    detection.status = "duplicate"
                    false
                } else {
                    alreadySeenTexts.add(text)
                    true
                }
            }

            // Ajoute uniquement les textes non dupliqués ou valides
            allResultsWithText.addAll(filteredResults)

            // Affiche toutes les détections pour visualisation
            val annotatedBitmap = drawBoundingBoxes(bitmap, toDraw)

            val frameFile = File(dir, "frame_$index.jpg")
            FileOutputStream(frameFile).use { fos ->
                annotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            }

            Log.d("VideoProcessing", "✅ Frame $index traitée : ${filteredResults.size} gardées / ${resultsWithText.size} initiales")
        }

        Log.d("VideoProcessing", "🎬 Traitement terminé. ${frames.size} frames, ${allResultsWithText.size} détections uniques.")
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

        results.forEach {
            canvas.drawRect(it.boundingBox, paint)
            canvas.drawText(it.label.take(20), it.boundingBox.left, it.boundingBox.top - 10, textPaint)
        }

        return mutableBitmap
    }

    private fun clearDirectory(dir: File) {
        dir.listFiles()?.forEach { it.delete() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        cameraExecutor.shutdown()
    }
}


fun extractFramesFromVideo(videoFile: File): List<Bitmap> {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(videoFile.absolutePath)
    val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L

    val frameIntervalMs = 3000L  // Extraction d'une frame toutes les 500ms (modifiable)
    val frames = mutableListOf<Bitmap>()

    var timeMs = 0L
    while (timeMs < duration) {
        val frame = retriever.getFrameAtTime(timeMs * 1000)  // microsecondes
        if (frame != null) {
            frames.add(frame)
        }
        timeMs += frameIntervalMs
    }
    retriever.release()
    return frames
}