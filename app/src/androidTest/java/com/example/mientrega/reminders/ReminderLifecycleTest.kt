package com.example.mientrega.reminders

import android.content.Context
import android.os.SystemClock
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.mientrega.data.Assignment
import com.example.mientrega.data.PlannerDao
import com.example.mientrega.data.PlannerDatabase
import com.example.mientrega.data.StudentUser
import com.example.mientrega.data.Subject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Pruebas instrumentadas del ciclo de vida completo de un recordatorio.
 *
 * Usan Room en memoria y el WorkManager de prueba. El reloj es mutable para
 * poder comprobar que al editar una actividad el trabajo se reemplaza y el
 * `initialDelayMillis` cambia sin dejar trabajos duplicados.
 */
@RunWith(AndroidJUnit4::class)
class ReminderLifecycleTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var database: PlannerDatabase
    private lateinit var dao: PlannerDao
    private lateinit var workManager: WorkManager
    private lateinit var preferences: FakeReminderPreferences
    private lateinit var notifier: FakeReminderNotifier
    private lateinit var clock: FixedClock
    private lateinit var coordinator: ReminderCoordinator
    private val subjectIds = mutableMapOf<Long, Long>()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, PlannerDatabase::class.java).build()
        dao = database.dao()
        preferences = FakeReminderPreferences(enabled = true)
        notifier = FakeReminderNotifier()
        clock = FixedClock(FIXED_NOW, ZONE)

        val configuration = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
        workManager = WorkManager.getInstance(context)
        workManager.cancelAllWork().result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        coordinator = ReminderCoordinator(
            assignments = DaoReminderAssignmentSource(dao),
            scheduler = WorkManagerReminderScheduler(workManager, clock),
            preferences = preferences,
            notifier = notifier,
            clock = clock,
            zoneId = ZONE
        )
    }

    @After
    fun tearDown() {
        workManager.cancelAllWork().result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        WorkManagerTestInitHelper.closeWorkDatabase()
        database.close()
    }

    /** Crear: una actividad nueva genera exactamente un trabajo activo. */
    @Test
    fun creatingAssignmentSchedulesExactlyOneActiveWork() = runBlocking {
        val owner = createOwner()
        val assignmentId = createAssignment(owner, dueDate = dueInDays(3))

        coordinator.onAssignmentSaved(assignmentId, owner)

        val infos = awaitWork(workName(owner, assignmentId)) { active(it).size == 1 }
        assertEquals(1, active(infos).size)
        assertEquals(WorkInfo.State.ENQUEUED, active(infos).single().state)
        assertEquals(expectedDelay(dueInDays(3)), active(infos).single().initialDelayMillis)
    }

    /**
     * Editar: volver a guardar la misma actividad mantiene un único trabajo y
     * actualiza el retraso con la nueva fecha de entrega.
     */
    @Test
    fun editingAssignmentKeepsOneWorkAndUpdatesDelay() = runBlocking {
        val owner = createOwner()
        val firstDueDate = dueInDays(3)
        val assignmentId = createAssignment(owner, dueDate = firstDueDate)
        val name = workName(owner, assignmentId)

        coordinator.onAssignmentSaved(assignmentId, owner)
        val firstDelay = awaitWork(name) { active(it).size == 1 }
            .let { active(it).single().initialDelayMillis }

        val newDueDate = dueInDays(6)
        dao.updateAssignment(
            id = assignmentId,
            userId = owner,
            title = "Entrega editada",
            description = "Fecha actualizada",
            subjectId = subjectId(owner),
            type = "Tarea",
            dueDate = newDueDate,
            priority = "Alta",
            status = PENDING
        )
        coordinator.onAssignmentSaved(assignmentId, owner)

        val infos = awaitWork(name) {
            active(it).size == 1 && active(it).single().initialDelayMillis != firstDelay
        }
        assertEquals(1, active(infos).size)
        assertEquals(expectedDelay(newDueDate), active(infos).single().initialDelayMillis)
        assertTrue(
            "El nuevo retraso debe ser mayor al mover la entrega más adelante",
            active(infos).single().initialDelayMillis > firstDelay
        )
    }

    /** Completar: el trabajo asociado queda cancelado. */
    @Test
    fun completingAssignmentCancelsWork() = runBlocking {
        val owner = createOwner()
        val assignmentId = createAssignment(owner, dueDate = dueInDays(3))
        val name = workName(owner, assignmentId)

        coordinator.onAssignmentSaved(assignmentId, owner)
        awaitWork(name) { active(it).size == 1 }

        dao.changeStatus(assignmentId, owner, COMPLETED)
        coordinator.onCompleted(owner, assignmentId)

        val infos = awaitNoActiveWork(name)
        assertTrue(
            "No debe quedar ningún trabajo activo tras completar la actividad",
            active(infos).isEmpty()
        )
        assertTrue(
            "El trabajo debe haber pasado a CANCELLED o haber desaparecido",
            infos.isEmpty() || infos.all { it.state == WorkInfo.State.CANCELLED }
        )
    }

    /** Eliminar: el trabajo asociado se cancela. */
    @Test
    fun deletingAssignmentCancelsWork() = runBlocking {
        val owner = createOwner()
        val assignmentId = createAssignment(owner, dueDate = dueInDays(3))
        val name = workName(owner, assignmentId)

        coordinator.onAssignmentSaved(assignmentId, owner)
        awaitWork(name) { active(it).size == 1 }

        dao.deleteAssignment(assignmentId, owner)
        coordinator.onDeleted(owner, assignmentId)

        val infos = awaitNoActiveWork(name)
        assertTrue(
            "No debe quedar ningún trabajo activo tras eliminar la actividad",
            active(infos).isEmpty()
        )
        assertTrue(
            "El trabajo debe haber pasado a CANCELLED o haber desaparecido",
            infos.isEmpty() || infos.all { it.state == WorkInfo.State.CANCELLED }
        )
    }

    /**
     * No duplicados: guardar tres veces la misma actividad deja un solo
     * trabajo activo gracias a ExistingWorkPolicy.REPLACE.
     */
    @Test
    fun savingSameAssignmentThreeTimesKeepsOnlyOneWork() = runBlocking {
        val owner = createOwner()
        val assignmentId = createAssignment(owner, dueDate = dueInDays(3))
        val name = workName(owner, assignmentId)

        repeat(3) {
            coordinator.onAssignmentSaved(assignmentId, owner)
        }

        val infos = awaitWork(name) { active(it).size == 1 }
        assertEquals(1, active(infos).size)
        assertEquals(expectedDelay(dueInDays(3)), active(infos).single().initialDelayMillis)
    }

    /**
     * Sincronizar dos veces la misma entrega de hoy no desplaza el respaldo:
     * se reutiliza la hora guardada en preferencias y solo disminuye el
     * retraso restante de WorkManager.
     */
    @Test
    fun syncingTwiceKeepsTodayFallbackTargetStable() = runBlocking {
        val owner = createOwner()
        val assignmentId = createAssignment(owner, dueDate = dueInDays(0))
        val name = workName(owner, assignmentId)

        coordinator.sync(owner)
        val firstDelay = awaitWork(name) { active(it).size == 1 }
            .let { active(it).single().initialDelayMillis }
        val storedTarget = requireNotNull(
            preferences.fallbackTargetMillis(assignmentId)
        ) { "El respaldo de hoy debe quedar guardado" }
        assertEquals(
            clock.instant().plus(Duration.ofMinutes(ReminderPolicy.FALLBACK_DELAY_MINUTES))
                .toEpochMilli(),
            storedTarget
        )

        clock.advance(Duration.ofMinutes(5))
        coordinator.sync(owner)

        val expectedRemaining = firstDelay - TimeUnit.MINUTES.toMillis(5)
        val infos = awaitWork(name) {
            active(it).size == 1 &&
                active(it).single().initialDelayMillis == expectedRemaining
        }
        assertEquals(storedTarget, preferences.fallbackTargetMillis(assignmentId))
        assertEquals(expectedRemaining, active(infos).single().initialDelayMillis)
    }

    /**
     * Si el respaldo de hoy ya venció y la actividad sigue pendiente, no se
     * programa otro trabajo: queda marcado como entregado.
     */
    @Test
    fun expiredTodayFallbackIsNotRescheduled() = runBlocking {
        val owner = createOwner()
        val assignmentId = createAssignment(owner, dueDate = dueInDays(0))
        val name = workName(owner, assignmentId)

        coordinator.onAssignmentSaved(assignmentId, owner)
        awaitWork(name) { active(it).size == 1 }

        clock.advance(
            Duration.ofMinutes(ReminderPolicy.FALLBACK_DELAY_MINUTES + 1)
        )
        coordinator.sync(owner)

        assertTrue(active(awaitNoActiveWork(name)).isEmpty())
        assertTrue(preferences.isFallbackDelivered(assignmentId))
    }

    /** Desactivar el toggle cancela todos los trabajos de la cuenta. */
    @Test
    fun disablingRemindersCancelsEveryWorkOfTheOwner() = runBlocking {
        val owner = createOwner()
        val firstId = createAssignment(owner, dueDate = dueInDays(3))
        val secondId = createAssignment(owner, dueDate = dueInDays(5))

        coordinator.onAssignmentSaved(firstId, owner)
        coordinator.onAssignmentSaved(secondId, owner)
        awaitWork(workName(owner, firstId)) { active(it).size == 1 }
        awaitWork(workName(owner, secondId)) { active(it).size == 1 }

        coordinator.setEnabled(enabled = false, ownerId = owner)

        assertTrue(active(awaitNoActiveWork(workName(owner, firstId))).isEmpty())
        assertTrue(active(awaitNoActiveWork(workName(owner, secondId))).isEmpty())
    }

    private suspend fun createOwner(): Long =
        dao.insertUser(
            StudentUser(
                displayName = "Owner",
                email = "owner@test.com",
                passwordHash = "test",
                passwordSalt = "test"
            )
        )

    private suspend fun createAssignment(ownerId: Long, dueDate: Long): Long =
        dao.insertAssignment(
            Assignment(
                title = "Entrega",
                subjectId = subjectId(ownerId),
                dueDate = dueDate,
                ownerId = ownerId
            )
        )

    private suspend fun subjectId(ownerId: Long): Long {
        subjectIds[ownerId]?.let { return it }
        return dao.insertSubject(
            Subject(name = "Tecnologías Móviles", ownerId = ownerId)
        ).also { subjectIds[ownerId] = it }
    }

    private fun workName(ownerId: Long, assignmentId: Long): String =
        ReminderPolicy.workName(ownerId, assignmentId)

    private fun expectedDelay(dueDate: Long): Long {
        val reminder = requireNotNull(
            ReminderPolicy.reminderInstant(dueDate, clock, ZONE)
        ) { "La fecha de prueba debería generar un recordatorio futuro" }
        return Duration.between(clock.instant(), reminder).toMillis()
    }

    private fun workInfos(name: String): List<WorkInfo> =
        workManager.getWorkInfosForUniqueWork(name)
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)

    private fun active(infos: List<WorkInfo>): List<WorkInfo> =
        infos.filter { !it.state.isFinished }

    private fun awaitWork(
        name: String,
        predicate: (List<WorkInfo>) -> Boolean
    ): List<WorkInfo> {
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MS
        var infos = workInfos(name)
        while (SystemClock.elapsedRealtime() < deadline) {
            infos = workInfos(name)
            if (predicate(infos)) return infos
            Thread.sleep(POLL_INTERVAL_MS)
        }
        throw AssertionError(
            "WorkManager no alcanzó el estado esperado para '$name': $infos"
        )
    }

    private fun awaitNoActiveWork(name: String): List<WorkInfo> =
        awaitWork(name) { active(it).isEmpty() }

    private fun dueInDays(days: Long): Long =
        clock.instant().atZone(ZONE).toLocalDate().plusDays(days).toEpochDay()

    private class FixedClock(
        private var current: Instant,
        private val zone: ZoneId
    ) : Clock() {
        fun advance(duration: Duration) {
            current = current.plus(duration)
        }

        override fun getZone(): ZoneId = zone
        override fun withZone(zone: ZoneId): Clock = FixedClock(current, zone)
        override fun instant(): Instant = current
    }

    private class FakeReminderPreferences(enabled: Boolean) : ReminderPreferences {
        private val state = MutableStateFlow(enabled)
        private val fallbackTargets = mutableMapOf<Long, Long>()
        private val fallbackDelivered = mutableSetOf<Long>()

        override val enabled: StateFlow<Boolean> = state.asStateFlow()

        override fun setEnabled(enabled: Boolean) {
            state.value = enabled
        }

        override fun fallbackTargetMillis(assignmentId: Long): Long? =
            fallbackTargets[assignmentId]

        override fun setFallbackTargetMillis(assignmentId: Long, targetMillis: Long) {
            fallbackTargets[assignmentId] = targetMillis
        }

        override fun clearFallbackTarget(assignmentId: Long) {
            fallbackTargets.remove(assignmentId)
        }

        override fun isFallbackDelivered(assignmentId: Long): Boolean =
            assignmentId in fallbackDelivered

        override fun setFallbackDelivered(assignmentId: Long, delivered: Boolean) {
            if (delivered) {
                fallbackDelivered += assignmentId
            } else {
                fallbackDelivered -= assignmentId
            }
        }

        override fun clearFallbackDelivered(assignmentId: Long) {
            fallbackDelivered -= assignmentId
        }
    }

    private class FakeReminderNotifier : ReminderNotifier {
        override fun show(assignment: Assignment, subjectName: String) = Unit
        override fun cancel(ownerId: Long, assignmentId: Long) = Unit
        override fun cancelAllFor(ownerId: Long) = Unit
    }

    private companion object {
        const val PENDING = "Pendiente"
        const val COMPLETED = "Completada"
        const val TIMEOUT_SECONDS = 5L
        const val TIMEOUT_MS = 5_000L
        const val POLL_INTERVAL_MS = 25L

        val ZONE: ZoneId = ZoneId.of("America/La_Paz")
        val FIXED_NOW: Instant = Instant.parse("2026-11-18T12:00:00Z")
    }
}
