package com.example.mientrega

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mientrega.ui.AuthScreen
import com.example.mientrega.ui.PlannerApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val colors = if (isSystemInDarkTheme()) darkColorScheme(primary = Color(0xFFBBB6FF))
                else lightColorScheme(primary = Color(0xFF4743C7), secondary = Color(0xFF087F8C), background = Color(0xFFF8F8FF))
            MaterialTheme(colorScheme = colors) {
                val model: PlannerViewModel = viewModel()
                val ready by model.authReady.collectAsStateWithLifecycle()
                val userId by model.activeUserId.collectAsStateWithLifecycle()
                when {
                    !ready -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    userId == null -> AuthScreen(model)
                    else -> key(userId) { PlannerApp(model) }
                }
            }
        }
    }
}
