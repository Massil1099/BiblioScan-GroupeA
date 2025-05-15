// FragmentAccueil.kt
package com.example.biblioscan.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.biblioscan.R
import com.example.biblioscan.databinding.FragmentAccueilBinding

class FragmentAccueil : Fragment() {

    private var _binding: FragmentAccueilBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccueilBinding.inflate(inflater, container, false)

        // Bouton Historique
        binding.historyButton.setOnClickListener {
            findNavController().navigate(R.id.action_accueil_to_historique)
        }

        // Bouton Favoris
        binding.favoritesButton.setOnClickListener {
            findNavController().navigate(R.id.action_accueil_to_favoris)
        }

        // Bouton Caméra
        binding.cameraButton.setOnClickListener {
            findNavController().navigate(R.id.action_accueil_to_camera)
        }

        // Bouton Déconnexion
        binding.authButton.setOnClickListener {
            findNavController().navigate(R.id.action_accueil_to_connexion)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
