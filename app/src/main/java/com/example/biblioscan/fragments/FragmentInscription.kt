package com.example.biblioscan.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.biblioscan.databinding.FragmentInscriptionBinding

class FragmentInscription : Fragment() {

    private var _binding: FragmentInscriptionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInscriptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.registerButton.setOnClickListener {
            val username = binding.usernameInput.text.toString()
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()

            if (username.isBlank() || email.isBlank() || password.isBlank()) {
                Toast.makeText(requireContext(), "Tous les champs sont obligatoires", Toast.LENGTH_SHORT).show()
            } else {
                // Simulation d'une inscription réussie
                Toast.makeText(requireContext(), "Inscription réussie !", Toast.LENGTH_SHORT).show()

                // Retour à l'écran de connexion
                findNavController().popBackStack()
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
