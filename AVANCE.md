# Avance de Mi Entrega

**Actualizado:** 9 de noviembre de 2026  
**Rama de la Etapa 3:** `etapa3/integrante3-recordatorios`  
**Responsable de la Etapa 1:** Dailer Almanza  
**Responsable de la Etapa 3:** Integrante 3  
**Estado:** Etapa 3 implementada y verificada; 66 pruebas unitarias y 31 instrumentadas en verde.

## Base anterior (commit 93ae4ba)

- Aplicación Android nativa Kotlin + Jetpack Compose.
- Materias y actividades: creación, consulta, edición y eliminación.
- Actividades con fecha, tipo, prioridad y estado.
- Resumen de pendientes, vencidas y completadas.
- Persistencia mediante Room, validaciones y restricciones al eliminar materias.
- Recuperación del proyecto a la nueva laptop y publicación del repositorio privado en GitHub.

## Etapa 1: cuentas locales (commit inicial fcdff2d)

- Registro de estudiantes e inicio de sesión sin internet.
- Contraseñas derivadas mediante PBKDF2-HMAC-SHA256 y sal aleatoria.
- Sesión guardada localmente hasta que el usuario cierre sesión.
- Room v1 → v2: tabla de usuarios y propietario de cada materia/actividad.
- Migración explícita, sin eliminación destructiva; primer registro asocia datos antiguos al primer estudiante.
- Operaciones y listados limitados a la cuenta activa.
- Pruebas de contraseñas y aislamiento de datos, documentación de pruebas y plan de futura migración online.

## Evidencias confirmadas por Dailer

| Prueba | Resultado |
| --- | --- |
| `gradlew.bat testDebugUnitTest` | BUILD SUCCESSFUL (26 s) |
| `gradlew.bat assembleDebug` | BUILD SUCCESSFUL (13 s) |
| `gradlew.bat connectedDebugAndroidTest` | 1 prueba ejecutada; BUILD SUCCESSFUL (6 min 17 s) |
| Inicio de la nueva aplicación | Correcto en emulador Medium Phone API 37 |
| Pantalla de acceso | Visible |
| Registro del primer estudiante e ingreso a Inicio | Correcto |
| Conservación de materias y actividades anteriores | Confirmada por el usuario |
| Sesión al cerrar y volver a abrir la aplicación | Persistente, confirmada por el usuario |
| Dos estudiantes con datos independientes | Confirmado por el usuario |

**Precisión:** los resultados manuales son reportados por Dailer durante el recorrido guiado. No constituyen pruebas adicionales automatizadas.

## Últimas comprobaciones manuales (confirmadas el 5 de noviembre)

- ✅ Se rechaza una contraseña equivocada.
- ✅ Se impide registrar otra cuenta con el mismo correo.
- ✅ Dos estudiantes pueden tener materias del mismo nombre sin interferirse.
- ✅ Se impide eliminar una materia que aún tiene actividades.

## Para cerrar definitivamente la etapa 1

- ✅ Esquema de Room `2.json` agregado y publicado en commit `db2d78b`.
- ✅ Pruebas unitarias repetidas tras el ajuste: `BUILD SUCCESSFUL` (11 s).
- ✅ Compilación APK repetida tras el ajuste: `BUILD SUCCESSFUL` (4 s).
- ⏳ Revisar e integrar el Pull Request hacia `main`, y registrar el commit de integración.
- ⏳ Completar la aceptación del relevo al integrante 2; hasta entonces no comienza a programar.

Ver `ETAPA1_USUARIOS_PRUEBAS.md` para el registro detallado.

## Trabajo posterior del equipo

- **Integrante 2:** filtros, sección Para hoy, ordenamiento y mejoras de interfaz.
- **Integrante 3:** ✅ recordatorios, notificaciones, pruebas finales, APK y documentación final (ver sección de la Etapa 3).
- **Los tres:** explicación y demostración de los módulos propios durante la defensa.

## Limitaciones y futuro

Las cuentas y los datos están almacenados en este dispositivo. No existe sincronización entre teléfonos ni servidor, y la contraseña local no cifra el archivo de Room. La propuesta de una posible migración está en `PLAN_MIGRACION_ONLINE.md`.

---

## Etapa 3: recordatorios (Integrante 3)

**Estado:** implementada, probada y lista para demostración.

### Funcionalidad entregada

- Recordatorios de entregas con **WorkManager**, un trabajo único por actividad.
- Política de aviso: **el día anterior a la entrega a las 08:00 hora local**. Si el aviso estándar ya pasó pero la entrega es **hoy**, se programa un respaldo **10 minutos** después de guardar. Si la entrega es **mañana o posterior** y el aviso estándar ya pasó, no se programa nada: ese aviso se considera omitido. La hora del respaldo se conserva en preferencias (`fallback_target_<assignmentId>`) para que las resincronizaciones no la desplacen.
- Si el respaldo ya venció y la actividad sigue pendiente, se marca como entregado (`fallback_delivered_<assignmentId>`) y **no se reprograma**, evitando notificaciones repetitivas. Editar la actividad borra ambas marcas para permitir un nuevo respaldo. La fecha de entrega sigue guardándose como `dueDate` en `epochDay`.
- Toggle global **“Activar recordatorios”** en la pantalla de Ajustes. Está apagado por defecto (opt-in) y se persiste en preferencias privadas.
- Solicitud y gestión de `POST_NOTIFICATIONS` en Android 13+:
  - botón para pedir el permiso,
  - aviso de “Notificaciones bloqueadas”,
  - acceso a los ajustes del sistema cuando la denegación es permanente.
- Un canal de notificaciones propio y notificaciones con **tag e ID estables** para evitar duplicados. El texto incluye la hora del aviso: `Recordatorio programado para las {HH:mm} · vence el {dd/MM/yyyy}`.
- Vista previa en el formulario de actividad: bajo el selector de fecha se muestra `🔔 Te recordaremos el {fecha} a las {hora}`; si el aviso está a menos de 15 minutos (respaldo de hoy), muestra `🔔 Te recordaremos hoy en {N} minutos (a las {HH:mm})`. Si no habrá aviso: `⚠️ La fecha ya pasó o no se programará aviso`.
- Cancelación de recordatorios al completar, eliminar o desactivar una actividad, al apagar el toggle y al cerrar sesión.
- Resincronización al volver a primer plano (`ON_RESUME`) y cuando cambian la fecha, la hora, la zona horaria o se alcanza la medianoche.
- El Worker relee la actividad desde Room y valida propietario, sesión activa, estado, fecha y materia antes de notificar.
- La programación es **best-effort**: un fallo de WorkManager no impide guardar la actividad en Room.
- Room permanece en **versión 2**; no se agregaron tablas ni columnas.

### Archivos principales

- `reminders/ReminderPolicy.kt`: cálculo del instante del aviso, nombres, tags e IDs.
- `reminders/ReminderScheduler.kt`: programación y cancelación con WorkManager.
- `reminders/AssignmentReminderWorker.kt`: validaciones y publicación de la notificación.
- `reminders/ReminderNotifier.kt`: canal, publicación y cancelación de notificaciones.
- `reminders/ReminderCoordinator.kt`: ciclo de vida completo de los recordatorios.
- `reminders/ReminderDependencies.kt`: dependencias reales del Worker y sustitutos de prueba.
- `reminders/NotificationPermission.kt`: estado del permiso y apertura de ajustes.
- `ui/SettingsScreen.kt`: switch, estado del permiso y textos explicativos.
- `PlannerViewModel.kt` y `ui/PlannerApp.kt`: integración con el guardado, la sesión y la navegación.

### Pruebas y evidencias

| Prueba | Resultado |
| --- | --- |
| `gradlew.bat testDebugUnitTest` | **66 pruebas, 0 fallos** |
| `gradlew.bat connectedDebugAndroidTest` | **31 pruebas, 0 fallos** |
| `gradlew.bat compileDebugKotlin` | `BUILD SUCCESSFUL` |
| `gradlew.bat compileDebugAndroidTestKotlin` | `BUILD SUCCESSFUL` |
| `gradlew.bat lintDebug` | `BUILD SUCCESSFUL` (solo advertencias) |
| Comprobación manual en emulador Medium Phone | Ajustes abre desde Inicio, el switch solicita el permiso y al concederlo muestra “Notificaciones activas” |

`lintDebug` detectó una llamada a `LocalDate.ofInstant` que requería API 34; se sustituyó por `instant().atZone(zona).toLocalDate()` para conservar la compatibilidad con API 26. Tras el cambio, unitarias, instrumentadas, lint y APK vuelven a pasar.

Las 31 pruebas instrumentadas incluyen 8 de `ReminderLifecycleTest`:

- crear una actividad deja exactamente un trabajo activo,
- editar mantiene un solo trabajo y actualiza el delay,
- completar cancela el trabajo y borra el respaldo guardado,
- eliminar cancela el trabajo y borra el respaldo guardado,
- guardar tres veces la misma actividad no genera duplicados,
- apagar el toggle cancela todos los trabajos de la cuenta,
- sincronizar dos veces una entrega de hoy conserva la misma hora de respaldo,
- un respaldo vencido no se reprograma y queda marcado como entregado.

También se conservan las pruebas del Worker, del DAO, del notificador y de las preferencias; estas últimas verifican las claves `fallback_target_<assignmentId>` y `fallback_delivered_<assignmentId>`, su persistencia y su eliminación selectiva. `ReminderPolicyTest` cubre el aviso estándar a las 08:00, el respaldo de 10 minutos solo para entregas de hoy, la omisión del aviso para una tarea de mañana cuyo aviso estándar ya pasó y el caso sin recordatorio cuando la fecha ya pasó. `ReminderNotifierTest` comprueba que el texto de la notificación incluye la hora programada. `ReminderPreviewTest` cubre el texto cercano de 10 minutos, el aviso estándar con fecha y hora, la advertencia para fechas sin aviso y el caso sin fecha seleccionada. `ReminderCoordinatorTest` simula dos `sync()` consecutivos con el reloj avanzado, comprueba que el respaldo no cambia, que un respaldo vencido no genera otro trabajo y que editar la actividad limpia la marca de entregado.

### APK de evidencia

El APK de debug se genera con:

```powershell
.\gradlew.bat :app:assembleDebug
```

Archivo resultante:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Para instalarlo en un teléfono o emulador conectado por ADB:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```
