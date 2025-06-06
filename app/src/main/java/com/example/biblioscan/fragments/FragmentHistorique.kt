package com.example.biblioscan.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.biblioscan.DetectedBookAdapter
import com.example.biblioscan.R
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
    private var allBooks = listOf<com.example.biblioscan.Book>()

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

        binding.clearHistoryButton.setOnClickListener {
            lifecycleScope.launch {
                val username = sessionManager.getUsername().first()
                if (username != null && username != "guest") {
                    AppDatabase.getDatabase(requireContext()).biblioScanDao().clearHistory(username)
                    loadHistory()
                    Toast.makeText(requireContext(), "Historique vidé", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterBooks(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        loadHistory()
    }

    private fun loadHistory() {
        val dao = AppDatabase.getDatabase(requireContext()).biblioScanDao()

        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.historyRecyclerView.visibility = View.GONE
            binding.emptyContainer.visibility = View.GONE

            val username = sessionManager.getUsername().first()

            if (username == null || username == "guest") {
                Toast.makeText(requireContext(), "Fonctionnalité réservée aux utilisateurs connectés", Toast.LENGTH_SHORT).show()
                binding.emptyContainer.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
                return@launch
            }

            val historyEntities = dao.getHistoryForUser(username)
            val historyBooks = historyEntities.map { it.toBook() }

            binding.progressBar.visibility = View.GONE

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


    private fun filterBooks(query: String) {
        val filtered = if (query.isEmpty()) {
            allBooks
        } else {
            allBooks.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.author.contains(query, ignoreCase = true)
            }
        }
        adapter.submitList(filtered)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
