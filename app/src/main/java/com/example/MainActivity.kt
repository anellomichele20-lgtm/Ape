package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.AppTab
import com.example.ui.OmniViewModel
import com.example.ui.components.ApiKeyDialog
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.GalleryAndHistoryScreen
import com.example.ui.screens.ImageGeneratorScreen
import com.example.ui.theme.OmniAITheme

class MainActivity : ComponentActivity() {
    private val viewModel: OmniViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OmniAITheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: OmniViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val showApiKeyDialog by viewModel.showApiKeyDialog.collectAsState()

    if (showApiKeyDialog) {
        ApiKeyDialog(
            initialKey = viewModel.repository.getApiKey(),
            onSave = { viewModel.saveApiKey(it) },
            onDismiss = { viewModel.setShowApiKeyDialog(false) }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.CHAT,
                    onClick = { viewModel.selectTab(AppTab.CHAT) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.CHAT) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat,
                            contentDescription = "Chat AI"
                        )
                    },
                    label = { Text("Chat AI") },
                    modifier = Modifier.testTag("nav_chat_tab")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.IMAGE_GENERATOR,
                    onClick = { viewModel.selectTab(AppTab.IMAGE_GENERATOR) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.IMAGE_GENERATOR) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                            contentDescription = "Crea Immagine"
                        )
                    },
                    label = { Text("Immagini HD") },
                    modifier = Modifier.testTag("nav_image_tab")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.GALLERY_HISTORY,
                    onClick = { viewModel.selectTab(AppTab.GALLERY_HISTORY) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.GALLERY_HISTORY) Icons.Filled.Collections else Icons.Outlined.Collections,
                            contentDescription = "Galleria & Cronologia"
                        )
                    },
                    label = { Text("Galleria") },
                    modifier = Modifier.testTag("nav_gallery_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.CHAT -> ChatScreen(viewModel = viewModel)
                AppTab.IMAGE_GENERATOR -> ImageGeneratorScreen(viewModel = viewModel)
                AppTab.GALLERY_HISTORY -> GalleryAndHistoryScreen(viewModel = viewModel)
            }
        }
    }
}
