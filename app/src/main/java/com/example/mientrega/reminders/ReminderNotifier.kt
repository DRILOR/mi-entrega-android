package com.example.mientrega.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mientrega.MainActivity
import com.example.mientrega.R
import com.example.mientrega.data.Assignment
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Publica y cancela los recordatorios de entregas.
 *
 * Se define como interfaz para poder usar una implementación falsa en las
 * pruebas del Worker y del coordinador.
 */
interface ReminderNotifier {
    /**
     * Muestra la notificación de una actividad.
     *
     * @param assignment actividad persistida, con `ownerId` no nulo.
     * @param subjectName nombre actual de la materia.
     */
    fun show(assignment: Assignment, subjectName: String)

    /**
     * Cancela la notificación de una actividad concreta.
     */
    fun cancel(ownerId: Long, assignmentId: Long)

    /**
     * Cancela todas las notificaciones mostradas de una cuenta.
     *
     * Se usa al desactivar los recordatorios y al cerrar sesión, para que no
     * queden avisos visibles de una cuenta inactiva.
     */
    fun cancelAllFor(ownerId: Long)
}

/**
 * Implementación con notificaciones del sistema.
 *
 * Crea el canal una sola vez al construirse y comprueba en cada aviso que las
 * notificaciones sigan habilitadas, ya que el usuario puede revocarlas desde
 * los ajustes del sistema en cualquier momento.
 */
class AndroidReminderNotifier(
    context: Context
) : ReminderNotifier {

    private val appContext: Context = context.applicationContext
    private val notificationManager = NotificationManagerCompat.from(appContext)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    init {
        createChannelIfNeeded()
    }

    override fun show(assignment: Assignment, subjectName: String) {
        if (!notificationManager.areNotificationsEnabled()) return

        val ownerId = assignment.ownerId ?: return
        val notificationId = ReminderPolicy.notificationId(assignment.id)
        val tag = ReminderPolicy.notificationTag(ownerId, assignment.id)
        val dueDate = LocalDate.ofEpochDay(assignment.dueDate)

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_assignment_reminder)
            .setContentTitle(titleFor(assignment, dueDate))
            .setContentText("$subjectName · vence el ${dueDate.format(dateFormatter)}")
            .setContentIntent(openAppIntent(assignment, notificationId))
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)
            .build()

        try {
            notificationManager.notify(tag, notificationId, notification)
        } catch (_: SecurityException) {
            // El permiso pudo revocarse entre la comprobación y notify().
        }
    }

    override fun cancel(ownerId: Long, assignmentId: Long) {
        notificationManager.cancel(
            ReminderPolicy.notificationTag(ownerId, assignmentId),
            ReminderPolicy.notificationId(assignmentId)
        )
    }

    override fun cancelAllFor(ownerId: Long) {
        val manager = appContext.getSystemService(NotificationManager::class.java) ?: return
        val prefix = ReminderPolicy.notificationTagPrefix(ownerId)

        // El sistema no permite cancelar por prefijo de tag, así que se
        // consultan las notificaciones activas y se filtran por propietario.
        manager.activeNotifications
            .filter { it.tag?.startsWith(prefix) == true }
            .forEach { manager.cancel(it.tag, it.id) }
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos de las próximas entregas de actividades."
        }

        appContext.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    private fun titleFor(assignment: Assignment, dueDate: LocalDate): String {
        val daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), dueDate)
        return when (daysUntilDue) {
            1L -> "Entrega mañana: ${assignment.title}"
            0L -> "Entrega hoy: ${assignment.title}"
            else -> "Entrega: ${assignment.title}"
        }
    }

    private fun openAppIntent(assignment: Assignment, requestCode: Int): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OWNER_ID, assignment.ownerId)
            putExtra(EXTRA_ASSIGNMENT_ID, assignment.id)
        }

        return PendingIntent.getActivity(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_ID = "assignment_reminders"
        const val CHANNEL_NAME = "Recordatorios de entregas"

        /** Extra para que MainActivity pueda abrir la actividad recordada. */
        const val EXTRA_ASSIGNMENT_ID = "extra_assignment_id"

        /** Extra de integridad para validar la cuenta al abrir la notificación. */
        const val EXTRA_OWNER_ID = "extra_owner_id"
    }
}
