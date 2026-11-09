package com.example.mientrega.reminders

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Utilidades para el permiso `POST_NOTIFICATIONS` y el estado de las
 * notificaciones del sistema.
 *
 * En Android 13+ el permiso es runtime. En versiones anteriores no existe,
 * pero el usuario puede desactivar las notificaciones de la app desde los
 * ajustes del sistema; ambos casos se tratan como "no se puede notificar".
 */
object NotificationPermission {

    /** Permiso runtime de Android 13+. */
    val permission: String = Manifest.permission.POST_NOTIFICATIONS

    fun requiresRuntimePermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** Indica si el permiso runtime está concedido (siempre true en API < 33). */
    fun isGranted(context: Context): Boolean =
        !requiresRuntimePermission() ||
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED

    /** Indica si el usuario no desactivó las notificaciones de la app. */
    fun areSystemNotificationsEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context.applicationContext)
            .areNotificationsEnabled()

    /** True solo cuando se puede publicar una notificación. */
    fun canPost(context: Context): Boolean =
        isGranted(context) && areSystemNotificationsEnabled(context)

    /**
     * El sistema recomienda mostrar una explicación antes de volver a pedir
     * el permiso.
     */
    fun shouldShowRationale(activity: Activity): Boolean =
        requiresRuntimePermission() &&
            activity.shouldShowRequestPermissionRationale(permission)

    /** Indica si la app ya lanzó el diálogo de permiso al menos una vez. */
    fun hasBeenRequested(context: Context): Boolean =
        preferences(context).getBoolean(KEY_REQUESTED, false)

    /** Marca que se lanzó el diálogo; debe llamarse justo antes de lanzarlo. */
    fun markRequested(context: Context) {
        preferences(context).edit().putBoolean(KEY_REQUESTED, true).apply()
    }

    /**
     * Heurística para detectar una denegación permanente.
     *
     * Android no expone una API directa: se considera permanente cuando el
     * permiso no está concedido, ya se pidió antes y el sistema ya no muestra
     * la explicación ni el diálogo.
     */
    fun isPermanentlyDenied(context: Context, activity: Activity?): Boolean =
        requiresRuntimePermission() &&
            !isGranted(context) &&
            hasBeenRequested(context) &&
            (activity == null || !shouldShowRationale(activity))

    /** Abre los ajustes de notificaciones de la app en el sistema. */
    fun openNotificationSettings(context: Context) {
        val appContext = context.applicationContext
        val notificationSettings = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            appContext.startActivity(notificationSettings)
        } catch (_: ActivityNotFoundException) {
            // Algunos fabricantes no exponen la pantalla de notificaciones.
            appContext.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${appContext.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(
            SharedPreferencesReminderPreferences.PREFS_NAME,
            Context.MODE_PRIVATE
        )

    private const val KEY_REQUESTED = "notifications_permission_requested"
}
