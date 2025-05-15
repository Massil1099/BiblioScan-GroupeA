// FragmentFavoris.kt
package com.example.biblioscan.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.biblioscan.Book
import com.example.biblioscan.R
import com.example.biblioscan.DetectedBookAdapter
import com.example.biblioscan.databinding.FragmentFavorisBinding

class FragmentFavoris : Fragment() {

    private var _binding: FragmentFavorisBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: DetectedBookAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavorisBinding.inflate(inflater, container, false)

        // Initialiser l'adaptateur
        adapter = DetectedBookAdapter { book ->
            // Gérer le clic sur un livre favori (exemple)
            val bundle = Bundle().apply {
                putString("title", book.title)
                putString("author", book.author)
                putString("description", book.description)
            }
            findNavController().navigate(R.id.action_favoris_to_accueil, bundle)
        }

        // Configurer le RecyclerView
        binding.favoritesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.favoritesRecyclerView.adapter = adapter

        // Charger les favoris
        loadFavorites()

        // Bouton retour
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        return binding.root
    }

    private fun loadFavorites() {
        // Simuler une liste vide pour le test
        val favoriteBooks = emptyList<Book>()

        // Afficher ou masquer le message d'état vide
        if (favoriteBooks.isEmpty()) {
            binding.emptyContainer.visibility = View.VISIBLE
            binding.favoritesRecyclerView.visibility = View.GONE
        } else {
            binding.emptyContainer.visibility = View.GONE
            binding.favoritesRecyclerView.visibility = View.VISIBLE
            adapter.submitList(favoriteBooks)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
