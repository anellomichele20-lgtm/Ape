package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.GeminiModelConstants
import com.example.data.model.AiModelType
import com.example.data.model.ChatRole
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
}
