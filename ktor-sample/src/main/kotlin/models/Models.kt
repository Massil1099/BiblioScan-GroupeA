package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class Livre(
    val id: String,
    val titre: String,
    val auteur: String
)

@Serializable
data class ScanRequest(
    val livres_detectes: List<String>
)

@Serializable
data class ScanResponse(
    val livres_trouves: List<Livre>
)
