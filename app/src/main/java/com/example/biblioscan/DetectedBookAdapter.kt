import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.biblioscan.Book
import com.example.biblioscan.R
import com.example.biblioscan.databinding.ItemBookBinding
import com.example.biblioscan.multipleEditions.BookDiffCallback

class DetectedBookAdapter(
    private val onBookClick: (Book) -> Unit,
    private val onChooseEditionClick: (List<Book>) -> Unit,
    editionsMap: Map<String, List<Book>> = emptyMap()
) : ListAdapter<Book, DetectedBookAdapter.BookViewHolder>(BookDiffCallback()) {

    var editionsMap: Map<String, List<Book>> = editionsMap
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class BookViewHolder(private val binding: ItemBookBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            binding.bookTitle.text = book.title
            binding.bookAuthor.text = book.author ?: "Auteur inconnu"
            binding.root.contentDescription = "${book.title} par ${book.author ?: "inconnu"}"

            // Affichage d'image si disponible
            if (!book.imageUrl.isNullOrEmpty()) {
                Glide.with(binding.bookImage.context)
                    .load(book.imageUrl)
                    .placeholder(R.drawable.result_placeholder)
                    .into(binding.bookImage)
            } else {
                binding.bookImage.setImageResource(R.drawable.result_placeholder)
            }

            // Récupération des éditions avec clé normalisée
            val editions = editionsMap[book.editionKey()] ?: listOf(book)


            if (editions.size > 1) {
                binding.buttonChooseEdition.visibility = View.VISIBLE
                binding.buttonChooseEdition.setOnClickListener {
                    onChooseEditionClick(editions)
                }
            } else {
                binding.buttonChooseEdition.visibility = View.GONE
            }

            // Action de clic sur tout le livre
            binding.root.setOnClickListener {
                onBookClick(book)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}



