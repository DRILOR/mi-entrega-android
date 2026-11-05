# Decisiones del proyecto

## Plataforma
Aplicación Android nativa con Kotlin y Jetpack Compose, de acuerdo con la elección del equipo. Android mínimo 8.0 (API 26). La primera entrega usa compileSdk y targetSdk 36.

## Datos
Room con dos tablas: subjects y assignments. Cada actividad pertenece a una materia mediante subjectId. Las fechas se guardan como días desde 1970 (LocalDate.toEpochDay), sin hora ni zona horaria, porque esta entrega organiza fechas de vencimiento completas.

## Integridad
No se permite borrar una materia con actividades. La interfaz lo explica y la base de datos también lo impide mediante una clave foránea RESTRICT. No se borran actividades en cascada.

## Formularios
El nombre de materia y el título de actividad se recortan al guardar. La descripción es opcional. Las fechas pasadas son válidas: permiten registrar trabajos ya vencidos. Tipo, prioridad y estado se eligen de listas controladas.

## Alcance de la primera entrega
CRUD de materias y actividades, persistencia, navegación, validaciones y resumen básico de Inicio. Los filtros avanzados, Para hoy con prioridades y los recordatorios quedan para las siguientes etapas. El resumen actual ordena los pendientes por fecha.

## Organización
Pantallas en ui/PlannerApp.kt, operaciones en PlannerViewModel.kt y persistencia en data/PlannerDatabase.kt. Se mantiene una estructura pequeña para facilitar la explicación académica. Antes de ampliar la app se puede dividir cada pantalla en su propio archivo.

## Verificación
Pruebas unitarias para nombres y fechas. Prueba instrumentada para persistencia al reabrir, estados y restricción de eliminación. Recorrido manual de creación, edición, cancelación, eliminación y cierre/reapertura. Los resultados deben registrarse en AVANCE.md; escribir una prueba no equivale a ejecutarla.
