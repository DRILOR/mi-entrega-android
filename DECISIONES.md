# Decisiones del proyecto

## Etapa 1: cuentas locales (noviembre de 2026)
El docente solicitó una aplicación sencilla. Por ello, las cuentas se almacenan en Room y
funcionan sin internet, API ni servicio de autenticación externo. Las contraseñas se
guardan como hash PBKDF2-HMAC-SHA256 con sal aleatoria, no en texto plano. La sesión
guarda el ID local del estudiante en preferencias privadas. Esto separa la información
mostrada por la app, pero **no cifra** el archivo de base de datos.

El esquema de Room evoluciona de 1 a 2 con una migración explícita. Los registros previos
se conservan y se asignan a la primera cuenta creada en una transacción. Cada materia y
actividad nueva queda asociada a su usuario. Se validará la migración en una instalación
que ya tenga registros; hasta entonces, la rama no se integra en `main`.

La sincronización entre teléfonos es trabajo futuro, no parte de esta etapa. Véase
`PLAN_MIGRACION_ONLINE.md`.

## Plataforma
Aplicación Android nativa con Kotlin y Jetpack Compose, de acuerdo con la elección del equipo. Android mínimo 8.0 (API 26). La primera entrega usa compileSdk y targetSdk 36.

## Datos
Room con tres tablas: users, subjects y assignments. Cada actividad pertenece a una materia mediante subjectId. Las fechas se guardan como días desde 1970 (LocalDate.toEpochDay), sin hora ni zona horaria, porque esta entrega organiza fechas de vencimiento completas.

## Integridad
No se permite borrar una materia con actividades. La interfaz lo explica y la base de datos también lo impide mediante una clave foránea RESTRICT. No se borran actividades en cascada.

## Formularios
El nombre de materia y el título de actividad se recortan al guardar. La descripción es opcional. Las fechas pasadas son válidas: permiten registrar trabajos ya vencidos. Tipo, prioridad y estado se eligen de listas controladas.

## Alcance de la primera entrega
CRUD de materias y actividades, persistencia, navegación, validaciones y resumen básico de Inicio. Los filtros avanzados, Para hoy con prioridades y los recordatorios quedaron para las siguientes etapas. El resumen actual ordena los pendientes por fecha. La Etapa 3 incorpora los recordatorios; las decisiones correspondientes se registran más abajo.

## Organización
Pantallas en ui/PlannerApp.kt, operaciones en PlannerViewModel.kt y persistencia en data/PlannerDatabase.kt. Se mantiene una estructura pequeña para facilitar la explicación académica. Antes de ampliar la app se puede dividir cada pantalla en su propio archivo.

## Verificación
Pruebas unitarias para nombres y fechas. Prueba instrumentada para persistencia al reabrir, estados y restricción de eliminación. Recorrido manual de creación, edición, cancelación, eliminación y cierre/reapertura. Los resultados deben registrarse en AVANCE.md; escribir una prueba no equivale a ejecutarla.

## Etapa 3: recordatorios (noviembre de 2026)

### WorkManager en lugar de AlarmManager

Se eligió **WorkManager** para programar los avisos porque:

- sobrevive al cierre de la aplicación, a los reinicios del teléfono y a las actualizaciones;
- no requiere el permiso de alarmas exactas que Android 12+ exige a `AlarmManager`;
- ofrece trabajos únicos por nombre, etiquetas y estado observable;
- se puede probar de forma instrumentada con `WorkManagerTestInitHelper`;
- el aviso estándar es a las 08:00 y no necesita precisión al segundo, por lo que la ventana de ejecución de WorkManager es aceptable.

Con `AlarmManager` habría que gestionar manualmente `BOOT_COMPLETED`, reprogramaciones, permisos y cancelaciones. Para no acoplar el resto de la app, la interfaz `ReminderScheduler` aísla la implementación y permitiría sustituirla por `AlarmManager` si en el futuro se exigiera precisión exacta.

### Room permanece en versión 2

No se agregó una tabla de recordatorios ni columnas nuevas porque:

- WorkManager ya almacena la programación y los datos mínimos del trabajo;
- Room sigue siendo la fuente de verdad del contenido de la actividad;
- el Worker relee la fila antes de notificar y comprueba que siga existiendo y vigente;
- se evita una migración innecesaria y se conserva el esquema validado en la Etapa 1.

La base sigue en `version = 2` y no se genera un `3.json`. `dueDate` continúa guardándose como `epochDay`; la hora y la zona horaria se calculan al programar el aviso.

### Prevención de duplicados

Los recordatorios no se duplican por varias capas:

- cada actividad tiene un nombre de trabajo único: `assignment-reminder-<ownerId>-<assignmentId>`;
- al guardar o editar se usa `ExistingWorkPolicy.REPLACE`, que cancela el trabajo anterior y deja uno solo;
- la sincronización cancela primero todos los trabajos de la cuenta y luego reprograma los pendientes;
- la notificación usa un tag y un ID estables derivados de la cuenta y la actividad;
- el Worker revalida después de publicar para cubrir carreras con ediciones o eliminaciones;
- completar, eliminar, apagar el toggle o cerrar sesión cancela explícitamente el trabajo y la notificación.

### Recordatorios opt-in y permisos

El toggle global está **apagado por defecto**. El usuario debe activarlo en Ajustes; la preferencia se guarda en `mi-entrega-settings`. Esto evita notificar sin consentimiento y permite que la app funcione igual si el usuario nunca usa recordatorios.

En Android 13+ se solicita `POST_NOTIFICATIONS` en tiempo de ejecución. Si el usuario deniega permanentemente, la app no insiste con el diálogo y ofrece abrir los ajustes del sistema. En versiones anteriores se comprueba si las notificaciones de la app están habilitadas en el sistema.

### Entregas para hoy: recordatorio de respaldo

El aviso estándar es el día anterior a las 08:00. Si ese instante ya pasó, pero la actividad vence hoy o en una fecha futura, `ReminderPolicy` devuelve un respaldo a 10 minutos desde el reloj actual. Así una actividad creada para hoy no se queda sin recordatorio. Si la fecha de entrega ya pasó por completo, devuelve `null` y no se programa nada.

La hora del respaldo se guarda en las preferencias con la clave `fallback_target_<assignmentId>`. Antes de calcular un respaldo nuevo, el coordinador reutiliza el valor guardado si todavía es futuro. Sin esto, cada `sync()` en `ON_RESUME` volvería a calcular `now + 10 min` y la notificación nunca llegaría. El coordinador calcula el instante y se lo entrega al scheduler, de modo que WorkManager no vuelve a interpretar la política por su cuenta.

Cuando la hora guardada ya venció y la actividad sigue pendiente, el coordinador escribe `fallback_delivered_<assignmentId> = true` y no programa otro trabajo. Así un respaldo que ya se mostró no vuelve a dispararse cada vez que el usuario abre la aplicación. La marca se conserva junto con el objetivo para que el estado sea explícito y observable en pruebas. Al crear o editar una actividad se borran `fallback_target` y `fallback_delivered`, y al completarla o eliminarla también, de modo que un cambio de fecha pueda generar un respaldo nuevo sin dejar datos obsoletos.

### Programación best-effort y ciclo de vida

Guardar una actividad en Room es la operación principal. La programación del recordatorio se ejecuta después y se envuelve en `reminderSafely`: si WorkManager falla, el error se registra en Logcat pero no se muestra al usuario ni se pierde el guardado.

Los avisos se resincronizan al volver a primer plano y cuando cambian la fecha, la hora, la zona horaria o se alcanza la medianoche. Así se recalculan los retardos de WorkManager, que trabaja con tiempo transcurrido.

### Pruebas

- Pruebas JVM del coordinador con dobles en memoria para las decisiones de negocio.
- Pruebas instrumentadas con Room en memoria, notificador falso y WorkManager de prueba.
- Pruebas del ciclo de vida completo: crear, editar, completar, eliminar, no duplicados y desactivar.
- Pruebas del Worker, del DAO, del notificador real y de las preferencias.
- Resultado de cierre: **61 pruebas unitarias y 31 instrumentadas en verde**.
