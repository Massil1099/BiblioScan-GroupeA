package com.example.biblioscan.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.biblioscan.Book
import com.example.biblioscan.DetectedBookAdapter
import com.example.biblioscan.ImageProcessing.DetectionResult
import com.example.biblioscan.R
import com.example.biblioscan.backend.searchBooksFromTitles
import com.example.biblioscan.data_app.AppDatabase
import com.example.biblioscan.data_app.BookEntity
import com.example.biblioscan.data_app.HistoryEntity
import com.example.biblioscan.databinding.FragmentListeVideoBinding
import com.example.biblioscan.session.UserSessionManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class FragmentListeVideo : Fragment() {

    private var _binding: FragmentListeVideoBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: DetectedBookAdapter

    private var detectedTextsFromVideo: List<String> = emptyList()
    private var detectionResultsFromVideo: ArrayList<DetectionResult> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            detectedTextsFromVideo = it.getStringArray("detectedTextsFromVideo")?.toList() ?: emptyList() // Utilisez getStringArray
            detectionResultsFromVideo = it.getParcelableArrayList("detectionResultsFromVideo") ?: arrayListOf()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListeVideoBinding.inflate(inflater, container, false)

        // Initialisation du RecyclerView
        adapter = DetectedBookAdapter { book ->
            val bundle = Bundle().apply {
                putParcelable("book", book)
            }
            findNavController().navigate(R.id.action_listeVideo_to_resultat, bundle)
        }

        binding.detectedBooksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.detectedBooksRecyclerView.adapter = adapter

        // Bouton retour
        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_listeVideo_to_videoCapture)
        }

        // Bouton voir les tranches
        binding.buttonGoToDetection.setOnClickListener {
            val bundle = Bundle().apply {
                putParcelableArrayList("detectionResultsFromVideo", detectionResultsFromVideo)
            }
            findNavController().navigate(R.id.action_listeVideo_to_imageGalleryFragment, bundle)
        }

        // Chargement des livres
        loadDetectedBooksFromVideo()

        return binding.root
    }

    private fun loadDetectedBooksFromVideo() {
        if (detectedTextsFromVideo.isEmpty()) {
            binding.emptyContainer.visibility = View.VISIBLE
            binding.detectedBooksRecyclerView.visibility = View.GONE
            return
        }
        Log.d("FragmentListeVideo", "Detected texts for search: $detectedTextsFromVideo")

        lifecycleScope.launch {
            try {
                val books = searchBooksFromTitles(detectedTextsFromVideo)
                if (books.isEmpty()) {
                    binding.emptyContainer.visibility = View.VISIBLE
                    binding.detectedBooksRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyContainer.visibility = View.GONE
                    binding.detectedBooksRecyclerView.visibility = View.VISIBLE
                    adapter.submitList(books)
                    saveBooksToHistory(books)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val fallbackBooks = detectedTextsFromVideo.map {
                    Book(
                        title = it.take(30),
                        author = "Inconnu",
                        description = it,
                        imageUrl = null,
                        averageRating = 0.0,
                        categories = emptyList(),
                        isbn13 = "",
                        language = "fr",
                        pageCount = 0,
                        publishedDate = "",
                        publisher = "Inconnu",
                        ratingsCount = 0
                    )
                }

                adapter.submitList(fallbackBooks)
                saveBooksToHistory(fallbackBooks)
            }
        }
    }

    private suspend fun saveBooksToHistory(books: List<Book>) {
        val sessionManager = UserSessionManager(requireContext())
        val username = sessionManager.getUsername().firstOrNull()

        if (username.isNullOrEmpty() || username == "guest") return

        val dao = AppDatabase.getDatabase(requireContext()).biblioScanDao()

        for (book in books) {
            val bookEntity = BookEntity(
                title = book.title,
                author = book.author ?: "Auteur inconnu",
                description = book.description ?: "",
                imageUrl = book.imageUrl
            )
            dao.insertBook(bookEntity)

            val history = HistoryEntity(username = username, bookTitle = book.title)
            dao.addHistory(history)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

