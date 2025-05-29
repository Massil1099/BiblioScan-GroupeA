package com.example.biblioscan.data_app

import com.example.biblioscan.Book

// Entity -> Book (pour affichage)
fun BookEntity.toBook(): Book {
    return Book(
        title = this.title,
        author = this.author,
        description = this.description,
        imageUrl = this.imageUrl,
        publisher = this.publisher,
        publishedDate = this.publishedDate,
        pageCount = this.pageCount,
        categories = this.categories?.split(",")?.map { it.trim() },
        language = this.language,
        isbn13 = this.isbn13,
        averageRating = this.averageRating,
        ratingsCount = this.ratingsCount
    )
}

// Book -> Entity (pour sauvegarde)
fun Book.toEntity(): BookEntity {
    return BookEntity(
        title = this.title,
        author = this.author,
        description = this.description,
        imageUrl = this.imageUrl,
        publisher = this.publisher,
        publishedDate = this.publishedDate,
        pageCount = this.pageCount,
        categories = this.categories?.joinToString(","),
        language = this.language,
        isbn13 = this.isbn13,
        averageRating = this.averageRating,
        ratingsCount = this.ratingsCount
    )
}
