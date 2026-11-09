package com.example.mientrega.reminders

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import java.time.LocalDate

/**
 * Publica el recordatorio de una entrega.
 *
 * El Worker no confía en los datos que se enviaron al programar el trabajo:
 * vuelve a leer la actividad desde Room y valida propietario, sesión, estado,
 * fecha y materia antes de notificar. Así se evita mostrar avisos obsoletos,
 * duplicados o pertenecientes a otra cuenta.
 */
class AssignmentReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        runReminder()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        // Error transitorio de Room o del sistema: WorkManager reintentará.
        Result.retry()
    }

    private suspend fun runReminder(): Result {
        ReminderDependencies.initialize(applicationContext)

        val assignmentId = inputData.getLong(KEY_ASSIGNMENT_ID, -1L)
        val ownerId = inputData.getLong(KEY_OWNER_ID, -1L)
        val expectedDueDate = inputData.getLong(KEY_EXPECTED_DUE_DATE, Long.MIN_VALUE)
        if (assignmentId <= 0L || ownerId <= 0L || expectedDueDate == Long.MIN_VALUE) {
            return Result.success()
        }

        if (!ReminderDependencies.preferences.enabled.value) {
            return Result.success()
        }
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            return Result.success()
        }
        if (ReminderDependencies.session.activeUserId() != ownerId) {
            return Result.success()
        }

        val dao = ReminderDependencies.dao
        val assignment = dao.assignmentById(assignmentId, ownerId)
            ?: return Result.success()

        if (assignment.ownerId != ownerId) return Result.success()
        if (assignment.status == COMPLETED_STATUS) return Result.success()
        if (assignment.dueDate != expectedDueDate) return Result.success()

        // Si el dispositivo estuvo apagado y el trabajo llega después de la
        // fecha de entrega, ya no tiene sentido avisar.
        if (LocalDate.now().isAfter(LocalDate.ofEpochDay(assignment.dueDate))) {
            return Result.success()
        }

        val subjectName = dao.subjectName(assignment.subjectId, ownerId)
            ?: return Result.success()

        ReminderDependencies.notifier.show(assignment, subjectName)

        // Revalidación final para cubrir la carrera entre leer y publicar:
        // si la actividad se completó, editó o eliminó mientras tanto, se
        // retira la notificación recién mostrada.
        val current = dao.assignmentById(assignmentId, ownerId)
        if (current == null ||
            current.status == COMPLETED_STATUS ||
            current.dueDate != expectedDueDate
        ) {
            ReminderDependencies.notifier.cancel(ownerId, assignmentId)
        }

        return Result.success()
    }

    companion object {
        const val KEY_ASSIGNMENT_ID = "assignment_id"
        const val KEY_OWNER_ID = "owner_id"
        const val KEY_EXPECTED_DUE_DATE = "expected_due_date"

        private const val COMPLETED_STATUS = "Completada"
    }
}
