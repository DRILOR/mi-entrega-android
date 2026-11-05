# Etapa 1 — Dailer: cuentas locales y separación de datos

**Rama:** `etapa1/dailer-usuarios`  
**Fecha del registro:** 5 de noviembre de 2026  
**Responsable:** Dailer Almanza  
**Estado:** funciones principales comprobadas; falta cierre de verificaciones y publicación en `main`.

## Implementación

- Room pasa de la versión 1 a la 2; agrega tabla `users` y propietario en materias y actividades.
- La migración conserva registros antiguos y la primera cuenta los reclama en una transacción.
- Materias y actividades nuevas se registran con el propietario autenticado.
- Las consultas y operaciones de edición/eliminación están limitadas al usuario activo.
- Las contraseñas utilizan PBKDF2-HMAC-SHA256 con sal aleatoria; nunca se guardan en texto plano.
- La sesión es persistente y local. Las cuentas de diferentes dispositivos no se sincronizan.
- La base Room no está cifrada; esto es una limitación explícita del proyecto académico.

## Registro de pruebas

| ID | Comprobación | Estado / evidencia |
| --- | --- | --- |
| T01 | `testDebugUnitTest` | ✅ BUILD SUCCESSFUL — Dailer |
| T02 | `assembleDebug` | ✅ BUILD SUCCESSFUL — Dailer |
| T03 | `connectedDebugAndroidTest` | ✅ 1 prueba ejecutada; BUILD SUCCESSFUL — Dailer |
| T04 | Instalar y abrir app actualizada | ✅ Abre correctamente en emulador API 37 |
| T05 | Mostrar inicio de sesión | ✅ Confirmado |
| T06 | Registrar cuenta A y acceder a Inicio | ✅ Confirmado |
| T07 | Conservar materias y actividades preexistentes | ✅ Confirmado por Dailer |
| T08 | Mantener sesión tras cerrar/reabrir | ✅ Confirmado por Dailer |
| T09 | Dos cuentas con materias/actividades independientes | ✅ Confirmado por Dailer |
| T10 | Contraseña incorrecta rechazada | ⏳ Pendiente de confirmación |
| T11 | Correo duplicado rechazado | ⏳ Pendiente de confirmación |
| T12 | Mismo nombre de materia en cuentas distintas | ⏳ Pendiente de confirmación |
| T13 | Impedir borrar materia con actividades | ⏳ Pendiente de reconfirmación en v2 |
| T14 | Esquema exportado `2.json` incluido en Git | ⏳ Pendiente |
| T15 | Segunda ejecución de pruebas tras ajuste final | ⏳ Pendiente |
| T16 | PR, integración a `main` y acta de relevo | ⏳ Pendiente |

No marcar una verificación como aprobada sin ejecutarla. No incluir contraseñas en capturas ni documentación.

## Cierre y entrega al integrante 2

**Fecha de cierre:** ____________________  
**Commit final en `main`:** ____________________  
**PR integrado:** ____________________  
**Entregó:** Dailer Almanza  
**Recibió:** Integrante 2 — nombre pendiente  
**Pendientes conocidos:** ____________________  
**Evidencias y capturas:** ____________________  

La etapa se considera terminada cuando los puntos pendientes estén resueltos,
la versión compilada e instalada funcione y la rama haya sido integrada a `main`.
Ningún compañero debe comenzar a programar su etapa antes de este relevo.
