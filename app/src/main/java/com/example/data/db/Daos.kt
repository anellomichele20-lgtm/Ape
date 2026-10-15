package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity): Long

    @Update
    suspend fun updateSession(session: ChatSessionEntity)

    @Query("UPDATE chat_sessions SET updatedAt = :time, title = :title WHERE id = :sessionId")
    suspend fun updateSessionTitleAndTime(sessionId: Long, title: String, time: Long)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesListForSession(sessionId: Long): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun clearMessagesForSession(sessionId: Long)
}

@Dao
interface ImageDao {
    @Query("SELECT * FROM generated_images ORDER BY createdAt DESC")
    fun getAllImages(): Flow<List<GeneratedImageEntity>>

    @Query("SELECT * FROM generated_images WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteImages(): Flow<List<GeneratedImageEntity>>

    @Query("SELECT * FROM generated_images WHERE id = :id LIMIT 1")
    suspend fun getImageById(id: Long): GeneratedImageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: GeneratedImageEntity): Long

    @Query("UPDATE generated_images SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM generated_images WHERE id = :id")
    suspend fun deleteImage(id: Long)
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcard_decks ORDER BY createdAt DESC")
    fun getAllDecks(): Flow<List<FlashcardDeckEntity>>

    @Query("SELECT * FROM flashcard_decks WHERE id = :deckId LIMIT 1")
    suspend fun getDeckById(deckId: Long): FlashcardDeckEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: FlashcardDeckEntity): Long

    @Query("DELETE FROM flashcard_decks WHERE id = :deckId")
    suspend fun deleteDeck(deckId: Long)

    @Query("SELECT * FROM flashcard_items WHERE deckId = :deckId ORDER BY id ASC")
    fun getCardsForDeck(deckId: Long): Flow<List<FlashcardItemEntity>>

    @Query("SELECT * FROM flashcard_items WHERE deckId = :deckId ORDER BY id ASC")
    suspend fun getCardsListForDeck(deckId: Long): List<FlashcardItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<FlashcardItemEntity>)

    @Query("UPDATE flashcard_items SET isMastered = :isMastered WHERE id = :cardId")
    suspend fun updateCardMastered(cardId: Long, isMastered: Boolean)

    @Query("DELETE FROM flashcard_items WHERE id = :cardId")
    suspend fun deleteCard(cardId: Long)
}
