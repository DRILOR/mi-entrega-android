package com.example.mientrega

import com.example.mientrega.data.Assignment
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ActivityOrganizationTest {
    private val today = LocalDate.of(2026, 11, 8).toEpochDay()
    private fun activity(id: Long, date: Long = today, priority: String = "Media",
                         subject: Long = 1L, status: String = "Pendiente") =
        Assignment(id = id, title = "Actividad $id", subjectId = subject, dueDate = date,
            priority = priority, status = status, ownerId = 1L)

    @Test fun allSubjectsAndStatusesKeepAllRecords() {
        val items = listOf(activity(1), activity(2, subject = 2, status = "En progreso"),
            activity(3, status = "Completada"))
        assertEquals(items, ActivityOrganization.select(items))
    }

    @Test fun filtersBySubject() {
        assertEquals(listOf(2L), ActivityOrganization.select(listOf(activity(1), activity(2, subject = 2)),
            ActivityFilters(subjectId = 2)).map { it.id })
    }

    @Test fun filtersEveryExistingStatus() {
        val items = Rules.statuses.mapIndexed { index, status -> activity(index.toLong(), status = status) }
        Rules.statuses.forEach { status ->
            assertEquals(listOf(status), ActivityOrganization.select(items, ActivityFilters(status = status)).map { it.status })
        }
    }

    @Test fun combinesFiltersWithAnd() {
        val items = listOf(activity(1), activity(2, subject = 2), activity(3, status = "En progreso"))
        assertEquals(listOf(1L), ActivityOrganization.select(items,
            ActivityFilters(subjectId = 1, status = "Pendiente")).map { it.id })
    }

    @Test fun subjectWithoutActivitiesHasNoResults() {
        assertTrue(ActivityOrganization.select(listOf(activity(1)), ActivityFilters(subjectId = 99)).isEmpty())
    }

    @Test fun resetRestoresAllResultsWithoutMutatingInput() {
        val items = listOf(activity(1), activity(2, status = "Completada"))
        val original = items.toList()
        assertEquals(1, ActivityOrganization.select(items, ActivityFilters(status = "Completada")).size)
        assertEquals(original, ActivityOrganization.select(items, ActivityFilters()))
        assertEquals(original, items)
        assertFalse(ActivityFilters().isActive)
    }

    @Test fun changingOneFilterPreservesTheOther() {
        val filters = ActivityFilters(subjectId = 1, status = "Pendiente")
        assertEquals(1L, filters.copy(status = "Completada").subjectId)
        assertEquals("Pendiente", filters.copy(subjectId = 2).status)
    }

    @Test fun classifiesYesterdayTodayTomorrowAndFuture() {
        val groups = ActivityOrganization.group(listOf(activity(1, today - 1), activity(2),
            activity(3, today + 1), activity(4, today + 30)), today)
        assertEquals(listOf(1L), groups.getValue(ActivitySection.OVERDUE).map { it.id })
        assertEquals(listOf(2L), groups.getValue(ActivitySection.TODAY).map { it.id })
        assertEquals(listOf(3L, 4L), groups.getValue(ActivitySection.UPCOMING).map { it.id })
    }

    @Test fun completedNeverAppearsAsOverdueTodayOrUpcoming() {
        val items = listOf(activity(1, today - 1, status = "Completada"),
            activity(2, status = "Completada"), activity(3, today + 1, status = "Completada"))
        val groups = ActivityOrganization.group(items, today)
        assertEquals(items, groups.getValue(ActivitySection.COMPLETED))
        assertTrue(groups.filterKeys { it != ActivitySection.COMPLETED }.values.all { it.isEmpty() })
    }

    @Test fun sectionsAreMutuallyExclusiveAndKeepEveryRecord() {
        val items = listOf(activity(1, today - 1), activity(2), activity(3, today + 1),
            activity(4, today - 10, status = "Completada"))
        val ids = ActivityOrganization.group(items, today).values.flatten().map { it.id }
        assertEquals(items.size, ids.size)
        assertEquals(items.map { it.id }.toSet(), ids.toSet())
    }

    @Test fun sortsByDateThenHighMediumLowThenId() {
        val items = listOf(activity(8, today + 1, "Alta"), activity(4, priority = "Baja"),
            activity(3, priority = "Media"), activity(2, priority = "Alta"),
            activity(1, priority = "Alta"), activity(9, today - 1, "Baja"))
        assertEquals(listOf(9L, 1L, 2L, 3L, 4L, 8L), ActivityOrganization.select(items).map { it.id })
        assertEquals(ActivityOrganization.select(items), ActivityOrganization.select(items.reversed()))
    }

    @Test fun completedSectionUsesTheSameOrdering() {
        val items = listOf(activity(3, priority = "Baja", status = "Completada"),
            activity(2, priority = "Alta", status = "Completada"), activity(1, today - 1, status = "Completada"))
        assertEquals(listOf(1L, 2L, 3L), ActivityOrganization.group(items, today)
            .getValue(ActivitySection.COMPLETED).map { it.id })
    }

    @Test fun editingStatusOrSubjectUpdatesActiveFilterResults() {
        val filters = ActivityFilters(subjectId = 1, status = "Pendiente")
        val item = activity(1)
        assertEquals(1, ActivityOrganization.select(listOf(item), filters).size)
        assertTrue(ActivityOrganization.select(listOf(item.copy(status = "Completada")), filters).isEmpty())
        assertTrue(ActivityOrganization.select(listOf(item.copy(subjectId = 2)), filters).isEmpty())
    }

    @Test fun editingDateAndPriorityReordersResults() {
        val items = listOf(activity(1), activity(2, today + 1))
        assertEquals(listOf(2L, 1L), ActivityOrganization.select(listOf(items[0],
            items[1].copy(dueDate = today - 1))).map { it.id })
        assertEquals(listOf(2L, 1L), ActivityOrganization.select(listOf(items[0],
            items[1].copy(dueDate = today, priority = "Alta"))).map { it.id })
    }

    @Test fun deletionAndEmptyListsHaveNoStaleResults() {
        val filters = ActivityFilters(subjectId = 1)
        assertEquals(1, ActivityOrganization.select(listOf(activity(1)), filters).size)
        assertTrue(ActivityOrganization.select(emptyList(), filters).isEmpty())
        val groups = ActivityOrganization.group(emptyList(), today)
        assertEquals(ActivitySection.entries.toSet(), groups.keys)
        assertTrue(groups.values.all { it.isEmpty() })
    }

    @Test fun advancingLocalDayReclassifiesWithoutChangingDeadline() {
        val item = activity(1)
        assertEquals(ActivitySection.UPCOMING, ActivityOrganization.section(item, today - 1))
        assertEquals(ActivitySection.TODAY, ActivityOrganization.section(item, today))
        assertEquals(ActivitySection.OVERDUE, ActivityOrganization.section(item, today + 1))
        assertEquals(today, item.dueDate)
    }
}
