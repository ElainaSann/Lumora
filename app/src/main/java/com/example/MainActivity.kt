package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lumora.ui.EditorViewModel
import com.example.lumora.ui.StudioViewModel
import com.example.lumora.ui.screens.editor.EditorScreen
import com.example.lumora.ui.screens.studio.StudioScreen
import com.example.ui.theme.LumoraTheme

sealed class AppScreen {
    data object Studio : AppScreen()
    data class Editor(val projectId: String) : AppScreen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LumoraTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LumoraApp()
                }
            }
        }
    }
}

@Composable
fun LumoraApp() {
    val studioViewModel: StudioViewModel = viewModel()
    val editorViewModel: EditorViewModel = viewModel()

    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Studio) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            is AppScreen.Studio -> {
                StudioScreen(
                    viewModel = studioViewModel,
                    onOpenEditor = { projectId ->
                        currentScreen = AppScreen.Editor(projectId)
                    }
                )
            }
            is AppScreen.Editor -> {
                EditorScreen(
                    projectId = screen.projectId,
                    viewModel = editorViewModel,
                    onNavigateBack = {
                        currentScreen = AppScreen.Studio
                    }
                )
            }
        }
    }
}
