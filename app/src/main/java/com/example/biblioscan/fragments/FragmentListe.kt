package com.example.biblioscan.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.biblioscan.Book
import com.example.biblioscan.DetectedBookAdapter
import com.example.biblioscan.R
import com.example.biblioscan.backend.searchBooksFromTitles
import com.example.biblioscan.databinding.FragmentListeBinding
import com.example.biblioscan.ImageProcessing.DetectionResult
import kotlinx.coroutines.launch

class FragmentListe : Fragment() {

    private var _binding: FragmentListeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: DetectedBookAdapter

    private var capturedImagePath: String? = null
    private var detectionResults: ArrayList<DetectionResult> = arrayListOf()
    private var detectedTexts: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            capturedImagePath = it.getString("capturedImagePath")
            detectionResults = it.getParcelableArrayList("detectionResults") ?: arrayListOf()
            detectedTexts = it.getStringArrayList("detectedTexts") ?: emptyList()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListeBinding.inflate(inflater, container, false)

        adapter = DetectedBookAdapter { book ->
            val bundle = Bundle().apply {
                putParcelable("book", book)
            }
            findNavController().navigate(R.id.action_liste_to_resultat, bundle)
        }

        binding.detectedBooksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.detectedBooksRecyclerView.adapter = adapter

        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_liste_to_accueil)
        }

        binding.buttonGoToDetection.setOnClickListener {
            val bundle = Bundle().apply {
                putString("capturedImagePath", capturedImagePath)
                putParcelableArrayList("detectionResults", detectionResults)
            }
            findNavController().navigate(R.id.action_liste_to_watchBooksDetection, bundle)
        }

        loadDetectedBooks()
        return binding.root
    }

    private fun loadDetectedBooks() {
        if (detectedTexts.isEmpty()) {
            binding.emptyContainer.visibility = View.VISIBLE
            binding.detectedBooksRecyclerView.visibility = View.GONE
            return
        }

        lifecycleScope.launch {
            try {
                val books = searchBooksFromTitles(detectedTexts)
                if (books.isEmpty()) {
                    binding.emptyContainer.visibility = View.VISIBLE
                    binding.detectedBooksRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyContainer.visibility = View.GONE
                    binding.detectedBooksRecyclerView.visibility = View.VISIBLE
                    adapter.submitList(books)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val fallbackBooks = detectedTexts.map {
                    Book(title = it.take(30), author = "Inconnu", description = it)
                }
                adapter.submitList(fallbackBooks)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
