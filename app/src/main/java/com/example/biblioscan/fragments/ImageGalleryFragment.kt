package com.example.biblioscan.fragments

import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.biblioscan.ImageProcessing.DetectionResult
import com.example.biblioscan.R
import com.example.biblioscan.databinding.FragmentImageGalleryBinding
import com.example.biblioscan.databinding.ItemImageFrameBinding
import java.io.File

class ImageGalleryFragment : Fragment() {
    private var _binding: FragmentImageGalleryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ImageAdapter
    private val imageList = mutableListOf<File>()
    private val detectionResults = mutableListOf<List<DetectionResult>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImageGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolbarGallery.title = "Images annotées"
        binding.toolbarGallery.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // Récupérer les résultats de détection passés en arguments
        arguments?.getParcelableArrayList<DetectionResult>("detectionResultsFromVideo")?.let {
            // Associer chaque image à ses résultats (suppose que l'ordre est le même)
            detectionResults.clear()
            val dir = File(requireContext().filesDir, "video_frames")
            val frameCount = dir.listFiles()?.size ?: 0
            val resultsPerFrame = if (frameCount > 0) it.size / frameCount else 0

            if (resultsPerFrame > 0) {
                detectionResults.addAll(it.chunked(resultsPerFrame))
            } else {
                // Fallback si impossible de calculer
                detectionResults.addAll(List(frameCount) { emptyList() })
            }
        }

        val spanCount = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 2 else 3
        binding.recyclerViewImages.layoutManager = GridLayoutManager(requireContext(), spanCount)

        adapter = ImageAdapter(imageList, detectionResults) { file, results ->
            showImageDetail(file, results)
        }
        binding.recyclerViewImages.adapter = adapter

        loadImagesFromInternalStorage()
    }

    private fun showImageDetail(file: File, results: List<DetectionResult>) {
        val bundle = Bundle().apply {
            putString("imagePath", file.absolutePath)
            // Conversion explicite en Array de Parcelable
            putParcelableArray("detectionResults", results.toTypedArray())
        }

        try {
            // Navigation avec vérification de la destination actuelle
            if (findNavController().currentDestination?.id == R.id.imageGalleryFragment) {
                findNavController().navigate(
                    R.id.action_imageGalleryFragment_to_imageDetailFragment,
                    bundle
                )
            } else {
                // Fallback si on ne vient pas de imageGalleryFragment
                findNavController().navigate(
                    R.id.imageDetailFragment,
                    bundle
                )
            }
        } catch (e: Exception) {
            // Fallback ultime en cas d'échec
            findNavController().navigate(
                R.id.imageDetailFragment,
                bundle
            )
        }
    }

    private fun loadImagesFromInternalStorage() {
        val dir = File(requireContext().filesDir, "video_frames")
        if (dir.exists()) {
            imageList.clear()
            imageList.addAll(dir.listFiles()?.sortedBy { it.name } ?: emptyList())

            // Si on a moins de résultats que d'images, on complète avec des listes vides
            while (detectionResults.size < imageList.size) {
                detectionResults.add(emptyList())
            }

            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


class ImageAdapter(
    private val imageFiles: List<File>,
    private val detectionResults: List<List<DetectionResult>>,
    private val onItemClick: (File, List<DetectionResult>) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val binding: ItemImageFrameBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(file: File, results: List<DetectionResult>) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            binding.imageViewItem.setImageBitmap(bitmap)

            // Afficher le nombre de détections en badge
            binding.detectionCountText.text = results.size.toString()
            binding.detectionCountText.visibility = if (results.isNotEmpty()) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                onItemClick(file, results)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageFrameBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val file = imageFiles[position]
        val results = detectionResults.getOrElse(position) { emptyList() }
        holder.bind(file, results)
    }

    override fun getItemCount(): Int = imageFiles.size
}