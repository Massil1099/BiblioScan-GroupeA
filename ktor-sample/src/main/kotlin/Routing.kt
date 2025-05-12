package com.example

import com.example.models.Livre
import com.example.models.ScanRequest
import com.example.models.ScanResponse
import com.example.models.searchBookByTitle
import io.ktor.http.ContentType
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {

    routing {

        get("/hello") {
            call.respondText("Hello from backend!", ContentType.Text.Plain)
        }



        get("/") {
            call.respondText("Bienvenue sur BiblioScan ! Le serveur fonctionne.", ContentType.Text.Plain)
        }

        post("/scan") {
            val request = call.receive<ScanRequest>()
            val titres = request.livres_detectes

            val resultats = mutableListOf<Livre>()
            for (titre in titres) {
                val livresTrouves = searchBookByTitle(titre)
                resultats.addAll(livresTrouves)
            }

            call.respond(ScanResponse(resultats))
        }

    }


}

