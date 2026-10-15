package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ChatMessageEntity
import com.example.data.model.AiModelType
import com.example.data.model.ChatRole
import com.example.ui.OmniViewModel
import com.example.ui.components.ImageUtils
import com.example.ui.components.ModelSelectionBottomSheet
import com.example.ui.components.RoleSelectionBottomSheet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: OmniViewModel) {
    val context = LocalContext.current
    val messages by viewModel.currentMessages.collectAsState()
    val inputText by viewModel.chatInputText.collectAsState()
    val isLoading by viewModel.isChatLoading.collectAsState()
    val errorMessage by viewModel.chatErrorMessage.collectAsState()
    val selectedRole by viewModel.selectedRole.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()

    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val activeUtteranceId by viewModel.activeUtteranceId.collectAsState()
    val autoSpeakEnabled by viewModel.autoSpeakEnabled.collectAsState()

    var showRoleSheet by remember { mutableStateOf(false) }
    var showModelSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Speech-to-Text Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val spokenText = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.updateChatInput(spokenText)
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showRoleSheet) {
        RoleSelectionBottomSheet(
            currentRole = selectedRole,
            onRoleSelected = { viewModel.selectRole(it) },
            onDismiss = { showRoleSheet = false }
        )
    }

    if (showModelSheet) {
        ModelSelectionBottomSheet(
            currentModel = selectedModel,
            onModelSelected = { viewModel.selectModel(it) },
            onDismiss = { showModelSheet = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Top Bar Controls: Role, Model, Auto-Speak, New Chat
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Role Chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .clickable { showRoleSheet = true }
                        .testTag("role_selector_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = selectedRole.iconEmoji, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedRole.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Model Chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .clickable { showModelSheet = true }
                        .testTag("model_selector_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedModel.badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Auto-Speak Toggle Button
                    IconButton(
                        onClick = { viewModel.setAutoSpeak(!autoSpeakEnabled) },
                        modifier = Modifier.testTag("toggle_auto_speak_button")
                    ) {
                        Icon(
                            imageVector = if (autoSpeakEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Lettura vocale automatica",
                            tint = if (autoSpeakEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }

                    // New Chat Button
                    IconButton(
                        onClick = { viewModel.startNewChat() },
                        modifier = Modifier.testTag("new_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Nuova Chat",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Quick Study Shortcuts Pill Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    AssistChip(
                        onClick = {
                            viewModel.updateChatInput("Crea un quiz a 5 domande a risposta multipla su:")
                        },
                        label = { Text("📝 Quiz di Studio", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
                item {
                    AssistChip(
                        onClick = {
                            viewModel.updateChatInput("Spiegami questo argomento usando il Metodo Feynman in modo ultra semplice:")
                        },
                        label = { Text("🧠 Metodo Feynman", fontSize = 12.sp) }
                    )
                }
                item {
                    AssistChip(
                        onClick = {
                            viewModel.updateChatInput("Fammi un riassunto per punti chiave e formule di:")
                        },
                        label = { Text("📋 Schema di Sintesi", fontSize = 12.sp) }
                    )
                }
            }
        }

        // Error Banner if present
        AnimatedVisibility(visible = errorMessage != null) {
            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        AssistChip(
                            onClick = { viewModel.clearChatError() },
                            label = { Text("OK") }
                        )
                    }
                }
            }
        }

        // Messages List or Empty Starter State
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                ChatEmptyState(
                    role = selectedRole,
                    onPromptSelected = { prompt ->
                        viewModel.updateChatInput(prompt)
                        viewModel.sendChatMessage()
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("chat_messages_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isThisMessageSpeaking = isSpeaking && activeUtteranceId == "msg_${msg.id}"
                        ChatMessageBubble(
                            message = msg,
                            isSpeaking = isThisMessageSpeaking,
                            onToggleSpeak = {
                                viewModel.toggleSpeakMessage(msg.content, msg.id)
                            },
                            onCopy = {
                                ImageUtils.copyToClipboard(context, msg.content, "Messaggio")
                            }
                        )
                    }

                    if (isLoading) {
                        item {
                            ChatLoadingBubble(model = selectedModel)
                        }
                    }
                }
            }
        }

        // Input Bar with Text, Mic Button, and Send Button
        Surface(
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.updateChatInput(it) },
                    placeholder = {
                        Text("Scrivi o tocca il microfono...")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Microphone Input Button
                IconButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ITALIAN.language)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Parla adesso con OmniAI...")
                        }
                        try {
                            speechLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Riconoscimento vocale non disponibile sul dispositivo", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("chat_mic_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Parla al microfono",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                FilledIconButton(
                    onClick = { viewModel.sendChatMessage() },
                    enabled = inputText.isNotBlank() && !isLoading,
                    modifier = Modifier
                        .size(46.dp)
                        .testTag("chat_send_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Invia"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessageEntity,
    isSpeaking: Boolean,
    onToggleSpeak: () -> Unit,
    onCopy: () -> Unit
) {
    val isUser = message.role == "user"
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormatter.format(Date(message.timestamp)) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .align(Alignment.Top),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                color = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else if (message.isError) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                tonalElevation = if (isUser) 2.dp else 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (!isUser) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = "Gemini",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = message.modelUsed.substringAfterLast("/"),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else if (message.isError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isUser) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Voice Read Button
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                    modifier = Modifier.clickable { onToggleSpeak() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = if (isSpeaking) "Ferma voce" else "Ascolta vocalmente",
                                            modifier = Modifier
                                                .size(14.dp)
                                                .then(if (isSpeaking) Modifier.scale(pulseScale) else Modifier),
                                            tint = if (isSpeaking) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isSpeaking) "Parlando..." else "Ascolta",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSpeaking) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = onCopy,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copia testo",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatLoadingBubble(model: AiModelType) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${model.title} sta elaborando la risposta...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ChatEmptyState(
    role: ChatRole,
    onPromptSelected: (String) -> Unit
) {
    val samplePrompts = remember(role.id) {
        when (role.id) {
            "tutor" -> listOf(
                "Spiegami come preparare una tesina o un esame universitario.",
                "Quali sono le tecniche migliori per ricordare formule e date?",
                "Fammi 3 domande difficili su un argomento a mia scelta."
            )
            "coder" -> listOf(
                "Come implementare una LazyColumn performante in Jetpack Compose?",
                "Spiegami come funziona Coroutines Flow in Android.",
                "Scrivi una funzione Kotlin per ordinare una lista di oggetti."
            )
            "creative" -> listOf(
                "Scrivi un breve racconto di fantascienza su un'astronave senziente.",
                "Crea uno slogan accattivante per una nuova app di intelligenza artificiale.",
                "Scrivi una poesia evocativa sull'autunno."
            )
            "strategist" -> listOf(
                "Come posso pianificare la mia settimana di studio con il Time-Blocking?",
                "Consigli pratici per eliminare le distrazioni mentre studio.",
                "Definisci un piano di studio in 3 fasi per un esame imminente."
            )
            else -> listOf(
                "Crea un quiz di ripasso per mettere alla prova le mie conoscenze.",
                "Spiegami un argomento complesso usando il Metodo Feynman.",
                "Consigli pratici per studiare più velocemente e ricordare a lungo."
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = role.iconEmoji, fontSize = 36.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = role.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = role.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Primi spunti per lo studio e la chat:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        samplePrompts.forEach { prompt ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onPromptSelected(prompt) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Text(
                    text = "“$prompt”",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
