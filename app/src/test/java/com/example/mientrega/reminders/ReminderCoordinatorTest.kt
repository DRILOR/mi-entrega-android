package com.example.mientrega.reminders

import com.example.mientrega.data.Assignment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pruebas JVM del coordinador con dobles en memoria.
 *
 * No tocan Android ni Room: verifican las decisiones de negocio de los cinco
 * flujos (sync, guardado, completado, eliminación y toggle) y la limpieza al
 * cerrar sesión.
 */
class ReminderCoordinatorTest {

    private lateinit var source: FakeAssignmentSource
    private lateinit var scheduler: FakeScheduler
    private lateinit var preferences: FakePreferences
    private lateinit var notifier: FakeNotifier
    private lateinit var coordinator: ReminderCoordinator

    @Before
    fun setUp() {
        source = FakeAssignmentSource()
        scheduler = FakeScheduler()
        preferences = FakePreferences(initial = true)
        notifier = FakeNotifier()
        coordinator = ReminderCoordinator(
            assignments = source,
            scheduler = scheduler,
            preferences = preferences,
            notifier = notifier,
            clock = clock,
            zoneId = zone
        )
    }

    @Test
    fun syncSchedulesOnlyEligibleAssignmentsOfTheOwner() = runBlocking {
        source.pendingByOwner[OWNER] = listOf(
            assignment(id = 1L, dueDate = futureDue),
            assignment(id = 2L, dueDate = futureDue, status = COMPLETED),
            assignment(id = 3L, owner = OTHER_OWNER, dueDate = futureDue),
            assignment(id = 4L, dueDate = dueToday),
            assignment(id = 5L, dueDate = yesterday)
        )

        coordinator.sync(OWNER)

        // La actividad de hoy recibe el respaldo; la de ayer ya no es elegible.
        assertEquals(listOf(1L, 4L), scheduler.scheduled.map { it.id })
        assertEquals(listOf(OWNER), scheduler.cancelledAllFor)
        assertTrue(notifier.cancelledAllFor.isEmpty())
        // Se consultan pendientes desde ayer, tal como pide el plan.
        assertEquals(listOf(OWNER to yesterday), source.pendingRequests)
    }

    @Test
    fun syncCancelsWorkAndNotificationsWhenRemindersAreDisabled() = runBlocking {
        preferences.setEnabled(false)
        source.pendingByOwner[OWNER] = listOf(assignment(id = 1L, dueDate = futureDue))

        coordinator.sync(OWNER)

        assertTrue(scheduler.scheduled.isEmpty())
        assertEquals(listOf(OWNER), scheduler.cancelledAllFor)
        assertEquals(listOf(OWNER), notifier.cancelledAllFor)
        assertTrue(source.pendingRequests.isEmpty())
    }

    @Test
    fun syncWithInvalidOwnerCancelsEveryWork() = runBlocking {
        coordinator.sync(0L)

        assertEquals(1, scheduler.cancelAllCalls)
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun onAssignmentSavedSchedulesThePersistedRow() = runBlocking {
        source.byId[1L to OWNER] = assignment(id = 1L, dueDate = futureDue)

        coordinator.onAssignmentSaved(assignmentId = 1L, ownerId = OWNER)

        assertEquals(listOf(1L), scheduler.scheduled.map { it.id })
        assertTrue(scheduler.cancelled.isEmpty())
        assertTrue(notifier.cancelled.isEmpty())
    }

    @Test
    fun onAssignmentSavedCancelsWhenAssignmentIsCompleted() = runBlocking {
        source.byId[1L to OWNER] =
            assignment(id = 1L, dueDate = futureDue, status = COMPLETED)

        coordinator.onAssignmentSaved(assignmentId = 1L, ownerId = OWNER)

        assertTrue(scheduler.scheduled.isEmpty())
        assertEquals(listOf(OWNER to 1L), scheduler.cancelled)
        assertEquals(listOf(OWNER to 1L), notifier.cancelled)
    }

    @Test
    fun onAssignmentSavedCancelsWhenAssignmentWasDeleted() = runBlocking {
        coordinator.onAssignmentSaved(assignmentId = 1L, ownerId = OWNER)

        assertTrue(scheduler.scheduled.isEmpty())
        assertEquals(listOf(OWNER to 1L), scheduler.cancelled)
        assertEquals(listOf(OWNER to 1L), notifier.cancelled)
    }

    @Test
    fun onAssignmentSavedSchedulesFallbackWhenDueDateIsToday() = runBlocking {
        source.byId[1L to OWNER] = assignment(id = 1L, dueDate = dueToday)

        coordinator.onAssignmentSaved(assignmentId = 1L, ownerId = OWNER)

        assertEquals(listOf(1L), scheduler.scheduled.map { it.id })
        assertTrue(scheduler.cancelled.isEmpty())
        assertTrue(notifier.cancelled.isEmpty())
    }

    @Test
    fun onAssignmentSavedCancelsWhenDueDateAlreadyPassed() = runBlocking {
        source.byId[1L to OWNER] = assignment(id = 1L, dueDate = yesterday)

        coordinator.onAssignmentSaved(assignmentId = 1L, ownerId = OWNER)

        assertTrue(scheduler.scheduled.isEmpty())
        assertEquals(listOf(OWNER to 1L), scheduler.cancelled)
        assertEquals(listOf(OWNER to 1L), notifier.cancelled)
    }

    @Test
    fun onAssignmentSavedCancelsWhenRemindersAreDisabled() = runBlocking {
        preferences.setEnabled(false)
        source.byId[1L to OWNER] = assignment(id = 1L, dueDate = futureDue)

        coordinator.onAssignmentSaved(assignmentId = 1L, ownerId = OWNER)

        assertTrue(scheduler.scheduled.isEmpty())
        assertEquals(listOf(OWNER to 1L), scheduler.cancelled)
        assertEquals(listOf(OWNER to 1L), notifier.cancelled)
    }

    @Test
    fun onAssignmentSavedIgnoresInvalidIds() = runBlocking {
        coordinator.onAssignmentSaved(assignmentId = 0L, ownerId = OWNER)

        assertTrue(source.byIdRequests.isEmpty())
        assertTrue(scheduler.scheduled.isEmpty())
        assertTrue(scheduler.cancelled.isEmpty())
    }

    @Test
    fun onCompletedCancelsWorkAndShownNotification() = runBlocking {
        coordinator.onCompleted(ownerId = OWNER, assignmentId = 1L)

        assertEquals(listOf(OWNER to 1L), scheduler.cancelled)
        assertEquals(listOf(OWNER to 1L), notifier.cancelled)
    }

    @Test
    fun onDeletedCancelsWorkAndShownNotification() = runBlocking {
        coordinator.onDeleted(ownerId = OWNER, assignmentId = 1L)

        assertEquals(listOf(OWNER to 1L), scheduler.cancelled)
        assertEquals(listOf(OWNER to 1L), notifier.cancelled)
    }

    @Test
    fun disablingRemindersCancelsWorkAndNotificationsForTheOwner() = runBlocking {
        coordinator.setEnabled(enabled = false, ownerId = OWNER)

        assertFalse(preferences.enabled.value)
        assertTrue(scheduler.scheduled.isEmpty())
        assertEquals(listOf(OWNER), scheduler.cancelledAllFor)
        assertEquals(listOf(OWNER), notifier.cancelledAllFor)
    }

    @Test
    fun disablingRemindersWithoutOwnerCancelsAllWork() = runBlocking {
        coordinator.setEnabled(enabled = false, ownerId = null)

        assertFalse(preferences.enabled.value)
        assertEquals(1, scheduler.cancelAllCalls)
        assertTrue(notifier.cancelledAllFor.isEmpty())
    }

    @Test
    fun enablingRemindersSynchronizesPendingAssignments() = runBlocking {
        preferences.setEnabled(false)
        source.pendingByOwner[OWNER] = listOf(assignment(id = 1L, dueDate = futureDue))

        coordinator.setEnabled(enabled = true, ownerId = OWNER)

        assertTrue(preferences.enabled.value)
        assertEquals(listOf(1L), scheduler.scheduled.map { it.id })
        assertEquals(listOf(OWNER), scheduler.cancelledAllFor)
    }

    @Test
    fun logoutCancelsWorkAndNotificationsWithoutChangingTheToggle() = runBlocking {
        coordinator.onLogout(OWNER)

        assertTrue(preferences.enabled.value)
        assertEquals(listOf(OWNER), scheduler.cancelledAllFor)
        assertEquals(listOf(OWNER), notifier.cancelledAllFor)
    }

    @Test
    fun logoutWithInvalidOwnerCancelsAllWork() = runBlocking {
        coordinator.onLogout(0L)

        assertEquals(1, scheduler.cancelAllCalls)
        assertTrue(notifier.cancelledAllFor.isEmpty())
    }

    private fun assignment(
        id: Long,
        dueDate: Long,
        owner: Long = OWNER,
        status: String = PENDING
    ): Assignment = Assignment(
        id = id,
        title = "Entrega $id",
        subjectId = 1L,
        dueDate = dueDate,
        status = status,
        ownerId = owner
    )

    private class FakeAssignmentSource : ReminderAssignmentSource {
        val byId = mutableMapOf<Pair<Long, Long>, Assignment>()
        val byIdRequests = mutableListOf<Pair<Long, Long>>()
        val pendingByOwner = mutableMapOf<Long, List<Assignment>>()
        val pendingRequests = mutableListOf<Pair<Long, Long>>()

        override suspend fun byId(assignmentId: Long, ownerId: Long): Assignment? {
            byIdRequests += assignmentId to ownerId
            return byId[assignmentId to ownerId]
        }

        override suspend fun pending(ownerId: Long, fromDay: Long): List<Assignment> {
            pendingRequests += ownerId to fromDay
            return pendingByOwner[ownerId].orEmpty()
        }
    }

    private class FakeScheduler : ReminderScheduler {
        val scheduled = mutableListOf<Assignment>()
        val cancelled = mutableListOf<Pair<Long, Long>>()
        val cancelledAllFor = mutableListOf<Long>()
        var cancelAllCalls = 0

        override fun schedule(assignment: Assignment) {
            scheduled += assignment
        }

        override fun cancel(ownerId: Long, assignmentId: Long) {
            cancelled += ownerId to assignmentId
        }

        override fun cancelAllFor(ownerId: Long) {
            cancelledAllFor += ownerId
        }

        override fun cancelAll() {
            cancelAllCalls++
        }
    }

    private class FakePreferences(initial: Boolean) : ReminderPreferences {
        private val state = MutableStateFlow(initial)

        override val enabled: StateFlow<Boolean> = state.asStateFlow()

        override fun setEnabled(enabled: Boolean) {
            state.value = enabled
        }
    }

    private class FakeNotifier : ReminderNotifier {
        val shown = mutableListOf<Pair<Assignment, String>>()
        val cancelled = mutableListOf<Pair<Long, Long>>()
        val cancelledAllFor = mutableListOf<Long>()

        override fun show(assignment: Assignment, subjectName: String) {
            shown += assignment to subjectName
        }

        override fun cancel(ownerId: Long, assignmentId: Long) {
            cancelled += ownerId to assignmentId
        }

        override fun cancelAllFor(ownerId: Long) {
            cancelledAllFor += ownerId
        }
    }

    private companion object {
        const val OWNER = 7L
        const val OTHER_OWNER = 8L
        const val PENDING = "Pendiente"
        const val COMPLETED = "Completada"

        val zone: ZoneId = ZoneId.of("America/La_Paz")
        val clock: Clock = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), zone)
        val today: LocalDate = LocalDate.of(2026, 11, 18)
        val yesterday: Long = today.minusDays(1).toEpochDay()
        val dueToday: Long = today.toEpochDay()
        val futureDue: Long = today.plusDays(2).toEpochDay()
    }
}
