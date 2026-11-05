# Posible evolución de Mi Entrega: de local a online

**Estado:** propuesta futura. **No forma parte de la entrega actual de Tecnologías Móviles.**

## Decisión de la versión académica

La aplicación funciona sin servidor ni conexión obligatoria a internet. Las cuentas,
materias y actividades se almacenan en Room dentro del dispositivo. Una cuenta creada
en un teléfono **no existe** automáticamente en otro. El inicio de sesión local permite
separar registros dentro de la aplicación, pero no cifra por sí solo el archivo de Room.

## Inventario de información que deberá migrarse

| Datos locales | Equivalente remoto futuro | Consideración |
| --- | --- | --- |
| `users` (`id`, nombre, correo) | Identidad autenticada remota | Asociar `id` local con `remoteUserId`. |
| Hash/sal de contraseña | Proveedor de autenticación | No copiar hashes sin un plan específico; preferir restablecer credenciales. |
| `subjects` (`ownerId`, `id`, nombre, color) | Materias de cada usuario | Convertir identificadores locales a identificadores remotos. |
| `assignments` (`ownerId`, `subjectId`, datos de entrega) | Actividades remotas | Conservar la relación con su materia después de la migración. |

## Etapas recomendadas si el docente pide conexión online

1. **Definir alcance y proveedor.** Elegir autenticación y base de datos remota, analizar
   costo, disponibilidad, tratamiento de datos personales y conectividad.
2. **Extender el esquema sin destruir Room.** Incorporar campos de sincronización
   (`remoteId`, `updatedAt`, `syncStatus`) mediante una migración versionada.
3. **Vincular las cuentas.** Permitir que cada estudiante cree o vincule su identidad
   online. Verificar propiedad de los datos antes de subirlos.
4. **Subir datos con consentimiento.** Exportar materias y después actividades,
   conservando una tabla de equivalencias entre IDs locales y remotos.
5. **Resolver conflictos.** Definir cómo tratar una edición local y remota del mismo
   registro, duplicados, eliminaciones y fallos de red.
6. **Soportar modo offline.** Room puede mantenerse como caché local, con una cola
   de cambios pendientes y reintentos automáticos.
7. **Probar la migración.** Comprobar cuentas múltiples, cortes de internet,
   instalaciones existentes, permisos, duplicados y respaldo/restauración.

## Reglas de seguridad y privacidad

- No subir contraseñas en texto plano ni reutilizar a ciegas las credenciales locales.
- Transmitir información mediante HTTPS y aplicar autorización en el servidor.
- Migrar solamente la información que pertenece a la cuenta autenticada.
- Crear copia de seguridad y permitir verificar el resultado antes de retirar el modo local.
- Registrar fechas, versión del esquema, responsables y pruebas del cambio.

**Resultado esperado:** un proyecto futuro donde el estudiante pueda consultar sus
actividades desde varios dispositivos sin perder la continuidad de la aplicación local.
