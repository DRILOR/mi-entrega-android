# Avance de Mi Entrega

## En preparación: etapa 1 de Dailer (cuentas locales)
En la rama `etapa1/dailer-usuarios` se preparó el registro e inicio de sesión local,
la sesión persistente, la separación de datos por estudiante y la migración de Room de
la versión 1 a la 2. Se añadieron pruebas de seguridad de contraseñas y aislamiento
de estudiantes. **La compilación, las pruebas automatizadas y el recorrido manual de
esta nueva rama todavía deben realizarse en Android Studio; no están aprobados.**

El `BUILD SUCCESSFUL` comunicado previamente por Dailer corresponde a las pruebas
unitarias de la versión base, antes de implementar las cuentas.
Ver `ETAPA1_USUARIOS_PRUEBAS.md` y `PLAN_MIGRACION_ONLINE.md`.

## Primera entrega funcional
Se alcanzó el alcance inicial que el equipo definió como aproximadamente el 40–50 % del proyecto. Es una estimación de alcance, no una medición de líneas de código ni una calificación académica.

## Implementado
- Cinco pantallas y navegación.
- Crear, consultar, editar y eliminar materias y actividades.
- Validación de campos obligatorios y nombres de materia duplicados.
- Fechas, prioridades, tipos y estados.
- Almacenamiento local con Room y relación materia-actividad.
- Restricción de eliminación de materias con actividades, incluso completadas.
- Cancelación de formularios y confirmación de eliminación.
- Resumen de pendientes, vencidas y completadas.

## Evidencia de verificación
Observado directamente en Android Studio durante la sesión:
- Sincronización correcta tras actualizar KSP a 2.3.4.
- Compilación assembleDebug correcta.
- Instalación y arranque en emulador API 37.
- Mensaje de validación al guardar una materia sin nombre.

Confirmado por el usuario mediante capturas y pruebas manuales guiadas:
- Crear materia y actividad con todos sus campos.
- Editar estado a En progreso sin duplicar la actividad.
- Actualizar el resumen al marcar una actividad como completada.
- Conservar materias y actividades al cerrar y reabrir la app.
- Impedir eliminar una materia con actividades asociadas.
- Crear, renombrar y eliminar una segunda materia de prueba.
- Rechazar un nombre de materia duplicado.
- Rechazar una actividad sin campos obligatorios.
- Guardar una actividad sin descripción.
- Cancelar una edición sin guardar los cambios.
- Guardar un cambio de prioridad sin duplicar el registro.
- Cancelar y confirmar la eliminación de una actividad de prueba.
- Eliminar su materia cuando ya no tiene actividades.
- Conservar el estado final tras cerrar y reabrir la app.

## Último ajuste
Se corrigieron las etiquetas del resumen para mostrar 1 pendiente, 1 vencida y 1 completada, y usar plurales para las demás cantidades. Este ajuste de texto está guardado en el código, pero aún debe recompilarse y revisarse manualmente. La compilación e instalación verificadas corresponden a la versión anterior a este ajuste.

## Pruebas automatizadas
Existen ocho pruebas unitarias y una prueba instrumentada de persistencia. No se ha confirmado su ejecución; no se cuentan como aprobadas.

## Próximo paso exacto
Ejecutar app con Run en Android Studio para aplicar el ajuste de texto. Revisar Inicio: con el estado actual debería mostrar 0 pendientes y 1 completada. Después conservar esta entrega y preparar explicación del código para los tres integrantes.

## Pendiente para etapas posteriores
- Filtros por materia y estado.
- Sección Para hoy ordenada por urgencia y prioridad.
- Recordatorios y permisos de notificaciones.
- Mejoras visuales y accesibilidad.
- Ejecutar las pruebas automatizadas y ampliar la validación final.
- Documentación y defensa final.

## Entorno
Android Studio, Git, SDK y emulador configurados. Repositorio Git inicializado, todavía sin commit confirmado. El usuario continúa manejando Android Studio manualmente; no retomar el control de pantalla sin nueva autorización.
