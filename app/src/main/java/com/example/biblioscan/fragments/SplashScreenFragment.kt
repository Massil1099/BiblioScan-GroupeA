package com.example.biblioscan.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.biblioscan.R
import com.example.biblioscan.databinding.FragmentSplashScreenBinding
import com.example.biblioscan.session.UserSessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class FragmentSplashScreen : Fragment() {

    private var _binding: FragmentSplashScreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Attendre 3 secondes avant la navigation
        Handler(Looper.getMainLooper()).postDelayed({
            val sessionManager = UserSessionManager(requireContext())
            val username = runBlocking { sessionManager.getUsername().first() }

            if (username != null && username != "guest") {
                findNavController().navigate(R.id.action_splash_to_accueil)
            } else {
                findNavController().navigate(R.id.action_splash_to_connexion)
            }
        }, 3000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
