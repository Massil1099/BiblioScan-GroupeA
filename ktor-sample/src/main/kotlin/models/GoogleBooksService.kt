package com.example.models

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

suspend fun searchBookByTitle(title: String): List<Livre> {
    val response: JsonObject = client.get("https://www.googleapis.com/books/v1/volumes") {
        parameter("q", title)
        parameter("maxResults", 5)
    }.body()

    val items = response["items"]?.jsonArray ?: return emptyList()

    return items.mapNotNull { item ->
        val volumeInfo = item.jsonObject["volumeInfo"]?.jsonObject
        val id = item.jsonObject["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
        val titre = volumeInfo?.get("title")?.jsonPrimitive?.content ?: return@mapNotNull null
        val auteurs = volumeInfo["authors"]?.jsonArray
            ?.joinToString(", ") { it.jsonPrimitive.content }
            ?: "Inconnu"

        Livre(id, titre, auteurs)
    }
}
