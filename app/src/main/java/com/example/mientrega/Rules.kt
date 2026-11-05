package com.example.mientrega

import java.time.LocalDate

object Rules {
    val types = listOf("Tarea", "Proyecto", "Examen")
    val priorities = listOf("Baja", "Media", "Alta")
    val statuses = listOf("Pendiente", "En progreso", "Completada")
    fun subjectError(name: String): String? = when {
        name.isBlank() -> "Escribe el nombre de la materia."
        name.trim().length > 60 -> "Usa un máximo de 60 caracteres."
        else -> null
    }
    fun parseDate(value: String): Long? = runCatching { LocalDate.parse(value).toEpochDay() }.getOrNull()
    fun activityError(title: String, subjectId: Long?, date: String): String? = when {
        title.isBlank() -> "Escribe el título de la actividad."
        title.trim().length > 100 -> "El título admite hasta 100 caracteres."
        subjectId == null -> "Selecciona una materia."
        parseDate(date) == null -> "Selecciona una fecha válida."
        else -> null
    }
}
