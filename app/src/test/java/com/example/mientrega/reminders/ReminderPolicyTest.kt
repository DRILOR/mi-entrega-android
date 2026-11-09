package com.example.mientrega.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderPolicyTest {

    private val zone: ZoneId = ZoneId.of("America/La_Paz")
    private val dueDate: Long = LocalDate.of(2026, 11, 20).toEpochDay()
    private val expectedReminder: Instant =
        ZonedDateTime.of(2026, 11, 19, 8, 0, 0, 0, zone).toInstant()
    private val fallbackDelay: Duration =
        Duration.ofMinutes(ReminderPolicy.FALLBACK_DELAY_MINUTES)

    @Test
    fun reminderHourIsEightAm() {
        assertEquals(8, ReminderPolicy.REMINDER_HOUR)
    }

    @Test
    fun futureDueDateReturnsReminderOneDayBeforeAtEightAm() {
        val clock = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), zone)

        val result = ReminderPolicy.reminderInstant(dueDate, clock, zone)

        assertEquals(expectedReminder, result)
    }

    @Test
    fun dueDateTodayReturnsFallbackTenMinutesFromNow() {
        val now = Instant.parse("2026-11-18T12:00:00Z")
        val today = LocalDate.of(2026, 11, 18).toEpochDay()
        val clock = Clock.fixed(now, zone)

        val result = ReminderPolicy.reminderInstant(today, clock, zone)

        assertEquals(now.plus(fallbackDelay), result)
    }

    @Test
    fun dueDateTodayReturnsFallbackEvenAfterTheStandardReminderTime() {
        val now = Instant.parse("2026-11-18T23:30:00Z")
        val today = LocalDate.of(2026, 11, 18).toEpochDay()
        val clock = Clock.fixed(now, zone)

        val result = ReminderPolicy.reminderInstant(today, clock, zone)

        assertEquals(now.plus(fallbackDelay), result)
    }

    @Test
    fun standardReminderAlreadyPassedButDueDateIsFutureReturnsFallback() {
        val now = expectedReminder.plusNanos(1)
        val clock = Clock.fixed(now, zone)

        val result = ReminderPolicy.reminderInstant(dueDate, clock, zone)

        assertEquals(now.plus(fallbackDelay), result)
    }

    @Test
    fun savedFallbackIsReusedWhenItIsStillInTheFuture() {
        val now = Instant.parse("2026-11-18T12:00:00Z")
        val savedFallback = now.plus(Duration.ofMinutes(3))
        val today = LocalDate.of(2026, 11, 18).toEpochDay()

        val result = ReminderPolicy.reminderInstant(
            today,
            Clock.fixed(now, zone),
            zone,
            savedFallback
        )

        assertEquals(savedFallback, result)
    }

    @Test
    fun expiredFallbackIsRecalculated() {
        val now = Instant.parse("2026-11-18T12:00:00Z")
        val expiredFallback = now.minus(Duration.ofMinutes(1))
        val today = LocalDate.of(2026, 11, 18).toEpochDay()

        val result = ReminderPolicy.reminderInstant(
            today,
            Clock.fixed(now, zone),
            zone,
            expiredFallback
        )

        assertEquals(now.plus(fallbackDelay), result)
    }

    @Test
    fun pastDueDateReturnsNull() {
        val pastDueDate = LocalDate.of(2026, 11, 10).toEpochDay()
        val clock = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), zone)

        val result = ReminderPolicy.reminderInstant(pastDueDate, clock, zone)

        assertNull(result)
    }

    @Test
    fun dueDateYesterdayReturnsNull() {
        val now = Instant.parse("2026-11-18T12:00:00Z")
        val yesterday = LocalDate.of(2026, 11, 17).toEpochDay()
        val clock = Clock.fixed(now, zone)

        val result = ReminderPolicy.reminderInstant(yesterday, clock, zone)

        assertNull(result)
    }

    @Test
    fun clockExactlyAtReminderTimeReturnsFallback() {
        val clock = Clock.fixed(expectedReminder, zone)

        val result = ReminderPolicy.reminderInstant(dueDate, clock, zone)

        assertEquals(expectedReminder.plus(fallbackDelay), result)
    }

    @Test
    fun clockJustBeforeReminderTimeReturnsTheReminder() {
        val clock = Clock.fixed(expectedReminder.minusNanos(1), zone)

        val result = ReminderPolicy.reminderInstant(dueDate, clock, zone)

        assertEquals(expectedReminder, result)
    }

    @Test
    fun sameAssignmentProducesConsistentNamesTagsAndIds() {
        val ownerId = 7L
        val assignmentId = 42L

        assertEquals(
            ReminderPolicy.workName(ownerId, assignmentId),
            ReminderPolicy.workName(ownerId, assignmentId)
        )
        assertEquals(
            ReminderPolicy.ownerTag(ownerId),
            ReminderPolicy.ownerTag(ownerId)
        )
        assertEquals(
            ReminderPolicy.notificationTag(ownerId, assignmentId),
            ReminderPolicy.notificationTag(ownerId, assignmentId)
        )
        assertEquals(
            ReminderPolicy.notificationId(assignmentId),
            ReminderPolicy.notificationId(assignmentId)
        )
    }

    @Test
    fun differentAssignmentsAndOwnersProduceDifferentNamesAndTags() {
        val ownerA = 7L
        val ownerB = 8L
        val assignmentA = 42L
        val assignmentB = 43L

        assertNotEquals(
            ReminderPolicy.workName(ownerA, assignmentA),
            ReminderPolicy.workName(ownerA, assignmentB)
        )
        assertNotEquals(
            ReminderPolicy.workName(ownerA, assignmentA),
            ReminderPolicy.workName(ownerB, assignmentA)
        )
        assertNotEquals(
            ReminderPolicy.ownerTag(ownerA),
            ReminderPolicy.ownerTag(ownerB)
        )
        assertNotEquals(
            ReminderPolicy.notificationTag(ownerA, assignmentA),
            ReminderPolicy.notificationTag(ownerA, assignmentB)
        )
        assertNotEquals(
            ReminderPolicy.notificationTag(ownerA, assignmentA),
            ReminderPolicy.notificationTag(ownerB, assignmentA)
        )
        assertNotEquals(
            ReminderPolicy.notificationId(assignmentA),
            ReminderPolicy.notificationId(assignmentB)
        )
    }

    @Test
    fun namesTagsAndNotificationIdsHaveTheDocumentedStableFormat() {
        assertEquals("assignment-reminder-7-42", ReminderPolicy.workName(7, 42))
        assertEquals("assignment-reminders-owner-7", ReminderPolicy.ownerTag(7))
        assertEquals("assignment-7-42", ReminderPolicy.notificationTag(7, 42))
        assertTrue(ReminderPolicy.notificationId(42) >= 0)
        assertTrue(ReminderPolicy.notificationId(Long.MAX_VALUE) >= 0)
    }
}
