package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ChatSessionEntity
import com.example.data.db.GeneratedImageEntity
import com.example.data.model.AiModelType
import com.example.data.model.ChatRole
import com.example.data.repository.OmniRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    CHAT,
    IMAGE_GENERATOR,
    GALLERY_HISTORY
}

class OmniViewModel(application: Application) : AndroidViewModel(application) {
    val repository = OmniRepository(application)

    // Current Active Tab
    private val _currentTab = MutableStateFlow(AppTab.CHAT)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
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

                if (result.isFailure) {
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
}
