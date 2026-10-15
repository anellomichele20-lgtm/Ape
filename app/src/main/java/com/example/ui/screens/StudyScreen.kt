package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyTool
import com.example.ui.OmniViewModel

@Composable
fun StudyScreen(viewModel: OmniViewModel) {
    val selectedTool by viewModel.selectedStudyTool.collectAsState()

    val toolTabs = listOf(
        Pair(StudyTool.FLASHCARDS, "🗂️ Flashcards"),
        Pair(StudyTool.QUIZ, "📝 Quiz AI"),
        Pair(StudyTool.FEYNMAN_METHOD, "🧠 Feynman"),
        Pair(StudyTool.POMODORO_TIMER, "⏱️ Pomodoro")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = toolTabs.indexOfFirst { it.first == selectedTool },
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 12.dp
        ) {
            toolTabs.forEach { (tool, label) ->
                Tab(
                    selected = selectedTool == tool,
                    onClick = { viewModel.selectStudyTool(tool) },
                    text = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedTool == tool) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTool) {
                StudyTool.FLASHCARDS -> FlashcardsSection(viewModel)
                StudyTool.QUIZ -> QuizSection(viewModel)
                StudyTool.FEYNMAN_METHOD -> FeynmanSection(viewModel)
                StudyTool.POMODORO_TIMER -> PomodoroSection(viewModel)
            }
        }
    }
}

// ----------------------------------------------------
// FLASHCARDS SECTION
// ----------------------------------------------------
@Composable
fun FlashcardsSection(viewModel: OmniViewModel) {
    var topicInput by remember { mutableStateOf("") }
    val isGenerating by viewModel.isGeneratingCards.collectAsState()
    val activeDeck by viewModel.activeDeck.collectAsState()
    val currentCardIndex by viewModel.currentCardIndex.collectAsState()
    val isFlipped by viewModel.isCardFlipped.collectAsState()

    val quickTopics = listOf(
        "Biologia: La cellula",
        "Storia: Rinascimento",
        "Fisica: Le leggi di Newton",
        "Inglese: Phrasal Verbs"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Topic Input Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Genera Flashcard con AI:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = topicInput,
                    onValueChange = { topicInput = it },
                    placeholder = { Text("Es: Fotosintesi clorofilliana, Teorema di Pitagora...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("flashcard_topic_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ispirazioni:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickTopics.take(2).forEach { sample ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    topicInput = sample
                                    viewModel.generateFlashcards(sample)
                                }
                        ) {
                            Text(
                                text = sample,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(6.dp),
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.generateFlashcards(topicInput) },
                    enabled = topicInput.isNotBlank() && !isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generate_flashcards_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Creazione mazzo in corso...")
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Crea 6 Flashcard Interattive")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Active Deck Viewer
        if (activeDeck != null && activeDeck!!.cards.isNotEmpty()) {
            val totalCards = activeDeck!!.cards.size
            val card = activeDeck!!.cards.getOrNull(currentCardIndex) ?: activeDeck!!.cards.first()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${activeDeck!!.title} (${currentCardIndex + 1}/$totalCards)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = {
                    val textToRead = if (isFlipped) "Risposta: ${card.back}" else "Domanda: ${card.front}"
                    viewModel.voiceManager.speak(textToRead)
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Ascolta vocalmente",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { (currentCardIndex + 1).toFloat() / totalCards.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3D Flip Card
            val rotation by animateFloatAsState(
                targetValue = if (isFlipped) 180f else 0f,
                animationSpec = tween(durationMillis = 400),
                label = "cardFlip"
            )

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density
                    }
                    .clickable { viewModel.flipCard() }
                    .testTag("flashcard_item_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isFlipped) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .graphicsLayer {
                            if (rotation > 90f) rotationY = 180f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isFlipped) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = if (isFlipped) "RISPOSTA" else "DOMANDA / CONCETTO",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isFlipped) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (isFlipped) card.back else card.front,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = if (isFlipped) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (isFlipped) "Tocca per vedere la domanda" else "Tocca per girare e rivelare la risposta",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation & Mastery Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.prevCard() },
                    enabled = currentCardIndex > 0
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Precedente")
                }

                Button(
                    onClick = { viewModel.markCurrentCard(true) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Memorizzato!")
                }

                OutlinedButton(
                    onClick = { viewModel.nextCard() },
                    enabled = currentCardIndex < totalCards - 1
                ) {
                    Text("Successivo")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

// ----------------------------------------------------
// QUIZ SECTION
// ----------------------------------------------------
@Composable
fun QuizSection(viewModel: OmniViewModel) {
    var quizTopic by remember { mutableStateOf("") }
    val isGenerating by viewModel.isGeneratingQuiz.collectAsState()
    val activeQuiz by viewModel.activeQuiz.collectAsState()
    val userAnswers by viewModel.quizUserAnswers.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (activeQuiz == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Simulatore di Quiz & Verifiche AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mettiti alla prova con 5 domande a risposta multipla generate su misura dal modello Gemini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = quizTopic,
                        onValueChange = { quizTopic = it },
                        placeholder = { Text("Es: Storia moderna, Chimica organica, Capitali europee...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quiz_topic_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.generateQuiz(quizTopic) },
                        enabled = quizTopic.isNotBlank() && !isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_quiz_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generazione domande...")
                        } else {
                            Icon(imageVector = Icons.Default.Quiz, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Avvia Quiz di Verifica")
                        }
                    }
                }
            }
        } else {
            val questions = activeQuiz!!.questions
            val total = questions.size
            val answeredCount = userAnswers.size
            val correctCount = userAnswers.count { (qIdx, optIdx) ->
                questions.getOrNull(qIdx)?.correctIndex == optIdx
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Quiz: ${activeQuiz!!.topic}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Punteggio: $correctCount su $answeredCount risposte date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(onClick = { viewModel.resetQuiz() }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nuovo Quiz")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            questions.forEachIndexed { qIndex, q ->
                val selectedOption = userAnswers[qIndex]
                val isAnswered = selectedOption != null

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${qIndex + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = q.question,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(onClick = {
                                viewModel.voiceManager.speak("Domanda ${qIndex + 1}: ${q.question}")
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Leggi domanda",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        q.options.forEachIndexed { optIndex, optionText ->
                            val isCorrect = optIndex == q.correctIndex
                            val isChosen = selectedOption == optIndex

                            val containerColor = when {
                                !isAnswered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                isCorrect -> MaterialTheme.colorScheme.primaryContainer
                                isChosen -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable(enabled = !isAnswered) {
                                        viewModel.answerQuizQuestion(qIndex, optIndex)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = containerColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${('A' + optIndex)}.",
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Text(
                                        text = optionText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isAnswered) {
                                        if (isCorrect) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Corretto",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        } else if (isChosen) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Errato",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (isAnswered) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = q.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// FEYNMAN TECHNIQUE SECTION
// ----------------------------------------------------
@Composable
fun FeynmanSection(viewModel: OmniViewModel) {
    var conceptInput by remember { mutableStateOf("") }
    val isGenerating by viewModel.isGeneratingFeynman.collectAsState()
    val result by viewModel.feynmanResult.collectAsState()

    val quickConcepts = listOf(
        "Entanglement quantistico",
        "Come funziona l'inflazione economica",
        "Il ciclo di Krebs",
        "Algoritmo di crittografia RSA"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Metodo Feynman: Spiegazione Ultra-Semplice",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Richard Feynman diceva: 'Se non sai spiegarlo a un bambino di 10 anni, non l'hai capito veramente'. Inserisci qualsiasi argomento ostico:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = conceptInput,
                    onValueChange = { conceptInput = it },
                    placeholder = { Text("Es: Teoria della relatività, Fotosintesi, Blockchain...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("feynman_concept_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickConcepts.take(2).forEach { sample ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    conceptInput = sample
                                    viewModel.generateFeynman(sample)
                                }
                        ) {
                            Text(
                                text = sample,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(6.dp),
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { viewModel.generateFeynman(conceptInput) },
                    enabled = conceptInput.isNotBlank() && !isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generate_feynman_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Elaborazione metodo Feynman...")
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Spiegamelo Semplicemente")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (result != null) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Spiegazione Feynman:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        IconButton(onClick = { viewModel.voiceManager.speak(result!!) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Ascolta vocalmente",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = result!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// POMODORO SECTION
// ----------------------------------------------------
@Composable
fun PomodoroSection(viewModel: OmniViewModel) {
    val seconds by viewModel.pomodoroSeconds.collectAsState()
    val isRunning by viewModel.isPomodoroRunning.collectAsState()
    val isBreak by viewModel.isPomodoroBreak.collectAsState()

    val minutes = seconds / 60
    val secs = seconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, secs)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isBreak) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = if (isBreak) "☕ PAUSA RIPOSANTE" else "🎯 STUDIO CONCENTRATO",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isBreak) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Big Circular Dial
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(220.dp),
            tonalElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 52.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isRunning) "Sessione in corso" else "In pausa",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.togglePomodoro() },
                modifier = Modifier
                    .size(64.dp)
                    .testTag("pomodoro_toggle_button"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pausa" else "Avvia",
                    modifier = Modifier.size(32.dp)
                )
            }

            OutlinedButton(
                onClick = { viewModel.resetPomodoro() },
                modifier = Modifier.size(64.dp),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "La tecnica Pomodoro (25 minuti di focus totale + 5 minuti di pausa) ottimizza la ritenzione mnemonica e previene il sovraccarico cognitivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
