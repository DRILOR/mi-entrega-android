package com.example.mientrega

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mientrega.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityOrganizationPersistenceTest {
    @Test fun roomResultsReflectEditsCompletionDeletionAndOwnerFilters() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Solo una base de prueba en memoria; nunca abre ni elimina mi-entrega.db.
        val db = Room.inMemoryDatabaseBuilder(context, PlannerDatabase::class.java).build()
        try {
            withTimeout(10_000) {
                val dao = db.dao()
                val a = dao.insertUser(StudentUser(displayName = "A", email = "a@test.com", passwordHash = "test", passwordSalt = "test"))
                val b = dao.insertUser(StudentUser(displayName = "B", email = "b@test.com", passwordHash = "test", passwordSalt = "test"))
                val subject = dao.insertSubject(Subject(name = "Matemáticas", ownerId = a))
                val id = dao.insertAssignment(Assignment(title = "Tarea", subjectId = subject, dueDate = 100, ownerId = a))
                val filters = ActivityFilters(subjectId = subject, status = "Pendiente")
                val selected = dao.assignments(a).map { ActivityOrganization.select(it, filters) }
                assertEquals(id, selected.first().single().id)
                assertTrue(ActivityOrganization.select(dao.assignments(b).first(), filters).isEmpty())
                assertEquals(0, dao.changeStatus(id, b, "Completada"))

                assertEquals(1, dao.updateAssignment(id, a, "Editada", "", subject, "Tarea", 102, "Alta", "Pendiente"))
                val edited = selected.first { it.singleOrNull()?.dueDate == 102L }.single()
                assertEquals(ActivitySection.UPCOMING, ActivityOrganization.section(edited, 101))
                assertEquals(1, dao.changeStatus(id, a, "Completada"))
                assertTrue(selected.first { it.isEmpty() }.isEmpty())
                val completed = ActivityOrganization.group(dao.assignments(a).first(), 103)
                assertTrue(completed.getValue(ActivitySection.OVERDUE).isEmpty())
                assertEquals(id, completed.getValue(ActivitySection.COMPLETED).single().id)
                assertEquals(1, dao.deleteAssignment(id, a))
                assertTrue(dao.assignments(a).first { it.isEmpty() }.isEmpty())
            }
        } finally {
            db.close()
        }
    }
}
