package com.example.mientrega

import com.example.mientrega.data.Assignment

enum class ActivitySection(val label: String, val emptyMessage: String) {
    TODAY("Para hoy", "No tienes actividades para hoy."),
    OVERDUE("Vencidas", "No hay actividades vencidas."),
    UPCOMING("Próximas", "No hay actividades próximas."),
    COMPLETED("Completadas", "Todavía no hay actividades completadas.")
}

data class ActivityFilters(val subjectId: Long? = null, val status: String? = null) {
    val isActive: Boolean get() = subjectId != null || status != null
}

/** Transformaciones de lectura: no escriben en Room ni dependen de Compose o del reloj. */
object ActivityOrganization {
    private val order = compareBy<Assignment> { it.dueDate }
        .thenBy { when (it.priority) { "Alta" -> 0; "Media" -> 1; "Baja" -> 2; else -> 3 } }
        .thenBy { it.id }

    fun section(item: Assignment, today: Long): ActivitySection = when {
        item.status == "Completada" -> ActivitySection.COMPLETED
        item.dueDate < today -> ActivitySection.OVERDUE
        item.dueDate == today -> ActivitySection.TODAY
        else -> ActivitySection.UPCOMING
    }

    fun select(items: List<Assignment>, filters: ActivityFilters = ActivityFilters()): List<Assignment> =
        items.filter {
            (filters.subjectId == null || it.subjectId == filters.subjectId) &&
                (filters.status == null || it.status == filters.status)
        }.sortedWith(order)

    fun group(items: List<Assignment>, today: Long, filters: ActivityFilters = ActivityFilters()): Map<ActivitySection, List<Assignment>> {
        val selected = select(items, filters).groupBy { section(it, today) }
        return ActivitySection.entries.associateWith { selected[it].orEmpty() }
    }
}
