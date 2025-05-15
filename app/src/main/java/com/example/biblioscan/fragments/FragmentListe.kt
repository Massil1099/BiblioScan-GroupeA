// FragmentListe.kt
package com.example.biblioscan.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.biblioscan.R
import com.example.biblioscan.DetectedBookAdapter
import com.example.biblioscan.databinding.FragmentListeBinding
import com.example.biblioscan.Book

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
                putString("title", book.title)
                putString("author", book.author)
                putString("description", book.description)
            }
            findNavController().navigate(R.id.action_liste_to_accueil, bundle)
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

        return binding.root
    }

    private fun loadDetectedBooks() {
        // Simuler une liste vide pour le test
        val detectedBooks = emptyList<Book>()

        // Afficher ou masquer le message d'état vide
        if (detectedBooks.isEmpty()) {
            binding.emptyContainer.visibility = View.VISIBLE
            binding.detectedBooksRecyclerView.visibility = View.GONE
        } else {
            binding.emptyContainer.visibility = View.GONE
            binding.detectedBooksRecyclerView.visibility = View.VISIBLE
            adapter.submitList(detectedBooks)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
