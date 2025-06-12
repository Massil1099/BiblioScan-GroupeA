package com.example.biblioscan.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.biblioscan.databinding.FragmentParametresBinding
import com.example.biblioscan.shared.ThemePreferenceManager
import com.example.biblioscan.shared.TutorialManager

class FragmentParametres : Fragment() {

    private var _binding: FragmentParametresBinding? = null
    private val binding get() = _binding!!
    private lateinit var tutorialManager: TutorialManager
    private lateinit var themeManager: ThemePreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParametresBinding.inflate(inflater, container, false)
        tutorialManager = TutorialManager(requireContext())
        themeManager = ThemePreferenceManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialiser l'état du switch avec les préférences
        binding.darkModeSwitch.isChecked = themeManager.isDarkMode()

        // Gérer le changement de thème
        binding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            themeManager.setDarkMode(isChecked)
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            // Nécessaire pour appliquer immédiatement
            requireActivity().recreate()
        }

        // Bouton retour
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        // Bouton pour relancer le tutoriel
        binding.restartTutorialButton.setOnClickListener {
            tutorialManager.resetTutorial()
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
