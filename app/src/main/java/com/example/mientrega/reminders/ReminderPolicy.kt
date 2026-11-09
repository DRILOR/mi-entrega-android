package com.example.mientrega.reminders

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Reglas puras de los recordatorios de entregas.
 *
 * No depende de Android ni de Room: recibe la fecha civil de la actividad
 * (`dueDate` en epochDay), el reloj y la zona horaria para poder probarse
 * de forma determinista en JVM.
 */
object ReminderPolicy {
    /** Días de antelación con los que se avisa antes de la entrega. */
    const val REMINDER_DAYS_BEFORE: Long = 1

    /** Hora local a la que se programa el recordatorio. */
    const val REMINDER_HOUR: Int = 20

    /** Minuto local al que se programa el recordatorio. */
    const val REMINDER_MINUTE: Int = 0

    private val reminderTime: LocalTime = LocalTime.of(REMINDER_HOUR, REMINDER_MINUTE)

    /**
     * Devuelve el instante en el que debe sonar el recordatorio de una entrega:
     * el día anterior a [dueDate] a las 20:00 en [zoneId].
     *
     * Devuelve `null` si ese instante ya pasó o coincide con [clock]; en ese
     * caso no tiene sentido programar un trabajo.
     *
     * @param dueDate día civil de entrega, tal como se guarda en `Assignment.dueDate`.
     * @param clock reloj inyectable para pruebas.
     * @param zoneId zona horaria del dispositivo.
     */
    fun reminderInstant(dueDate: Long, clock: Clock, zoneId: ZoneId): Instant? {
        val target = LocalDate.ofEpochDay(dueDate)
            .minusDays(REMINDER_DAYS_BEFORE)
            .atTime(reminderTime)
            .atZone(zoneId)
            .toInstant()

        return target.takeIf { it.isAfter(clock.instant()) }
    }

    /**
     * Nombre único de WorkManager para el recordatorio de una actividad.
     * Incluye el propietario para que dos cuentas no compartan trabajo.
     */
    fun workName(ownerId: Long, assignmentId: Long): String =
        "assignment-reminder-$ownerId-$assignmentId"

    /**
     * Etiqueta para cancelar todos los recordatorios de una cuenta.
     */
    fun ownerTag(ownerId: Long): String =
        "assignment-reminders-owner-$ownerId"

    /**
     * Prefijo común de las notificaciones de una cuenta.
     */
    fun notificationTagPrefix(ownerId: Long): String =
        "assignment-$ownerId-"

    /**
     * Tag de la notificación. Junto con [notificationId] forma una identidad
     * única en Android, incluso si dos IDs numéricos llegaran a coincidir.
     */
    fun notificationTag(ownerId: Long, assignmentId: Long): String =
        "${notificationTagPrefix(ownerId)}$assignmentId"

    /**
     * ID numérico estable derivado del ID de la actividad.
     *
     * Se mezclan los 32 bits altos y bajos del `Long` y se fuerza un valor no
     * negativo. La unicidad real la garantiza el tag de la notificación.
     */
    fun notificationId(assignmentId: Long): Int =
        (assignmentId xor (assignmentId ushr 32)).toInt() and Int.MAX_VALUE
}
