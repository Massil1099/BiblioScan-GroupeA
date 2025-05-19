package com.example.biblioscan.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.biblioscan.R
import com.example.biblioscan.DetectedBookAdapter
import com.example.biblioscan.databinding.FragmentListeBinding
import com.example.biblioscan.Book
import com.example.biblioscan.backend.ScanRequest
import com.example.biblioscan.backend.ScanResponse
import com.example.biblioscan.backend.searchBooksFromTitles
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch

class FragmentListe : Fragment() {

    private var _binding: FragmentListeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: DetectedBookAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListeBinding.inflate(inflater, container, false)

        // Initialiser l'adaptateur
        adapter = DetectedBookAdapter { book ->
            // Gérer le clic sur un livre (par exemple, ouvrir les détails)
            val bundle = Bundle().apply {
                putParcelable("book", book)
            }
            findNavController().navigate(R.id.action_liste_to_resultat, bundle)
        }

        // Configurer le RecyclerView
        binding.detectedBooksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.detectedBooksRecyclerView.adapter = adapter

        // Charger les livres détectés
        loadDetectedBooks()

        // Bouton retour
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // ** Nouveau : bouton pour aller vers la détection en temps réel **
        binding.buttonGoToDetection.setOnClickListener {
            findNavController().navigate(R.id.action_liste_to_watchBooksDetection)
        }

        return binding.root
    }

    private fun loadDetectedBooks() {
        val texts = arguments?.getStringArrayList("detectedTexts") ?: emptyList<String>()

        lifecycleScope.launch {
            try {
                val detectedBooks = searchBooksFromTitles(texts)

                if (detectedBooks.isEmpty()) {
                    binding.emptyContainer.visibility = View.VISIBLE
                    binding.detectedBooksRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyContainer.visibility = View.GONE
                    binding.detectedBooksRecyclerView.visibility = View.VISIBLE
                    adapter.submitList(detectedBooks)
                }
            } catch (e: Exception) {
                e.printStackTrace()

                val fallbackBooks = texts.map { text ->
                    Book(title = text.take(30), author = "Inconnu", description = text)
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
