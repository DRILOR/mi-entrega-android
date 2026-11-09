package com.example.mientrega.reminders

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferencias de los recordatorios de entregas.
 *
 * Se mantiene como interfaz para poder sustituirla por una implementación
 * falsa en las pruebas del coordinador y del Worker.
 */
interface ReminderPreferences {
    /** Indica si el usuario activó los recordatorios. */
    val enabled: StateFlow<Boolean>

    /** Guarda la preferencia y actualiza el flujo observable por Compose. */
    fun setEnabled(enabled: Boolean)
}

/**
 * Implementación con [SharedPreferences], consistente con la sesión local
 * existente en el proyecto.
 *
 * Por defecto los recordatorios están desactivados (opt-in): el usuario debe
 * activarlos explícitamente desde Ajustes.
 *
 * @param context contexto de la aplicación.
 * @param preferencesName archivo de preferencias; se puede aislar en pruebas.
 */
class SharedPreferencesReminderPreferences(
    context: Context,
    preferencesName: String = PREFS_NAME
) : ReminderPreferences {

    private val preferences: SharedPreferences = context.applicationContext.getSharedPreferences(
        preferencesName,
        Context.MODE_PRIVATE
    )

    private val _enabled = MutableStateFlow(
        preferences.getBoolean(KEY_ENABLED, DEFAULT_ENABLED)
    )

    override val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    override fun setEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _enabled.value = enabled
    }

    companion object {
        /** Mismo dominio de preferencias que usa la app para ajustes locales. */
        const val PREFS_NAME = "mi-entrega-settings"

        /** Clave booleana del toggle de recordatorios. */
        const val KEY_ENABLED = "reminders_enabled"

        /** Los recordatorios son opt-in. */
        const val DEFAULT_ENABLED = false
    }
}
