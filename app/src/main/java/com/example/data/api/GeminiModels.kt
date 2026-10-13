package com.example.data.api

import kotlinx.serialization.Serializable

object GeminiModelConstants {
    const val IMAGE_MODEL = "gemini-3-pro-image-preview"
    const val CHAT_FAST = "gemini-3.1-flash-lite"
    const val CHAT_GENERAL = "gemini-3.8-flash"
    const val CHAT_COMPLEX = "gemini-4-argon"
}

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String? = null // "user" or "model"
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String // Base64 encoded data
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val imageConfig: ImageConfig? = null,
    val responseModalities: List<String>? = null
)

@Serializable
data class ImageConfig(
    val aspectRatio: String, // "1:1", "16:9", "9:16", "4:3", "3:4"
    val imageSize: String    // "1K", "2K", "4K"
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null,
    val promptFeedback: PromptFeedback? = null
)

@Serializable
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null
)

@Serializable
data class PromptFeedback(
    val blockReason: String? = null
)
