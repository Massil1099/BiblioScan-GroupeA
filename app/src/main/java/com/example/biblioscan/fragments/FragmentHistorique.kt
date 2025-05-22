// FragmentHistorique.kt
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
import com.example.biblioscan.databinding.FragmentHistoriqueBinding

class FragmentHistorique : Fragment() {

    private var _binding: FragmentHistoriqueBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: DetectedBookAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoriqueBinding.inflate(inflater, container, false)

        // Initialiser l'adaptateur
        adapter = DetectedBookAdapter { book ->
            // Gérer le clic sur un livre historique (exemple)
            val bundle = Bundle().apply {
                putString("title", book.title)
                putString("author", book.author)
                putString("description", book.description)
            }
            findNavController().navigate(R.id.action_historique_to_accueil, bundle)
        }

        // Configurer le RecyclerView
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.historyRecyclerView.adapter = adapter

        // Charger l'historique
        loadHistory()

        // Bouton retour
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        return binding.root
    }

    private fun loadHistory() {
        // Simuler une liste vide pour le test
        val historyBooks = emptyList<Book>()

        // Afficher ou masquer le message d'état vide
        if (historyBooks.isEmpty()) {
            binding.emptyContainer.visibility = View.VISIBLE
            binding.historyRecyclerView.visibility = View.GONE
        } else {
            binding.emptyContainer.visibility = View.GONE
            binding.historyRecyclerView.visibility = View.VISIBLE
            adapter.submitList(historyBooks)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
