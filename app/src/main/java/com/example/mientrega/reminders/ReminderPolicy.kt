package com.example.mientrega.reminders

import java.time.Clock
import java.time.Duration
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
    const val REMINDER_HOUR: Int = 8

    /** Minuto local al que se programa el recordatorio. */
    const val REMINDER_MINUTE: Int = 0

    /**
     * Minutos de espera del recordatorio de respaldo cuando el aviso estándar
     * ya pasó, pero la entrega todavía es hoy o en el futuro.
     */
    const val FALLBACK_DELAY_MINUTES: Long = 10

    private val reminderTime: LocalTime = LocalTime.of(REMINDER_HOUR, REMINDER_MINUTE)
    private val fallbackDelay: Duration = Duration.ofMinutes(FALLBACK_DELAY_MINUTES)

    /**
     * Devuelve el instante en el que debe sonar el recordatorio de una entrega.
     *
     * El aviso estándar es el día anterior a [dueDate] a las 08:00 en [zoneId].
     * Si ese instante ya pasó pero la entrega todavía no venció (es hoy o una
     * fecha futura), devuelve un respaldo a [FALLBACK_DELAY_MINUTES] minutos
     * desde [clock]. Así una actividad creada para hoy no se queda sin aviso.
     *
     * Devuelve `null` solo cuando la fecha de entrega ya pasó por completo.
     *
     * @param dueDate día civil de entrega, tal como se guarda en `Assignment.dueDate`.
     * @param clock reloj inyectable para pruebas.
     * @param zoneId zona horaria del dispositivo.
     */
    fun reminderInstant(dueDate: Long, clock: Clock, zoneId: ZoneId): Instant? {
        val now = clock.instant()
        val due = LocalDate.ofEpochDay(dueDate)
        val standardReminder = due
            .minusDays(REMINDER_DAYS_BEFORE)
            .atTime(reminderTime)
            .atZone(zoneId)
            .toInstant()

        if (standardReminder.isAfter(now)) return standardReminder

        // El aviso estándar ya pasó (o coincide con ahora). Solo se ofrece un
        // respaldo si la entrega aún no ha vencido.
        val today = now.atZone(zoneId).toLocalDate()
        if (due.isBefore(today)) return null

        return now.plus(fallbackDelay)
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
