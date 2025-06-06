package com.example.biblioscan.fragments

import android.os.Bundle
import android.view.*
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

        lifecycleScope.launch {
            val username = sessionManager.getUsername().first()
            val isGuest = username == "guest"

            // Cacher favoris/historique si invité
            if (isGuest) {
                binding.historyButton.visibility = View.GONE
                binding.favoritesButton.visibility = View.GONE
            }

            setupNavigation()
        }
    }

    private fun setupNavigation() {
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

        binding.settingsButton.setOnClickListener {
            findNavController().navigate(R.id.action_accueil_to_parametres)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
