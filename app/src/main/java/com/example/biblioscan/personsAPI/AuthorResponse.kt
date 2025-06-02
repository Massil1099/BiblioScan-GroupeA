package com.example.biblioscan.personsAPI

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

object NetworkClient {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }
}

@Serializable
data class Entity(
    val entity: String,
    val word: String,
    val start: Int,
    val end: Int,
    val score: Float
)

@Serializable
data class AuthorsResponse(
    val entities: List<Entity>
)

suspend fun detectAuthors(text: String, serverUrl: String): AuthorsResponse {
    val response = NetworkClient.client.post("$serverUrl/detect_authors/") {
        contentType(ContentType.Application.Json)
        setBody(mapOf("text" to text))
    }
    return response.body()
}
