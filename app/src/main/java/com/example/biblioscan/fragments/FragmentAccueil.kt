package com.example.biblioscan.fragments

import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.biblioscan.R
import com.example.biblioscan.databinding.FragmentAccueilBinding
import com.example.biblioscan.session.UserSessionManager
import com.example.biblioscan.shared.TutorialManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt

class FragmentAccueil : Fragment() {

    private var _binding: FragmentAccueilBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: UserSessionManager
    private lateinit var tutorialManager: TutorialManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccueilBinding.inflate(inflater, container, false)
        sessionManager = UserSessionManager(requireContext())
        tutorialManager = TutorialManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            val username = sessionManager.getUsername().first() ?: "guest"
            val isGuest = username == "guest"

            if (isGuest) {
                binding.historyButton.visibility = View.GONE
                binding.favoritesButton.visibility = View.GONE
            }

            setupNavigation()

            // Toujours afficher le tutoriel à chaque fois
            binding.root.postDelayed({
                showTutorial(username)
            }, 500)
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

    private fun showTutorial(username: String) {
        val steps = mutableListOf<Pair<View, Pair<String, String>>>()

        steps.add(binding.cameraButton to ("Scanner un livre" to "Appuyez ici pour lancer la détection de livres."))

        if (username != "guest") {
            steps.add(binding.favoritesButton to ("Favoris" to "Retrouvez ici vos livres enregistrés."))
            steps.add(binding.historyButton to ("Historique" to "Consultez l'historique de vos lectures."))
        }

        steps.add(binding.settingsButton to ("Paramètres" to "Accédez aux paramètres de l'application."))
        steps.add(
            binding.authButton to (
                    (if (username == "guest") "Connexion" else "Déconnexion") to
                            (if (username == "guest") "Connectez-vous à votre compte ici." else "Déconnectez-vous de l'application.")
                    )
        )

        showTutorialSteps(steps, 0)
    }

    private fun showTutorialSteps(steps: List<Pair<View, Pair<String, String>>>, index: Int) {
        if (index >= steps.size) {
            // Ne plus enregistrer comme "vu" pour forcer l'affichage à chaque fois
            return
        }

        val (view, texts) = steps[index]
        MaterialTapTargetPrompt.Builder(requireActivity())
            .setTarget(view)
            .setPrimaryText(texts.first)
            .setSecondaryText(texts.second)
            .setBackgroundColour(ContextCompat.getColor(requireContext(), R.color.teal_700))
            .setPromptStateChangeListener { _, state ->
                if (state == MaterialTapTargetPrompt.STATE_DISMISSED || state == MaterialTapTargetPrompt.STATE_FINISHED) {
                    showTutorialSteps(steps, index + 1)
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
