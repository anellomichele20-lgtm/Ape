package com.example.data.model

import com.example.data.api.GeminiModelConstants

enum class AiModelType(
    val modelId: String,
    val title: String,
    val description: String,
    val badge: String
) {
    FAST(
        modelId = GeminiModelConstants.CHAT_FAST,
        title = "Flash Lite",
        description = "Risposte ultra-rapide e sintetiche",
        badge = "⚡ Veloce"
    ),
    GENERAL(
        modelId = GeminiModelConstants.CHAT_GENERAL,
        title = "Flash 3.8",
        description = "Modello standard bilanciato e intelligente",
        badge = "⚖️ Bilanciato"
    ),
    COMPLEX(
        modelId = GeminiModelConstants.CHAT_COMPLEX,
        title = "Argon 4",
        description = "Massima capacità di ragionamento e logica",
        badge = "🧠 Complesso"
    );

    companion object {
        fun fromModelId(id: String): AiModelType {
            return entries.firstOrNull { it.modelId == id } ?: GENERAL
        }
    }
}

data class ChatRole(
    val id: String,
    val title: String,
    val iconEmoji: String,
    val description: String,
    val systemPrompt: String
) {
    companion object {
        val ALL_ROLES = listOf(
            ChatRole(
                id = "assistant",
                title = "Assistente Generale",
                iconEmoji = "🤖",
                description = "Utile, versatile ed equilibrato per ogni richiesta",
                systemPrompt = "Sei OmniAI, un assistente virtuale intelligente, preciso, cortese ed empatico. Rispondi in modo esaustivo, ben strutturato e chiaro."
            ),
            ChatRole(
                id = "coder",
                title = "Esperto Sviluppatore",
                iconEmoji = "💻",
                description = "Ingegnere del software per codice, debug e architetture",
                systemPrompt = "Sei un Senior Software Engineer esperto di programmazione (Kotlin, Compose, Python, TypeScript, algoritmi, system design). Fornisci codice pulito, idiomatico, commentato, buone pratiche e spiegazioni puntuali."
            ),
            ChatRole(
                id = "creative",
                title = "Scrittore & Creativo",
                iconEmoji = "✍️",
                description = "Storie avvincenti, copywriting, testi poetici e brainstorming",
                systemPrompt = "Sei uno scrittore professionista, poeta e copywriter creativo. Scrivi con stile vivace, ricco di metafore, ritmo e appeal emotivo."
            ),
            ChatRole(
                id = "tutor",
                title = "Tutor & Docente",
                iconEmoji = "🎓",
                description = "Spiegazioni didattiche passo-passo, scienze e lingue",
                systemPrompt = "Sei un docente e tutor didattico empatico e chiarissimo. Spieghi concetti difficili con esempi intuitivi, verifiche guidate e un approccio pedagogico stimolante."
            ),
            ChatRole(
                id = "strategist",
                title = "Stratega di Produttività",
                iconEmoji = "💼",
                description = "Pianificazione obiettivi, time-management e piani d'azione",
                systemPrompt = "Sei un consulente di produttività, business e organizzazione personale. Fornisci piani operativi concreti, priorità chiare, matrici decisionali e checklist pratiche."
            )
        )

        fun getById(id: String): ChatRole {
            return ALL_ROLES.firstOrNull { it.id == id } ?: ALL_ROLES.first()
        }
    }
}
