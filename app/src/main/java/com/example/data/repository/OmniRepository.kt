package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GeminiApiClient
import com.example.data.api.GeminiModelConstants
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.ImageConfig
import com.example.data.api.Part
import com.example.data.db.AppDatabase
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ChatSessionEntity
import com.example.data.db.GeneratedImageEntity
import com.example.data.model.AiModelType
import com.example.data.model.ChatRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class OmniRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val chatDao = database.chatDao()
    private val imageDao = database.imageDao()

    private val prefs = context.getSharedPreferences("omni_ai_prefs", Context.MODE_PRIVATE)

    fun getApiKey(): String {
        val customKey = prefs.getString("custom_api_key", "")?.trim() ?: ""
        if (customKey.isNotEmpty()) return customKey

        val buildKey = BuildConfig.GEMINI_API_KEY.trim()
        if (buildKey.isNotEmpty() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return ""
    }

    fun setCustomApiKey(key: String) {
        prefs.edit().putString("custom_api_key", key.trim()).apply()
    }

    fun isApiKeyConfigured(): Boolean = getApiKey().isNotEmpty()

    // --- Chat Sessions & Messages ---

    fun getAllChatSessions(): Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>> =
        chatDao.getMessagesForSession(sessionId)

    suspend fun createNewSession(roleId: String, modelId: String): Long = withContext(Dispatchers.IO) {
        val role = ChatRole.getById(roleId)
        val session = ChatSessionEntity(
            title = "Nuova conversazione con ${role.title}",
            roleId = roleId,
            modelId = modelId
        )
        chatDao.insertSession(session)
    }

    suspend fun deleteSession(sessionId: Long) = withContext(Dispatchers.IO) {
        chatDao.deleteSession(sessionId)
    }

    suspend fun sendChatMessage(
        sessionId: Long,
        userMessageText: String,
        role: ChatRole,
        modelType: AiModelType
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("Chiave API Gemini mancante. Configurala nel pannello Secrets di AI Studio o nelle impostazioni.")
            )
        }

        // 1. Save user message locally
        val userMsg = ChatMessageEntity(
            sessionId = sessionId,
            role = "user",
            content = userMessageText,
            modelUsed = modelType.modelId
        )
        chatDao.insertMessage(userMsg)

        // 2. Fetch history for this session
        val historyEntities = chatDao.getMessagesListForSession(sessionId)

        // If this is the first message, update session title
        if (historyEntities.size <= 2) {
            val title = if (userMessageText.length > 32) {
                userMessageText.take(30).trim() + "..."
            } else {
                userMessageText
            }
            chatDao.updateSessionTitleAndTime(sessionId, title, System.currentTimeMillis())
        } else {
            chatDao.updateSessionTitleAndTime(
                sessionId,
                chatDao.getSessionById(sessionId)?.title ?: "Chat",
                System.currentTimeMillis()
            )
        }

        // 3. Build multi-turn contents list
        val contentsList = mutableListOf<Content>()
        // Filter out any error messages and keep last 20 messages for context window management
        val contextHistory = historyEntities.filter { !it.isError }.takeLast(20)
        for (msg in contextHistory) {
            val apiRole = if (msg.role == "user") "user" else "model"
            contentsList.add(
                Content(
                    role = apiRole,
                    parts = listOf(Part(text = msg.content))
                )
            )
        }

        // 4. Construct System Instruction
        val systemInstruction = Content(
            parts = listOf(Part(text = role.systemPrompt))
        )

        val request = GenerateContentRequest(
            contents = contentsList,
            generationConfig = GenerationConfig(
                temperature = 0.7f,
                topP = 0.95f
            ),
            systemInstruction = systemInstruction
        )

        try {
            val response = GeminiApiClient.service.generateContent(
                model = modelType.modelId,
                apiKey = apiKey,
                request = request
            )

            val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.text != null }?.text
                ?: throw IllegalStateException("Nessun testo ricevuto dal modello.")

            // Save assistant reply
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = "model",
                    content = replyText,
                    modelUsed = modelType.modelId
                )
            )

            Result.success(replyText)
        } catch (e: Exception) {
            Log.e("OmniRepository", "Error generating chat content", e)
            val errorMessage = "Errore durante la generazione: ${e.localizedMessage ?: e.message}"
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = "model",
                    content = errorMessage,
                    modelUsed = modelType.modelId,
                    isError = true
                )
            )
            Result.failure(e)
        }
    }

    // --- Image Generation ---

    fun getAllGeneratedImages(): Flow<List<GeneratedImageEntity>> = imageDao.getAllImages()

    suspend fun generateImage(
        prompt: String,
        imageSize: String, // "1K", "2K", "4K"
        aspectRatio: String // "1:1", "16:9", "9:16", "4:3", "3:4"
    ): Result<GeneratedImageEntity> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("Chiave API Gemini mancante per generare immagini.")
            )
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    role = "user",
                    parts = listOf(Part(text = prompt))
                )
            ),
            generationConfig = GenerationConfig(
                imageConfig = ImageConfig(
                    aspectRatio = aspectRatio,
                    imageSize = imageSize
                ),
                responseModalities = listOf("TEXT", "IMAGE")
            )
        )

        try {
            val response = GeminiApiClient.service.generateContent(
                model = GeminiModelConstants.IMAGE_MODEL,
                apiKey = apiKey,
                request = request
            )

            val inlineData = response.candidates?.firstOrNull()?.content?.parts
                ?.firstOrNull { it.inlineData != null }?.inlineData
                ?: throw IllegalStateException("L'API non ha restituito dati immagine per questo prompt.")

            val entity = GeneratedImageEntity(
                prompt = prompt,
                model = GeminiModelConstants.IMAGE_MODEL,
                imageSize = imageSize,
                aspectRatio = aspectRatio,
                base64Data = inlineData.data,
                createdAt = System.currentTimeMillis()
            )

            val insertedId = imageDao.insertImage(entity)
            Result.success(entity.copy(id = insertedId))
        } catch (e: Exception) {
            Log.e("OmniRepository", "Error generating image", e)
            Result.failure(e)
        }
    }

    suspend fun toggleFavoriteImage(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        imageDao.updateFavorite(id, isFavorite)
    }

    suspend fun deleteImage(id: Long) = withContext(Dispatchers.IO) {
        imageDao.deleteImage(id)
    }
}
