package com.example.biblioscan.backend

import com.example.biblioscan.Book
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

object HttpClientProvider {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }
}
suspend fun searchBooksFromTitles(titles: List<String>): List<Book> = withContext(Dispatchers.IO) {
    val client = HttpClientProvider.client

    val results = coroutineScope {
        titles.mapNotNull { rawTitle ->
            async {
                val title = normalizeTitleForSearch(rawTitle)
                if (title.isEmpty()) return@async null

                try {
                    val query = "intitle:\"$title\""

                    val response: JsonObject = client.get("https://www.googleapis.com/books/v1/volumes") {
                        parameter("q", query)
                        parameter("maxResults", 5)
                        accept(ContentType.Application.Json)
                    }.body()

                    val items = response["items"]?.jsonArray ?: return@async null

                    val bestItem = items.map { it.jsonObject }
                        .minByOrNull {
                            val googleTitle = it["volumeInfo"]?.jsonObject?.get("title")?.jsonPrimitive?.content ?: ""
                            levenshteinDistance(normalizeTitleForSearch(googleTitle), title)
                        } ?: return@async null

                    val volumeInfo = bestItem["volumeInfo"]?.jsonObject ?: return@async null
                    val imageLinks = volumeInfo["imageLinks"]?.jsonObject
                    val rawUrl = imageLinks?.get("large")?.jsonPrimitive?.content
                        ?: imageLinks?.get("medium")?.jsonPrimitive?.content
                        ?: imageLinks?.get("thumbnail")?.jsonPrimitive?.content
                    val secureImageUrl = rawUrl?.replace("http://", "https://")

                    val categories = volumeInfo["categories"]?.jsonArray?.map { it.jsonPrimitive.content }

                    val isbn13 = volumeInfo["industryIdentifiers"]?.jsonArray
                        ?.firstOrNull {
                            it.jsonObject["type"]?.jsonPrimitive?.content == "ISBN_13"
                        }?.jsonObject?.get("identifier")?.jsonPrimitive?.content

                    val averageRating = volumeInfo["averageRating"]?.jsonPrimitive?.doubleOrNull
                    val ratingsCount = volumeInfo["ratingsCount"]?.jsonPrimitive?.intOrNull

                    Book(
                        title = volumeInfo["title"]?.jsonPrimitive?.content ?: "Sans titre",
                        author = volumeInfo["authors"]?.jsonArray
                            ?.joinToString(", ") { it.jsonPrimitive.content } ?: "Auteur inconnu",
                        description = volumeInfo["description"]?.jsonPrimitive?.content ?: "Pas de description",
                        imageUrl = secureImageUrl,
                        publisher = volumeInfo["publisher"]?.jsonPrimitive?.content,
                        publishedDate = volumeInfo["publishedDate"]?.jsonPrimitive?.content,
                        pageCount = volumeInfo["pageCount"]?.jsonPrimitive?.intOrNull,
                        categories = categories,
                        language = volumeInfo["language"]?.jsonPrimitive?.content,
                        isbn13 = isbn13,
                        averageRating = averageRating,
                        ratingsCount = ratingsCount
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    return@withContext results
}


fun normalizeTitleForSearch(title: String): String {
    return title.lowercase()
        .replace(Regex("[^a-z0-9 ]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun levenshteinDistance(lhs: String, rhs: String): Int {
    val lhsLen = lhs.length
    val rhsLen = rhs.length

    val dp = Array(lhsLen + 1) { IntArray(rhsLen + 1) }

    for (i in 0..lhsLen) dp[i][0] = i
    for (j in 0..rhsLen) dp[0][j] = j

    for (i in 1..lhsLen) {
        for (j in 1..rhsLen) {
            val cost = if (lhs[i - 1] == rhs[j - 1]) 0 else 1
            dp[i][j] = minOf(
                dp[i - 1][j] + 1,
                dp[i][j - 1] + 1,
                dp[i - 1][j - 1] + cost
            )
        }
    }

    return dp[lhsLen][rhsLen]
}
