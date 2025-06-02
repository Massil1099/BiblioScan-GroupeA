package com.example.biblioscan.multipleEditions

import androidx.recyclerview.widget.DiffUtil
import com.example.biblioscan.Book

class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
    override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
        // Compare un identifiant unique, par exemple l’ISBN ou le titre
        return oldItem.isbn13 == newItem.isbn13
    }

    override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
        // Compare le contenu complet
        return oldItem == newItem
    }
}
