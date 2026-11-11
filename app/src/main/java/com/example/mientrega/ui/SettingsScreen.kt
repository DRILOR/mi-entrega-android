package com.example.mientrega.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mientrega.PlannerViewModel
import com.example.mientrega.reminders.NotificationPermission

/**
 * Pantalla de ajustes de los recordatorios de entregas.
 *
 * El switch controla la preferencia global. Si Android 13+ no tiene el
 * permiso, se muestra el estado bloqueado y se ofrece solicitarlo o abrir los
 * ajustes del sistema cuando ya no se puede volver a preguntar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(model: PlannerViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val remindersEnabled by model.remindersEnabled.collectAsStateWithLifecycle()

    // Se incrementa al volver del diálogo o de los ajustes del sistema para
    // recalcular el estado real del permiso.
    var permissionRevision by remember { mutableIntStateOf(0) }

    val requiresRuntimePermission = remember { NotificationPermission.requiresRuntimePermission() }
    val permissionGranted = remember(permissionRevision) { NotificationPermission.isGranted(context) }
    val systemNotificationsEnabled = remember(permissionRevision) {
        NotificationPermission.areSystemNotificationsEnabled(context)
    }
    val canPostNotifications = permissionGranted && systemNotificationsEnabled
    val permanentlyDenied = remember(permissionRevision, activity) {
        NotificationPermission.isPermanentlyDenied(context, activity)
    }
    val canAskForPermission = requiresRuntimePermission &&
        !permissionGranted &&
        !permanentlyDenied

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionRevision++
        if (granted) {
            // Al conceder el permiso hay que reprogramar los avisos futuros.
            model.syncReminders()
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionRevision++
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    fun requestPermission() {
        NotificationPermission.markRequested(context)
        permissionLauncher.launch(NotificationPermission.permission)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Recordatorios de entregas",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "Recibe un aviso el día anterior a la entrega, a las 08:00 " +
                            "hora local del dispositivo. Si la entrega es para hoy, " +
                            "el aviso se programa unos minutos después de guardarla. " +
                            "Los avisos se programan solo para actividades pendientes " +
                            "de tu cuenta.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Activar recordatorios",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (remindersEnabled) {
                                "Los avisos futuros se programarán automáticamente."
                            } else {
                                "No se enviarán avisos de tus entregas."
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = { enabled ->
                            model.setRemindersEnabled(enabled)
                            if (enabled && canAskForPermission) {
                                requestPermission()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            if (canPostNotifications) {
                NotificationStatus(
                    title = "Notificaciones activas",
                    text = "La app puede mostrarte recordatorios de entregas.",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    icon = Icons.Default.Notifications
                )
            } else {
                BlockedNotificationsCard(
                    permanentlyDenied = permanentlyDenied,
                    canAskForPermission = canAskForPermission,
                    requiresRuntimePermission = requiresRuntimePermission,
                    onRequestPermission = ::requestPermission,
                    onOpenSettings = {
                        NotificationPermission.openNotificationSettings(context)
                    }
                )
            }

            Text(
                "Puedes desactivar los recordatorios en cualquier momento. " +
                    "Al hacerlo se cancelan los avisos programados de la cuenta activa.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BlockedNotificationsCard(
    permanentlyDenied: Boolean,
    canAskForPermission: Boolean,
    requiresRuntimePermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.NotificationsOff, contentDescription = null)
                Text(
                    "Notificaciones bloqueadas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                when {
                    permanentlyDenied ->
                        "El permiso fue denegado y Android ya no mostrará el diálogo. " +
                            "Habilita las notificaciones desde los ajustes del sistema."
                    !requiresRuntimePermission ->
                        "Las notificaciones de la app están desactivadas en el sistema. " +
                            "Habilítalas para recibir recordatorios."
                    else ->
                        "Concede el permiso de notificaciones para recibir los " +
                            "recordatorios de tus entregas."
                },
                style = MaterialTheme.typography.bodyMedium
            )
            if (canAskForPermission) {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Permitir notificaciones")
                }
            }
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Abrir ajustes del sistema")
            }
        }
    }
}

@Composable
private fun NotificationStatus(
    title: String,
    text: String,
    containerColor: Color,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

/** Busca la Activity que aloja el contexto de Compose. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
