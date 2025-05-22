package com.example.biblioscan.fragments

import android.graphics.BitmapFactory
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentListe : Fragment() {

    private var _binding: FragmentListeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: DetectedBookAdapter

    private var detectedTexts: List<String> = emptyList()

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

        // Extraire les textes détectés
        detectedTexts = arguments?.getStringArrayList("detectedTexts") ?: emptyList()

        // Afficher l'image capturée si disponible
        val imagePath = arguments?.getString("capturedImagePath")
        if (!imagePath.isNullOrEmpty()) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap != null) {
                binding.capturedImagePreview.setImageBitmap(bitmap)
                binding.capturedImagePreview.visibility = View.VISIBLE
            }
        }

        loadBooks(detectedTexts)

        binding.backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.buttonGoToDetection.setOnClickListener {
            findNavController().navigate(R.id.action_liste_to_watchBooksDetection)
        }

        return binding.root
    }

    private fun loadBooks(texts: List<String>) {
        if (texts.isEmpty()) {
            showEmptyState()
            return
        }

        lifecycleScope.launch {
            try {
                val books = withContext(Dispatchers.IO) {
                    searchBooksFromTitles(texts)
                }

                if (books.isEmpty()) {
                    showFallbackBooks(texts)
                } else {
                    showBooks(books)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showFallbackBooks(texts)
            }
        }
    }

    private fun showBooks(books: List<Book>) {
        binding.emptyContainer.visibility = View.GONE
        binding.detectedBooksRecyclerView.visibility = View.VISIBLE
        adapter.submitList(books)
    }

    private fun showFallbackBooks(texts: List<String>) {
        val fallback = texts.map {
            Book(title = it.take(30), author = "Inconnu", description = it)
        }
        showBooks(fallback)
    }

    private fun showEmptyState() {
        binding.emptyContainer.visibility = View.VISIBLE
        binding.detectedBooksRecyclerView.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
