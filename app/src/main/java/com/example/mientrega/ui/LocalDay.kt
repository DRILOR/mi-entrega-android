package com.example.mientrega.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

/** Usa días civiles, igual que dueDate; se refresca a medianoche y al volver al primer plano. */
@Composable
internal fun rememberLocalDay(): Long {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var today by remember { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var clockRevision by remember { mutableIntStateOf(0) }
    DisposableEffect(context, lifecycle) {
        fun refresh() {
            today = LocalDate.now().toEpochDay()
            clockRevision++
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) { refresh() }
        }
        // Solo broadcasts protegidos del sistema; compatible con minSdk 26.
        context.registerReceiver(receiver, IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        })
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycle.addObserver(observer)
        onDispose {
            context.unregisterReceiver(receiver)
            lifecycle.removeObserver(observer)
        }
    }
    LaunchedEffect(today, clockRevision) {
        val now = ZonedDateTime.now()
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        delay(Duration.between(now, midnight).toMillis().coerceAtLeast(1L))
        today = LocalDate.now().toEpochDay()
        clockRevision++
    }
    return today
}
