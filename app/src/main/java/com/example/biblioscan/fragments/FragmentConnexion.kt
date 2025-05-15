// FragmentConnexion.kt
package com.example.biblioscan.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.biblioscan.R

class FragmentConnexion : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_connexion, container, false)

        val emailEditText = view.findViewById<EditText>(R.id.email_edit_text)
        val passwordEditText = view.findViewById<EditText>(R.id.password_edit_text)
        val loginButton = view.findViewById<Button>(R.id.login_button)
        val guestButton = view.findViewById<Button>(R.id.guest_button)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Navigation vers l'accueil après connexion
                findNavController().navigate(R.id.action_connexion_to_accueil)
            } else {
                Toast.makeText(requireContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            }
        }

        guestButton.setOnClickListener {
            // Navigation en tant qu'invité
            findNavController().navigate(R.id.action_connexion_to_accueil)
        }

        return view
    }
}
