package com.example.biblioscan.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.biblioscan.R
import com.example.biblioscan.databinding.FragmentAccueilBinding
import com.example.biblioscan.session.UserSessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FragmentAccueil : Fragment() {

    private var _binding: FragmentAccueilBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: UserSessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccueilBinding.inflate(inflater, container, false)
        sessionManager = UserSessionManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Accès au nom d'utilisateur via Flow
        viewLifecycleOwner.lifecycleScope.launch {
            val username = sessionManager.getUsername().first()  // récupère la valeur du Flow
            val isGuest = username == "guest"

            if (isGuest) {
                binding.historyButton.visibility = View.GONE
                binding.favoritesButton.visibility = View.GONE
            }

            // Navigation
            binding.cameraButton.setOnClickListener {
                findNavController().navigate(R.id.action_accueil_to_camera)
            }
            binding.historyButton.setOnClickListener {
                findNavController().navigate(R.id.action_accueil_to_historique)
            }
            binding.favoritesButton.setOnClickListener {
                findNavController().navigate(R.id.action_accueil_to_favoris)
            }
            binding.authButton.setOnClickListener {
                lifecycleScope.launch {
                    sessionManager.clearUsername()
                    findNavController().navigate(R.id.action_accueil_to_connexion)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
