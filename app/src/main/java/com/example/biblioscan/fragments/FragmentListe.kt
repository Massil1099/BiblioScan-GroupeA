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
import com.example.biblioscan.databinding.FragmentListeBinding
import com.example.biblioscan.ImageProcessing.DetectionResult
import com.example.biblioscan.backend.searchBooksByAuthorAndTitle
import com.example.biblioscan.data_app.AppDatabase
import com.example.biblioscan.data_app.BookEntity
import com.example.biblioscan.data_app.HistoryEntity
import com.example.biblioscan.session.UserSessionManager
import kotlinx.coroutines.flow.firstOrNull
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

    private val serverUrl = "http://172.16.1.217:8000" // à adapter (localhost ne marche pas sur Android émulateur)

    private fun loadDetectedBooks() {
        if (detectedTexts.isEmpty()) {
            binding.emptyContainer.visibility = View.VISIBLE
            binding.detectedBooksRecyclerView.visibility = View.GONE
            return
        }

        lifecycleScope.launch {
            try {
                // Ici on appelle ta nouvelle fonction, avec le serveur et la liste de textes OCR
                val books = searchBooksByAuthorAndTitle(detectedTexts, serverUrl)
                if (books.isEmpty()) {
                    binding.emptyContainer.visibility = View.VISIBLE
                    binding.detectedBooksRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyContainer.visibility = View.GONE
                    binding.detectedBooksRecyclerView.visibility = View.VISIBLE
                    adapter.submitList(books)

                    // Sauvegarde dans l'historique
                    saveBooksToHistory(books)
                }
            } catch (e: Exception) {
                e.printStackTrace()

                val fallbackBooks = detectedTexts.map {
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

                // Même pour fallback :
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
            // Sauvegarde du livre s'il n'est pas déjà dans la table "books"
            val bookEntity = BookEntity(
                title = book.title,
                author = book.author ?: "Auteur inconnu",
                description = book.description ?: "",
                imageUrl = book.imageUrl
            )
            dao.insertBook(bookEntity)

            // Enregistrement dans l'historique
            val history = HistoryEntity(username = username, bookTitle = book.title)
            dao.addHistory(history)
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
