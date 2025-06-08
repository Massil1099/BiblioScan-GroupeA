package com.example.biblioscan.fragments

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.biblioscan.ImageProcessing.DetectionResult
import com.example.biblioscan.databinding.FragmentImageDetailBinding
import com.example.biblioscan.databinding.ItemTextBinding

class ImageDetailFragment : Fragment() {
    private var _binding: FragmentImageDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var textsAdapter: TextsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImageDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolbarDetail.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        arguments?.let { bundle ->
            val imagePath = bundle.getString("imagePath")
            val results = arguments?.getParcelableArray("detectionResults")
                ?.filterIsInstance<DetectionResult>()
                ?.toList() ?: emptyList()
            // Afficher l'image
            BitmapFactory.decodeFile(imagePath)?.let { bitmap ->
                binding.imageViewDetail.setImageBitmap(bitmap)
            }

            // Configurer la liste des textes détectés
            textsAdapter = TextsAdapter(results.map { it.label })
            binding.recyclerTexts.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = textsAdapter
            }

            // Masquer la liste si aucun texte détecté
            if (results.isEmpty()) {
                binding.textsTitle.visibility = View.GONE
                binding.recyclerTexts.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class TextsAdapter(private val texts: List<String>) :
    RecyclerView.Adapter<TextsAdapter.TextViewHolder>() {

    inner class TextViewHolder(val binding: ItemTextBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(text: String) {
            binding.textItem.text = text
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextViewHolder {
        val binding = ItemTextBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TextViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TextViewHolder, position: Int) {
        val text = texts.getOrNull(position) ?: "Texte indisponible"
        holder.bind(text)
    }

    override fun getItemCount(): Int = texts.size
}