# Relevo de la etapa 1 a la etapa 2 — Mi Entrega

**Proyecto:** Mi Entrega — Tecnologías Móviles  
**Fecha de preparación:** 5 de noviembre de 2026  
**Entrega:** Dailer Almanza  
**Recepción:** Integrante 2 (nombre por confirmar)  
**Modalidad:** estrictamente secuencial, sin programación simultánea entre etapas.

## Estado recibido

Dailer recuperó y consolidó la app Android Kotlin/Compose y completó su módulo:
registro e inicio de sesión local, contraseñas con hash y sal, sesión persistente,
conservación de materias/actividades al migrar Room 1→2 y separación por estudiante.
La app no necesita internet, servidor ni cuentas compartidas entre teléfonos.

Las pruebas automatizadas, la compilación y los escenarios de dos usuarios
fueron confirmados. Consultar `AVANCE.md`, `ETAPA1_USUARIOS_PRUEBAS.md`
y `DECISIONES.md`. El plan futuro de nube está en `PLAN_MIGRACION_ONLINE.md`.

**Condición para empezar:** el Pull Request de la etapa 1 debe estar integrado
en `main`; el integrante 2 debe confirmar que pudo descargar, compilar
y abrir esa versión. No comenzar a editar sobre la rama inconclusa del integrante 1.

## Preparación de la laptop del integrante 2

1. Tener Git y Android Studio con SDK Android 36 y JDK compatible.
2. Ser colaborador autorizado del repositorio privado.
3. Abrir PowerShell y ejecutar los siguientes comandos uno por uno:

```powershell
cd C:\Proyectos
git clone https://github.com/DRILOR/mi-entrega-android.git
cd mi-entrega-android
git branch --show-current
git log -1 --oneline
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

4. Ejecutar la app en un emulador o teléfono y probar acceso, materias y actividades.
5. Crear una rama propia **después** de validar la versión recibida:

```powershell
git switch main
git pull --ff-only origin main
git switch -c etapa2/integrante2-filtros
```

## Trabajo exclusivo del integrante 2

1. Filtrar actividades por materia y por estado, pudiendo combinar los filtros.
2. Incorporar la sección `Para hoy` con actividades próximas y vencidas.
3. Ordenar por fecha de entrega y, cuando corresponda, por prioridad.
4. Mejorar la navegación, mensajes vacíos, accesibilidad y presentación visual.
5. Mantener correctamente la separación de datos de cada estudiante.
6. Ampliar pruebas automatizadas y manuales y documentar resultados.
7. Preparar commit, publicar rama y abrir Pull Request para revisión antes de integrar.

**No incluye:** desarrollar recordatorios/notificaciones (etapa 3), reemplazar
el sistema de cuentas, borrar datos de Room, añadir servidor ni cambiar alcance
sin acuerdo previo del equipo.

## Criterio de entrega hacia el integrante 3

- Filtros y `Para hoy` funcionan sin mezclarse entre estudiantes.
- CRUD, autenticación, sesiones y persistencia siguen operativos.
- Pruebas y compilación aprobadas; errores conocidos documentados.
- Cambios integrados en `main` por Pull Request revisado.
- Acta de relevo con nombre, fecha, commit, pruebas y pendientes.

## Acta breve para completar

**Recibe integrante 2:** _________________________________  
**Usuario de GitHub:** ____________________________________  
**Fecha de recepción:** ___________________________________  
**Commit de `main` recibido:** __________________________  
**Compilación y prueba inicial:** _________________________  
**Observaciones:** ________________________________________  
**Firma/confirmación de recepción:** ______________________  

Al finalizar la etapa 2, el siguiente responsable será el integrante 3.
