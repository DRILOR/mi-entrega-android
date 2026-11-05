package com.example.mientrega.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mientrega.PlannerViewModel

@Composable
fun AuthScreen(model: PlannerViewModel) {
    val busy by model.busy.collectAsStateWithLifecycle()
    val message by model.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var registering by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            model.clearMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
                .padding(padding).padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null,
                modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Mi Entrega", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold)
            Text(if (registering) "Crea tu cuenta local" else "Inicia sesión",
                style = MaterialTheme.typography.titleLarge)
            Text(
                "Tus materias y actividades se guardan en este dispositivo. No necesitas internet.",
                style = MaterialTheme.typography.bodyMedium
            )

            if (registering) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(80) },
                    label = { Text("Nombre completo") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), enabled = !busy
                )
            }
            OutlinedTextField(
                value = email, onValueChange = { email = it.take(254) },
                label = { Text("Correo electrónico") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(), enabled = !busy
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it.take(128) },
                label = { Text("Contraseña") }, singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = { showPassword = !showPassword }) {
                        Text(if (showPassword) "Ocultar" else "Ver")
                    }
                },
                modifier = Modifier.fillMaxWidth(), enabled = !busy
            )
            if (registering) {
                OutlinedTextField(
                    value = confirmation, onValueChange = { confirmation = it.take(128) },
                    label = { Text("Confirmar contraseña") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(), enabled = !busy
                )
                Text("Mínimo 8 caracteres. La contraseña se guarda mediante un hash con sal.",
                    style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = {
                    if (registering) model.register(name, email, password, confirmation)
                    else model.login(email, password)
                },
                enabled = !busy, modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "Espera…" else if (registering) "Crear cuenta" else "Entrar") }

            TextButton(onClick = {
                registering = !registering
                password = ""
                confirmation = ""
                showPassword = false
                model.clearMessage()
            }, enabled = !busy) {
                Text(if (registering) "¿Ya tienes cuenta? Inicia sesión" else "¿No tienes cuenta? Regístrate")
            }
        }
    }
}
