package com.example.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FlashcardItem(
    val id: Long = 0,
    val front: String,
    val back: String,
    val isMastered: Boolean = false
)

@Serializable
data class FlashcardDeck(
    val id: Long = 0,
    val title: String,
    val subject: String,
    val cards: List<FlashcardItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

@Serializable
data class QuizSession(
    val topic: String,
    val questions: List<QuizQuestion> = emptyList(),
    val userAnswers: Map<Int, Int> = emptyMap(),
    val isCompleted: Boolean = false
)

enum class StudyTool {
    FLASHCARDS,
    QUIZ,
    FEYNMAN_METHOD,
    POMODORO_TIMER
}
