# Plan de arquitectura — Etapa 3 (Recordatorios)

**Proyecto:** Mi Entrega  
**Etapa:** 3  
**Alcance:** I3-2, I3-3, I3-4, I3-5 e I3-6  
**Tecnologías:** Kotlin, Jetpack Compose, Room, WorkManager  
**Fecha:** 8 de noviembre de 2026

---

## 0. Lectura del proyecto actual

Antes de proponer archivos, hay que alinear el vocabulario:

- En el código, la entidad **Actividad** se llama `Assignment` y vive en la tabla `assignments`.
- `dueDate` **no es un timestamp**: es un `Long` con el día civil (`LocalDate.toEpochDay()`).
- `status` es un `String` (`"Pendiente"`, `"En progreso"`, `"Completada"`).
- Cada actividad tiene `ownerId`, y los DAOs ya están filtrados por usuario.
- `id` es `INTEGER PRIMARY KEY AUTOINCREMENT`, así que un ID eliminado **no se reutiliza**. Esto ayuda a evitar avisos asociados a una actividad nueva.
- Ya existen `PlannerDatabase`, `PlannerViewModel`, `LocalDay` y un patrón de sesión con `SharedPreferences`.
- Actualmente no existe ningún código de notificaciones ni de WorkManager.

---

## 1. Decisiones arquitectónicas recomendadas

### 1.1 WorkManager en lugar de AlarmManager

**Recomendación: WorkManager** con `OneTimeWorkRequest` y `enqueueUniqueWork`.

Motivos:

- Se adapta mejor a `minSdk 26` y evita el permiso `SCHEDULE_EXACT_ALARM` de Android 12+.
- WorkManager sobrevive a reinicios del dispositivo y actualizaciones de la app sin un `BOOT_COMPLETED` propio.
- El recordatorio no necesita precisión al segundo; una diferencia de minutos por Doze es aceptable.
- Tiene soporte oficial de pruebas con `WorkManagerTestInitHelper` y `TestDriver`.
- Se puede encapsular detrás de una interfaz `ReminderScheduler`; si más adelante se exigiera precisión exacta, se cambia la implementación por AlarmManager sin tocar ViewModel/UI.

**Contrapartida:** WorkManager no garantiza la hora exacta y su `initialDelay` se basa en tiempo transcurrido, no en reloj de pared. Por eso el plan incluye una **resincronización** al volver a la app, al cambiar la fecha/hora/zona y al activar el toggle.

### 1.2 Una sola política de recordatorio, sin cambiar Room

Para el MVP:

- Un recordatorio por actividad.
- Se programa para **el día anterior a la entrega a las 20:00 hora local** (constante configurable).
- Si la hora calculada ya pasó, no se programa nada.
- Si la actividad está completada, vencida o eliminada, se cancela.
- No se agrega una columna `reminderAt` a `Assignment` ni una tabla nueva.

Ventajas:

- No hay migración de Room: la base sigue en versión 2 y `2.json` no cambia.
- WorkManager es la fuente de verdad de “qué está programado”.
- Room sigue siendo la fuente de verdad del contenido de la notificación.

> La política exacta (día anterior a las 20:00 vs. mismo día a las 09:00) es una decisión de producto. Lo importante es que quede aislada en `ReminderPolicy` y cubierta por pruebas. Más abajo dejo esta decisión como punto a confirmar.

### 1.3 Toggle global, no por actividad

`I3-3` pide activar/desactivar los recordatorios. Se implementa como preferencia global:

- `reminders_enabled` en `SharedPreferences` (`mi-entrega-settings`), consistente con la sesión actual.
- Valor inicial recomendado: **desactivado** (opt-in). El usuario lo activa y ahí se solicita el permiso.
- Si el permiso está denegado, la preferencia puede seguir activa, pero la UI muestra el estado “Notificaciones bloqueadas” y el Worker no publica nada.

### 1.4 Integridad y no duplicados

- Nombre único de trabajo: `assignment-reminder-{ownerId}-{assignmentId}`.
- Política `ExistingWorkPolicy.REPLACE`: al editar, se reemplaza el trabajo anterior.
- Etiqueta por usuario: `assignment-reminders-owner-{ownerId}`, para cancelar todo lo de una cuenta.
- Identidad de notificación: `tag = "assignment-{ownerId}-{assignmentId}"` e `id` derivado del ID. Android identifica una notificación por la pareja tag + id, así que el tag ya garantiza unicidad aunque el `Long` se convierta a `Int`.
- El Worker **relee la actividad desde Room** antes de notificar y valida:
  - que exista,
  - que pertenezca al `ownerId`,
  - que no esté completada,
  - que la fecha siga siendo la esperada,
  - que el usuario activo siga siendo el dueño,
  - que los recordatorios sigan activos,
  - que las notificaciones estén habilitadas en el sistema.
- Si algo falla, termina con éxito sin notificar. Si falla por un error transitorio de base de datos, puede devolver `Result.retry()`.

---

## 2. Clases y archivos a crear/modificar

### 2.1 Archivos nuevos

| Archivo | Responsabilidad |
|---|---|
| `reminders/ReminderPolicy.kt` | Lógica pura: calcular el instante del recordatorio, decidir si aplica, generar nombres únicos, tags e IDs. Sin Android. |
| `reminders/ReminderPreferences.kt` | Interfaz + implementación con `SharedPreferences`. Expone `enabled` como `StateFlow<Boolean>` y `setEnabled`. |
| `reminders/ReminderScheduler.kt` | Interfaz `schedule/cancel/cancelAllFor/cancelAll` + implementación con WorkManager. |
| `reminders/ReminderNotifier.kt` | Interfaz + implementación Android: crear canal, construir y publicar la notificación, cancelarla. |
| `reminders/ReminderCoordinator.kt` | Orquesta el ciclo de vida: sincronizar pendientes, programar tras guardar, cancelar al completar/eliminar, aplicar toggle. |
| `reminders/AssignmentReminderWorker.kt` | `CoroutineWorker` que valida y publica la notificación. |
| `reminders/ReminderDependencies.kt` | Localizador mínimo de dependencias para que el Worker sea testeable con base en memoria y notificador falso. Alternativa: `WorkerFactory` personalizado. |
| `reminders/NotificationPermission.kt` | Utilidades para saber si se puede notificar y si se debe solicitar `POST_NOTIFICATIONS`. |
| `MiEntregaApplication.kt` | Crear el canal de notificaciones al iniciar la app e inicializar dependencias. Opcional si se prefiere crear el canal de forma perezosa en el notificador. |
| `ui/SettingsScreen.kt` | Pantalla de ajustes con el switch de recordatorios, estado del permiso y acceso a ajustes del sistema. |
| Pruebas unitarias y de instrumentación | Ver sección 6. |

### 2.2 Archivos a modificar

| Archivo | Cambio |
|---|---|
| `app/build.gradle.kts` | Agregar `work-runtime-ktx`, `work-testing` y dependencias de prueba necesarias. |
| `AndroidManifest.xml` | Declarar `POST_NOTIFICATIONS`; registrar `MiEntregaApplication`; opcionalmente `launchMode="singleTop"` en `MainActivity`. |
| `data/PlannerDatabase.kt` | Agregar consultas DAO de lectura por ID/propietario y de pendientes. **Sin cambiar entidades ni versión de la base.** |
| `PlannerViewModel.kt` | Estado del toggle, coordinador, hooks de guardado/edición/completado/eliminación, login/logout y sincronización. |
| `ui/PlannerApp.kt` | Ruta `settings`, icono de ajustes en la barra superior, paso de estado/permiso y sincronización al volver a primer plano. |
| `MainActivity.kt` | Crear canal si no se hace en Application; opcionalmente manejar el extra de la notificación para abrir una actividad. |
| Documentación | Actualizar `AVANCE.md`, `DECISIONES.md` y agregar un documento de pruebas de la etapa 3. |

### 2.3 Dependencias sugeridas

- `implementation("androidx.work:work-runtime-ktx:2.10.1")` — fijar la misma versión en `work-testing`.
- `androidTestImplementation("androidx.work:work-testing:2.10.1")`.
- `testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")` si se prueba el coordinador en JVM.
- `androidTestImplementation("androidx.test:rules:1.6.1")` para `GrantPermissionRule`.
- Opcional para probar el switch: `androidTestImplementation("androidx.compose.ui:ui-test-junit4")` y `debugImplementation("androidx.compose.ui:ui-test-manifest")`.
- Si `NotificationManagerCompat` no resuelve, agregar explícitamente `androidx.core:core-ktx`.

---

## 3. Interacción con `Assignment` y los DAOs

### 3.1 Consultas nuevas en `PlannerDao`

No hace falta tocar las entidades ni la versión de Room.

| Consulta | Uso |
|---|---|
| `assignmentById(id, ownerId): Assignment?` | El Worker relee la actividad concreta y valida propiedad. |
| `pendingForReminders(ownerId, fromDay): List<Assignment>` | Sincronización: actividades no completadas con `dueDate >= fromDay`. |
| `subjectName(subjectId, ownerId): String?` | Texto de la notificación con la materia correcta y validada por propietario. |

Todas deben ser `suspend` y estar filtradas por `ownerId`; nunca consultar una actividad solo por `id`.

### 3.2 Uso de los métodos existentes

| Evento actual | Método DAO | Acción de recordatorio |
|---|---|---|
| Crear actividad | `insertAssignment(...)` | Capturar el `Long` que ya devuelve el método y llamar a `coordinator.onAssignmentSaved(id, ownerId)`. |
| Editar actividad | `updateAssignment(...)` | Si devuelve `1`, releer la fila y reprogramar o cancelar según fecha/estado. |
| Completar | `changeStatus(id, ownerId, "Completada")` | Cancelar trabajo y notificación mostrada. |
| Eliminar | `deleteAssignment(id, ownerId)` | Cancelar trabajo y notificación mostrada. |
| Login/registro/restauración de sesión | `pendingForReminders(...)` | Cancelar trabajos de esa cuenta y reprogramar los pendientes futuros. |
| Logout | — | Cancelar todos los trabajos y notificaciones de la cuenta activa. |
| Eliminar materia | `deleteSubject(...)` | No aplica: la regla actual impide borrar una materia con actividades. Si en el futuro se agrega cascada, habría que resincronizar. |
| Renombrar materia | `updateSubject(...)` | No hace falta reprogramar: el Worker lee el nombre actual al ejecutarse. |

### 3.3 Punto importante sobre `saveAssignment`

El ViewModel actual descarta el ID devuelto por `insertAssignment`. Hay que capturarlo para poder programar el trabajo. Además, después de guardar conviene **releer la fila persistida** y no confiar en el objeto que vino del formulario: así el recordatorio siempre refleja título, fecha, estado y materia reales.

### 3.4 Punto importante sobre `write()`

El helper `write()` actualmente convierte cualquier excepción en “No se pudo guardar”. La programación del recordatorio debe quedar **fuera del camino crítico de Room** y en su propio `try/catch`: si WorkManager falla, la actividad ya quedó guardada y solo se muestra un aviso secundario, no un error de guardado. Tampoco se debe programar dentro de `database.withTransaction`.

---

## 4. Flujos detallados

### 4.1 Cálculo del recordatorio

`ReminderPolicy` debe recibir:

- `dueDate` (epochDay),
- `Clock`,
- `ZoneId`.

Y devolver el `Instant` objetivo o `null` si ya pasó.

Fórmula conceptual:

1. Convertir `dueDate` a `LocalDate`.
2. Restar los días configurados (`REMINDER_DAYS_BEFORE`, por defecto 1).
3. Aplicar la hora local (`REMINDER_HOUR`, por defecto 20).
4. Convertir a `ZonedDateTime` con la zona del dispositivo.
5. Comparar con `Clock.instant()`.
6. Si el instante objetivo es pasado, devolver `null`.

Esta clase debe ser **Kotlin puro**, sin `Context`, para poder probarla en JVM con `Clock.fixed`.

### 4.2 Programación

`WorkManagerReminderScheduler.schedule(assignment)` debe:

- Calcular el delay con `ReminderPolicy`.
- Si es `null`, cancelar el trabajo existente.
- Crear un `OneTimeWorkRequest` con:
  - datos de entrada: `assignmentId`, `ownerId`, fecha esperada,
  - etiquetas: recordatorio general + etiqueta del propietario,
  - `setInitialDelay(delay, MILLISECONDS)`,
  - sin constraints de red.
- Encolar con `enqueueUniqueWork(nombreUnico, REPLACE, request)`.

No usar trabajos periódicos: cada actividad tiene una sola fecha.

### 4.3 Ejecución del Worker

Orden de validaciones:

1. ¿Recordatorios activos en preferencias? Si no, terminar con éxito.
2. ¿Notificaciones habilitadas en el sistema? Si no, terminar con éxito sin publicar.
3. ¿Usuario activo de la sesión coincide con `ownerId`? Si no, terminar.
4. Leer `assignmentById(assignmentId, ownerId)`. Si no existe, cancelar cualquier notificación y terminar.
5. Verificar estado distinto de `"Completada"`.
6. Verificar que la fecha actual siga siendo la esperada; si cambió, no notificar y dejar que la resincronización reprograme.
7. Verificar que el recordatorio siga siendo relevante (por ejemplo, no notificar si ya pasó el día de entrega).
8. Leer el nombre de la materia con `subjectName(subjectId, ownerId)`.
9. Publicar la notificación.
10. Revalidar estado/preferencias después de publicar y cancelar si cambiaron durante la carrera. Esto cubre el caso de completar/eliminar justo mientras el Worker se ejecuta.

Resultado:

- Casos normales, datos inválidos o ya no aplicables: `Result.success()`.
- Error transitorio de base de datos: `Result.retry()`.

### 4.4 Notificación

- Canal: `assignment_reminders`, importancia alta o predeterminada, nombre “Recordatorios de entregas”.
- Título: “Entrega mañana: {título}” o “Recordatorio: {título}”.
- Texto: “{materia} · vence el {dd/MM/yyyy} · {tipo}”.
- `tag` único por propietario y actividad; `id` estable.
- `PendingIntent` inmutable y con `FLAG_UPDATE_CURRENT`.
- Tocar la notificación abre la app; opcionalmente navega a la actividad usando un extra.
- `setAutoCancel(true)`.
- `setVisibility(VISIBILITY_PRIVATE)` para no mostrar detalles en la pantalla de bloqueo.
- Al completar, eliminar o desactivar el toggle, cancelar la notificación mostrada.

### 4.5 Toggle y `POST_NOTIFICATIONS`

- Declarar `android.permission.POST_NOTIFICATIONS` en el manifiesto.
- En `SettingsScreen`, el switch controla `remindersEnabled`.
- Al activar:
  1. Guardar la preferencia.
  2. Si Android >= 33 y no hay permiso, lanzar `RequestPermission`.
  3. Si se concede, sincronizar recordatorios.
  4. Si se deniega, mantener la preferencia pero mostrar “Notificaciones bloqueadas” con un botón para abrir los ajustes de la app. El Worker no publicará nada mientras esté bloqueado.
- En Android < 33 no hay permiso runtime, pero igual conviene comprobar `areNotificationsEnabled()`.
- Si el usuario denegó permanentemente, ofrecer `Settings.ACTION_APP_NOTIFICATION_SETTINGS` con el paquete de la app.
- Al volver del sistema a la app (`ON_RESUME`), resincronizar y refrescar el estado del permiso.

### 4.6 Sincronización y ciclo de vida

Puntos de resincronización:

- Después de restaurar la sesión activa.
- Después de login o registro.
- Al activar el toggle.
- Al volver a primer plano (`ON_RESUME`).
- Al cambiar fecha/hora/zona del dispositivo (puede reutilizarse el patrón de `LocalDay` o hacerlo al reanudar).
- Tras guardar, editar, completar o eliminar una actividad (puntos concretos, no sincronización completa).

Algoritmo de sincronización de una cuenta:

1. Si el toggle está apagado: cancelar todos los trabajos de la cuenta y terminar.
2. Si está encendido: obtener pendientes con `pendingForReminders(ownerId, hoy - 1)`.
3. Para cada una, calcular si aplica y programar.
4. Cancelar trabajos obsoletos. Para el MVP se puede hacer `cancelAllFor(ownerId)` y volver a encolar; es simple y suficiente. Una optimización futura sería comparar trabajos existentes y cancelar solo los que cambiaron.

Al cerrar sesión:

- Cancelar todos los trabajos de la cuenta.
- Cancelar las notificaciones mostradas.
- No dejar avisos de una cuenta visible mientras otra está activa.

### 4.7 Checklist de integridad

- [ ] El trabajo incluye `ownerId` y `assignmentId`.
- [ ] El nombre único incluye ambos.
- [ ] El Worker consulta por ID **y** propietario.
- [ ] El Worker valida la sesión activa.
- [ ] El Worker valida estado, fecha y materia.
- [ ] El contenido se construye con datos releídos de Room.
- [ ] La notificación usa tag único.
- [ ] Editar reemplaza el trabajo, no crea otro.
- [ ] Completar/eliminar cancelan trabajo y notificación.
- [ ] Logout cancela todo lo de la cuenta.
- [ ] Los IDs `AUTOINCREMENT` evitan reutilización.
- [ ] No se sincroniza desde una recomposición de Compose.

---

## 5. Plan paso a paso para el modo Build

### Paso 0 — Preparación

- Crear rama `etapa3/recordatorios`.
- Ejecutar la línea base: `gradlew.bat testDebugUnitTest`, `gradlew.bat connectedDebugAndroidTest`, `gradlew.bat assembleDebug`.
- Confirmar que los tests actuales pasan antes de tocar nada.

### Paso 1 — Dependencias y manifiesto

- Agregar WorkManager y `work-testing`.
- Declarar `POST_NOTIFICATIONS`.
- Registrar `MiEntregaApplication` si se usa el localizador de dependencias.
- **Criterio de cierre:** compila sin cambios funcionales.

### Paso 2 — `ReminderPolicy` + pruebas unitarias

- Implementar cálculo de fecha/hora, nombres únicos, tags e IDs.
- Probar: fecha futura, fecha pasada, borde exacto, cambio de zona, horario de verano, estado completado.
- **Criterio:** pruebas JVM en verde, sin dependencias de Android.

### Paso 3 — DAO

- Agregar `assignmentById`, `pendingForReminders` y `subjectName`.
- Probar aislamiento entre usuarios y exclusión de completadas.
- **Criterio:** Room sigue en versión 2; no aparece un `3.json`; los tests de persistencia existentes pasan.

### Paso 4 — Preferencias

- Implementar `ReminderPreferences` con `SharedPreferences`.
- Exponer `StateFlow<Boolean>`.
- **Criterio:** el valor persiste tras reiniciar la app y el valor por defecto es el acordado.

### Paso 5 — Notificador y canal

- Crear canal, construir notificación y cancelar por tag.
- Implementar comprobación de notificaciones habilitadas.
- **Criterio:** una notificación de prueba se publica y se cancela correctamente en el emulador.

### Paso 6 — Scheduler, Worker y dependencias

- Implementar `ReminderScheduler` con WorkManager.
- Implementar `AssignmentReminderWorker` con todas las validaciones.
- Configurar el acceso a DAO/notificador/preferencias de forma testeable.
- **Criterio:** programar un trabajo con retraso corto produce una notificación; cancelarlo evita la notificación.

### Paso 7 — Coordinador

- Implementar `sync`, `onAssignmentSaved`, `onCompleted`, `onDeleted`, `setEnabled`.
- Inyectar `Clock`, `ZoneId`, DAO, scheduler, preferencias y notificador.
- **Criterio:** pruebas JVM con fakes para los cinco flujos.

### Paso 8 — Integración con el ViewModel

- Capturar el ID de `insertAssignment`.
- Releer la fila después de crear/editar.
- Llamar al coordinador tras el éxito de Room.
- Cancelar en completar/eliminar.
- Sincronizar al restaurar sesión, login y registro.
- Cancelar al logout.
- Exponer `remindersEnabled` y `setRemindersEnabled`.
- **Criterio:** los flujos de UI siguen funcionando y las operaciones de recordatorio son best-effort.

### Paso 9 — Sincronización al reanudar y cambios de reloj

- Enganchar `ON_RESUME` en `PlannerApp` o `MainActivity`.
- Reutilizar el patrón de receptores de `LocalDay` si se quiere reaccionar a cambios de fecha/hora/zona.
- Evitar llamadas desde recomposiciones.
- **Criterio:** al conceder el permiso desde Ajustes y volver, los recordatorios futuros se reprograman.

### Paso 10 — UI del toggle y permiso

- Agregar icono de ajustes en la barra superior.
- Agregar ruta `settings` y `SettingsScreen`.
- Implementar switch, estado del permiso, botón “Permitir” y botón “Abrir ajustes”.
- Opcional: banner en Inicio cuando hay pendientes y los recordatorios están apagados.
- **Criterio:** activar/desactivar cambia el estado, solicita permiso en Android 13+ y muestra el estado bloqueado si se deniega.

### Paso 11 — Pruebas instrumentadas

- Configurar `WorkManagerTestInitHelper`.
- Probar Worker con base en memoria y notificador falso.
- Probar ciclo de vida completo y ausencia de duplicados.
- Probar una notificación real con permiso concedido.
- **Criterio:** todas las pruebas nuevas pasan en emulador API 33+.

### Paso 12 — Regresión, QA manual y documentación

- Ejecutar tests unitarios, instrumentados, `assembleDebug` y `lintDebug`.
- Revisar la matriz manual de la sección 6.4.
- Actualizar `AVANCE.md`, `DECISIONES.md` y el documento de pruebas.
- Generar el APK de evidencia.
- **Criterio:** no hay regresiones en usuarios, materias, actividades ni filtros.

---

## 6. Estrategia de pruebas

### 6.1 Pruebas unitarias (`src/test`)

| Archivo | Qué cubre |
|---|---|
| `reminders/ReminderPolicyTest.kt` | Cálculo de instante, fecha pasada, borde exacto, zona horaria, DST, estado completado, nombres/tags/IDs únicos. |
| `reminders/ReminderCoordinatorTest.kt` | Sincronización, guardado, edición, completado, eliminación, toggle, logout, con fakes de DAO/scheduler/prefs/notifier. |
| `reminders/ReminderIdsTest.kt` | Que dos actividades distintas generen nombres y tags distintos, y que la misma actividad genere siempre el mismo valor. |

Ventaja: estas pruebas corren rápido en JVM y no necesitan emulador.

### 6.2 Pruebas instrumentadas (`src/androidTest`)

| Archivo | Qué cubre |
|---|---|
| `reminders/ReminderDaoTest.kt` | `assignmentById` y `pendingForReminders` respetan `ownerId`; no devuelven datos de otra cuenta. |
| `reminders/AssignmentReminderWorkerTest.kt` | Worker con base en memoria, WorkManager de prueba y notificador falso: notifica solo si todo es válido; no notifica si está completada, eliminada, apagada o con fecha cambiada. |
| `reminders/ReminderLifecycleTest.kt` | Crear → un trabajo; editar → sigue habiendo uno y con el nuevo delay; completar → cancelado; eliminar → cancelado; desactivar → todos cancelados. |
| `reminders/ReminderNotificationTest.kt` | Notificación real: se publica, tiene el contenido correcto y se cancela. En API 33+ usar `GrantPermissionRule` para `POST_NOTIFICATIONS`; en API < 33 no hace falta. |
| `reminders/ReminderSettingsUiTest.kt` | Opcional: el switch cambia de estado y muestra el aviso de permiso. |

Configuración de WorkManager en pruebas:

- `WorkManagerTestInitHelper.initializeTestWorkManager(context, config)`.
- Ejecutor síncrono para evitar esperas.
- `TestDriver.setInitialDelayMet(workId)` para disparar el Worker.
- Verificar con `getWorkInfosForUniqueWork(nombre)` que existe exactamente un trabajo.
- Para el Worker, inyectar base en memoria y notificador falso mediante el localizador de dependencias o un `WorkerFactory` de prueba.

### 6.3 Pruebas de no duplicados

- Guardar la misma actividad varias veces y comprobar que solo hay un `WorkInfo` para su nombre único.
- Ejecutar el Worker dos veces y comprobar que la notificación se actualiza, no se duplica.
- Editar título/fecha repetidamente y comprobar que el trabajo anterior se reemplazó.
- Completar y luego reabrir la actividad: debe programarse de nuevo, sin duplicar.

### 6.4 Matriz manual sugerida

1. Activar recordatorios en Android 13+ y conceder permiso.
2. Crear una actividad para mañana; verificar la notificación a la hora configurada.
3. Editar la fecha y confirmar que solo llega el aviso nuevo.
4. Completar la actividad y confirmar que no llega aviso y que el existente desaparece.
5. Eliminar la actividad y confirmar lo mismo.
6. Desactivar el toggle: no deben llegar avisos; reactivarlo: deben reprogramarse los futuros.
7. Denegar el permiso: la app no debe fallar y debe mostrar el estado bloqueado.
8. Conceder el permiso desde Ajustes y volver: debe resincronizar.
9. Dos cuentas: el aviso de A no debe aparecer con B activo; cerrar sesión debe cancelar.
10. Cambiar zona horaria y confirmar que la hora local se recalcula.
11. Reiniciar el dispositivo: WorkManager debe conservar el trabajo.
12. Forzar detención de la app: documentar que Android puede posponer la ejecución hasta que se vuelva a abrir.
13. Ejecutar toda la suite existente y la nueva.

### 6.5 Regresión

Comandos de cierre:

- `gradlew.bat testDebugUnitTest`
- `gradlew.bat connectedDebugAndroidTest`
- `gradlew.bat assembleDebug`
- `gradlew.bat lintDebug` si el proyecto lo permite

Verificar especialmente:

- `PersistenceTest` y `ActivityOrganizationPersistenceTest` siguen pasando.
- Room no exige migración nueva.
- Los filtros, el resumen y el login/logout no cambian de comportamiento.

---

## 7. Criterios de aceptación por requisito

| Requisito | Criterio de aceptación |
|---|---|
| **I3-2** | Una actividad pendiente con fecha futura genera un trabajo único y una notificación con título, materia y fecha correctos. |
| **I3-3** | El toggle persiste; activarlo solicita `POST_NOTIFICATIONS` en Android 13+; desactivarlo cancela trabajos y notificaciones; el estado bloqueado se muestra sin fallos. |
| **I3-4** | Editar fecha reprograma; completar cancela; eliminar cancela; reabrir una completada vuelve a programar. |
| **I3-5** | El Worker valida propietario, sesión, estado y fecha; no hay trabajos ni notificaciones duplicadas; el contenido se relee de Room. |
| **I3-6** | Hay pruebas unitarias de política/coordinador, instrumentadas de DAO/Worker/ciclo de vida/notificación, y la suite existente sigue en verde. |

---

## 8. Riesgos y decisiones a confirmar

1. **Hora del recordatorio:** el plan asume día anterior a las 20:00. Si el equipo prefiere “mismo día a las 09:00”, solo cambia `ReminderPolicy` y sus pruebas.
2. **Valor inicial del toggle:** se recomienda apagado por privacidad y para no pedir permiso al primer arranque. Si el profesor espera que funcione sin configurar, se puede dejar encendido, pero entonces hay que pedir permiso en un momento contextual.
3. **WorkManager no es exacto:** si la entrega exige precisión al minuto, habría que cambiar la implementación de `ReminderScheduler` a AlarmManager, con `SCHEDULE_EXACT_ALARM`, receptor de arranque y resincronización. La arquitectura ya deja ese cambio aislado.
4. **Cambios de hora/zona:** WorkManager no ajusta el delay por reloj de pared; por eso la resincronización al reanudar es obligatoria.
5. **Force-stop:** Android puede no ejecutar trabajos hasta que el usuario vuelva a abrir la app. Documentarlo como limitación del sistema.
6. **Carrera Worker vs. completar/eliminar:** mitigar con la revalidación posterior a publicar la notificación.
7. **No agregar tabla de recordatorios:** WorkManager ya persiste el estado. Si se quisiera historial o auditoría, sería una etapa posterior con migración a versión 3.
8. **Canal de notificaciones:** una vez creado, el usuario puede cambiar su importancia; elegir el nivel definitivo desde la primera versión.
9. **Sincronización al reanudar:** no debe ejecutarse en cada recomposición; solo en eventos de ciclo de vida, toggle o cambios reales de datos.

Con este plan, la implementación queda separada en cuatro capas claras: **política pura**, **persistencia/DAO**, **WorkManager/notificaciones** y **UI/ViewModel**, lo que facilita las pruebas y evita mezclar reglas de negocio con Android.
