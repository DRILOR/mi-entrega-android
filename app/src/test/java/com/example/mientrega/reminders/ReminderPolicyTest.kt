package com.example.mientrega.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderPolicyTest {

    private val zone: ZoneId = ZoneId.of("America/La_Paz")
    private val dueDate: Long = LocalDate.of(2026, 11, 20).toEpochDay()
    private val expectedReminder: Instant =
        ZonedDateTime.of(2026, 11, 19, 20, 0, 0, 0, zone).toInstant()

    @Test
    fun futureDueDateReturnsReminderOneDayBeforeAtEightPm() {
        val clock = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), zone)

        val result = ReminderPolicy.reminderInstant(dueDate, clock, zone)

        assertEquals(expectedReminder, result)
    }

    @Test
    fun pastDueDateReturnsNull() {
        val pastDueDate = LocalDate.of(2026, 11, 10).toEpochDay()
        val clock = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), zone)

        val result = ReminderPolicy.reminderInstant(pastDueDate, clock, zone)

        assertNull(result)
    }

    @Test
    fun clockExactlyAtReminderTimeReturnsNull() {
        val clock = Clock.fixed(expectedReminder, zone)

        val result = ReminderPolicy.reminderInstant(dueDate, clock, zone)

        assertNull(result)
    }

    @Test
    fun clockJustBeforeReminderTimeReturnsTheReminder() {
        val clock = Clock.fixed(expectedReminder.minusNanos(1), zone)

        val result = ReminderPolicy.reminderInstant(dueDate, clock, zone)

        assertEquals(expectedReminder, result)
    }

    @Test
    fun clockJustAfterReminderTimeReturnsNull() {
        val clock = Clock.fixed(expectedReminder.plusNanos(1), zone)

        val result = ReminderPolicy.reminderInstant(dueDate, clock, zone)

        assertNull(result)
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
