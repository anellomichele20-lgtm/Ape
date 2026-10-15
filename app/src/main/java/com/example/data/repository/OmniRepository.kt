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
import com.example.data.db.FlashcardDeckEntity
import com.example.data.db.FlashcardItemEntity
import com.example.data.db.GeneratedImageEntity
import com.example.data.model.AiModelType
import com.example.data.model.ChatRole
import com.example.data.model.FlashcardDeck
import com.example.data.model.FlashcardItem
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class OmniRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val chatDao = database.chatDao()
    private val imageDao = database.imageDao()
    private val flashcardDao = database.flashcardDao()

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
                IllegalStateException("Chiave API Gemini mancante. Configurala nei Secrets di AI Studio o nelle impostazioni.")
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

    // --- Study & Flashcards System ---

    fun getAllFlashcardDecks(): Flow<List<FlashcardDeckEntity>> = flashcardDao.getAllDecks()

    fun getCardsForDeck(deckId: Long): Flow<List<FlashcardItemEntity>> = flashcardDao.getCardsForDeck(deckId)

    suspend fun toggleCardMastered(cardId: Long, isMastered: Boolean) = withContext(Dispatchers.IO) {
        flashcardDao.updateCardMastered(cardId, isMastered)
    }

    suspend fun deleteFlashcardDeck(deckId: Long) = withContext(Dispatchers.IO) {
        flashcardDao.deleteDeck(deckId)
    }

    suspend fun generateFlashcards(topic: String): Result<FlashcardDeck> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Chiave API Gemini mancante."))
        }

        val prompt = """
            Sei un esperto didattico e professore universitario.
            Crea un set di 6 flashcard efficaci, chiare e mirate per lo studio sull'argomento: "$topic".
            Rispondi ESCLUSIVAMENTE con un JSON valido con questo formato (senza testo prima o dopo, senza markdown backtick):
            [
              {
                "front": "Domanda o concetto chiave sintetico",
                "back": "Spiegazione chiara, esaustiva ma concisa"
              }
            ]
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)), role = "user")),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.5f
            )
        )

        try {
            val response = GeminiApiClient.service.generateContent(
                model = GeminiModelConstants.CHAT_GENERAL,
                apiKey = apiKey,
                request = request
            )

            val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw IllegalStateException("Nessuna risposta dal modello")

            val cleanJson = rawJson.replace("```json", "").replace("```", "").trim()
            val jsonArray = JSONArray(cleanJson)

            val deckEntity = FlashcardDeckEntity(
                title = topic,
                subject = "Studio & Ripasso"
            )
            val deckId = flashcardDao.insertDeck(deckEntity)

            val cardEntities = mutableListOf<FlashcardItemEntity>()
            val cardsList = mutableListOf<FlashcardItem>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val front = obj.getString("front")
                val back = obj.getString("back")

                cardEntities.add(
                    FlashcardItemEntity(
                        deckId = deckId,
                        front = front,
                        back = back,
                        isMastered = false
                    )
                )
                cardsList.add(
                    FlashcardItem(
                        id = i.toLong(),
                        front = front,
                        back = back,
                        isMastered = false
                    )
                )
            }

            flashcardDao.insertCards(cardEntities)

            Result.success(
                FlashcardDeck(
                    id = deckId,
                    title = topic,
                    subject = "Studio & Ripasso",
                    cards = cardsList
                )
            )
        } catch (e: Exception) {
            Log.e("OmniRepository", "Error generating flashcards", e)
            Result.failure(e)
        }
    }

    suspend fun generateQuiz(topic: String): Result<QuizSession> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Chiave API Gemini mancante."))
        }

        val prompt = """
            Genera un quiz a risposta multipla di 5 domande per testare la comprensione approfondita dell'argomento: "$topic".
            Rispondi ESCLUSIVAMENTE con un JSON valido con questa struttura (senza markdown o commenti):
            [
              {
                "question": "Testo della domanda",
                "options": ["Opzione A", "Opzione B", "Opzione C", "Opzione D"],
                "correctIndex": 0,
                "explanation": "Spiegazione dettagliata della risposta corretta"
              }
            ]
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)), role = "user")),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.4f
            )
        )

        try {
            val response = GeminiApiClient.service.generateContent(
                model = GeminiModelConstants.CHAT_GENERAL,
                apiKey = apiKey,
                request = request
            )

            val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw IllegalStateException("Nessun dato quiz ricevuto")

            val cleanJson = rawJson.replace("```json", "").replace("```", "").trim()
            val jsonArray = JSONArray(cleanJson)

            val questions = mutableListOf<QuizQuestion>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val qText = obj.getString("question")
                val optArray = obj.getJSONArray("options")
                val options = mutableListOf<String>()
                for (j in 0 until optArray.length()) {
                    options.add(optArray.getString(j))
                }
                val correctIndex = obj.getInt("correctIndex")
                val explanation = obj.getString("explanation")

                questions.add(
                    QuizQuestion(
                        question = qText,
                        options = options,
                        correctIndex = correctIndex,
                        explanation = explanation
                    )
                )
            }

            Result.success(QuizSession(topic = topic, questions = questions))
        } catch (e: Exception) {
            Log.e("OmniRepository", "Error generating quiz", e)
            Result.failure(e)
        }
    }

    suspend fun explainConceptFeynman(concept: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Chiave API Gemini mancante."))
        }

        val prompt = """
            Spiega il seguente concetto applicando la celebre Tecnica di Feynman:
            Concetto: "$concept"
            
            Struttura la spiegazione in 4 parti ben distinte:
            1. 🧒 **Spiegazione come a un bambino di 10 anni**: usa parole semplici, zero gergo tecnico, esempi quotidiani.
            2. 🔍 **I concetti cardine**: identifica le 3 idee fondamentali.
            3. 💡 **Analogia memorabile**: una metafora potente che rimane impressa.
            4. ⚠️ **Trabocchetti ed errori comuni**: cosa si fraintende di solito.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)), role = "user")),
            generationConfig = GenerationConfig(temperature = 0.7f)
        )

        try {
            val response = GeminiApiClient.service.generateContent(
                model = GeminiModelConstants.CHAT_GENERAL,
                apiKey = apiKey,
                request = request
            )

            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw IllegalStateException("Nessun testo generato")
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
