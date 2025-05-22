package com.example.biblioscan.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.biblioscan.databinding.FragmentConnexionBinding

class FragmentConnexion : Fragment() {

    private var _binding: FragmentConnexionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnexionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (email.isNotBlank() && password.isNotBlank()) {
                findNavController().navigate(com.example.biblioscan.R.id.action_connexion_to_accueil)
            } else {
                Toast.makeText(requireContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            }
        }

        binding.guestButton.setOnClickListener {
            findNavController().navigate(com.example.biblioscan.R.id.action_connexion_to_accueil)
        }

        binding.registerButton.setOnClickListener {
            findNavController().navigate(com.example.biblioscan.R.id.action_connexion_to_inscription)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
