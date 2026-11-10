package com.example.mientrega.ui

import com.example.mientrega.reminders.ReminderPolicy
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Minutos máximos para presentar el aviso como "hoy en N minutos". */
internal const val IMMINENT_REMINDER_THRESHOLD_MINUTES: Long = 15

/** Texto mostrado cuando la política no programará ningún aviso. */
internal const val REMINDER_PREVIEW_WARNING =
    "⚠️ La fecha ya pasó o no se programará aviso"

private val previewDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val previewTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Texto informativo que se muestra bajo el selector de fecha del formulario.
 *
 * Reutiliza [ReminderPolicy.reminderInstant] para no prometer un aviso que no
 * se programará. Cuando el instante está a menos de
 * [IMMINENT_REMINDER_THRESHOLD_MINUTES] minutos se usa un texto más cercano
 * ("hoy en N minutos") porque corresponde al respaldo de una entrega de hoy.
 *
 * @return `null` si todavía no hay una fecha seleccionada.
 */
internal fun reminderPreviewMessage(
    dueDate: Long?,
    clock: Clock = Clock.systemDefaultZone(),
    zoneId: ZoneId = ZoneId.systemDefault()
): String? {
    if (dueDate == null) return null

    val now = clock.instant()
    val reminder = ReminderPolicy.reminderInstant(dueDate, clock, zoneId)
        ?: return REMINDER_PREVIEW_WARNING

    val localReminder = reminder.atZone(zoneId)
    val wait = Duration.between(now, reminder)
    return if (!wait.isNegative && wait < Duration.ofMinutes(IMMINENT_REMINDER_THRESHOLD_MINUTES)) {
        val minutes = minutesUntil(now, reminder)
        val unit = if (minutes == 1L) "minuto" else "minutos"
        "🔔 Te recordaremos hoy en $minutes $unit " +
            "(a las ${localReminder.format(previewTimeFormatter)})"
    } else {
        "🔔 Te recordaremos el ${localReminder.format(previewDateFormatter)} " +
            "a las ${localReminder.format(previewTimeFormatter)}"
    }
}

/**
 * Minutos completos o fracción restante redondeada hacia arriba, para que un
 * respaldo de exactamente 10 minutos no se muestre como "9 minutos".
 */
private fun minutesUntil(from: Instant, to: Instant): Long {
    val millis = Duration.between(from, to).toMillis()
    if (millis <= 0L) return 0L
    return (millis + 59_999L) / 60_000L
}
