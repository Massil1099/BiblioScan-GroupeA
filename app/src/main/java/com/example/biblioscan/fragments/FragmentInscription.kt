package com.example.biblioscan.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.biblioscan.data_app.AppDatabase
import com.example.biblioscan.data_app.BiblioScanDao
import com.example.biblioscan.data_app.UserEntity
import com.example.biblioscan.databinding.FragmentInscriptionBinding
import kotlinx.coroutines.launch

class FragmentInscription : Fragment() {

    private var _binding: FragmentInscriptionBinding? = null
    private val binding get() = _binding!!

    private lateinit var dao: BiblioScanDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInscriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Récupération du DAO
        dao = AppDatabase.getDatabase(requireContext()).biblioScanDao()

        binding.registerButton.setOnClickListener {
            val username = binding.usernameInput.text.toString()
            val password = binding.passwordInput.text.toString()

            if (username.isBlank() || password.isBlank()) {
                Toast.makeText(requireContext(), "Tous les champs sont obligatoires", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Lancement de la coroutine Room
            viewLifecycleOwner.lifecycleScope.launch {
                val existingUser = dao.getUserByUsername(username)
                if (existingUser != null) {
                    Toast.makeText(requireContext(), "Ce nom d'utilisateur existe déjà", Toast.LENGTH_SHORT).show()
                } else {
                    val user = UserEntity(username = username, password = password)
                    dao.insertUser(user)
                    Log.d("Inscription", "Utilisateur inséré : $user")

                    // On recupere tous les utilisateurs
                    val allUsers = dao.getAllUsers()
                    Log.d("Inscription", "Tous les utilisateurs en base : $allUsers")

                    Toast.makeText(requireContext(), "Inscription réussie !", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }

        }

        binding.goBackButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
