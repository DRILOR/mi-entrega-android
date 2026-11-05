# Mi Entrega

Aplicación Android para organizar materias, tareas, proyectos y exámenes. Proyecto académico de Tecnologías Móviles para tres integrantes.

## Primera entrega
Incluye código para navegación, materias, actividades, fechas, prioridades, estados, validaciones y almacenamiento local. La compilación y las pruebas en dispositivo deben confirmarse en AVANCE.md antes de considerar esta entrega funcional.

## Abrir
1. En Android Studio, elegir Open y seleccionar esta carpeta completa.
2. Esperar la sincronización. Se necesita internet para descargar Gradle y las bibliotecas la primera vez.
3. Usar el JDK incluido en Android Studio (Java 25 en este equipo; Gradle 9.1 lo admite).
4. Instalar Android SDK Platform 36 y las herramientas solicitadas por Android Studio si faltan.
5. Seleccionar un emulador o teléfono y ejecutar la configuración app.

La aplicación requiere Android 8.0 o posterior. No requiere servidor ni internet. La rama `etapa1/dailer-usuarios` incorpora cuentas **locales**, todavía pendientes de validación en Android Studio. La imagen del emulador puede tener una versión superior a la API mínima de la aplicación.

## Dónde está cada parte
- app/src/main/java/com/example/mientrega/MainActivity.kt: punto de entrada y tema.
- ui/PlannerApp.kt: navegación, listados, formularios y diálogos.
- PlannerViewModel.kt: operaciones de la aplicación y mensajes de error.
- data/PlannerDatabase.kt: tablas, relaciones y consultas Room.
- Rules.kt: validación de nombres, títulos y fechas.
- app/src/test/java/com/example/mientrega/RulesTest.kt: pruebas unitarias de validación.

## Recorrido de demostración
1. Crear Tecnologías Móviles y Matemáticas.
2. Intentar guardar una materia sin nombre y comprobar el mensaje.
3. Crear una actividad con fecha, tipo Proyecto y prioridad Alta.
4. Editar su descripción y cambiar su estado a En progreso.
5. Volver a Inicio y comprobar el resumen.
6. Cerrar y reabrir la app; comprobar persistencia.
7. Marcar la actividad como completada y comprobar el resumen.
8. Intentar eliminar su materia; debe impedirlo mientras haya actividades asociadas.
9. Cancelar una eliminación; el registro debe conservarse.

## Etapa actual y próximos sprints
En `etapa1/dailer-usuarios`, Dailer está incorporando registro/inicio de sesión, sesión local,
separación de actividades por estudiante y migración Room 1 a 2 sin pérdida de datos.
Revisar `ETAPA1_USUARIOS_PRUEBAS.md` antes de considerarlo terminado.
Después corresponden al integrante 2 filtros, sección Para hoy y mejoras de interfaz; al
integrante 3, recordatorios, QA final y APK. No se ha añadido sincronización entre dispositivos.
La posible evolución futura se explica en `PLAN_MIGRACION_ONLINE.md`.

## Herramientas fijadas
Android Gradle Plugin 9.0.1, Gradle 9.1.0, Kotlin integrado 2.2.10, Compose Compiler 2.2.10, Compose BOM 2025.06.01, Room 2.7.2 y KSP 2.3.4. Estas versiones se fijan para reproducibilidad; no se pretende usar siempre la versión más reciente.

