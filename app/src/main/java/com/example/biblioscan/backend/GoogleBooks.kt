package com.example.biblioscan.backend

import com.example.biblioscan.Book
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
}

suspend fun searchBooksFromTitles(titles: List<String>): List<Book> {
    val results = mutableListOf<Book>()

    for (title in titles) {
        try {
            val response: JsonObject = client.get("https://www.googleapis.com/books/v1/volumes") {
                parameter("q", title)
                parameter("maxResults", 1)
            }.body()

            val items = response["items"]?.jsonArray ?: continue

            val item = items.first().jsonObject
            val volumeInfo = item["volumeInfo"]?.jsonObject ?: continue

            // Extraire l'image de meilleure qualité disponible
            val imageLinks = volumeInfo["imageLinks"]?.jsonObject
            val rawUrl = when {
                imageLinks?.get("large") != null -> imageLinks["large"]!!.jsonPrimitive.content
                imageLinks?.get("medium") != null -> imageLinks["medium"]!!.jsonPrimitive.content
                imageLinks?.get("thumbnail") != null -> imageLinks["thumbnail"]!!.jsonPrimitive.content
                else -> null
            }

            val secureImageUrl = rawUrl?.replace("http://", "https://")

            //  Création de l'objet Book proprement
            val book = Book(
                title = volumeInfo["title"]?.jsonPrimitive?.content ?: "Sans titre",
                author = volumeInfo["authors"]?.jsonArray
                    ?.joinToString(", ") { it.jsonPrimitive.content } ?: "Auteur inconnu",
                description = volumeInfo["description"]?.jsonPrimitive?.content ?: "Pas de description",
                imageUrl = secureImageUrl
            )

            results.add(book)

        } catch (e: Exception) {
            // Ignorer les erreurs réseau ou parsing
        }
    }

    return results
}
