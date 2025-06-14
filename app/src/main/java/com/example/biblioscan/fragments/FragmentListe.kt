package com.example.biblioscan.fragments

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.biblioscan.Book
import com.example.biblioscan.DetectedBookAdapter
import com.example.biblioscan.R
import com.example.biblioscan.backend.searchBooksFromTitles
import com.example.biblioscan.data_app.AppDatabase
import com.example.biblioscan.data_app.BookEntity
import com.example.biblioscan.data_app.HistoryEntity
import com.example.biblioscan.databinding.FragmentListeBinding
import com.example.biblioscan.imageProcessing.DetectionResult
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
    private var allBooks: List<Book> = emptyList()
    private var currentSort = "Plus récent"
    private var processingStartTime: Long = 0
    private var yoloDuration: Long = 0
    private var ocrDuration: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            capturedImagePath = it.getString("capturedImagePath")
            detectionResults = it.getParcelableArrayList("detectionResults") ?: arrayListOf()
            detectedTexts = it.getStringArrayList("detectedTexts") ?: emptyList()
            processingStartTime = it.getLong("processingStartTime", 0)
            yoloDuration = it.getLong("yoloDuration", 0)
            ocrDuration = it.getLong("ocrDuration", 0)
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

        setupSortSpinner()
        loadDetectedBooks()
        return binding.root
    }

    private fun setupSortSpinner() {
        val options = listOf("Plus récent", "Titre A-Z", "Titre Z-A")
        val adapterSpinner = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.sortSpinner.adapter = adapterSpinner

        binding.sortSpinner.setSelection(0)
        binding.sortSpinner.setOnItemSelectedListener { _, _, position, _ ->
            currentSort = options[position]
            applySorting()
        }
    }

    private fun loadDetectedBooks() {
        if (detectedTexts.isEmpty()) {
            binding.emptyContainer.visibility = View.VISIBLE
            binding.detectedBooksRecyclerView.visibility = View.GONE
            return
        }

        lifecycleScope.launch {
            try {
                val apiStart = System.currentTimeMillis()
                val books = searchBooksFromTitles(detectedTexts)
                allBooks = books
                val apiEnd = System.currentTimeMillis()
                val apiDuration = apiEnd - apiStart

                applySorting()
                val processingEndTime = System.currentTimeMillis()
                val totalDuration = processingEndTime - processingStartTime

                val durationMs = processingEndTime - processingStartTime
                Log.d("PERF_PHOTO", """
                   Performance détection :
                  - YOLO          : ${yoloDuration} ms
                  - OCR           : ${ocrDuration} ms
                  - GoogleBooksAPI: ${apiDuration} ms
                  - TOTAL         : ${totalDuration} ms
                """.trimIndent())

                saveBooksToHistory(books)
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
                allBooks = fallbackBooks
                applySorting()

                saveBooksToHistory(fallbackBooks)
            }
        }
    }

    private fun applySorting() {
        val sortedList = when (currentSort) {
            "Titre A-Z" -> allBooks.sortedBy { it.title }
            "Titre Z-A" -> allBooks.sortedByDescending { it.title }
            else -> allBooks
        }

        if (sortedList.isEmpty()) {
            binding.emptyContainer.visibility = View.VISIBLE
            binding.detectedBooksRecyclerView.visibility = View.GONE
        } else {
            binding.emptyContainer.visibility = View.GONE
            binding.detectedBooksRecyclerView.visibility = View.VISIBLE
        }

        adapter.submitList(sortedList)
    }

    private suspend fun saveBooksToHistory(books: List<Book>) {
        val sessionManager = UserSessionManager(requireContext())
        val username = sessionManager.getUsername().firstOrNull() ?: return
        if (username == "guest") return

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

    private fun Spinner.setOnItemSelectedListener(listener: (adapter: Spinner, view: View?, position: Int, id: Long) -> Unit) {
        this.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                listener(this@setOnItemSelectedListener, view, position, id)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
    }
}
