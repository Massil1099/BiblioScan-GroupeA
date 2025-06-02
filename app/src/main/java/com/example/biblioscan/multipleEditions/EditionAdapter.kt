package com.example.biblioscan.multipleEditions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.biblioscan.Book
import com.example.biblioscan.R

class EditionAdapter(
    private val editions: List<Book>,
    private val onClick: (Book) -> Unit
) : RecyclerView.Adapter<EditionAdapter.EditionViewHolder>() {

    inner class EditionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title = itemView.findViewById<TextView>(R.id.textTitle)
        val publisher = itemView.findViewById<TextView>(R.id.textPublisher)
        val year = itemView.findViewById<TextView>(R.id.textYear)

        fun bind(book: Book) {
            title.text = book.title
            publisher.text = "Éditeur : ${book.publisher ?: "Inconnu"}"
            year.text = "Date : ${book.publishedDate ?: "?"}"

            itemView.setOnClickListener {
                onClick(book)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EditionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_edition, parent, false)
        return EditionViewHolder(view)
    }

    override fun onBindViewHolder(holder: EditionViewHolder, position: Int) {
        holder.bind(editions[position])
    }

    override fun getItemCount(): Int = editions.size
}
