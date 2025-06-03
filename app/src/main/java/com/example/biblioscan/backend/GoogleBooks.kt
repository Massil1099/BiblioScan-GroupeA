package com.example.biblioscan.backend

import android.util.Log
import com.example.biblioscan.Book
import com.example.biblioscan.personsAPI.AuthorsResponse
import com.example.biblioscan.personsAPI.NetworkClient
import com.example.biblioscan.sem_matching_API.MatchResponse
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
            Log.d("OCR_ANALYSIS", "1. Texte OCR brut : \"$text\"")

            // 1. Appel NER pour détecter les auteurs
            val authorsResponse = client.post("$serverUrl/detect_authors/") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("text" to text))
            }.body<AuthorsResponse>()

            val authors = authorsResponse.entities.map { it.word }
            val authorDetected = authors.firstOrNull() ?: "aucun"
            Log.d("OCR_ANALYSIS", "2. Auteur détecté : $authorDetected")

            // Nettoyage : supprimer auteur du texte brut
            val cleanedTitle = text.replace(authorDetected, "", ignoreCase = true).trim()
            Log.d("OCR_ANALYSIS", "3. Texte sans auteur : \"$cleanedTitle\"")

            // 2. Appel au serveur FastAPI pour titre similaire
            val matchResponse = client.post("$serverUrl/match_title/") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("text" to text))
            }.body<MatchResponse>()

            val bestMatchTitle = matchResponse.matches.firstOrNull()?.title ?: ""
            Log.d("OCR_ANALYSIS", "4. Titre le plus similaire : \"$bestMatchTitle\"")

            // 3. Création de la requête Google Books
            val queryTitle = if (bestMatchTitle.isNotEmpty()) bestMatchTitle else cleanedTitle
            val googleQuery = (if (authorDetected != "aucun") "inauthor:$authorDetected " else "") + queryTitle
            Log.d("OCR_ANALYSIS", "5. Requête Google Books : \"$googleQuery\"")

            val response: JsonObject = client.get("https://www.googleapis.com/books/v1/volumes") {
                parameter("q", googleQuery)
                parameter("maxResults", 1)
                accept(ContentType.Application.Json)
            }.body()

            val items = response["items"]?.jsonArray ?: continue
            val item = items.first().jsonObject
            val volumeInfo = item["volumeInfo"]?.jsonObject ?: continue

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

            Log.d("OCR_ANALYSIS", "6. Livre détecté : ${book.title} par ${book.author}")

            results.add(book)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("OCR_ANALYSIS", "Erreur pendant la recherche : ${e.message}")
        }
    }

    client.close()
    return@withContext results
}


