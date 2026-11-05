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
    @Test fun persistsAcrossReopenAndProtectsSubjects() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "test-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, PlannerDatabase::class.java, name).build()
        var db = open()
        try {
            db.dao().save(Subject(id = 1, name = "Tecnologías Móviles"))
            db.dao().save(Assignment(id = 1, title = "Propuesta", subjectId = 1, dueDate = 21000))
            db.close()
            db = open()
            assertEquals("Propuesta", db.dao().assignments().first().single().title)
            var rejected = false
            try { db.dao().delete(db.dao().subjects().first().single()) }
            catch (_: SQLiteConstraintException) { rejected = true }
            assertTrue("No borrar una materia con actividades", rejected)
            db.dao().changeStatus(1, "Completada")
            assertEquals("Completada", db.dao().assignments().first().single().status)
            db.dao().delete(db.dao().assignments().first().single())
            db.dao().delete(db.dao().subjects().first().single())
            assertTrue(db.dao().subjects().first().isEmpty())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
