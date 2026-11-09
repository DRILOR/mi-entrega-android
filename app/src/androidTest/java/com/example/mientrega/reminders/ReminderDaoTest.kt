package com.example.mientrega.reminders

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mientrega.data.Assignment
import com.example.mientrega.data.PlannerDao
import com.example.mientrega.data.PlannerDatabase
import com.example.mientrega.data.StudentUser
import com.example.mientrega.data.Subject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderDaoTest {

    private lateinit var database: PlannerDatabase
    private lateinit var dao: PlannerDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PlannerDatabase::class.java).build()
        dao = database.dao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun assignmentByIdReturnsOnlyTheActivityOfTheGivenOwner() = runBlocking {
        val owner = createUser("Owner", "owner@test.com")
        val other = createUser("Other", "other@test.com")
        val subject = dao.insertSubject(Subject(name = "Matemáticas", ownerId = owner))
        val assignmentId = dao.insertAssignment(
            Assignment(title = "Tarea de álgebra", subjectId = subject, dueDate = 100, ownerId = owner)
        )

        val found = dao.assignmentById(assignmentId, owner)

        assertEquals(assignmentId, found?.id)
        assertEquals("Tarea de álgebra", found?.title)
        assertEquals(owner, found?.ownerId)
        assertNull(dao.assignmentById(assignmentId, other))
    }

    @Test
    fun pendingForRemindersExcludesCompletedAndDatesBeforeFromDay() = runBlocking {
        val owner = createUser("Owner", "owner@test.com")
        val other = createUser("Other", "other@test.com")
        val subject = dao.insertSubject(Subject(name = "Física", ownerId = owner))
        val otherSubject = dao.insertSubject(Subject(name = "Química", ownerId = other))

        val before = dao.insertAssignment(
            Assignment(title = "Anterior", subjectId = subject, dueDate = 99, ownerId = owner)
        )
        val onBoundary = dao.insertAssignment(
            Assignment(title = "En el límite", subjectId = subject, dueDate = 100, ownerId = owner)
        )
        val future = dao.insertAssignment(
            Assignment(title = "Futura", subjectId = subject, dueDate = 120, ownerId = owner)
        )
        dao.insertAssignment(
            Assignment(
                title = "Completada",
                subjectId = subject,
                dueDate = 130,
                status = "Completada",
                ownerId = owner
            )
        )
        dao.insertAssignment(
            Assignment(title = "De otra cuenta", subjectId = otherSubject, dueDate = 140, ownerId = other)
        )

        val result = dao.pendingForReminders(owner, fromDay = 100)

        assertEquals(listOf(onBoundary, future), result.map { it.id })
        assertEquals(false, result.any { it.id == before })
        assertEquals(false, result.any { it.status == "Completada" })
        assertEquals(false, result.any { it.ownerId != owner })
    }

    @Test
    fun subjectNameIsReturnedOnlyForTheSubjectOwner() = runBlocking {
        val owner = createUser("Owner", "owner@test.com")
        val other = createUser("Other", "other@test.com")
        val subjectId = dao.insertSubject(Subject(name = "Tecnologías Móviles", ownerId = owner))

        assertEquals("Tecnologías Móviles", dao.subjectName(subjectId, owner))
        assertNull(dao.subjectName(subjectId, other))
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
}
