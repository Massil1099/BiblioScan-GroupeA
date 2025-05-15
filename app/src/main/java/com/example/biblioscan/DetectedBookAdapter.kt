// DetectedBookAdapter.kt
package com.example.biblioscan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class DetectedBookAdapter(
    private val onBookClick: (Book) -> Unit
) : ListAdapter<Book, DetectedBookAdapter.BookViewHolder>(BookDiffCallback()) {

    class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bookImage: ImageView = itemView.findViewById(R.id.book_image)
        private val bookTitle: TextView = itemView.findViewById(R.id.book_title)
        private val bookAuthor: TextView = itemView.findViewById(R.id.book_author)

        fun bind(book: Book, onBookClick: (Book) -> Unit) {
            bookTitle.text = book.title
            bookAuthor.text = book.author

            // Description pour les lecteurs d'écran
            itemView.contentDescription = "${book.title} par ${book.author}"

            // Placeholder pour l'image du livre
            bookImage.setImageResource(R.drawable.result_placeholder)

            // Gestion du clic
            itemView.setOnClickListener { onBookClick(book) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position), onBookClick)
    }
}

class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
    override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
        return oldItem.title == newItem.title
    }

    override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
        return oldItem == newItem
    }
}
