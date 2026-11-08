# Mi Entrega

Aplicación Android para organizar materias, tareas, proyectos y exámenes. Proyecto académico de Tecnologías Móviles para tres integrantes.

## Funcionalidades actuales

- Registro e inicio de sesión local, sesión persistente y datos separados por estudiante.
- Gestión de materias y actividades: tareas, proyectos y exámenes.
- Fechas de entrega, prioridades, estados, validaciones y confirmaciones de eliminación.
- Persistencia local con Room y migración 1→2 conservando los datos existentes.
- Filtros combinables por materia y estado, con restablecimiento y mensajes sin resultados.
- Secciones **Para hoy**, **Vencidas**, **Próximas** y **Completadas**.
- Orden por fecha ascendente, prioridad Alta → Media → Baja e identificador como desempate.
- Resumen de actividades, contadores y mejoras de legibilidad y accesibilidad.

## Etapa 2 — Aporte del Integrante 2

| Tarea | Implementación |
|---|---|
| I2-2 | Filtros por materias reales de la cuenta y por Pendiente, En progreso o Completada; combinables y conservados al navegar/editar. |
| I2-3 | Clasificación por fecha local en cuatro secciones excluyentes, con actualización de contadores y del día del dispositivo. |
| I2-4 | Comparador compartido por fecha, prioridad e identificador; se recalcula después de editar datos. |
| I2-5 | Panel de filtros, estados vacíos, tarjetas más claras, encabezados accesibles, menús desplazables y acciones de guardado identificadas. |
| I2-6 | 16 pruebas unitarias nuevas y una prueba instrumentada nueva de integración con Room. |
| I2-7 | Documentación técnica, registro de verificaciones y checklist en [CAMBIOS_ETAPA2.md](CAMBIOS_ETAPA2.md). |

**Validación:** el Integrante 2 confirmó que todo funciona correctamente en su
entorno de Android Studio y autorizó la publicación. Los informes individuales
de Gradle y las capturas no se adjuntaron; la documentación distingue esa
confirmación de las verificaciones ejecutadas por el agente.

Las actividades completadas pertenecen únicamente a **Completadas**, incluso si
su fecha es hoy o anterior. En Inicio, **Pendientes** conserva la definición
original: todas las actividades no completadas, incluidas las que están En progreso.
El esquema de Room y el sistema de autenticación se mantienen.

## Abrir
1. En Android Studio, elegir Open y seleccionar esta carpeta completa.
2. Esperar la sincronización. Se necesita internet para descargar Gradle y las bibliotecas la primera vez.
3. Usar el JDK 25 de JetBrains configurado para el daemon de Gradle; permitir su descarga si Android Studio la solicita.
4. Instalar Android SDK Platform 36 y las herramientas solicitadas por Android Studio si faltan.
5. Seleccionar un emulador o teléfono y ejecutar la configuración app.

La aplicación requiere Android 8.0 o posterior. Las cuentas y los datos son
**locales al dispositivo**: no requiere servidor ni internet para funcionar y no
sincroniza información entre teléfonos. La primera preparación del entorno y la
descarga de herramientas y dependencias sí pueden necesitar internet.

## Dónde está cada parte
- app/src/main/java/com/example/mientrega/MainActivity.kt: punto de entrada y tema.
- ui/PlannerApp.kt: navegación, listados, formularios y diálogos.
- PlannerViewModel.kt: operaciones de la aplicación y mensajes de error.
- data/PlannerDatabase.kt: tablas, relaciones y consultas Room.
- Rules.kt: validación de nombres, títulos y fechas.
- app/src/test/java/com/example/mientrega/RulesTest.kt: pruebas unitarias de validación.
- AuthSecurity.kt y ui/AuthScreen.kt: reglas de autenticación y acceso local.
- ActivityOrganization.kt: filtros, categorías y ordenamiento compartidos.
- ui/LocalDay.kt: actualización de la fecha local al cambiar el día, reanudar la app o ajustar el reloj/zona.
- app/src/test/java/com/example/mientrega/ActivityOrganizationTest.kt: pruebas unitarias de organización.
- app/src/androidTest/java/com/example/mientrega/ActivityOrganizationPersistenceTest.kt: integración de organización con Room.
- CAMBIOS_ETAPA2.md: detalle del aporte del Integrante 2 y guía de verificación.

## Compilar y ejecutar pruebas

Desde la raíz del proyecto, en PowerShell de Android Studio:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Con un teléfono o emulador conectado:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Para revisar la experiencia de uso, ejecutar `app` con **Run** y seguir el
checklist de `CAMBIOS_ETAPA2.md`. Compilar no sustituye la prueba funcional.

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
10. Crear actividades para ayer, hoy y mañana, y una completada con fecha anterior.
11. Verificar las cuatro secciones; la completada antigua no debe figurar como vencida.
12. Aplicar filtros de materia y estado juntos, editar una actividad y restablecer los filtros.
13. Crear actividades con la misma fecha y distintas prioridades para comprobar el orden.
14. Cerrar sesión y entrar con otra cuenta; sus materias y actividades deben permanecer separadas.

## Etapas del equipo

- **Integrante 1 — Dailer Almanza:** cuentas locales, sesión persistente,
  separación por estudiante y migración Room 1→2. Historial de pruebas en
  [ETAPA1_USUARIOS_PRUEBAS.md](ETAPA1_USUARIOS_PRUEBAS.md).
- **Integrante 2:** filtros, clasificación por fecha, ordenamiento, mejoras de
  interfaz y pruebas de organización. Funcionamiento confirmado por el usuario;
  documentación en [CAMBIOS_ETAPA2.md](CAMBIOS_ETAPA2.md).
- **Integrante 3 — siguiente etapa:** recordatorios, QA final y APK.

La posible evolución futura hacia sincronización se explica en
[PLAN_MIGRACION_ONLINE.md](PLAN_MIGRACION_ONLINE.md).

## Herramientas fijadas
Android Gradle Plugin 9.5.1, Gradle 9.6.0, plugin Compose Compiler 2.2.10,
Compose BOM 2025.06.01, Room 2.7.2 y KSP 2.3.6. El daemon de Gradle está
configurado para JDK 25 de JetBrains; la app mantiene el objetivo JVM 17.
Se conserva la configuración de herramientas presente en la copia validada
por el Integrante 2. No se añadieron bibliotecas de aplicación para los filtros.
