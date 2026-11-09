package com.example.mientrega.reminders

import android.content.Context
import com.example.mientrega.data.PlannerDao
import com.example.mientrega.data.PlannerDatabase

/**
 * Localizador mínimo de dependencias para el Worker de recordatorios.
 *
 * WorkManager instancia los Worker con el constructor estándar
 * `(Context, WorkerParameters)`, por lo que no se pueden inyectar dependencias
 * por constructor. Este objeto permite que producción use Room, notificaciones
 * y preferencias reales, mientras que las pruebas pueden sustituirlos por
 * implementaciones falsas.
 */
object ReminderDependencies {

    private data class Graph(
        val dao: PlannerDao,
        val notifier: ReminderNotifier,
        val preferences: ReminderPreferences,
        val session: ReminderSession
    )

    @Volatile
    private var graph: Graph? = null

    /**
     * Inicializa las dependencias reales de la aplicación.
     * Es idempotente: las llamadas posteriores no reemplazan el grafo actual.
     */
    @Synchronized
    fun initialize(context: Context) {
        if (graph != null) return

        val appContext = context.applicationContext
        graph = Graph(
            dao = PlannerDatabase.get(appContext).dao(),
            notifier = AndroidReminderNotifier(appContext),
            preferences = SharedPreferencesReminderPreferences(appContext),
            session = SharedPreferencesReminderSession(appContext)
        )
    }

    /**
     * Inicializa dependencias explícitas. Pensado para pruebas de
     * instrumentación con Room en memoria y un notificador falso.
     */
    @Synchronized
    fun initialize(
        dao: PlannerDao,
        notifier: ReminderNotifier,
        preferences: ReminderPreferences,
        session: ReminderSession
    ) {
        graph = Graph(dao, notifier, preferences, session)
    }

    val dao: PlannerDao
        get() = requireGraph().dao

    val notifier: ReminderNotifier
        get() = requireGraph().notifier

    val preferences: ReminderPreferences
        get() = requireGraph().preferences

    val session: ReminderSession
        get() = requireGraph().session

    /** Limpia el grafo para aislar pruebas entre sí. */
    @Synchronized
    fun reset() {
        graph = null
    }

    private fun requireGraph(): Graph = checkNotNull(graph) {
        "ReminderDependencies no se ha inicializado. " +
            "Llama a ReminderDependencies.initialize(context) antes de usar el Worker."
    }
}
