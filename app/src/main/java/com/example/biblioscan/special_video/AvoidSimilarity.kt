package com.example.biblioscan.special_video

import android.util.Log
import com.example.biblioscan.Book
import com.example.biblioscan.backend.searchBooksFromTitles

fun areTextsSimilar(text1: String, text2: String): Boolean {
    val clean1 = text1.lowercase().filter { it.isLetterOrDigit() || it.isWhitespace() }
    val clean2 = text2.lowercase().filter { it.isLetterOrDigit() || it.isWhitespace() }

    val words1 = clean1.split(" ").filter { it.length > 2 }
    val words2 = clean2.split(" ").filter { it.length > 2 }

    val intersection = words1.intersect(words2.toSet())
    val similarity = intersection.size.toDouble() / maxOf(words1.size, words2.size)

    return similarity >= 0.7 // seuil ajustable
}

suspend fun searchBooksAvoidingDuplicates(
    titles: List<String>,
    alreadyProcessed: MutableMap<String, Book>
): List<Book> {
    val results = mutableListOf<Book>()

    for (title in titles) {
        Log.d("AvoidSimilarity", "🔍 Analyse de: \"$title\"")

        // Vérifier similarité avec les titres déjà traités
        val similarKey = alreadyProcessed.keys.find { areTextsSimilar(it, title) }

        if (similarKey != null) {
            Log.d("AvoidSimilarity", "⚠️ \"$title\" est similaire à \"$similarKey\", on évite la requête.")
            val book = alreadyProcessed[similarKey]
            if (book != null) {
                results.add(book)
                Log.d("AvoidSimilarity", "✅ Livre récupéré depuis la mémoire: ${book.title}")
            }
            continue
        }

        // Sinon requête à Google Books
        try {
            Log.d("AvoidSimilarity", "🌐 Requête à Google Books pour: \"$title\"")
            val foundBooks = searchBooksFromTitles(listOf(title))
            val book = foundBooks.firstOrNull()

            if (book != null) {
                alreadyProcessed[title] = book
                results.add(book)
                Log.d("AvoidSimilarity", "📚 Livre ajouté: ${book.title}")
            } else {
                Log.d("AvoidSimilarity", "❌ Aucun livre trouvé pour: \"$title\"")
            }

        } catch (e: Exception) {
            Log.e("AvoidSimilarity", "💥 Erreur lors de la recherche du livre: \"$title\"", e)
        }
    }

    return results
}
