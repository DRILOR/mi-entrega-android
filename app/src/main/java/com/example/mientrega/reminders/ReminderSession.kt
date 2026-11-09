package com.example.mientrega.reminders

import android.content.Context

/**
 * Acceso a la cuenta activa de la sesión local.
 *
 * Se define como interfaz para poder inyectar una sesión falsa en las pruebas
 * del Worker sin tocar las preferencias reales de la aplicación.
 */
fun interface ReminderSession {
    /** ID del estudiante con sesión iniciada, o `null` si no hay sesión. */
    fun activeUserId(): Long?
}

/**
 * Implementación que lee la misma sesión local usada por [PlannerViewModel].
 */
class SharedPreferencesReminderSession(
    context: Context
) : ReminderSession {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    override fun activeUserId(): Long? =
        preferences.getLong(KEY_ACTIVE_USER_ID, NO_ACTIVE_USER)
            .takeIf { it > 0L }

    companion object {
        const val PREFS_NAME = "mi-entrega-session"
        const val KEY_ACTIVE_USER_ID = "active_user_id"

        private const val NO_ACTIVE_USER = -1L
    }
}
