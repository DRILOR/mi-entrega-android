package com.example.mientrega.reminders

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.example.mientrega.data.Assignment
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RunWith(AndroidJUnit4::class)
class ReminderNotifierTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val notificationManager: NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    private val ownerId = 900L
    private val assignmentId = 9_001L
    private val subjectName = "Tecnologías Móviles"
    private val dueDate = LocalDate.now().plusDays(1)
    private val assignment = Assignment(
        id = assignmentId,
        title = "Entrega de prueba",
        subjectId = 1L,
        dueDate = dueDate.toEpochDay(),
        ownerId = ownerId
    )

    private val notificationTag = ReminderPolicy.notificationTag(ownerId, assignmentId)
    private val notificationId = ReminderPolicy.notificationId(assignmentId)

    /**
     * En Android 13+ se necesita POST_NOTIFICATIONS. En versiones anteriores
     * el permiso no existe como permiso runtime, así que la regla se omite.
     */
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
        notificationManager.cancel(notificationTag, notificationId)
        // Sin notificaciones habilitadas no se puede verificar la publicación.
        assumeTrue(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }

    @After
    fun tearDown() {
        AndroidReminderNotifier(context).cancel(ownerId, assignmentId)
    }

    @Test
    fun showPublishesNotificationWithExpectedTagContentAndChannel() {
        AndroidReminderNotifier(context).show(assignment, subjectName)

        val posted = awaitNotification(notificationTag, notificationId)

        assertNotNull("La notificación debería estar activa", posted)
        assertEquals(notificationTag, posted!!.tag)
        assertEquals(notificationId, posted.id)
        assertEquals(AndroidReminderNotifier.CHANNEL_ID, posted.notification.channelId)

        val extras = posted.notification.extras
        assertEquals(
            "Entrega mañana: ${assignment.title}",
            extras.getString(Notification.EXTRA_TITLE)
        )
        assertEquals(
            "$subjectName · vence el ${dueDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
            extras.getString(Notification.EXTRA_TEXT)
        )
    }

    @Test
    fun cancelRemovesTheActiveNotification() {
        val notifier = AndroidReminderNotifier(context)
        notifier.show(assignment, subjectName)
        assertNotNull(
            "La notificación debe existir antes de cancelarla",
            awaitNotification(notificationTag, notificationId)
        )

        notifier.cancel(ownerId, assignmentId)

        assertTrue(
            "La notificación debería dejar de estar activa",
            awaitNoNotification(notificationTag, notificationId)
        )
    }

    @Test
    fun cancelAllForRemovesOnlyTheNotificationsOfThatOwner() {
        val notifier = AndroidReminderNotifier(context)
        val otherAssignment = assignment.copy(
            id = assignmentId + 1,
            ownerId = ownerId + 1
        )
        val otherOwnerId = requireNotNull(otherAssignment.ownerId)
        val otherTag = ReminderPolicy.notificationTag(otherOwnerId, otherAssignment.id)
        val otherId = ReminderPolicy.notificationId(otherAssignment.id)

        try {
            notifier.show(assignment, subjectName)
            notifier.show(otherAssignment, subjectName)
            assertNotNull(awaitNotification(notificationTag, notificationId))
            assertNotNull(awaitNotification(otherTag, otherId))

            notifier.cancelAllFor(ownerId)

            assertTrue(
                "La notificación de la cuenta desactivada debe desaparecer",
                awaitNoNotification(notificationTag, notificationId)
            )
            assertNotNull(
                "La notificación de otra cuenta no debe cancelarse",
                awaitNotification(otherTag, otherId)
            )
        } finally {
            notifier.cancelAllFor(otherOwnerId)
        }
    }

    private fun awaitNotification(tag: String, id: Int): android.service.notification.StatusBarNotification? {
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            findNotification(tag, id)?.let { return it }
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return findNotification(tag, id)
    }

    private fun awaitNoNotification(tag: String, id: Int): Boolean {
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            if (findNotification(tag, id) == null) return true
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return findNotification(tag, id) == null
    }

    private fun findNotification(tag: String, id: Int): android.service.notification.StatusBarNotification? =
        notificationManager.activeNotifications.firstOrNull { it.tag == tag && it.id == id }

    private companion object {
        const val TIMEOUT_MS = 5_000L
        const val POLL_INTERVAL_MS = 50L
    }
}
