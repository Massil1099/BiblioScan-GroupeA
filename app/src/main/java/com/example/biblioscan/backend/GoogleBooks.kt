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
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

suspend fun searchBooksFromTitles(titles: List<String>): List<Book> = withContext(Dispatchers.IO) {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    val results = mutableListOf<Book>()

    for (title in titles) {
        try {
            val response: JsonObject = client.get("https://www.googleapis.com/books/v1/volumes") {
                parameter("q", title)
                parameter("maxResults", 1)
                accept(ContentType.Application.Json)
            }.body()

            val items = response["items"]?.jsonArray ?: continue
            val item = items.first().jsonObject
            val volumeInfo = item["volumeInfo"]?.jsonObject ?: continue

            val imageLinks = volumeInfo["imageLinks"]?.jsonObject
            val rawUrl = when {
                imageLinks?.get("large") != null -> imageLinks["large"]!!.jsonPrimitive.content
                imageLinks?.get("medium") != null -> imageLinks["medium"]!!.jsonPrimitive.content
                imageLinks?.get("thumbnail") != null -> imageLinks["thumbnail"]!!.jsonPrimitive.content
                else -> null
            }
            val secureImageUrl = rawUrl?.replace("http://", "https://")

            val book = Book(
                title = volumeInfo["title"]?.jsonPrimitive?.content ?: "Sans titre",
                author = volumeInfo["authors"]?.jsonArray
                    ?.joinToString(", ") { it.jsonPrimitive.content } ?: "Auteur inconnu",
                description = volumeInfo["description"]?.jsonPrimitive?.content ?: "Pas de description",
                imageUrl = secureImageUrl
            )

            results.add(book)
        } catch (e: Exception) {
            e.printStackTrace()
            // Ignorer et continuer avec les titres suivants
        }
    }

    client.close()
    return@withContext results
}
