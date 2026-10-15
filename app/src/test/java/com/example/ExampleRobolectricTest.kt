package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.GeminiModelConstants
import com.example.data.model.AiModelType
import com.example.data.model.ChatRole
import com.example.data.model.FlashcardItem
import com.example.data.model.QuizQuestion
import com.example.data.model.StudyTool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app_name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("OmniAI", appName)
  }

  @Test
  fun `verify model constants and roles`() {
    assertEquals("gemini-3-pro-image-preview", GeminiModelConstants.IMAGE_MODEL)
    assertEquals("gemini-3.8-flash", GeminiModelConstants.CHAT_GENERAL)
    assertEquals("gemini-3.1-flash-lite", GeminiModelConstants.CHAT_FAST)
    assertEquals("gemini-4-argon", GeminiModelConstants.CHAT_COMPLEX)

    val defaultRole = ChatRole.getById("assistant")
    assertNotNull(defaultRole)
    assertEquals("Assistente Generale", defaultRole.title)

    val defaultModel = AiModelType.fromModelId(GeminiModelConstants.CHAT_GENERAL)
    assertEquals(AiModelType.GENERAL, defaultModel)
  }

  @Test
  fun `verify study tools and models`() {
    val tools = StudyTool.entries
    assertEquals(4, tools.size)

    val card = FlashcardItem(front = "Cos'è il DNA?", back = "Acido desossiribonucleico")
    assertEquals("Cos'è il DNA?", card.front)

    val quiz = QuizQuestion(
        question = "Qual è la capitale d'Italia?",
        options = listOf("Milano", "Roma", "Napoli", "Torino"),
        correctIndex = 1,
        explanation = "Roma è la capitale dal 1871"
    )
    assertEquals(1, quiz.correctIndex)
  }
}
