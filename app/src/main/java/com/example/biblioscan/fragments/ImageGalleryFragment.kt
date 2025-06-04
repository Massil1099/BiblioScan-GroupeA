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
import com.example.biblioscan.databinding.FragmentImageGalleryBinding
import com.example.biblioscan.databinding.ItemImageFrameBinding
import java.io.File

class ImageGalleryFragment : Fragment() {

    private var _binding: FragmentImageGalleryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ImageAdapter
    private val imageList = mutableListOf<File>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImageGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Toolbar config
        binding.toolbarGallery.title = "Images annotées"
        binding.toolbarGallery.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // Grid layout : 2 colonnes portrait, 3 paysage
        val spanCount = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 2 else 3
        binding.recyclerViewImages.layoutManager = GridLayoutManager(requireContext(), spanCount)

        adapter = ImageAdapter(imageList)
        binding.recyclerViewImages.adapter = adapter

        loadImagesFromInternalStorage()
    }

    private fun loadImagesFromInternalStorage() {
        val dir = File(requireContext().filesDir, "video_frames")
        if (dir.exists()) {
            imageList.clear()
            imageList.addAll(dir.listFiles()?.sortedBy { it.name } ?: emptyList())
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


class ImageAdapter(private val imageFiles: List<File>) :
    RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val binding: ItemImageFrameBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageFrameBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val file = imageFiles[position]
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        holder.binding.imageViewItem.setImageBitmap(bitmap)
    }

    override fun getItemCount(): Int = imageFiles.size
}
