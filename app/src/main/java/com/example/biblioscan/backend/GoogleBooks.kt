package com.example.biblioscan.backend

import android.util.Log
import com.example.biblioscan.Book
import com.example.biblioscan.personsAPI.AuthorsResponse
import com.example.biblioscan.personsAPI.NetworkClient
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

suspend fun searchBooksByAuthorAndTitle(
    textsFromOCR: List<String>,
    serverUrl: String
): List<Book> = withContext(Dispatchers.IO) {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) { json() }
    }

    val results = mutableListOf<Book>()

    for (text in textsFromOCR) {
        try {
            // Log texte brut extrait par OCR
            Log.d("OCR_ANALYSIS", "Texte OCR brut : \"$text\"")

            // 1. Appel NER
            val authorsResponse = NetworkClient.client.post("$serverUrl/detect_authors/") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("text" to text))
            }.body<AuthorsResponse>()

            val authors = authorsResponse.entities.map { it.word }
            val authorDetected = authors.firstOrNull() ?: "aucun"

            // Supprimer l'auteur pour avoir un titre approximatif
            val titleQuery = text.replace(authorDetected, "").trim()

            // Log nom d'auteur détecté et texte restant
            Log.d("OCR_ANALYSIS", "Auteur détecté : \"$authorDetected\" | Texte sans auteur : \"$titleQuery\"")

            // Requête Google Books
            val googleQuery = (if (authorDetected != "aucun") "inauthor:$authorDetected " else "") + titleQuery

            val response: JsonObject = client.get("https://www.googleapis.com/books/v1/volumes") {
                parameter("q", googleQuery)
                parameter("maxResults", 1)
                accept(ContentType.Application.Json)
            }.body()

            val items = response["items"]?.jsonArray ?: continue
            val item = items.first().jsonObject
            val volumeInfo = item["volumeInfo"]?.jsonObject ?: continue

            // Extraction infos livre...
            val imageLinks = volumeInfo["imageLinks"]?.jsonObject
            val rawUrl = imageLinks?.get("large")?.jsonPrimitive?.content
                ?: imageLinks?.get("medium")?.jsonPrimitive?.content
                ?: imageLinks?.get("thumbnail")?.jsonPrimitive?.content
            val secureImageUrl = rawUrl?.replace("http://", "https://")
            val categories = volumeInfo["categories"]?.jsonArray?.map { it.jsonPrimitive.content }
            val isbn13 = volumeInfo["industryIdentifiers"]?.jsonArray
                ?.firstOrNull { it.jsonObject["type"]?.jsonPrimitive?.content == "ISBN_13" }
                ?.jsonObject?.get("identifier")?.jsonPrimitive?.content
            val averageRating = volumeInfo["averageRating"]?.jsonPrimitive?.doubleOrNull
            val ratingsCount = volumeInfo["ratingsCount"]?.jsonPrimitive?.intOrNull

            val book = Book(
                title = volumeInfo["title"]?.jsonPrimitive?.content ?: "Sans titre",
                author = volumeInfo["authors"]?.jsonArray?.joinToString(", ") { it.jsonPrimitive.content }
                    ?: "Auteur inconnu",
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

            results.add(book)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    client.close()
    return@withContext results
}
