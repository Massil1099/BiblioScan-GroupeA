package com.example.biblioscan.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.biblioscan.R
import com.example.biblioscan.DetectedBookAdapter
import com.example.biblioscan.data_app.AppDatabase
import com.example.biblioscan.data_app.toBook
import com.example.biblioscan.databinding.FragmentHistoriqueBinding
import com.example.biblioscan.session.UserSessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FragmentHistorique : Fragment() {

    private var _binding: FragmentHistoriqueBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: DetectedBookAdapter
    private lateinit var sessionManager: UserSessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoriqueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = UserSessionManager(requireContext())

        // Initialiser l’adaptateur
        adapter = DetectedBookAdapter { book ->
            val bundle = Bundle().apply {
                putParcelable("book", book)
            }
            findNavController().navigate(R.id.action_historique_to_resultat, bundle)
        }

        binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.historyRecyclerView.adapter = adapter

        binding.backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Charger l’historique
        loadHistory()


        binding.clearHistoryButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val username = sessionManager.getUsername().first()
                if (username != null && username != "guest") {
                    val dao = AppDatabase.getDatabase(requireContext()).biblioScanDao()
                    dao.clearHistory(username)
                    loadHistory()
                    Toast.makeText(requireContext(), "Historique vidé", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    private fun loadHistory() {
        val dao = AppDatabase.getDatabase(requireContext()).biblioScanDao()

        viewLifecycleOwner.lifecycleScope.launch {
            val username = sessionManager.getUsername().first()

            if (username == null || username == "guest") {
                Toast.makeText(requireContext(), "Fonctionnalité réservée aux utilisateurs connectés", Toast.LENGTH_SHORT).show()
                binding.emptyContainer.visibility = View.VISIBLE
                binding.historyRecyclerView.visibility = View.GONE
                return@launch
            }

            val historyEntities = dao.getHistoryForUser(username)
            val historyBooks = historyEntities.map { it.toBook() }

            if (historyBooks.isEmpty()) {
                binding.emptyContainer.visibility = View.VISIBLE
                binding.historyRecyclerView.visibility = View.GONE
            } else {
                binding.emptyContainer.visibility = View.GONE
                binding.historyRecyclerView.visibility = View.VISIBLE
                adapter.submitList(historyBooks)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
