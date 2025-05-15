// Book.kt
package com.example.biblioscan

data class Book(
    val title: String,
    val author: String,
    val description: String,
    val imageUrl: String? = null
)
