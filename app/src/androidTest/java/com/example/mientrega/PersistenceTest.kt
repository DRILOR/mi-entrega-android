package com.example.mientrega

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mientrega.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PersistenceTest {
    @Test fun isolatesStudentsAndPersistsAcrossReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "test-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, PlannerDatabase::class.java, databaseName).build()
        var db = open()
        try {
            val dao = db.dao()
            val studentA = dao.insertUser(StudentUser(displayName = "Ana", email = "ana@test.com",
                passwordHash = "test", passwordSalt = "test"))
            val studentB = dao.insertUser(StudentUser(displayName = "Luis", email = "luis@test.com",
                passwordHash = "test", passwordSalt = "test"))
            val subjectId = dao.insertSubject(Subject(name = "Tecnologías Móviles", ownerId = studentA))
            val assignmentId = dao.insertAssignment(Assignment(title = "Propuesta", subjectId = subjectId,
                dueDate = 21000, ownerId = studentA))
            db.close()

            db = open()
            val reopened = db.dao()
            assertEquals("Propuesta", reopened.assignments(studentA).first().single().title)
            assertTrue(reopened.subjects(studentB).first().isEmpty())
            assertTrue(reopened.assignments(studentB).first().isEmpty())
            assertEquals(0, reopened.changeStatus(assignmentId, studentB, "Completada"))
            assertEquals(0, reopened.deleteAssignment(assignmentId, studentB))

            var rejected = false
            try { reopened.deleteSubject(subjectId, studentA) }
            catch (_: SQLiteConstraintException) { rejected = true }
            assertTrue("No borrar una materia con actividades", rejected)
            assertEquals(1, reopened.changeStatus(assignmentId, studentA, "Completada"))
            assertEquals("Completada", reopened.assignments(studentA).first().single().status)
            assertEquals(1, reopened.deleteAssignment(assignmentId, studentA))
            assertEquals(1, reopened.deleteSubject(subjectId, studentA))
            assertTrue(reopened.subjects(studentA).first().isEmpty())
        } finally {
            db.close()
            context.deleteDatabase(databaseName)
        }
    }
}
