package com.example.biblioscan.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.biblioscan.Book
import com.example.biblioscan.R
import com.example.biblioscan.databinding.FragmentResultatBinding

class FragmentResultat : Fragment() {

    companion object {
        private const val ARG_BOOK = "book"

        fun newInstance(book: Book): FragmentResultat {
            val fragment = FragmentResultat()
            val args = Bundle()
            args.putParcelable(ARG_BOOK, book)
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: FragmentResultatBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val book = arguments?.getParcelable<Book>(ARG_BOOK) ?: return
        if (book != null) {
            binding.bookTitle.text = book.title
            binding.bookAuthor.text = book.author
            binding.bookDescription.text = book.description

            if (!book.imageUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(book.imageUrl)
                    .placeholder(R.drawable.placeholder_book)
                    .error(R.drawable.placeholder_book)
                    .into(binding.bookImage)
            } else {
                binding.bookImage.setImageResource(R.drawable.placeholder_book)
            }

            binding.favoritesButton.setOnClickListener {
                // TODO : Enregistrer en favori ou afficher un Toast
                // Toast.makeText(requireContext(), "Ajouté aux favoris", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
