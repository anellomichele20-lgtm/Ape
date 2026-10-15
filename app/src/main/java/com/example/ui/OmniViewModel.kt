package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ChatSessionEntity
import com.example.data.db.FlashcardDeckEntity
import com.example.data.db.GeneratedImageEntity
import com.example.data.model.AiModelType
import com.example.data.model.ChatRole
import com.example.data.model.FlashcardDeck
import com.example.data.model.FlashcardItem
import com.example.data.model.QuizSession
import com.example.data.model.StudyTool
import com.example.data.repository.OmniRepository
import com.example.ui.voice.VoiceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    CHAT,
    STUDY,
    IMAGE_GENERATOR,
    GALLERY_HISTORY
}

class OmniViewModel(application: Application) : AndroidViewModel(application) {
    val repository = OmniRepository(application)
    val voiceManager = VoiceManager(application)

    // Current Active Tab
    private val _currentTab = MutableStateFlow(AppTab.CHAT)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // Voice State delegating to VoiceManager
    val isSpeaking: StateFlow<Boolean> = voiceManager.isSpeaking
    val activeUtteranceId: StateFlow<String?> = voiceManager.activeUtteranceId
    val autoSpeakEnabled: StateFlow<Boolean> = voiceManager.autoSpeakEnabled

    fun toggleSpeakMessage(content: String, messageId: Long) {
        voiceManager.toggleSpeak(content, "msg_$messageId")
    }

    fun stopSpeaking() {
        voiceManager.stop()
    }

    fun setAutoSpeak(enabled: Boolean) {
        voiceManager.setAutoSpeak(enabled)
    }

    // --- Chat State ---
    val allSessions: StateFlow<List<ChatSessionEntity>> = repository.getAllChatSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSessionId = MutableStateFlow<Long?>(null)
    val selectedSessionId: StateFlow<Long?> = _selectedSessionId.asStateFlow()

    private val _selectedRole = MutableStateFlow(ChatRole.ALL_ROLES.first())
    val selectedRole: StateFlow<ChatRole> = _selectedRole.asStateFlow()

    private val _selectedModel = MutableStateFlow(AiModelType.GENERAL)
    val selectedModel: StateFlow<AiModelType> = _selectedModel.asStateFlow()

    private val _chatInputText = MutableStateFlow("")
    val chatInputText: StateFlow<String> = _chatInputText.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _chatErrorMessage = MutableStateFlow<String?>(null)
    val chatErrorMessage: StateFlow<String?> = _chatErrorMessage.asStateFlow()

    // Current Messages Flow
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentMessages: StateFlow<List<ChatMessageEntity>> = _selectedSessionId
        .flatMapLatest { sessionId ->
            if (sessionId != null) {
                repository.getMessagesForSession(sessionId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateChatInput(text: String) {
        _chatInputText.value = text
    }

    fun selectRole(role: ChatRole) {
        _selectedRole.value = role
    }

    fun selectModel(model: AiModelType) {
        _selectedModel.value = model
    }

    fun selectSession(sessionId: Long) {
        _selectedSessionId.value = sessionId
        viewModelScope.launch {
            val sessions = allSessions.value
            val session = sessions.firstOrNull { it.id == sessionId }
            if (session != null) {
                _selectedRole.value = ChatRole.getById(session.roleId)
                _selectedModel.value = AiModelType.fromModelId(session.modelId)
            }
        }
    }

    fun startNewChat(role: ChatRole = _selectedRole.value, model: AiModelType = _selectedModel.value) {
        viewModelScope.launch {
            val newSessionId = repository.createNewSession(role.id, model.modelId)
            _selectedSessionId.value = newSessionId
            _selectedRole.value = role
            _selectedModel.value = model
            _chatInputText.value = ""
            _chatErrorMessage.value = null
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_selectedSessionId.value == sessionId) {
                _selectedSessionId.value = null
            }
        }
    }

    fun sendChatMessage() {
        val prompt = _chatInputText.value.trim()
        if (prompt.isEmpty() || _isChatLoading.value) return

        _chatInputText.value = ""
        _chatErrorMessage.value = null
        _isChatLoading.value = true

        viewModelScope.launch {
            try {
                var sessionId = _selectedSessionId.value
                if (sessionId == null) {
                    sessionId = repository.createNewSession(_selectedRole.value.id, _selectedModel.value.modelId)
                    _selectedSessionId.value = sessionId
                }

                val result = repository.sendChatMessage(
                    sessionId = sessionId,
                    userMessageText = prompt,
                    role = _selectedRole.value,
                    modelType = _selectedModel.value
                )

                if (result.isSuccess) {
                    val reply = result.getOrNull() ?: ""
                    if (voiceManager.autoSpeakEnabled.value && reply.isNotEmpty()) {
                        voiceManager.speak(reply)
                    }
                } else {
                    _chatErrorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Errore nella risposta"
                }
            } catch (e: Exception) {
                _chatErrorMessage.value = e.localizedMessage ?: "Errore imprevisto"
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    fun clearChatError() {
        _chatErrorMessage.value = null
    }

    // --- Study & Educational Tools State ---
    private val _selectedStudyTool = MutableStateFlow(StudyTool.FLASHCARDS)
    val selectedStudyTool: StateFlow<StudyTool> = _selectedStudyTool.asStateFlow()

    fun selectStudyTool(tool: StudyTool) {
        _selectedStudyTool.value = tool
    }

    // Flashcard Deck State
    val allFlashcardDecks: StateFlow<List<FlashcardDeckEntity>> = repository.getAllFlashcardDecks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeDeck = MutableStateFlow<FlashcardDeck?>(null)
    val activeDeck: StateFlow<FlashcardDeck?> = _activeDeck.asStateFlow()

    private val _currentCardIndex = MutableStateFlow(0)
    val currentCardIndex: StateFlow<Int> = _currentCardIndex.asStateFlow()

    private val _isCardFlipped = MutableStateFlow(false)
    val isCardFlipped: StateFlow<Boolean> = _isCardFlipped.asStateFlow()

    private val _isGeneratingCards = MutableStateFlow(false)
    val isGeneratingCards: StateFlow<Boolean> = _isGeneratingCards.asStateFlow()

    fun flipCard() {
        _isCardFlipped.value = !_isCardFlipped.value
    }

    fun nextCard() {
        val total = _activeDeck.value?.cards?.size ?: 0
        if (total > 0 && _currentCardIndex.value < total - 1) {
            _currentCardIndex.value++
            _isCardFlipped.value = false
        }
    }

    fun prevCard() {
        if (_currentCardIndex.value > 0) {
            _currentCardIndex.value--
            _isCardFlipped.value = false
        }
    }

    fun markCurrentCard(isMastered: Boolean) {
        val deck = _activeDeck.value ?: return
        val currentCard = deck.cards.getOrNull(_currentCardIndex.value) ?: return

        viewModelScope.launch {
            repository.toggleCardMastered(currentCard.id, isMastered)
            val updatedCards = deck.cards.mapIndexed { idx, item ->
                if (idx == _currentCardIndex.value) item.copy(isMastered = isMastered) else item
            }
            _activeDeck.value = deck.copy(cards = updatedCards)
            nextCard()
        }
    }

    fun generateFlashcards(topic: String) {
        if (topic.isBlank() || _isGeneratingCards.value) return
        _isGeneratingCards.value = true

        viewModelScope.launch {
            try {
                val result = repository.generateFlashcards(topic)
                if (result.isSuccess) {
                    _activeDeck.value = result.getOrNull()
                    _currentCardIndex.value = 0
                    _isCardFlipped.value = false
                }
            } finally {
                _isGeneratingCards.value = false
            }
        }
    }

    fun loadDeck(deckId: Long) {
        viewModelScope.launch {
            val entities = repository.getCardsForDeck(deckId).firstOrNull() ?: emptyList()
            val deckEntity = allFlashcardDecks.value.firstOrNull { it.id == deckId }
            if (deckEntity != null) {
                _activeDeck.value = FlashcardDeck(
                    id = deckEntity.id,
                    title = deckEntity.title,
                    subject = deckEntity.subject,
                    cards = entities.map {
                        FlashcardItem(id = it.id, front = it.front, back = it.back, isMastered = it.isMastered)
                    }
                )
                _currentCardIndex.value = 0
                _isCardFlipped.value = false
            }
        }
    }

    fun deleteDeck(deckId: Long) {
        viewModelScope.launch {
            repository.deleteFlashcardDeck(deckId)
            if (_activeDeck.value?.id == deckId) {
                _activeDeck.value = null
            }
        }
    }

    // Quiz Simulator State
    private val _activeQuiz = MutableStateFlow<QuizSession?>(null)
    val activeQuiz: StateFlow<QuizSession?> = _activeQuiz.asStateFlow()

    private val _isGeneratingQuiz = MutableStateFlow(false)
    val isGeneratingQuiz: StateFlow<Boolean> = _isGeneratingQuiz.asStateFlow()

    private val _quizUserAnswers = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val quizUserAnswers: StateFlow<Map<Int, Int>> = _quizUserAnswers.asStateFlow()

    fun generateQuiz(topic: String) {
        if (topic.isBlank() || _isGeneratingQuiz.value) return
        _isGeneratingQuiz.value = true
        _quizUserAnswers.value = emptyMap()

        viewModelScope.launch {
            try {
                val result = repository.generateQuiz(topic)
                if (result.isSuccess) {
                    _activeQuiz.value = result.getOrNull()
                }
            } finally {
                _isGeneratingQuiz.value = false
            }
        }
    }

    fun answerQuizQuestion(questionIndex: Int, optionIndex: Int) {
        val current = _quizUserAnswers.value.toMutableMap()
        current[questionIndex] = optionIndex
        _quizUserAnswers.value = current
    }

    fun resetQuiz() {
        _activeQuiz.value = null
        _quizUserAnswers.value = emptyMap()
    }

    // Feynman Technique State
    private val _feynmanResult = MutableStateFlow<String?>(null)
    val feynmanResult: StateFlow<String?> = _feynmanResult.asStateFlow()

    private val _isGeneratingFeynman = MutableStateFlow(false)
    val isGeneratingFeynman: StateFlow<Boolean> = _isGeneratingFeynman.asStateFlow()

    fun generateFeynman(concept: String) {
        if (concept.isBlank() || _isGeneratingFeynman.value) return
        _isGeneratingFeynman.value = true
        _feynmanResult.value = null

        viewModelScope.launch {
            try {
                val result = repository.explainConceptFeynman(concept)
                _feynmanResult.value = result.getOrNull()
                if (voiceManager.autoSpeakEnabled.value && _feynmanResult.value != null) {
                    voiceManager.speak(_feynmanResult.value!!)
                }
            } finally {
                _isGeneratingFeynman.value = false
            }
        }
    }

    // Pomodoro Timer State
    private val _pomodoroSeconds = MutableStateFlow(25 * 60)
    val pomodoroSeconds: StateFlow<Int> = _pomodoroSeconds.asStateFlow()

    private val _isPomodoroRunning = MutableStateFlow(false)
    val isPomodoroRunning: StateFlow<Boolean> = _isPomodoroRunning.asStateFlow()

    private val _isPomodoroBreak = MutableStateFlow(false)
    val isPomodoroBreak: StateFlow<Boolean> = _isPomodoroBreak.asStateFlow()

    private var pomodoroJob: Job? = null

    fun togglePomodoro() {
        if (_isPomodoroRunning.value) {
            pomodoroJob?.cancel()
            _isPomodoroRunning.value = false
        } else {
            _isPomodoroRunning.value = true
            pomodoroJob = viewModelScope.launch {
                while (_pomodoroSeconds.value > 0) {
                    delay(1000)
                    _pomodoroSeconds.value--
                }
                _isPomodoroRunning.value = false
                val nextIsBreak = !_isPomodoroBreak.value
                _isPomodoroBreak.value = nextIsBreak
                _pomodoroSeconds.value = if (nextIsBreak) 5 * 60 else 25 * 60
                voiceManager.speak(
                    if (nextIsBreak) "Ottimo lavoro! Sessione di studio completata. È il momento di 5 minuti di pausa."
                    else "Pausa terminata. Riprendiamo la sessione di studio concentrato!"
                )
            }
        }
    }

    fun resetPomodoro() {
        pomodoroJob?.cancel()
        _isPomodoroRunning.value = false
        _isPomodoroBreak.value = false
        _pomodoroSeconds.value = 25 * 60
    }

    // --- Image Generator State ---
    val allImages: StateFlow<List<GeneratedImageEntity>> = repository.getAllGeneratedImages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _imagePrompt = MutableStateFlow("")
    val imagePrompt: StateFlow<String> = _imagePrompt.asStateFlow()

    private val _imageSize = MutableStateFlow("2K") // Options: "1K", "2K", "4K"
    val imageSize: StateFlow<String> = _imageSize.asStateFlow()

    private val _aspectRatio = MutableStateFlow("1:1") // Options: "1:1", "16:9", "9:16", "4:3", "3:4"
    val aspectRatio: StateFlow<String> = _aspectRatio.asStateFlow()

    private val _selectedStyle = MutableStateFlow<String?>(null)
    val selectedStyle: StateFlow<String?> = _selectedStyle.asStateFlow()

    private val _isImageGenerating = MutableStateFlow(false)
    val isImageGenerating: StateFlow<Boolean> = _isImageGenerating.asStateFlow()

    private val _imageErrorMessage = MutableStateFlow<String?>(null)
    val imageErrorMessage: StateFlow<String?> = _imageErrorMessage.asStateFlow()

    private val _lastGeneratedImage = MutableStateFlow<GeneratedImageEntity?>(null)
    val lastGeneratedImage: StateFlow<GeneratedImageEntity?> = _lastGeneratedImage.asStateFlow()

    private val _selectedImageDetail = MutableStateFlow<GeneratedImageEntity?>(null)
    val selectedImageDetail: StateFlow<GeneratedImageEntity?> = _selectedImageDetail.asStateFlow()

    fun updateImagePrompt(prompt: String) {
        _imagePrompt.value = prompt
    }

    fun setImageSize(size: String) {
        _imageSize.value = size
    }

    fun setAspectRatio(ratio: String) {
        _aspectRatio.value = ratio
    }

    fun setStyleChip(style: String?) {
        _selectedStyle.value = if (_selectedStyle.value == style) null else style
    }

    fun openImageDetail(image: GeneratedImageEntity?) {
        _selectedImageDetail.value = image
    }

    fun generateImage() {
        val basePrompt = _imagePrompt.value.trim()
        if (basePrompt.isEmpty() || _isImageGenerating.value) return

        val fullPrompt = if (_selectedStyle.value != null) {
            "$basePrompt, in ${_selectedStyle.value} style, ultra detailed, award winning"
        } else {
            basePrompt
        }

        _isImageGenerating.value = true
        _imageErrorMessage.value = null

        viewModelScope.launch {
            try {
                val result = repository.generateImage(
                    prompt = fullPrompt,
                    imageSize = _imageSize.value,
                    aspectRatio = _aspectRatio.value
                )

                if (result.isSuccess) {
                    val entity = result.getOrNull()
                    _lastGeneratedImage.value = entity
                    _selectedImageDetail.value = entity
                } else {
                    _imageErrorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Errore durante la generazione dell'immagine"
                }
            } catch (e: Exception) {
                _imageErrorMessage.value = e.localizedMessage ?: "Errore sconosciuto"
            } finally {
                _isImageGenerating.value = false
            }
        }
    }

    fun toggleFavoriteImage(id: Long, currentFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavoriteImage(id, !currentFav)
            if (_selectedImageDetail.value?.id == id) {
                _selectedImageDetail.value = _selectedImageDetail.value?.copy(isFavorite = !currentFav)
            }
        }
    }

    fun deleteImage(id: Long) {
        viewModelScope.launch {
            repository.deleteImage(id)
            if (_selectedImageDetail.value?.id == id) {
                _selectedImageDetail.value = null
            }
        }
    }

    fun clearImageError() {
        _imageErrorMessage.value = null
    }

    // --- API Key & Settings ---
    private val _showApiKeyDialog = MutableStateFlow(false)
    val showApiKeyDialog: StateFlow<Boolean> = _showApiKeyDialog.asStateFlow()

    fun setShowApiKeyDialog(show: Boolean) {
        _showApiKeyDialog.value = show
    }

    fun saveApiKey(key: String) {
        repository.setCustomApiKey(key)
        _showApiKeyDialog.value = false
    }

    fun isApiKeyReady(): Boolean = repository.isApiKeyConfigured()

    override fun onCleared() {
        super.onCleared()
        pomodoroJob?.cancel()
        voiceManager.shutdown()
    }
}
