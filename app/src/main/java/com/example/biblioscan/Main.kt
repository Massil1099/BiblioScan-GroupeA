package com.example.biblioscan

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*

class Main {
    fun runClient() = runBlocking {
        val client = HttpClient(CIO)
        try {
            val response: HttpResponse = client.get("http://localhost:8080/hello")
            val text = response.bodyAsText()
            println("Réponse du backend : $text")
        } catch (e: Exception) {
            println("Erreur lors de l'appel HTTP : ${e.message}")
        } finally {
            client.close()
        }
    }
}

// point d’entrée de l’application
fun main() {
    Main().runClient()
}
