package com.example.mientrega.reminders

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.mientrega.data.Assignment
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Programa y cancela los recordatorios de entregas.
 *
 * La interfaz permite sustituir WorkManager por otra implementación
 * (por ejemplo, AlarmManager) sin cambiar el ViewModel ni el coordinador.
 */
interface ReminderScheduler {
    /**
     * Programa o reemplaza el recordatorio de una actividad.
     *
     * [reminderInstant] lo calcula el coordinador para que el respaldo de las
     * entregas de hoy use siempre la misma hora acordada.
     */
    fun schedule(assignment: Assignment, reminderInstant: Instant)

    /** Cancela el recordatorio de una actividad concreta. */
    fun cancel(ownerId: Long, assignmentId: Long)

    /** Cancela todos los recordatorios de una cuenta. */
    fun cancelAllFor(ownerId: Long)

    /** Cancela todos los recordatorios de todas las cuentas. */
    fun cancelAll()
}

/**
 * Implementación con WorkManager.
 *
 * Cada actividad tiene un único trabajo identificado por
 * [ReminderPolicy.workName]. Al editar una actividad se usa REPLACE para que
 * no queden dos avisos programados para la misma entrega.
 *
 * [clock] se inyecta para poder probar el retraso sin depender del reloj real
 * del dispositivo.
 */
class WorkManagerReminderScheduler(
    private val workManager: WorkManager,
    private val clock: Clock = Clock.systemDefaultZone()
) : ReminderScheduler {

    constructor(context: Context) : this(WorkManager.getInstance(context.applicationContext))

    override fun schedule(assignment: Assignment, reminderInstant: Instant) {
        val ownerId = assignment.ownerId ?: return
        val workName = ReminderPolicy.workName(ownerId, assignment.id)

        if (assignment.status == COMPLETED_STATUS) {
            workManager.cancelUniqueWork(workName)
            return
        }

        val delayMillis = Duration.between(clock.instant(), reminderInstant)
            .toMillis()
            .coerceAtLeast(0L)

        val request = OneTimeWorkRequestBuilder<AssignmentReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    AssignmentReminderWorker.KEY_ASSIGNMENT_ID to assignment.id,
                    AssignmentReminderWorker.KEY_OWNER_ID to ownerId,
                    AssignmentReminderWorker.KEY_EXPECTED_DUE_DATE to assignment.dueDate
                )
            )
            .addTag(TAG_ALL_REMINDERS)
            .addTag(ReminderPolicy.ownerTag(ownerId))
            .build()

        workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel(ownerId: Long, assignmentId: Long) {
        workManager.cancelUniqueWork(ReminderPolicy.workName(ownerId, assignmentId))
    }

    override fun cancelAllFor(ownerId: Long) {
        workManager.cancelAllWorkByTag(ReminderPolicy.ownerTag(ownerId))
    }

    override fun cancelAll() {
        workManager.cancelAllWorkByTag(TAG_ALL_REMINDERS)
    }

    companion object {
        /** Etiqueta común para poder cancelar todos los recordatorios. */
        const val TAG_ALL_REMINDERS = "assignment-reminders"

        private const val COMPLETED_STATUS = "Completada"
    }
}
