package com.example.biblioscan.fragments

import android.os.Bundle
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
import com.example.biblioscan.databinding.FragmentFavorisBinding
import com.example.biblioscan.session.UserSessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FragmentFavoris : Fragment() {

    private var _binding: FragmentFavorisBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: DetectedBookAdapter
    private lateinit var sessionManager: UserSessionManager
    private var allBooks = listOf<com.example.biblioscan.Book>()
    private var currentFilter = "Tous"
    private var currentSort = "Plus récent"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavorisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = UserSessionManager(requireContext())
        adapter = DetectedBookAdapter { book ->
            val bundle = Bundle().apply {
                putParcelable("book", book)
            }
            findNavController().navigate(R.id.action_favoris_to_resultat, bundle)
        }

        binding.favorisRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.favorisRecyclerView.adapter = adapter

        binding.backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.clearFavoritesButton.setOnClickListener {
            lifecycleScope.launch {
                val username = sessionManager.getUsername().first() ?: return@launch
                val dao = AppDatabase.getDatabase(requireContext()).biblioScanDao()
                val favorites = dao.getFavoritesForUser(username)
                favorites.forEach {
                    val title = it.title ?: return@forEach
                    dao.removeFavorite(username, title)
                }
                loadFavorites()
                Toast.makeText(requireContext(), "Favoris vidés", Toast.LENGTH_SHORT).show()
            }
        }


        setupFilterSpinner()
        setupSortSpinner()
        loadFavorites()
    }

    private fun setupFilterSpinner() {
        val options = listOf("Tous", "Favoris", "Non favoris")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.filterSpinner.adapter = spinnerAdapter

        binding.filterSpinner.setSelection(0)
        binding.filterSpinner.setOnItemSelectedListener { _, _, position, _ ->
            currentFilter = options[position]
            filterBooks()
        }
    }

    private fun setupSortSpinner() {
        val options = listOf("Plus récent", "Titre A-Z", "Titre Z-A")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.sortSpinner.adapter = spinnerAdapter

        binding.sortSpinner.setSelection(0)
        binding.sortSpinner.setOnItemSelectedListener { _, _, position, _ ->
            currentSort = options[position]
            filterBooks()
        }
    }

    private fun loadFavorites() {
        val dao = AppDatabase.getDatabase(requireContext()).biblioScanDao()

        viewLifecycleOwner.lifecycleScope.launch {
            val username = sessionManager.getUsername().first() ?: return@launch
            val favorites = dao.getFavoritesForUser(username)
            allBooks = favorites.map { it.toBook() }
            filterBooks()
        }
    }

    private fun filterBooks() {
        var filtered = allBooks

        // Appliquer le filtre
        filtered = when (currentFilter) {
            "Favoris" -> filtered.filter { it.isFavorite }
            "Non favoris" -> filtered.filter { !it.isFavorite }
            else -> filtered
        }

        // Appliquer le tri
        filtered = when (currentSort) {
            "Titre A-Z" -> filtered.sortedBy { it.title }
            "Titre Z-A" -> filtered.sortedByDescending { it.title }
            "Plus récent" -> filtered // on considère que la liste l'est déjà
            else -> filtered
        }

        adapter.submitList(filtered)
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
