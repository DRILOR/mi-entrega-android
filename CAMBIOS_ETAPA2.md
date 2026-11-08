# Cambios de la Etapa 2 — Mi Entrega

## 1. Información general

- **Proyecto:** Mi Entrega.
- **Etapa:** 2.
- **Responsable:** Integrante 2 (nombre pendiente de completar).
- **Fecha de elaboración:** 8 de noviembre de 2026.
- **Tecnologías utilizadas:** Kotlin, Jetpack Compose, Material Design 3, Navigation
  Compose, AndroidViewModel, Coroutines/StateFlow, Room, java.time y JUnit 4.
- **Modalidad:** implementación local, validación en la máquina del Integrante 2 y
  publicación autorizada posteriormente por el usuario.

## 2. Estado inicial e inspección

I2-1 ya había sido verificada manualmente por el usuario en Android Studio. Esa
verificación corresponde a la versión inicial, no certifica los cambios de esta etapa.

Componentes encontrados en el paquete `com.example.mientrega`:

| Componente | Estado recibido |
|---|---|
| `MainActivity.kt` | Punto de entrada, tema claro/oscuro, pantalla de acceso y contenido asociado a la cuenta mediante `key(userId)`. |
| `ui/PlannerApp.kt` | Inicio con resumen y cinco pendientes por fecha; materias, actividades, formularios y confirmaciones. Rutas `home`, `subjects`, `assignments`, `subject/{id}`, `assignment/{id}`. |
| `ui/AuthScreen.kt`, `AuthSecurity.kt` | Registro, inicio de sesión local y hash de contraseñas con sal. |
| `PlannerViewModel.kt` | AndroidViewModel, sesión en SharedPreferences, Flow por propietario, altas/ediciones/bajas y cambio a Completada. No existe repositorio separado. |
| `data/PlannerDatabase.kt` | Entidades StudentUser, Subject y Assignment; PlannerDao; Room versión 2, migración explícita 1→2. |
| `Rules.kt` | Validaciones y catálogos: Tarea/Proyecto/Examen; Baja/Media/Alta; Pendiente/En progreso/Completada. |
| Pruebas | `RulesTest`, `AuthSecurityTest` y prueba instrumentada `PersistenceTest`. |

Las consultas de materias y actividades ya se limitaban por `ownerId`. El DAO
ordenaba actividades por `dueDate, id`, sin desempatar por prioridad. No existían
filtros por materia/estado ni clasificación completa en las cuatro secciones.
Los componentes Choice, EmptyMessage, ActivityCard y ConfirmDelete se reutilizaron.

Configuración recibida: Gradle 9.1.0, Android Gradle Plugin 9.0.1, plugin Compose
2.2.10, KSP 2.3.4, Compose BOM 2025.06.01, Room 2.7.2, Navigation 2.9.0,
Lifecycle 2.9.1, Activity Compose 1.10.1, JUnit 4.13.2, SDK mínimo 26 y
compile/target SDK 36, objetivo JVM 17. La implementación inicial no cambió estas
versiones. Antes de publicar se encontraron ajustes de herramientas en la copia
local: AGP 9.5.1, Gradle 9.6.0, KSP 2.3.6, resolución Foojay 1.0.0 y daemon
con JDK 25 de JetBrains. Se conservan esos ajustes de la copia validada por el usuario.

## 3. Funcionalidades implementadas

### I2-2 — Filtros por materia y estado

- Selectores en Actividades con Todas las materias y Todos los estados.
- Materias procedentes del Flow de Room de la cuenta activa; estados de `Rules.statuses`.
- Combinación AND: deben coincidir materia y estado cuando ambos están seleccionados.
- Cada selector conserva el valor del otro. Botón Restablecer filtros.
- Número de resultados y mensaje específico cuando la combinación no tiene resultados.
- Selección conservada con `rememberSaveable` durante recomposición, edición y
  navegación entre pestañas mediante el mecanismo de restauración ya existente.
- Los nuevos resultados se calculan a partir de las emisiones de actividades:
  una edición que deja de coincidir o una eliminación retira el registro de la lista.
- Si se elimina una materia sin actividades mientras estaba seleccionada, se
  muestra Materia no disponible y cero resultados. Se puede cambiar o restablecer
  el filtro; no se restablece el otro selector silenciosamente.
- No se escriben registros ni se ejecutan consultas adicionales al cambiar filtros.

### I2-3 — Para hoy y clasificación

Inicio y Actividades muestran estas secciones excluyentes, en este orden:

1. **Para hoy:** fecha igual a hoy y estado distinto de Completada.
2. **Vencidas:** fecha anterior a hoy y estado distinto de Completada.
3. **Próximas:** fecha posterior a hoy y estado distinto de Completada.
4. **Completadas:** estado Completada, cualquiera que sea su fecha.

Cada sección muestra su cantidad y un mensaje cuando está vacía. En Actividades,
si el resultado total está vacío se muestra un mensaje global específico en lugar
de cuatro secciones vacías. Una completada con fecha de hoy pertenece solamente
a Completadas; no existe un campo de fecha de finalización en el modelo.

Inicio deja de limitarse a cinco pendientes: las cuatro secciones se desplazan
en una LazyColumn y sus tarjetas permiten abrir la edición.

**Definición exacta de contadores de Inicio:**

- **Pendientes:** se conserva la definición previa: todos los registros que NO
  estén completados, incluyendo En progreso y los vencidos. Se aclara en pantalla.
- **En progreso:** estado exactamente En progreso; es un subconjunto del anterior.
- **Vencidas:** no completadas con fecha anterior a hoy.
- **Para hoy:** no completadas con fecha igual a hoy.
- **Completadas:** estado exactamente Completada.

Los contadores de Inicio corresponden a toda la cuenta, no al filtro de Actividades.
Los contadores de las secciones de Actividades sí corresponden al filtro aplicado.

### I2-4 — Ordenamiento

Una única lógica compartida ordena por:

1. Fecha de entrega ascendente.
2. Prioridad Alta, Media, Baja para la misma fecha.
3. Identificador ascendente cuando coinciden fecha y prioridad.

Se aplica dentro de cada categoría, también Completadas, en ambas pantallas.
La posición de las categorías es fija: no se pretende un orden cronológico global
entre categorías. Editar fecha/prioridad recalcula el orden sin escribir datos para
ordenarlos. Los valores de prioridad no reconocidos, si hubiera datos antiguos
inconsistentes, se sitúan después de Baja, sin modificarlos.

### I2-5 — Interfaz y experiencia de usuario

- Se conservan tema, navegación inferior, rutas, CRUD y confirmaciones existentes.
- Panel de filtros vertical, selecciones visibles, restablecimiento y total de resultados.
- Títulos de sección identificados semánticamente como encabezados para accesibilidad.
- Selectores con descripción de etiqueta y valor para lectores de pantalla.
- Menús con altura limitada y desplazamiento para listas largas de materias.
- Fecha, tipo, prioridad y estado legibles en las tarjetas, con indicación textual
  de vencimiento; la información no depende exclusivamente del color.
- Botones Guardar materia y Guardar actividad para identificar la acción.
- Se conservan las validaciones, botones Cancelar, confirmaciones de eliminación,
  listas desplazables y formularios con `imePadding` y desplazamiento vertical.

La adaptación real a pantalla pequeña, texto ampliado, teclado, modo oscuro y
TalkBack queda pendiente de comprobación en dispositivo/emulador.

### I2-6 — Pruebas preparadas y verificaciones realizadas

Se añadieron **16 pruebas unitarias** en `ActivityOrganizationTest` que cubren:
todos los datos, materia, cada estado existente, combinación, falta de resultados,
restablecimiento, conservación del otro filtro, ayer/hoy/mañana/futuro, exclusión
de completadas, ausencia de duplicados, orden y desempates, orden de completadas,
ediciones de materia/estado/fecha/prioridad, eliminación/listas vacías y cambio de día.

Se añadió **1 prueba instrumentada** en `ActivityOrganizationPersistenceTest`:
consulta Room filtrada, actualización de fecha, cambio a completada, exclusión de
vencidas, eliminación y aislamiento entre dos propietarios. Usa una base en memoria
exclusiva de prueba. Las pruebas anteriores se conservan, incluida la de reapertura
de almacenamiento en `PersistenceTest`.

**Ejecución por el agente:** el intento con el wrapper se detuvo antes de
iniciar Gradle porque no hay Java en este entorno. No se instaló ni configuró un
entorno Android local, conforme al acuerdo de probar en la otra máquina.

**Confirmación posterior del Integrante 2:** el usuario indicó que comprobó que
todo funciona correctamente y autorizó subir los cambios a GitHub. Se registra
esta validación funcional como reportada por el usuario. No se recibieron informes
de Gradle, capturas ni un desglose de escenarios; no se atribuyen resultados
individuales de pruebas automatizadas ni del checklist sin esa evidencia.

## 4. Archivos creados y modificados

Las rutas son relativas a la raíz del proyecto.

| Archivo | Tipo de modificación | Motivo | Funcionalidad |
|---|---|---|---|
| `app/src/main/java/com/example/mientrega/ActivityOrganization.kt` | Creado | Transformaciones puras, filtros, categorías y comparador compartido. | I2-2, I2-3, I2-4 |
| `app/src/main/java/com/example/mientrega/ui/LocalDay.kt` | Creado | Fecha local observable a medianoche, al reanudar y ante ajustes del reloj/zona. | I2-3 |
| `app/src/main/java/com/example/mientrega/ui/PlannerApp.kt` | Modificado | Panel de filtros, secciones, contadores, tarjetas, accesibilidad y etiquetas. | I2-2 a I2-5 |
| `app/src/test/java/com/example/mientrega/ActivityOrganizationTest.kt` | Creado | 16 pruebas de lógica independientes del reloj real. | I2-6 |
| `app/src/androidTest/java/com/example/mientrega/ActivityOrganizationPersistenceTest.kt` | Creado | Integración con Room y aislamiento de datos. | I2-6 |
| `CAMBIOS_ETAPA2.md` | Creado | Registro exclusivo de esta etapa y comprobaciones pendientes. | I2-7 |
| `README.md` | Modificado | Reflejar las funciones de Etapa 2, aportes del equipo y comandos de verificación. | I2-7 |
| `build.gradle.kts` | Ajuste local conservado | AGP 9.5.1 y KSP 2.3.6 presentes al preparar la publicación. | Herramientas de la copia validada |
| `gradle/wrapper/gradle-wrapper.properties` | Ajuste local conservado | Wrapper Gradle 9.6.0 presente al preparar la publicación. | Herramientas de la copia validada |
| `settings.gradle.kts` | Ajuste local conservado | Resolución de toolchains mediante Foojay 1.0.0. | Herramientas de la copia validada |
| `gradle/gradle-daemon-jvm.properties` | Archivo local incorporado | Criterios de JVM del daemon: JetBrains JDK 25. | Herramientas de la copia validada |

**Bibliotecas de aplicación/pruebas agregadas:** ninguna. Se incluye el plugin
de herramientas Foojay que ya estaba en la copia local.

## 5. Decisiones técnicas y conservación de datos

- Se conserva la arquitectura Compose → AndroidViewModel → DAO Room. No se introduce
  un repositorio artificial ni se reescribe el sistema de sesiones.
- La lógica de organización se extrae a un objeto Kotlin sin reloj interno ni
  dependencia de la interfaz. Recibe las actividades ya limitadas a la cuenta.
- Los filtros son estado de presentación local del destino Actividades, no columnas
  de base de datos. Se usa `rememberSaveable`; los resultados derivados se recalculan
  con `remember(assignments, today, filters)`.
- Se reutilizan las suscripciones existentes. `collectAsStateWithLifecycle` recibe
  cambios de Room y las listas continúan filtrándose por propietario antes de llegar
  a la nueva lógica. `key(userId)` en MainActivity sigue delimitando la interfaz de sesión.
- **Fechas:** `dueDate` ya representa un día civil (`LocalDate.toEpochDay()`), no
  milisegundos ni un instante UTC. Se compara con `LocalDate.now()` del dispositivo.
  No se convierte la fecha de entrega a medianoche UTC.
- `rememberLocalDay` mantiene un día compartido por ambas pantallas; observa
  ON_RESUME y broadcasts protegidos del sistema DATE_CHANGED, TIME_SET y
  TIMEZONE_CHANGED. Una coroutine espera a la siguiente medianoche local usando
  ZonedDateTime, considerando cambios de horario. El observador y el receptor se
  liberan al salir de la composición. No hay notificaciones ni recordatorios.
- No se modificaron DAO, consultas, entidades, nombre de base de datos, versión 2
  ni migración 1→2. No hay migración nueva porque no cambia el esquema.
- No se agregó migración destructiva ni se abrió/eliminó la base de datos real.
- La prueba nueva usa memoria; la prueba antigua de persistencia solo elimina su
  propia base temporal de nombre aleatorio, comportamiento ya existente.

## 6. Evidencia real de verificaciones

APROBADA significa únicamente lo indicado en Método, no una prueba funcional implícita.
Las filas iniciales conservan el resultado histórico del intento del agente.

| ID | Prueba o verificación | Resultado | Método | Observaciones |
|---|---|---|---|---|
| INS-01 | Inspección de arquitectura, modelos, autenticación y pruebas | APROBADA | Lectura del código y configuración | No equivale a ejecución. |
| INS-02 | Revisión de integración de llamadas, categorías y fuentes de datos | APROBADA | Revisión estática de archivos y referencias | Compilador Kotlin pendiente. |
| BUILD-01 | Compilación debug | NO EJECUTADA | `bash ./gradlew testDebugUnitTest assembleDebug --offline` | Wrapper terminó con error: JAVA_HOME no definido y comando java no encontrado. Gradle no llegó a iniciarse. |
| UNIT-01 | 16 pruebas nuevas de organización | NO EJECUTADA | Mismo intento de wrapper | Bloqueado por ausencia de Java. |
| UNIT-02 | RulesTest y AuthSecurityTest existentes | NO EJECUTADA | Mismo intento de wrapper | Bloqueado por ausencia de Java. |
| INST-01 | Prueba nueva de organización con Room | NO EJECUTADA | Pendiente `connectedDebugAndroidTest` | Requiere entorno Android y dispositivo/emulador de la otra máquina. |
| INST-02 | PersistenceTest existente | NO EJECUTADA | Pendiente `connectedDebugAndroidTest` | Incluye reapertura, aislamiento y restricciones de eliminación. |
| MAN-01 | Filtros y navegación real | NO EJECUTADA | Checklist siguiente | Completar en Android Studio. |
| MAN-02 | Fechas, contadores y orden real | NO EJECUTADA | Checklist siguiente | Incluye medianoche y ajustes de zona. |
| MAN-03 | CRUD y persistencia al reiniciar | NO EJECUTADA | Checklist siguiente | No se accedió al almacenamiento real del usuario. |
| MAN-04 | Inicio/cierre de sesión y dos estudiantes | NO EJECUTADA | Checklist siguiente | Aislamiento existente preservado en código; ejecución pendiente. |
| GIT-01 | Identificar rama y estado Git | NO EJECUTADA | `git status --short --branch` | Respuesta: no es un repositorio Git. La carpeta no contiene `.git`. |
| USER-01 | Funcionamiento general de la Etapa 2 | APROBADA | Confirmación del Integrante 2 en la conversación | Usuario: «ya comprobé que todo está funcionando correctamente». No se adjuntaron informes individuales. |
| GIT-02 | Preparar publicación sobre el historial existente | APROBADA | Fetch de GitHub y comparación con `origin/main` | Rama `etapa2/integrante2-filtros` basada en `d78349b`; archivos locales preservados. |

## 7. Capturas de pantalla pendientes

No se ejecutó la aplicación ni se generaron capturas. Completar con imágenes reales:

| Captura | Espacio para imagen/ruta y observación |
|---|---|
| Pantalla principal y contadores | **PENDIENTE: agregar captura.** |
| Filtros de materia y estado aplicados | **PENDIENTE: agregar captura.** |
| Actividades Para hoy | **PENDIENTE: agregar captura.** |
| Actividades Vencidas | **PENDIENTE: agregar captura.** |
| Actividades Completadas | **PENDIENTE: agregar captura.** |
| Mejoras visuales/formularios/estados vacíos | **PENDIENTE: agregar captura.** |

## 8. Problemas conocidos y limitaciones

- La ejecución por el agente estuvo bloqueada por falta de Java. Posteriormente
  el usuario confirmó el funcionamiento en su entorno; no se recibieron los informes
  individuales de compilación/pruebas ni capturas para adjuntar.
- No se identificaron errores ajenos a la etapa que requirieran intervención durante
  la inspección. Esto no sustituye la regresión real.
- Las preferencias de filtro no se guardan en Room ni como preferencias permanentes:
  se conservan con el estado de navegación/Compose, no se garantiza su conservación
  después de cerrar sesión o terminar explícitamente la aplicación.
- Si una materia filtrada es eliminada sin actividades, el filtro permanece como
  Materia no disponible hasta cambiarlo o restablecerlo.
- Las listas completas del estudiante se organizan en memoria reutilizando el Flow
  existente, adecuado al alcance local académico; no se añadió paginación.
- La copia inicialmente no tenía `.git`. Con autorización de publicación se vinculó
  al historial real del repositorio y se preparó `etapa2/integrante2-filtros` desde
  `origin/main`, sin sustituir los archivos de trabajo ni integrar cambios en main.

## 9. Verificación manual final en Android Studio (Windows)

### Preparación y ejecución automatizada

Copiar los archivos creados/modificados a la misma estructura de paquetes en la
máquina preparada, o trasladar la carpeta del proyecto. Conservar la configuración
local de SDK/JDK de esa máquina; no reemplazar su `local.properties` con rutas de otra.
No desinstalar la aplicación ni borrar datos para probar esta etapa.

Desde la raíz, en la terminal PowerShell de Android Studio:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Con un emulador o teléfono conectado (API 26 o superior):

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Informes esperados:

- `app/build/reports/tests/testDebugUnitTest/index.html`
- `app/build/reports/androidTests/connected/` (revisar el informe generado).
- APK debug: `app/build/outputs/apk/debug/app-debug.apk`.

### Datos sugeridos y resultado esperado

Crear Matemáticas, Programación y una materia sin actividades. Registrar:

| Actividad | Fecha relativa al día del dispositivo | Prioridad | Estado | Categoría esperada |
|---|---|---|---|---|
| Ayer pendiente | Ayer | Media | Pendiente | Vencidas |
| Hoy alta | Hoy | Alta | Pendiente | Para hoy |
| Hoy media | Hoy | Media | En progreso | Para hoy |
| Hoy baja | Hoy | Baja | Pendiente | Para hoy |
| Hoy alta 2 | Hoy | Alta | Pendiente | Para hoy, después de Hoy alta si su ID es mayor |
| Mañana | Mañana | Baja | Pendiente | Próximas |
| Futura | Hoy + 7 días | Alta | En progreso | Próximas, después de Mañana |
| Completada antigua | Ayer | Alta | Completada | Solo Completadas |
| Completada hoy | Hoy | Media | Completada | Solo Completadas |

Repartir las actividades entre Matemáticas y Programación para probar combinaciones.

### Checklist que debe completar el Integrante 2

El usuario confirmó el funcionamiento general. Las casillas se conservan como
registro detallado por completar, porque no se proporcionaron resultados por escenario.

- [ ] Compilación debug y pruebas unitarias; registrar informes y resultados reales.
- [ ] Pruebas instrumentadas nuevas y existentes con dispositivo/emulador.
- [ ] Todas las materias + todos los estados muestran todas las actividades de la cuenta.
- [ ] Filtro individual de Matemáticas y de Programación.
- [ ] Cada estado: Pendiente, En progreso y Completada.
- [ ] Matemáticas + Pendiente aplica ambos criterios a la vez.
- [ ] Cambiar un selector conserva el otro; los valores seleccionados son visibles.
- [ ] Materia sin actividades y combinación sin coincidencias muestran mensaje claro.
- [ ] Restablecer filtros recupera todos los resultados.
- [ ] Con filtros activos, editar título mantiene la selección y muestra el cambio.
- [ ] Cambiar estado/materia al editar retira la actividad si deja de coincidir.
- [ ] Eliminar una actividad filtrada actualiza lista y contadores; cancelar no elimina.
- [ ] Cambiar entre pestañas y volver de edición conserva filtros; probar rotación.
- [ ] Verificar ayer, hoy, mañana y futuro según la tabla, sin duplicaciones.
- [ ] Completada antigua no es vencida; completada hoy no cuenta como pendiente/Para hoy.
- [ ] Marcar como completada mueve la tarjeta y actualiza Inicio sin reiniciar la app.
- [ ] Verificar contadores: pendientes incluye En progreso; Para hoy excluye completadas.
- [ ] Misma fecha ordena Alta → Media → Baja; misma prioridad desempata por ID.
- [ ] Editar fecha/prioridad reordena; la fecha prevalece sobre la prioridad.
- [ ] Verificar cambio de día con app abierta, regreso desde segundo plano y cambio de zona/reloj en emulador; restaurar luego el reloj.
- [ ] Crear/editar/eliminar materias; duplicados rechazados; materias relacionadas protegidas.
- [ ] Crear/editar/eliminar actividades de los tres tipos y probar validaciones y Cancelar.
- [ ] Reiniciar la app: materias, actividades, estado editado y sesión siguen guardados.
- [ ] Cerrar sesión A, entrar B: sin materias/actividades de A, tampoco brevemente al navegar.
- [ ] Crear datos de B, aplicar filtros y regresar a A: solo datos propios en cada cuenta.
- [ ] Confirmar que los datos preexistentes siguen disponibles, sin limpiar almacenamiento.
- [ ] Probar pantalla habitual/pequeña, teclado visible, título y materia largos, texto ampliado, menús con muchas materias y listas desplazables.
- [ ] Comprobar modo oscuro, contraste, botones accesibles y lectura de selectores/encabezados con TalkBack.
- [ ] Agregar capturas, completar responsable y actualizar resultados de esta documentación.

## 10. Estado de entrega

**Estado: funcionamiento confirmado y publicación autorizada por el Integrante 2.**

**GitHub: rama de entrega autorizada `etapa2/integrante2-filtros`.**

La autorización posterior del usuario permite crear el commit y publicar la rama.
La integración en main queda a cargo del equipo. No se solicitó crear Pull Request
ni realizar merges. Los informes y capturas individuales quedan por adjuntar.
