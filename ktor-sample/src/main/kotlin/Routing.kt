package com.example

import com.example.models.Livre
import com.example.models.ScanRequest
import com.example.models.ScanResponse
import io.ktor.http.ContentType
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val livresFakeDB = listOf(
        Livre("1", "L'Étranger", "Albert Camus"),
        Livre("2", "1984", "George Orwell"),
        Livre("3", "Le Petit Prince", "Antoine de Saint-Exupéry"),
        Livre("4", "Les Misérables", "Victor Hugo"),
        Livre("5", "La Peste", "Albert Camus")
    )

    routing {
        get("/livres") {
            call.respond(livresFakeDB)
        }

        get("/") {
            call.respondText("Bienvenue sur BiblioScan ! Le serveur fonctionne.", ContentType.Text.Plain)
        }

        post("/scan") {
            val request = call.receive<ScanRequest>()

            println("Livres détectés: ${request.livres_detectes}")

            // Normaliser les titres pour la comparaison (minuscules et suppression des accents)
            val titresDetectesNormalises = request.livres_detectes.map {
                it.lowercase().replace("'", " ").trim()
            }

            val livresTrouves = livresFakeDB.filter { livre ->
                val titreNormalise = livre.titre.lowercase().replace("'", " ").trim()
                titresDetectesNormalises.any { it == titreNormalise }
            }

            println("Livres trouvés: $livresTrouves")

            call.respond(ScanResponse(livresTrouves))
        }

    }
}

