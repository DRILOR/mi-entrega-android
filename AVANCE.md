# Avance de Mi Entrega

**Actualizado:** 5 de noviembre de 2026  
**Rama en validación:** `etapa1/dailer-usuarios`  
**Responsable:** Dailer Almanza  
**Estado:** funciones principales de usuarios verificadas; cierre técnico pendiente. La rama `main` sigue estable y sin fusionar.

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

## Para cerrar definitivamente la etapa 1

- Comprobar contraseña incorrecta y correo duplicado; registrar el resultado.
- Confirmar que dos estudiantes puedan usar el mismo nombre de materia sin interferirse.
- Comprobar que sigue bloqueada la eliminación de una materia con actividades.
- Confirmar el archivo de esquema exportado de Room `2.json` y agregarlo al commit.
- Ejecutar nuevamente pruebas después del pequeño ajuste que elimina una advertencia de Kotlin.
- Completar el acta de entrega (fecha, commit y pruebas) y abrir PR hacia `main`.
- Integrar únicamente después de aprobar las comprobaciones; entonces inicia el integrante 2.

Ver `ETAPA1_USUARIOS_PRUEBAS.md` para el registro detallado.

## Trabajo posterior del equipo

- **Integrante 2:** filtros, sección Para hoy, ordenamiento y mejoras de interfaz.
- **Integrante 3:** recordatorios, notificaciones, pruebas finales, APK y documentación final.
- **Los tres:** explicación y demostración de los módulos propios durante la defensa.

## Limitaciones y futuro

Las cuentas y los datos están almacenados en este dispositivo. No existe sincronización entre teléfonos ni servidor, y la contraseña local no cifra el archivo de Room. La propuesta de una posible migración está en `PLAN_MIGRACION_ONLINE.md`.
