package com.example.mientrega.reminders

import android.content.Context
import androidx.work.WorkManager
import com.example.mientrega.data.Assignment
import com.example.mientrega.data.PlannerDao
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * Fuente de datos que necesita el coordinador.
 *
 * Se define como interfaz para que las pruebas JVM puedan usar un doble en
 * memoria sin construir un `PlannerDao` completo. En producción se adapta el
 * DAO real mediante [DaoReminderAssignmentSource].
 */
interface ReminderAssignmentSource {
    /** Actividad concreta validada por propietario. */
    suspend fun byId(assignmentId: Long, ownerId: Long): Assignment?

    /** Actividades no completadas del propietario con fecha >= [fromDay]. */
    suspend fun pending(ownerId: Long, fromDay: Long): List<Assignment>
}

/** Adaptador entre `PlannerDao` y [ReminderAssignmentSource]. */
class DaoReminderAssignmentSource(
    private val dao: PlannerDao
) : ReminderAssignmentSource {

    override suspend fun byId(assignmentId: Long, ownerId: Long): Assignment? =
        dao.assignmentById(assignmentId, ownerId)

    override suspend fun pending(ownerId: Long, fromDay: Long): List<Assignment> =
        dao.pendingForReminders(ownerId, fromDay)
}

/**
 * Orquesta el ciclo de vida de los recordatorios.
 *
 * El coordinador concentra las decisiones de negocio para que el ViewModel no
 * tenga que conocer WorkManager ni el notificador. Toda operación es
 * *best-effort*: el llamador debe envolverla en su propio `try/catch` para que
 * un fallo de recordatorios no convierta una escritura correcta en Room en un
 * error visible para el usuario.
 *
 * @param assignments fuente de actividades (DAO real o fake en pruebas).
 * @param scheduler programa y cancela trabajos.
 * @param preferences toggle global persistente.
 * @param notifier publica y cancela notificaciones.
 * @param clock reloj inyectable para calcular qué recordatorios siguen vigentes.
 * @param zoneId zona horaria con la que se interpreta `dueDate`.
 */
class ReminderCoordinator(
    private val assignments: ReminderAssignmentSource,
    private val scheduler: ReminderScheduler,
    private val preferences: ReminderPreferences,
    private val notifier: ReminderNotifier,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {

    /**
     * Constructor cómodo para producción: adapta directamente el DAO de Room.
     */
    constructor(
        dao: PlannerDao,
        scheduler: ReminderScheduler,
        preferences: ReminderPreferences,
        notifier: ReminderNotifier,
        clock: Clock = Clock.systemDefaultZone(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ) : this(
        DaoReminderAssignmentSource(dao),
        scheduler,
        preferences,
        notifier,
        clock,
        zoneId
    )

    /**
     * Sincroniza todos los recordatorios de una cuenta.
     *
     * Si el toggle está apagado, cancela trabajos y notificaciones mostradas.
     * Si está encendido, reprograma desde cero las actividades pendientes:
     * primero cancela los trabajos de la cuenta y luego vuelve a encolar solo
     * los que todavía aplican. Es simple y garantiza que no queden trabajos
     * obsoletos tras cambios de fecha, hora o zona horaria.
     */
    suspend fun sync(ownerId: Long) {
        if (ownerId <= 0L) {
            scheduler.cancelAll()
            return
        }

        if (!preferences.enabled.value) {
            scheduler.cancelAllFor(ownerId)
            notifier.cancelAllFor(ownerId)
            return
        }

        val fromDay = LocalDate.ofInstant(clock.instant(), zoneId)
            .minusDays(1)
            .toEpochDay()

        val pending = assignments.pending(ownerId, fromDay)
            .filter { it.ownerId == ownerId }
            .filter { it.status != COMPLETED_STATUS }
            .filter { ReminderPolicy.reminderInstant(it.dueDate, clock, zoneId) != null }

        scheduler.cancelAllFor(ownerId)
        pending.forEach(scheduler::schedule)
    }

    /**
     * Reacciona a una actividad creada o editada.
     *
     * Relee la fila persistida para no confiar en el objeto del formulario y
     * decide entre programar o cancelar. Si la actividad ya no existe, está
     * completada, cambió a una fecha sin recordatorio o el toggle está apagado,
     * se elimina cualquier trabajo o notificación previa.
     */
    suspend fun onAssignmentSaved(assignmentId: Long, ownerId: Long) {
        if (assignmentId <= 0L || ownerId <= 0L) return

        val assignment = assignments.byId(assignmentId, ownerId)
        val eligible = assignment != null &&
            assignment.ownerId == ownerId &&
            assignment.status != COMPLETED_STATUS &&
            preferences.enabled.value &&
            ReminderPolicy.reminderInstant(assignment.dueDate, clock, zoneId) != null

        if (eligible) {
            scheduler.schedule(assignment!!)
        } else {
            scheduler.cancel(ownerId, assignmentId)
            notifier.cancel(ownerId, assignmentId)
        }
    }

    /** Cancela el recordatorio de una actividad completada. */
    suspend fun onCompleted(ownerId: Long, assignmentId: Long) {
        cancelReminder(ownerId, assignmentId)
    }

    /** Cancela el recordatorio de una actividad eliminada. */
    suspend fun onDeleted(ownerId: Long, assignmentId: Long) {
        cancelReminder(ownerId, assignmentId)
    }

    /**
     * Aplica el toggle global.
     *
     * Guarda la preferencia antes de tocar WorkManager para que el Worker, si
     * llegara a ejecutarse durante la transición, vea ya el estado correcto.
     * Al desactivar se cancelan trabajos y notificaciones de la cuenta activa;
     * al activar se reprograman los pendientes.
     */
    suspend fun setEnabled(enabled: Boolean, ownerId: Long? = null) {
        preferences.setEnabled(enabled)

        if (!enabled) {
            if (ownerId != null && ownerId > 0L) {
                scheduler.cancelAllFor(ownerId)
                notifier.cancelAllFor(ownerId)
            } else {
                scheduler.cancelAll()
            }
            return
        }

        if (ownerId != null && ownerId > 0L) {
            sync(ownerId)
        }
    }

    /**
     * Limpia los recordatorios de una cuenta al cerrar sesión.
     *
     * No toca la preferencia global: el usuario conserva su elección para la
     * próxima vez que inicie sesión.
     */
    suspend fun onLogout(ownerId: Long) {
        if (ownerId <= 0L) {
            scheduler.cancelAll()
            return
        }
        scheduler.cancelAllFor(ownerId)
        notifier.cancelAllFor(ownerId)
    }

    private fun cancelReminder(ownerId: Long, assignmentId: Long) {
        if (ownerId <= 0L || assignmentId <= 0L) return
        scheduler.cancel(ownerId, assignmentId)
        notifier.cancel(ownerId, assignmentId)
    }

    companion object {
        private const val COMPLETED_STATUS = "Completada"

        /**
         * Crea el coordinador con las dependencias reales de la aplicación.
         * Pensado para usarse desde el ViewModel.
         */
        fun from(
            context: Context,
            clock: Clock = Clock.systemDefaultZone(),
            zoneId: ZoneId = ZoneId.systemDefault()
        ): ReminderCoordinator {
            ReminderDependencies.initialize(context)
            val appContext = context.applicationContext

            return ReminderCoordinator(
                assignments = DaoReminderAssignmentSource(ReminderDependencies.dao),
                scheduler = WorkManagerReminderScheduler(
                    WorkManager.getInstance(appContext),
                    clock,
                    zoneId
                ),
                preferences = ReminderDependencies.preferences,
                notifier = ReminderDependencies.notifier,
                clock = clock,
                zoneId = zoneId
            )
        }
    }
}
