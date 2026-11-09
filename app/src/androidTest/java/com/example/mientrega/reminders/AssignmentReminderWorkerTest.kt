package com.example.mientrega.reminders

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AssignmentReminderWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var database: PlannerDatabase
    private lateinit var dao: PlannerDao
    private lateinit var notifier: FakeReminderNotifier
    private lateinit var preferences: FakeReminderPreferences
    private lateinit var session: FakeReminderSession
    private lateinit var workManager: WorkManager

    @get:Rule
    val notificationPermissionRule: TestRule = object : TestRule {
        private val delegate = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

        override fun apply(base: Statement, description: Description): Statement =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                delegate.apply(base, description)
            } else {
                base
            }
    }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, PlannerDatabase::class.java).build()
        dao = database.dao()
        notifier = FakeReminderNotifier()
        preferences = FakeReminderPreferences(enabled = true)
        session = FakeReminderSession(userId = null)

        ReminderDependencies.initialize(dao, notifier, preferences, session)

        val configuration = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
        workManager = WorkManager.getInstance(context)
        workManager.cancelAllWork().result.get(5, TimeUnit.SECONDS)
    }

    @After
    fun tearDown() {
        workManager.cancelAllWork().result.get(5, TimeUnit.SECONDS)
        WorkManagerTestInitHelper.closeWorkDatabase()
        database.close()
        ReminderDependencies.reset()
    }

    @Test
    fun showsNotificationWhenAllDataIsValid() = runBlocking {
        val owner = createUser("Owner", "owner@test.com")
        val subject = dao.insertSubject(Subject(name = "Tecnologías Móviles", ownerId = owner))
        val dueDate = LocalDate.now().plusDays(1).toEpochDay()
        val assignmentId = dao.insertAssignment(
            Assignment(
                title = "Propuesta",
                subjectId = subject,
                dueDate = dueDate,
                ownerId = owner
            )
        )
        session.userId = owner

        val info = enqueueAndAwait(assignmentId, owner, dueDate)

        assertEquals(WorkInfo.State.SUCCEEDED, info.state)
        assertEquals(1, notifier.shown.size)
        assertEquals(assignmentId, notifier.shown.single().first.id)
        assertEquals("Tecnologías Móviles", notifier.shown.single().second)
    }

    @Test
    fun doesNotShowNotificationWhenAssignmentIsCompleted() = runBlocking {
        val owner = createUser("Owner", "owner@test.com")
        val subject = dao.insertSubject(Subject(name = "Física", ownerId = owner))
        val dueDate = LocalDate.now().plusDays(1).toEpochDay()
        val assignmentId = dao.insertAssignment(
            Assignment(
                title = "Laboratorio",
                subjectId = subject,
                dueDate = dueDate,
                status = "Completada",
                ownerId = owner
            )
        )
        session.userId = owner

        val info = enqueueAndAwait(assignmentId, owner, dueDate)

        assertEquals(WorkInfo.State.SUCCEEDED, info.state)
        assertTrue(notifier.shown.isEmpty())
    }

    @Test
    fun doesNotShowNotificationWhenAssignmentWasDeleted() = runBlocking {
        val owner = createUser("Owner", "owner@test.com")
        val subject = dao.insertSubject(Subject(name = "Química", ownerId = owner))
        val dueDate = LocalDate.now().plusDays(1).toEpochDay()
        val assignmentId = dao.insertAssignment(
            Assignment(
                title = "Práctica",
                subjectId = subject,
                dueDate = dueDate,
                ownerId = owner
            )
        )
        dao.deleteAssignment(assignmentId, owner)
        session.userId = owner

        val info = enqueueAndAwait(assignmentId, owner, dueDate)

        assertEquals(WorkInfo.State.SUCCEEDED, info.state)
        assertTrue(notifier.shown.isEmpty())
    }

    @Test
    fun doesNotShowNotificationWhenSessionBelongsToAnotherUser() = runBlocking {
        val owner = createUser("Owner", "owner@test.com")
        val other = createUser("Other", "other@test.com")
        val subject = dao.insertSubject(Subject(name = "Matemáticas", ownerId = owner))
        val dueDate = LocalDate.now().plusDays(1).toEpochDay()
        val assignmentId = dao.insertAssignment(
            Assignment(
                title = "Examen",
                subjectId = subject,
                dueDate = dueDate,
                ownerId = owner
            )
        )
        session.userId = other

        val info = enqueueAndAwait(assignmentId, owner, dueDate)

        assertEquals(WorkInfo.State.SUCCEEDED, info.state)
        assertTrue(notifier.shown.isEmpty())
    }

    @Test
    fun doesNotShowNotificationWhenDueDateChanged() = runBlocking {
        val owner = createUser("Owner", "owner@test.com")
        val subject = dao.insertSubject(Subject(name = "Historia", ownerId = owner))
        val oldDueDate = LocalDate.now().plusDays(1).toEpochDay()
        val newDueDate = LocalDate.now().plusDays(5).toEpochDay()
        val assignmentId = dao.insertAssignment(
            Assignment(
                title = "Ensayo",
                subjectId = subject,
                dueDate = newDueDate,
                ownerId = owner
            )
        )
        session.userId = owner

        val info = enqueueAndAwait(assignmentId, owner, oldDueDate)

        assertEquals(WorkInfo.State.SUCCEEDED, info.state)
        assertTrue(notifier.shown.isEmpty())
    }

    @Test
    fun doesNotShowNotificationWhenRemindersAreDisabled() = runBlocking {
        val owner = createUser("Owner", "owner@test.com")
        val subject = dao.insertSubject(Subject(name = "Programación", ownerId = owner))
        val dueDate = LocalDate.now().plusDays(1).toEpochDay()
        val assignmentId = dao.insertAssignment(
            Assignment(
                title = "Proyecto",
                subjectId = subject,
                dueDate = dueDate,
                ownerId = owner
            )
        )
        session.userId = owner
        preferences.setEnabled(false)

        val info = enqueueAndAwait(assignmentId, owner, dueDate)

        assertEquals(WorkInfo.State.SUCCEEDED, info.state)
        assertTrue(notifier.shown.isEmpty())
    }

    private fun enqueueAndAwait(
        assignmentId: Long,
        ownerId: Long,
        expectedDueDate: Long
    ): WorkInfo {
        val request = OneTimeWorkRequestBuilder<AssignmentReminderWorker>()
            .setInputData(
                workDataOf(
                    AssignmentReminderWorker.KEY_ASSIGNMENT_ID to assignmentId,
                    AssignmentReminderWorker.KEY_OWNER_ID to ownerId,
                    AssignmentReminderWorker.KEY_EXPECTED_DUE_DATE to expectedDueDate
                )
            )
            .build()

        workManager.enqueue(request)

        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            val info = runCatching {
                workManager.getWorkInfoById(request.id).get(1, TimeUnit.SECONDS)
            }.getOrNull()

            if (info?.state?.isFinished == true) return info
            Thread.sleep(POLL_INTERVAL_MS)
        }

        throw AssertionError("El Worker no terminó dentro del tiempo esperado")
    }

    private suspend fun createUser(name: String, email: String): Long =
        dao.insertUser(
            StudentUser(
                displayName = name,
                email = email,
                passwordHash = "test",
                passwordSalt = "test"
            )
        )

    private class FakeReminderNotifier : ReminderNotifier {
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

    private class FakeReminderPreferences(enabled: Boolean) : ReminderPreferences {
        private val state = MutableStateFlow(enabled)
        private val fallbackTargets = mutableMapOf<Long, Long>()

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
    }

    private class FakeReminderSession(var userId: Long?) : ReminderSession {
        override fun activeUserId(): Long? = userId
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
        const val POLL_INTERVAL_MS = 25L
    }
}
