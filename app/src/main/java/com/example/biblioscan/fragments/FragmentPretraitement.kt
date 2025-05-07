package com.example.biblioscan.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.biblioscan.R
import com.example.biblioscan.databinding.FragmentPretraitementBinding
import java.io.File

class FragmentPretraitement : Fragment() {

    private var _binding: FragmentPretraitementBinding? = null
    private val binding get() = _binding!!

    private var imagePath: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPretraitementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imagePath = arguments?.getString("imagePath")

        binding.processButton.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("imagePath", imagePath)
            findNavController().navigate(R.id.action_fragmentPretraitement_to_fragmentResultat, bundle)

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}