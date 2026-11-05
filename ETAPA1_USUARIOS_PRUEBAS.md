# Etapa 1 — Dailer: cuentas locales y separación de datos

**Rama:** `etapa1/dailer-usuarios`  
**Estado:** implementación preparada; requiere compilación y pruebas en el equipo de Dailer.

## Cambios técnicos

- Room pasa del esquema 1 al 2, crea `users` y añade `ownerId` a materias y actividades.
- La migración preserva los registros existentes con propietario temporalmente nulo.
- Al registrar **la primera cuenta**, se asignan en una transacción los registros antiguos
  a esa cuenta. Las siguientes cuentas empiezan sin datos.
- El nombre de una materia debe ser único **dentro de cada cuenta**, no globalmente.
- Las consultas y operaciones de creación, edición y eliminación se limitan al usuario activo.
- Las contraseñas se derivan con PBKDF2-HMAC-SHA256, sal aleatoria y comparación segura.
- La sesión se mantiene localmente en este dispositivo hasta cerrar sesión.
- La base Room no queda cifrada; es una limitación de este proyecto académico local.

## Validación solicitada (no marcar aprobada sin ejecutarla)

1. Ejecutar `./gradlew.bat testDebugUnitTest` en PowerShell; confirmar `BUILD SUCCESSFUL`.
2. Ejecutar `./gradlew.bat assembleDebug`; confirmar `BUILD SUCCESSFUL`.
3. Instalar la actualización sin desinstalar la versión anterior con datos.
4. Registrar el estudiante A. Confirmar que se conservan las materias y actividades
   que existían antes del registro y que aparecen solo para A.
5. Cerrar sesión e iniciar con contraseña equivocada: debe rechazarla.
6. Iniciar sesión con A: deben reaparecer sus materias y actividades.
7. Registrar estudiante B, cerrar sesión, iniciar B: debe ver sus listas vacías.
8. Crear una materia con el mismo nombre para A y B; ambas deben existir por separado.
9. Crear una actividad en B; iniciar A y comprobar que no aparece allí.
10. Cerrar la aplicación, abrirla de nuevo y comprobar que se conserva la sesión.
11. Eliminar una materia con actividades: debe impedirlo.
12. Ejecutar `./gradlew.bat connectedDebugAndroidTest` con el emulador encendido.
13. Confirmar la existencia del esquema Room exportado `app/schemas/.../2.json`.
14. Registrar resultados, errores, versión y capturas antes de integrar a `main`.

## Reglas para cerrar la etapa

- No utilizar `fallbackToDestructiveMigration` ni borrar los datos de la aplicación.
- No registrar capturas con contraseñas o información privada.
- No integrar en `main` hasta superar compilación, pruebas y recorrido manual.
- Dejar commit con resumen y descripción, registrar el SHA y notificar al integrante 2.
