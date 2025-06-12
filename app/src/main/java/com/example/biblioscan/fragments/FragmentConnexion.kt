package com.example.biblioscan.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.biblioscan.R
import com.example.biblioscan.data_app.AppDatabase
import com.example.biblioscan.data_app.BiblioScanDao
import com.example.biblioscan.databinding.FragmentConnexionBinding
import com.example.biblioscan.session.UserSessionManager
import kotlinx.coroutines.launch

class FragmentConnexion : Fragment() {

    private var _binding: FragmentConnexionBinding? = null
    private val binding get() = _binding!!

    private lateinit var dao: BiblioScanDao
    private lateinit var sessionManager: UserSessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnexionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialisation de la base de données et session
        dao = AppDatabase.getDatabase(requireContext()).biblioScanDao()
        sessionManager = UserSessionManager(requireContext())

        // Connexion utilisateur
        binding.loginButton.setOnClickListener {
            val username = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val user = dao.getUserByUsername(username)
                if (user != null && user.password == password) {
                    sessionManager.saveUsername(username)
                    Toast.makeText(requireContext(), "Connexion réussie", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_connexion_to_accueil)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Nom d'utilisateur ou mot de passe incorrect",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Connexion en tant qu'invité
        binding.guestButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                sessionManager.saveUsername("guest")
                findNavController().navigate(R.id.action_connexion_to_accueil)
            }
        }

        // Navigation vers l'inscription
        binding.registerButton.setOnClickListener {
            findNavController().navigate(R.id.action_connexion_to_inscription)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
