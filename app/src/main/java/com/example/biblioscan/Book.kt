package com.example.biblioscan

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Book(
    val title: String,
    val author: String,
    val description: String,
    val imageUrl: String? = null,
    val publisher: String?,
    val publishedDate: String?,
    val pageCount: Int?,
    val categories: List<String>?,
    val language: String?,
    val isbn13: String?,
    val averageRating: Double?,
    val ratingsCount: Int?,
) : Parcelable {
    val uniqueKey: String
        get() = isbn13?.takeIf { it.isNotBlank() } ?: title.trim().lowercase()


    val normalizedTitle: String
        get() = title
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "") // enlève espaces, ponctuations, etc.
            .replace(Regex("(with.*|version.*|edition.*|notes.*|study.*)"), "") // enlève les variantes
            .take(20) // limite à 20 caractères pour éviter trop d'écarts




    fun editionKey(): String {
        // Récupérer les deux premiers mots du titre, en minuscule, sans ponctuation
        val words = title
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "") // enlever la ponctuation sauf espaces
            .split("\\s+".toRegex())

        return if (words.size >= 2) {
            // Concaténer les deux premiers mots sans espace pour la clé
            words[0] + words[1]
        } else if (words.isNotEmpty()) {
            words[0]
        } else {
            ""
        }
    }



}




