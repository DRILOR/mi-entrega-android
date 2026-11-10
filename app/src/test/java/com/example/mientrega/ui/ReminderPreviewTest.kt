package com.example.mientrega.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ReminderPreviewTest {

    private val zone: ZoneId = ZoneId.of("America/La_Paz")

    @Test
    fun fallbackForTodayShowsFriendlyCountdown() {
        val now = Instant.parse("2026-11-18T12:00:00Z")
        val today = LocalDate.of(2026, 11, 18).toEpochDay()

        val result = reminderPreviewMessage(today, Clock.fixed(now, zone), zone)

        assertEquals(
            "🔔 Te recordaremos hoy en 10 minutos (a las 08:10)",
            result
        )
    }

    @Test
    fun standardReminderShowsDateAndTime() {
        val now = Instant.parse("2026-11-18T12:00:00Z")
        val dueDate = LocalDate.of(2026, 11, 20).toEpochDay()

        val result = reminderPreviewMessage(dueDate, Clock.fixed(now, zone), zone)

        assertEquals("🔔 Te recordaremos el 19/11/2026 a las 08:00", result)
    }

    @Test
    fun pastDateShowsWarning() {
        val now = Instant.parse("2026-11-18T12:00:00Z")
        val pastDueDate = LocalDate.of(2026, 11, 10).toEpochDay()

        val result = reminderPreviewMessage(pastDueDate, Clock.fixed(now, zone), zone)

        assertEquals(REMINDER_PREVIEW_WARNING, result)
    }

    @Test
    fun tomorrowWithStandardReminderAlreadyPassedShowsWarning() {
        val now = Instant.parse("2026-11-19T12:00:00Z").plusNanos(1)
        val dueDate = LocalDate.of(2026, 11, 20).toEpochDay()

        val result = reminderPreviewMessage(dueDate, Clock.fixed(now, zone), zone)

        assertEquals(REMINDER_PREVIEW_WARNING, result)
    }

    @Test
    fun noSelectedDateReturnsNoMessage() {
        val now = Instant.parse("2026-11-18T12:00:00Z")

        assertNull(reminderPreviewMessage(null, Clock.fixed(now, zone), zone))
    }
}
