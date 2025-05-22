package com.example.biblioscan.data_app

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Représente un livre détecté ou favori.
 */
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val title: String, // Peut être remplacé par un id unique si besoin
    val author: String,
    val description: String,
    val imageUrl: String? = null
)

/**
 * Représente un utilisateur de l'application.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val password: String // ⚠️ Pour une vraie app, ne jamais stocker de mot de passe en clair
)

/**
 * Représente un favori associé à un utilisateur.
 */
@Entity(tableName = "favorites", primaryKeys = ["username", "bookTitle"])
data class FavoriteEntity(
    val username: String,
    val bookTitle: String
)

/**
 * Représente un livre vu ou détecté précédemment par un utilisateur.
 */
@Entity(tableName = "history", primaryKeys = ["username", "bookTitle"])
data class HistoryEntity(
    val username: String,
    val bookTitle: String,
    val timestamp: Long = System.currentTimeMillis()
)
