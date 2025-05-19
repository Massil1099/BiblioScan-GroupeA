package com.example.biblioscan.backend


import kotlinx.serialization.Serializable

@Serializable
data class ScanRequest(
    val livres_detectes: List<String>
)

@Serializable
data class ScanResponse(
    val livres_trouves: List<Livre>
)

@Serializable
data class Livre(
    val id: String,
    val titre: String,
    val auteur: String
)
