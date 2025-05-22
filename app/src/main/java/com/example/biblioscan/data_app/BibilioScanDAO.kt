package com.example.biblioscan.data_app

import androidx.room.*

@Dao
interface BiblioScanDao {

    // ------------------------------
    // BOOKS
    // ------------------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Query("SELECT * FROM books WHERE title = :title")
    suspend fun getBookByTitle(title: String): BookEntity?

    @Query("SELECT * FROM books")
    suspend fun getAllBooks(): List<BookEntity>

    @Delete
    suspend fun deleteBook(book: BookEntity)


    // ------------------------------
    // USERS
    // ------------------------------
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE username = :username AND password = :password")
    suspend fun authenticate(username: String, password: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUser(username: String): UserEntity?


    // ------------------------------
    // FAVORITES
    // ------------------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("""
        SELECT b.* FROM books b 
        INNER JOIN favorites f ON b.title = f.bookTitle 
        WHERE f.username = :username
    """)
    suspend fun getFavoritesForUser(username: String): List<BookEntity>

    @Query("DELETE FROM favorites WHERE username = :username AND bookTitle = :bookTitle")
    suspend fun removeFavorite(username: String, bookTitle: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE username = :username AND bookTitle = :bookTitle)")
    suspend fun isFavorite(username: String, bookTitle: String): Boolean


    // ------------------------------
    // HISTORY
    // ------------------------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addHistory(history: HistoryEntity)

    @Query("""
        SELECT b.* FROM books b 
        INNER JOIN history h ON b.title = h.bookTitle 
        WHERE h.username = :username 
        ORDER BY h.timestamp DESC
    """)
    suspend fun getHistoryForUser(username: String): List<BookEntity>

    @Query("DELETE FROM history WHERE username = :username")
    suspend fun clearHistory(username: String)
}
