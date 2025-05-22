package com.example.biblioscan.data_app

import com.example.biblioscan.Book

// Entity -> Book (pour affichage)
fun BookEntity.toBook(): Book {
    return Book(
        title = this.title,
        author = this.author,
        description = this.description,
        imageUrl = this.imageUrl
    )
}

// Book -> Entity (pour sauvegarde)
fun Book.toEntity(): BookEntity {
    return BookEntity(
        title = this.title,
        author = this.author,
        description = this.description,
        imageUrl = this.imageUrl
    )
}
