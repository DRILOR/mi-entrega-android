package com.example.mientrega

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
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
                PlannerApp(model)
            }
        }
    }
}
