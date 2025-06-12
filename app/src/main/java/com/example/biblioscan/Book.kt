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
    val isFavorite: Boolean = false,
    val ocrStatus: String = "valid" // "valid", "ignored", "no_text"
) : Parcelable
