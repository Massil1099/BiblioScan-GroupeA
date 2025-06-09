package com.example.biblioscan.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Spinner
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
    private var currentFilter = "Tous"

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
                filterBooks()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        setupFilterSpinner()
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
            allBooks = historyEntities.map { it.toBook() }

            binding.progressBar.visibility = View.GONE

            if (allBooks.isEmpty()) {
                binding.emptyContainer.visibility = View.VISIBLE
                binding.historyRecyclerView.visibility = View.GONE
            } else {
                binding.emptyContainer.visibility = View.GONE
                binding.historyRecyclerView.visibility = View.VISIBLE
                filterBooks()
            }
        }
    }

    private fun setupFilterSpinner() {
        val options = listOf("Tous", "Favoris", "Non favoris", "A-Z", "Z-A", "Plus récent")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.filterSpinner.adapter = spinnerAdapter

        binding.filterSpinner.setSelection(0)
        binding.filterSpinner.setOnItemSelectedListener { _, _, position, _ ->
            currentFilter = options[position]
            filterBooks()
        }
    }

    private fun filterBooks() {
        val query = binding.searchEditText.text.toString().trim()

        var filtered = allBooks
        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.author.contains(query, ignoreCase = true)
            }
        }

        filtered = when (currentFilter) {
            "Favoris" -> filtered.filter { it.isFavorite }
            "Non favoris" -> filtered.filter { !it.isFavorite }
            else -> filtered
        }

        filtered = when (currentFilter) {
            "A-Z" -> filtered.sortedBy { it.title.lowercase() }
            "Z-A" -> filtered.sortedByDescending { it.title.lowercase() }
            "Plus récent" -> filtered.sortedByDescending { it.publishedDate ?: "" }
            else -> filtered
        }

        adapter.submitList(filtered)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Extension pour Spinner moderne
    private fun Spinner.setOnItemSelectedListener(listener: (adapter: Spinner, view: View?, position: Int, id: Long) -> Unit) {
        this.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                listener(this@setOnItemSelectedListener, view, position, id)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
    }
}
