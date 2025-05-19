package com.example

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.http.content.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.routing.routing
import java.io.File


fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(DefaultHeaders)
    install(ContentNegotiation) {
        json()
    }

    routing {
        // Cette partie est utile seulement si tu veux servir des fichiers statiques (ex: une page HTML locale)
        static("/") {
            staticRootFolder = File("src/main/resources/static")
            files(".") // Cela servira tous les fichiers du dossier static
            default("index.html")
        }


    }

    configureRouting()   // contient les vraies routes utiles pour l'application Android

}


