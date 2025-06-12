import android.util.Log
import com.example.biblioscan.Book
import com.example.biblioscan.backend.searchBooksFromTitles
import kotlin.math.max
import kotlin.math.min

fun areTextsSimilar(text1: String, text2: String): Boolean {
    // Nettoyage + normalisation
    val clean1 = text1.lowercase().replace("[^a-z0-9]".toRegex(), " ").replace("\\s+".toRegex(), " ").trim()
    val clean2 = text2.lowercase().replace("[^a-z0-9]".toRegex(), " ").replace("\\s+".toRegex(), " ").trim()

    if (clean1.isEmpty() || clean2.isEmpty()) return false

    val distance = levenshtein(clean1, clean2)
    val maxLen = max(clean1.length, clean2.length)

    val similarity = 1.0 - distance.toDouble() / maxLen
    return similarity >= 0.4 // seuil ajustable, faut trouver le bon compromis
}

// Distance de Levenshtein standard
fun levenshtein(s1: String, s2: String): Int {
    val len1 = s1.length
    val len2 = s2.length
    val dp = Array(len1 + 1) { IntArray(len2 + 1) }

    for (i in 0..len1) dp[i][0] = i
    for (j in 0..len2) dp[0][j] = j

    for (i in 1..len1) {
        for (j in 1..len2) {
            val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
            dp[i][j] = min(
                dp[i - 1][j] + 1,
                min(dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            )
        }
    }

    return dp[len1][len2]
}

fun filterSimilarTitles(titles: List<String>): List<String> {
    val filtered = mutableListOf<String>()

    for (title in titles) {
        val isDuplicate = filtered.any { areTextsSimilar(it, title) }
        if (!isDuplicate) {
            filtered.add(title)
        }
    }

    return filtered
}

suspend fun searchBooksAvoidingDuplicates(
    titles: List<String>,
    alreadyProcessed: MutableMap<String, Book>
): List<Book> {
    val results = mutableListOf<Book>()

    val filteredTitles = filterSimilarTitles(titles)
    Log.d("AvoidSimilarity", "🔎 Titres après filtrage : $filteredTitles")

    for (title in filteredTitles) {
        Log.d("AvoidSimilarity", "🔍 Analyse de: \"$title\"")

        val similarKey = alreadyProcessed.keys.find { areTextsSimilar(it, title) }

        if (similarKey != null) {
            Log.d("AvoidSimilarity", "⚠️ \"$title\" est similaire à \"$similarKey\", on évite la requête.")
            alreadyProcessed[similarKey]?.let { results.add(it) }
            continue
        }

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
