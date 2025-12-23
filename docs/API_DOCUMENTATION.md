# ScreenLeads API - Documentación Completa

## Tabla de Contenidos

1. [Autenticación](#autenticación)
2. [Authentication API](#authentication-api)
3. [Users API](#users-api)
4. [Roles API](#roles-api)
5. [Companies API](#companies-api)
6. [Company Tokens API](#company-tokens-api)
7. [Devices API](#devices-api)
8. [Device Types API](#device-types-api)
9. [Media API](#media-api)
10. [Media Types API](#media-types-api)
11. [API Keys Management](#api-keys-management)
12. [Clients API](#clients-api)
13. [Customers API](#customers-api)
14. [Promotions API](#promotions-api)
15. [Coupons API](#coupons-api)
16. [Advices API](#advices-api)
17. [Billing API](#billing-api)
18. [App Versions API](#app-versions-api)
19. [App Entities API](#app-entities-api)
20. [Códigos de Estado HTTP](#códigos-de-estado-http)

---

## Autenticación

La API soporta dos métodos de autenticación:

### 1. JWT Authentication
```http
Authorization: Bearer {jwt_token}
```

### 2. API Key Authentication
```http
X-API-KEY: {api_key}
client-id: {client_id}
```

**Sistema de Permisos:**
Los endpoints utilizan el sistema `@perm.can(resource, action)` donde:
- **resource**: user, company, device, media, customer, promotion, coupon, lead
- **action**: read, create, update, delete

---

## Authentication API

### POST `/auth/login`
Autenticar usuario y obtener tokens JWT.

**Autenticación:** No requerida  
**Permisos:** Público

**Body:**
```json
{
  "username": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Respuesta exitosa (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": 1,
    "username": "user@example.com",
    "role": "ROLE_USER"
  }
}
```

**Respuesta error (401):**
```json
{"error": "Invalid credentials"}
```

---

### POST `/auth/register`
Registrar un nuevo usuario en el sistema.

**Autenticación:** No requerida (condicional: debe estar habilitado el registro)  
**Permisos:** Sistema debe permitir registros

**Body:**
```json
{
  "username": "newuser@example.com",
  "password": "SecurePassword123!",
  "email": "newuser@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Respuesta exitosa (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": 2,
    "username": "newuser@example.com"
  }
}
```

**Respuestas de error:**
- **400**: Datos de validación incorrectos
- **403**: Registro no permitido por el sistema

---

### POST `/auth/refresh`
Refrescar el access token usando el token actual.

**Autenticación:** Requerida (JWT Bearer Token)  
**Permisos:** Usuario autenticado

**Parámetros:** Ninguno (el token se extrae del header Authorization)

**Respuesta exitosa (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer"
}
```

**Respuesta error (401):**
```json
{"error": "Invalid or expired token"}
```

---

### GET `/auth/me`
Obtener información del usuario autenticado actual.

**Autenticación:** Requerida  
**Permisos:** Usuario autenticado

**Parámetros:** Ninguno

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "username": "user@example.com",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "ROLE_USER",
  "companyId": 123
}
```

---

### POST `/auth/change-password`
Cambiar la contraseña del usuario autenticado.

**Autenticación:** Requerida  
**Permisos:** Usuario autenticado

**Body:**
```json
{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword456!"
}
```

**Respuesta exitosa (200):** Sin contenido

**Respuestas de error:**
- **400**: Contraseña actual incorrecta
- **401**: No autenticado

---

## Users API

### GET `/users`
Listar todos los usuarios del sistema.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('user', 'read')`

**Parámetros:** Ninguno

**Respuesta exitosa (200):**
```json
[
  {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "roles": [
      {"id": 1, "name": "ROLE_ADMIN"}
    ],
    "enabled": true,
    "createdAt": "2025-12-23T10:00:00Z"
  }
]
```

---

### GET `/users/{id}`
Obtener un usuario específico por ID.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('user', 'read')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del usuario | ✅ |

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "roles": [{"id": 1, "name": "ROLE_ADMIN"}],
  "enabled": true,
  "createdAt": "2025-12-23T10:00:00Z"
}
```

**Respuesta error (404):**
```json
{"error": "User not found"}
```

---

### POST `/users`
Crear un nuevo usuario en el sistema.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('user', 'create')`

**Body:**
```json
{
  "username": "new_user",
  "email": "newuser@example.com",
  "password": "SecurePassword123!",
  "roles": [1, 2]
}
```

**Respuesta exitosa (200):**
```json
{
  "user": {
    "id": 2,
    "username": "new_user",
    "email": "newuser@example.com",
    "roles": [{"id": 1, "name": "ROLE_USER"}],
    "enabled": true
  },
  "plainPassword": "SecurePassword123!"
}
```

**Respuestas de error:**
- **400**: Datos inválidos
- **409**: Usuario ya existe
- **500**: Error del servidor

---

### PUT `/users/{id}`
Actualizar un usuario existente.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('user', 'update')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del usuario | ✅ |

**Body:**
```json
{
  "username": "john_updated",
  "email": "johnupdated@example.com",
  "roles": [1, 3]
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "username": "john_updated",
  "email": "johnupdated@example.com",
  "roles": [{"id": 1, "name": "ROLE_ADMIN"}, {"id": 3, "name": "ROLE_MANAGER"}],
  "enabled": true
}
```

---

### DELETE `/users/{id}`
Eliminar un usuario del sistema.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('user', 'delete')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del usuario | ✅ |

**Respuesta exitosa (204):** Sin contenido

---

## Roles API

### GET `/roles`
Listar todos los roles del sistema.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('user', 'read')`

**Parámetros:** Ninguno

**Respuesta exitosa (200):**
```json
[
  {
    "id": 1,
    "name": "ROLE_ADMIN",
    "level": 1
  },
  {
    "id": 2,
    "name": "ROLE_USER",
    "level": 10
  }
]
```

---

### GET `/roles/{id}`
Obtener un rol específico por ID.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('user', 'read')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del rol | ✅ |

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "name": "ROLE_ADMIN",
  "level": 1
}
```

---

### GET `/roles/assignable`
Listar roles asignables según el nivel del usuario autenticado.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('user', 'create')` o `@perm.can('user', 'update')`

**Descripción:** Devuelve roles con level >= nivel efectivo del usuario solicitante.

**Parámetros:** Ninguno

**Respuesta exitosa (200):**
```json
[
  {
    "id": 2,
    "name": "ROLE_USER",
    "level": 10
  },
  {
    "id": 3,
    "name": "ROLE_MANAGER",
    "level": 5
  }
]
```

---

### POST `/roles`
Crear un nuevo rol.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('user', 'update')`

**Body:**
```json
{
  "name": "ROLE_MANAGER",
  "level": 5
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 3,
  "name": "ROLE_MANAGER",
  "level": 5
}
```

---

### PUT `/roles/{id}`
Actualizar un rol existente.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('user', 'update')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del rol | ✅ |

**Body:**
```json
{
  "name": "ROLE_MANAGER_UPDATED",
  "level": 4
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 3,
  "name": "ROLE_MANAGER_UPDATED",
  "level": 4
}
```

---

### DELETE `/roles/{id}`
Eliminar un rol.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('user', 'delete')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del rol | ✅ |

**Respuesta exitosa (204):** Sin contenido

---

## Companies API

### GET `/companies`
Listar todas las compañías.

**Autenticación:** Requerida  
**Permisos:** `hasAuthority('ROLE_ADMIN')` o `@perm.can('company', 'read')`

**Respuesta exitosa (200):**
```json
[
  {
    "id": 1,
    "name": "Company A",
    "cif": "B12345678",
    "address": "Street 123, City",
    "phone": "+34666777888",
    "email": "info@companya.com",
    "active": true
  }
]
```

---

### GET `/companies/{id}`
Obtener una compañía específica.

**Autenticación:** Requerida  
**Permisos:** `hasAuthority('ROLE_ADMIN')` o `@perm.can('company', 'read')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la compañía | ✅ |

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "name": "Company A",
  "cif": "B12345678",
  "address": "Street 123, City",
  "phone": "+34666777888",
  "email": "info@companya.com",
  "active": true
}
```

---

### POST `/companies`
Crear una nueva compañía.

**Autenticación:** Requerida  
**Permisos:** `hasAuthority('ROLE_ADMIN')` o `@perm.can('company', 'create')`

**Body:**
```json
{
  "name": "New Company",
  "cif": "B87654321",
  "address": "Avenue 456, Town",
  "phone": "+34611222333",
  "email": "contact@newcompany.com"
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 2,
  "name": "New Company",
  "cif": "B87654321",
  "address": "Avenue 456, Town",
  "phone": "+34611222333",
  "email": "contact@newcompany.com",
  "active": true
}
```

---

### PUT `/companies/{id}`
Actualizar una compañía existente.

**Autenticación:** Requerida  
**Permisos:** `hasAuthority('ROLE_ADMIN')` o `@perm.can('company', 'update')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la compañía | ✅ |

**Body:**
```json
{
  "name": "Company Updated",
  "address": "New Address 789"
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "name": "Company Updated",
  "address": "New Address 789",
  "cif": "B12345678",
  "active": true
}
```

---

### DELETE `/companies/{id}`
Eliminar una compañía.

**Autenticación:** Requerida  
**Permisos:** `hasAuthority('ROLE_ADMIN')` o `@perm.can('company', 'delete')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la compañía | ✅ |

**Respuesta exitosa (204):** Sin contenido

---

## Company Tokens API

### POST `/company-tokens`
Crear un token de compañía para el usuario autenticado.

**Autenticación:** Requerida (JWT Bearer Token)  
**Permisos:** Usuario autenticado

**Body (opcional):**
```json
{
  "descripcion": "Token para producción"
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "companyId": 123,
  "token": "generated-token-abc123xyz",
  "role": "COMPANY_ADMIN",
  "createdAt": "2025-12-23T10:00:00Z",
  "expiresAt": "2026-12-23T10:00:00Z",
  "descripcion": "Token para producción"
}
```

---

### GET `/company-tokens`
Listar todos los tokens del usuario autenticado.

**Autenticación:** Requerida  
**Permisos:** Usuario autenticado

**Parámetros:** Ninguno

**Respuesta exitosa (200):**
```json
[
  {
    "id": 1,
    "companyId": 123,
    "token": "token-abc123",
    "role": "COMPANY_ADMIN",
    "createdAt": "2025-12-23T10:00:00Z",
    "expiresAt": "2026-12-23T10:00:00Z",
    "descripcion": "Token producción"
  }
]
```

---

### GET `/company-tokens/{id}`
Obtener un token específico por ID.

**Autenticación:** Requerida  
**Permisos:** Usuario autenticado

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del token | ✅ |

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "companyId": 123,
  "token": "token-abc123",
  "role": "COMPANY_ADMIN",
  "descripcion": "Token producción"
}
```

---

### PUT `/company-tokens/{id}`
Actualizar un token por ID.

**Autenticación:** Requerida  
**Permisos:** Usuario autenticado

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del token | ✅ |

**Body:**
```json
{
  "descripcion": "Token actualizado para desarrollo"
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "token": "token-abc123",
  "descripcion": "Token actualizado para desarrollo"
}
```

---

### DELETE `/company-tokens/{id}`
Eliminar un token por ID.

**Autenticación:** Requerida  
**Permisos:** Usuario autenticado

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del token | ✅ |

**Respuesta exitosa (204):** Sin contenido

---

### PUT `/company-tokens/{token}/renew`
Renovar un token usando el token string.

**Autenticación:** Requerida  
**Permisos:** Usuario autenticado

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `token` | String | Path | Token string a renovar | ✅ |

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "token": "renewed-token-xyz789",
  "expiresAt": "2027-12-23T10:00:00Z"
}
```

---

### PUT `/company-tokens/{token}/description`
Actualizar la descripción de un token usando el token string.

**Autenticación:** Requerida  
**Permisos:** Usuario autenticado

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `token` | String | Path | Token string | ✅ |

**Body:**
```json
{
  "descripcion": "Nueva descripción del token"
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "token": "token-abc123",
  "descripcion": "Nueva descripción del token"
}
```

---

## Devices API

### GET `/devices`
Listar todos los dispositivos.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('device', 'read')`

**Respuesta exitosa (200):**
```json
[
  {
    "id": 1,
    "uuid": "abc-123-def-456",
    "name": "Device Shop 1",
    "deviceTypeId": 1,
    "companyId": 1,
    "active": true,
    "lastConnection": "2025-12-23T11:30:00Z"
  }
]
```

---

### GET `/devices/{id}`
Obtener un dispositivo por ID.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('device', 'read')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del dispositivo | ✅ |

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "uuid": "abc-123-def-456",
  "name": "Device Shop 1",
  "deviceTypeId": 1,
  "companyId": 1,
  "active": true,
  "lastConnection": "2025-12-23T11:30:00Z"
}
```

---

### POST `/devices`
Crear un nuevo dispositivo.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('device', 'create')`

**Body:**
```json
{
  "uuid": "new-device-uuid-789",
  "name": "Device Store 2",
  "deviceTypeId": 1,
  "companyId": 1
}
```

**Respuesta exitosa (201):**
```json
{
  "id": 2,
  "uuid": "new-device-uuid-789",
  "name": "Device Store 2",
  "deviceTypeId": 1,
  "companyId": 1,
  "active": true
}
```

---

### PUT `/devices/{id}`
Actualizar un dispositivo existente.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('device', 'update')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del dispositivo | ✅ |

**Body:**
```json
{
  "name": "Device Shop Updated",
  "active": true
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "uuid": "abc-123-def-456",
  "name": "Device Shop Updated",
  "deviceTypeId": 1,
  "companyId": 1,
  "active": true
}
```

---

### DELETE `/devices/{id}`
Eliminar un dispositivo.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('device', 'delete')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del dispositivo | ✅ |

**Respuesta exitosa (204):** Sin contenido

---

### GET `/devices/uuid/{uuid}`
Obtener un dispositivo por su UUID.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('device', 'read')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `uuid` | String | Path | UUID del dispositivo | ✅ |

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "uuid": "abc-123-def-456",
  "name": "Device Shop 1",
  "deviceTypeId": 1,
  "companyId": 1,
  "active": true
}
```

---

### HEAD `/devices/uuid/{uuid}`
Comprobar si existe un dispositivo por UUID (sin cuerpo de respuesta).

**Autenticación:** No requerida

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `uuid` | String | Path | UUID del dispositivo | ✅ |

**Respuesta:**
- **200**: Dispositivo existe
- **404**: Dispositivo no encontrado

---

### GET `/devices/{deviceId}/advices`
Listar los advices (avisos/consejos) asignados a un dispositivo.

**Autenticación:** No requerida

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `deviceId` | Long | Path | ID del dispositivo | ✅ |

**Respuesta exitosa (200):**
```json
[
  {
    "id": 1,
    "title": "Consejo 1",
    "content": "Contenido del consejo",
    "active": true
  }
]
```

---

### POST `/devices/{deviceId}/advices/{adviceId}`
Asignar un advice a un dispositivo.

**Autenticación:** No requerida

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `deviceId` | Long | Path | ID del dispositivo | ✅ |
| `adviceId` | Long | Path | ID del advice | ✅ |

**Respuesta exitosa (200):** Asignación exitosa

---

### DELETE `/devices/{deviceId}/advices/{adviceId}`
Quitar un advice de un dispositivo.

**Autenticación:** No requerida

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `deviceId` | Long | Path | ID del dispositivo | ✅ |
| `adviceId` | Long | Path | ID del advice | ✅ |

**Respuesta exitosa (204):** Sin contenido

---

## Device Types API

### GET `/devices/types`
Listar todos los tipos de dispositivo.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('devicetype', 'read')`

**Parámetros:** Ninguno

**Respuesta exitosa (200):**
```json
[
  {
    "id": 1,
    "name": "Tablet",
    "description": "Tablet para punto de venta"
  },
  {
    "id": 2,
    "name": "Kiosk",
    "description": "Quiosco interactivo"
  }
]
```

---

### GET `/devices/types/{id}`
Obtener un tipo de dispositivo por ID.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('devicetype', 'read')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del tipo de dispositivo | ✅ |

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "name": "Tablet",
  "description": "Tablet para punto de venta"
}
```

---

### POST `/devices/types`
Crear un nuevo tipo de dispositivo.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('devicetype', 'create')`

**Body:**
```json
{
  "name": "Smart Display",
  "description": "Pantalla inteligente para marketing"
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 3,
  "name": "Smart Display",
  "description": "Pantalla inteligente para marketing"
}
```

---

### PUT `/devices/types/{id}`
Actualizar un tipo de dispositivo existente.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('devicetype', 'update')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del tipo de dispositivo | ✅ |

**Body:**
```json
{
  "name": "Tablet Pro",
  "description": "Tablet profesional actualizada"
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "name": "Tablet Pro",
  "description": "Tablet profesional actualizada"
}
```

---

### DELETE `/devices/types/{id}`
Eliminar un tipo de dispositivo.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('devicetype', 'delete')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del tipo de dispositivo | ✅ |

**Respuesta exitosa (200):**
```
Device Type (1) deleted successfully
```

---

## Media API

### GET `/medias`
Listar todos los archivos multimedia.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('media', 'read')`

**Respuesta exitosa (200):**
```json
[
  {
    "id": 1,
    "src": "compressed-uuid-image.jpg",
    "url": "https://storage.googleapis.com/.../compressed-uuid-image.jpg",
    "thumbnail": "https://storage.googleapis.com/.../thumb-320-uuid-image.jpg",
    "type": "image",
    "createdAt": "2025-12-23T10:00:00Z"
  }
]
```

---

### POST `/medias/upload`
Subir un archivo multimedia (multipart). Procesa, comprime y genera thumbnails de forma **síncrona**.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('media', 'create')`

**Content-Type:** `multipart/form-data`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `file` | File | Form Data | Archivo (imagen/video) | ✅ |

**Límites:**
- Imágenes: 50 MB
- Videos: 100 MB

**Formatos soportados:**
- Imágenes: JPG, PNG, GIF, WebP, AVIF, HEIC, HEIF
- Videos: MP4, AVI, MOV, MKV, WebM

**Respuesta exitosa (200):**
```json
{
  "status": "ready",
  "type": "video",
  "url": "https://storage.googleapis.com/.../compressed-uuid-video.mp4",
  "thumbnails": [
    "https://storage.googleapis.com/.../thumbnails/320/thumb-320-uuid-video.jpg",
    "https://storage.googleapis.com/.../thumbnails/640/thumb-640-uuid-video.jpg"
  ],
  "processingTimeMs": 5000
}
```

**Respuestas de error:**
- **400**: Archivo vacío
- **413**: Archivo demasiado grande
- **500**: Error de procesamiento

---

### POST `/medias/upload-from-url`
Descargar y procesar un archivo desde una URL externa de forma **síncrona**.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('media', 'create')`

**Content-Type:** `application/json`

**Body:**
```json
{
  "url": "https://example.com/video.mp4"
}
```

**Timeouts:**
- Conexión: 10 segundos
- Lectura: 30 segundos

**Respuesta exitosa (200):**
```json
{
  "status": "ready",
  "type": "video",
  "url": "https://storage.googleapis.com/.../compressed-uuid-video.mp4",
  "thumbnails": [
    "https://storage.googleapis.com/.../thumbnails/320/thumb-320-uuid-video.jpg",
    "https://storage.googleapis.com/.../thumbnails/640/thumb-640-uuid-video.jpg"
  ],
  "processingTimeMs": 8000
}
```

**Respuestas de error:**
- **400**: URL vacía o inválida
- **500**: Error descargando o procesando

---

### GET `/medias/{id}`
Obtener información de un archivo multimedia por ID.

**Autenticación:** No requerida

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del archivo | ✅ |

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "src": "compressed-uuid-image.jpg",
  "url": "https://storage.googleapis.com/.../compressed-uuid-image.jpg",
  "type": "image"
}
```

---

### POST `/medias`
Crear un registro de media (solo base de datos, sin archivo físico).

**Autenticación:** No requerida

**Body:**
```json
{
  "src": "file.jpg",
  "url": "https://storage.googleapis.com/.../file.jpg",
  "type": "image"
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 2,
  "src": "file.jpg",
  "url": "https://storage.googleapis.com/.../file.jpg",
  "type": "image"
}
```

---

### PUT `/medias/{id}`
Actualizar un registro de media.

**Autenticación:** No requerida

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del archivo | ✅ |

**Body:**
```json
{
  "src": "file_updated.jpg",
  "url": "https://storage.googleapis.com/.../file_updated.jpg"
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "src": "file_updated.jpg",
  "url": "https://storage.googleapis.com/.../file_updated.jpg"
}
```

---

### DELETE `/medias/{id}`
Eliminar un registro de media.

**Autenticación:** No requerida

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del archivo | ✅ |

**Respuesta exitosa (204):** Sin contenido

---

### GET `/medias/render/{id}`
Descargar el archivo físico desde almacenamiento local (funcionalidad legacy).

**Autenticación:** No requerida

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del archivo | ✅ |

**Respuesta exitosa (200):**
- Content-Type: `application/octet-stream`
- Content-Disposition: `attachment; filename="archivo.jpg"`
- Body: Archivo binario

---

## Media Types API

### GET `/medias/types`
Listar todos los tipos de media.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('mediatype', 'read')`

**Parámetros:** Ninguno

**Respuesta exitosa (200):**
```json
[
  {
    "id": 1,
    "name": "Promocional",
    "description": "Contenido promocional"
  },
  {
    "id": 2,
    "name": "Informativo",
    "description": "Contenido informativo"
  }
]
```

---

### GET `/medias/types/{id}`
Obtener un tipo de media por ID.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('mediatype', 'read')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del tipo de media | ✅ |

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "name": "Promocional",
  "description": "Contenido promocional"
}
```

---

### POST `/medias/types`
Crear un nuevo tipo de media.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('mediatype', 'create')`

**Body:**
```json
{
  "name": "Educativo",
  "description": "Contenido educativo y tutoriales"
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 3,
  "name": "Educativo",
  "description": "Contenido educativo y tutoriales"
}
```

---

### PUT `/medias/types/{id}`
Actualizar un tipo de media existente.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('mediatype', 'update')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del tipo de media | ✅ |

**Body:**
```json
{
  "name": "Promocional Premium",
  "description": "Contenido promocional de alta calidad"
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "name": "Promocional Premium",
  "description": "Contenido promocional de alta calidad"
}
```

---

### DELETE `/medias/types/{id}`
Eliminar un tipo de media.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('mediatype', 'delete')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del tipo de media | ✅ |

**Respuesta exitosa (200):**
```
Media Type (1) deleted successfully
```

---

## API Keys Management

### POST `/api-keys`
Crear una nueva API Key.

**Autenticación:** No requerida

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `clientDbId` | Long | Query | ID del cliente | ✅ |
| `permissions` | String | Query | Permisos (ej: "read,write") | ✅ |
| `daysValid` | int | Query | Días de validez | ❌ (default: 365) |

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "keyValue": "sl_abc123xyz...",
  "clientDbId": 1,
  "permissions": "read,write",
  "active": true,
  "expiresAt": "2026-12-23T10:00:00Z",
  "createdAt": "2025-12-23T10:00:00Z"
}
```

---

### POST `/api-keys/{id}/activate`
Activar una API Key.

**Autenticación:** No requerida

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la API Key | ✅ |

**Respuesta exitosa (200):** API Key activada

---

### POST `/api-keys/{id}/deactivate`
Desactivar una API Key.

**Autenticación:** No requerida

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la API Key | ✅ |

**Respuesta exitosa (200):** API Key desactivada

---

### DELETE `/api-keys/{id}`
Eliminar una API Key.

**Autenticación:** No requerida

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la API Key | ✅ |

**Respuesta exitosa (204):** Sin contenido

---

### GET `/api-keys/client/{clientDbId}`
Listar todas las API Keys de un cliente.

**Autenticación:** No requerida

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `clientDbId` | Long | Path | ID del cliente | ✅ |

**Respuesta exitosa (200):**
```json
[
  {
    "id": 1,
    "keyValue": "sl_abc123...",
    "clientDbId": 1,
    "permissions": "read,write",
    "active": true,
    "expiresAt": "2026-12-23T10:00:00Z"
  }
]
```

---

### PATCH `/api-keys/{id}/permissions`
Actualizar los permisos de una API Key.

**Autenticación:** No requerida

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la API Key | ✅ |
| `permissions` | String | Query | Nuevos permisos | ✅ |

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "keyValue": "sl_abc123...",
  "permissions": "read",
  "active": true
}
```

---

### PATCH `/api-keys/{id}/description`
Actualizar la descripción de una API Key.

**Autenticación:** No requerida

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la API Key | ✅ |
| `description` | String | Query | Nueva descripción | ✅ |

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "keyValue": "sl_abc123...",
  "description": "My Production API Key",
  "active": true
}
```

---

### PATCH `/api-keys/{id}/company-scope`
Actualizar el alcance de compañía de una API Key.

**Autenticación:** No requerida

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la API Key | ✅ |
| `companyScope` | Long | Query | ID de compañía o null | ❌ |

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "keyValue": "sl_abc123...",
  "companyScope": 5,
  "active": true
}
```

---

## Clients API

### GET `/clients`
Listar todos los clientes con sus API Keys.

**Autenticación:** No requerida (endpoint público)  
**Permisos:** Ninguno

**Parámetros:** Ninguno

**Respuesta exitosa (200):**
```json
[
  {
    "id": 1,
    "clientId": "abc123-uuid-def456",
    "name": "Cliente Test",
    "active": true,
    "apiKeys": [
      {
        "id": 1,
        "keyValue": "sl_abc123...",
        "permissions": "read,write",
        "active": true
      }
    ]
  }
]
```

---

### GET `/clients/{id}`
Obtener un cliente por ID.

**Autenticación:** No requerida  
**Permisos:** Ninguno

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del cliente | ✅ |

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "clientId": "abc123-uuid-def456",
  "name": "Cliente Test",
  "active": true
}
```

---

### POST `/clients`
Crear un nuevo cliente.

**Autenticación:** No requerida  
**Permisos:** Ninguno

**Descripción:** Auto-genera un clientId UUID y crea automáticamente la primera API Key con permisos básicos de lectura.

**Body:**
```json
{
  "name": "Nuevo Cliente"
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 2,
  "clientId": "generated-uuid-xyz789",
  "name": "Nuevo Cliente",
  "active": true
}
```

---

### PUT `/clients/{id}`
Actualizar un cliente existente.

**Autenticación:** No requerida  
**Permisos:** Ninguno

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del cliente | ✅ |

**Body:**
```json
{
  "name": "Cliente Actualizado",
  "clientId": "abc123",
  "active": true
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "clientId": "abc123",
  "name": "Cliente Actualizado",
  "active": true
}
```

---

### DELETE `/clients/{id}`
Eliminar un cliente.

**Autenticación:** No requerida  
**Permisos:** Ninguno

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del cliente | ✅ |

**Respuesta exitosa (204):** Sin contenido

---

### POST `/clients/{id}/activate`
Activar un cliente.

**Autenticación:** No requerida  
**Permisos:** Ninguno

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del cliente | ✅ |

**Respuesta exitosa (200):** Cliente activado

---

### POST `/clients/{id}/deactivate`
Desactivar un cliente.

**Autenticación:** No requerida  
**Permisos:** Ninguno

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del cliente | ✅ |

**Respuesta exitosa (200):** Cliente desactivado

---

## Customers API

### GET `/customers`
Listar clientes con filtros opcionales.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('customer', 'read')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `companyId` | Long | Query | Filtrar por compañía | ❌ |
| `search` | String | Query | Búsqueda parcial en identifier | ❌ |

**Respuesta exitosa (200):**
```json
[
  {
    "id": 1,
    "companyId": 1,
    "identifierType": "EMAIL",
    "identifier": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "createdAt": "2025-12-23T10:00:00Z"
  }
]
```

---

### GET `/customers/{id}`
Obtener un cliente por ID.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('customer', 'read')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del cliente | ✅ |

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "companyId": 1,
  "identifierType": "EMAIL",
  "identifier": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "createdAt": "2025-12-23T10:00:00Z"
}
```

---

### POST `/customers`
Crear un nuevo cliente con normalización y validación de unicidad por empresa.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('customer', 'create')`

**Tipos de identificador:** `EMAIL`, `PHONE`

**Body:**
```json
{
  "companyId": 1,
  "identifierType": "EMAIL",
  "identifier": "newcustomer@example.com",
  "firstName": "Jane",
  "lastName": "Smith"
}
```

**Normalización automática:**
- **EMAIL**: Convertido a minúsculas
- **PHONE**: Formato internacional (+34666777888)

**Respuesta exitosa (201):**
```json
{
  "id": 2,
  "companyId": 1,
  "identifierType": "EMAIL",
  "identifier": "newcustomer@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "createdAt": "2025-12-23T12:00:00Z"
}
```

**Respuestas de error:**
- **400**: Datos inválidos o cliente duplicado en la empresa

---

### PUT `/customers/{id}`
Actualizar un cliente existente.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('customer', 'update')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del cliente | ✅ |

**Body:**
```json
{
  "identifierType": "PHONE",
  "identifier": "+34611222333",
  "firstName": "Jane",
  "lastName": "Smith-Updated"
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "companyId": 1,
  "identifierType": "PHONE",
  "identifier": "+34611222333",
  "firstName": "Jane",
  "lastName": "Smith-Updated"
}
```

---

### DELETE `/customers/{id}`
Eliminar un cliente.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('customer', 'delete')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del cliente | ✅ |

**Respuesta exitosa (204):** Sin contenido

---

## Promotions API

### GET `/promotions`
Listar todas las promociones.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('promotion', 'read')`

**Respuesta exitosa (200):**
```json
[
  {
    "id": 1,
    "name": "Promoción Verano 2025",
    "description": "Descuento especial de verano",
    "startDate": "2025-06-01",
    "endDate": "2025-08-31",
    "active": true
  }
]
```

---

### GET `/promotions/{id}`
Obtener una promoción por ID.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('promotion', 'read')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la promoción | ✅ |

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "name": "Promoción Verano 2025",
  "description": "Descuento especial de verano",
  "startDate": "2025-06-01",
  "endDate": "2025-08-31",
  "active": true
}
```

---

### POST `/promotions`
Crear una nueva promoción.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('promotion', 'create')`

**Body:**
```json
{
  "name": "Black Friday 2025",
  "description": "Ofertas especiales Black Friday",
  "startDate": "2025-11-25",
  "endDate": "2025-11-27"
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 2,
  "name": "Black Friday 2025",
  "description": "Ofertas especiales Black Friday",
  "startDate": "2025-11-25",
  "endDate": "2025-11-27",
  "active": true
}
```

---

### PUT `/promotions/{id}`
Actualizar una promoción existente.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('promotion', 'update')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la promoción | ✅ |

**Body:**
```json
{
  "name": "Black Friday 2025 Extended",
  "endDate": "2025-11-30"
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "name": "Black Friday 2025 Extended",
  "endDate": "2025-11-30",
  "active": true
}
```

---

### DELETE `/promotions/{id}`
Eliminar una promoción.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('promotion', 'delete')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la promoción | ✅ |

**Respuesta exitosa (204):** Sin contenido

---

### POST `/promotions/{id}/leads`
Registrar un lead (participación) en una promoción.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('lead', 'create')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la promoción | ✅ |

**Body:**
```json
{
  "customerId": 1,
  "deviceId": 1
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "promotionId": 1,
  "customerId": 1,
  "deviceId": 1,
  "couponCode": "ABC123XYZ",
  "couponStatus": "VALID",
  "createdAt": "2025-12-23T12:00:00Z"
}
```

---

### GET `/promotions/{id}/leads`
Listar todos los leads de una promoción.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('lead', 'read')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la promoción | ✅ |

**Respuesta exitosa (200):**
```json
[
  {
    "id": 1,
    "promotionId": 1,
    "customerId": 1,
    "deviceId": 1,
    "couponCode": "ABC123XYZ",
    "couponStatus": "VALID",
    "createdAt": "2025-12-23T12:00:00Z"
  }
]
```

---

### POST `/promotions/{id}/leads/test`
Crear un lead de prueba para testing.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('lead', 'create')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la promoción | ✅ |

**Body (opcional):**
```json
{
  "customerId": 1
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 2,
  "promotionId": 1,
  "customerId": 1,
  "deviceId": 1,
  "couponCode": "TEST123ABC",
  "couponStatus": "VALID",
  "createdAt": "2025-12-23T12:30:00Z"
}
```

---

### GET `/promotions/{id}/leads/export.csv`
Exportar leads a CSV con filtros de fecha (streaming).

**Autenticación:** Requerida  
**Permisos:** `@perm.can('lead', 'read')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la promoción | ✅ |
| `from` | String | Query | Fecha desde (ISO o YYYY-MM-DD) | ❌ |
| `to` | String | Query | Fecha hasta (ISO o YYYY-MM-DD) | ❌ |

**Ejemplo de URL:**
```
GET /promotions/1/leads/export.csv?from=2025-01-01&to=2025-12-31
```

**Respuesta exitosa (200):**
- Content-Type: `text/csv`
- Content-Disposition: `attachment; filename="leads-promotion-1.csv"`
- Body: Archivo CSV

**Formato CSV:**
```csv
id,promotionId,customerId,deviceId,couponCode,couponStatus,createdAt
1,1,5,10,ABC123,VALID,2025-12-23T12:00:00Z
2,1,6,10,XYZ789,REDEEMED,2025-12-23T13:00:00Z
```

---

### GET `/promotions/{id}/leads/summary`
Obtener resumen estadístico de leads.

**Autenticación:** No requerida

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la promoción | ✅ |
| `from` | String | Query | Fecha desde (ISO o YYYY-MM-DD) | ❌ |
| `to` | String | Query | Fecha hasta (ISO o YYYY-MM-DD) | ❌ |

**Respuesta exitosa (200):**
```json
{
  "totalLeads": 150,
  "validCoupons": 120,
  "redeemedCoupons": 25,
  "expiredCoupons": 5,
  "conversionRate": 16.67,
  "dateRange": {
    "from": "2025-06-01",
    "to": "2025-08-31"
  }
}
```

---

## Coupons API

### GET `/coupons/{code}`
Validar un cupón por su código.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('coupon', 'read')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `code` | String | Path | Código del cupón | ✅ |

**Respuesta exitosa (200):**
```json
{
  "couponCode": "ABC123XYZ",
  "valid": true,
  "status": "VALID",
  "redeemedAt": null,
  "expiresAt": "2025-12-31T23:59:59Z",
  "message": null
}
```

**Respuesta cupón inválido (200):**
```json
{
  "couponCode": "ABC123XYZ",
  "valid": false,
  "status": "REDEEMED",
  "redeemedAt": "2025-12-20T10:00:00Z",
  "expiresAt": "2025-12-31T23:59:59Z",
  "message": "Cupón ya canjeado"
}
```

**Respuestas de error:**
- **400**: Código no encontrado o inválido

---

### POST `/coupons/{code}/redeem`
Canjear un cupón (marca como REDEEMED).

**Autenticación:** Requerida  
**Permisos:** `@perm.can('coupon', 'update')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `code` | String | Path | Código del cupón | ✅ |

**Respuesta exitosa (200):**
```json
{
  "couponCode": "ABC123XYZ",
  "valid": true,
  "status": "REDEEMED",
  "redeemedAt": "2025-12-23T14:00:00Z",
  "expiresAt": "2025-12-31T23:59:59Z",
  "message": "REDEEMED"
}
```

**Respuestas de error:**
- **400**: Cupón ya canjeado, expirado o inválido

---

### POST `/coupons/{code}/expire`
Caducar manualmente un cupón (marca como EXPIRED).

**Autenticación:** Requerida  
**Permisos:** `@perm.can('coupon', 'update')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `code` | String | Path | Código del cupón | ✅ |

**Respuesta exitosa (200):**
```json
{
  "couponCode": "ABC123XYZ",
  "valid": false,
  "status": "EXPIRED",
  "redeemedAt": null,
  "expiresAt": "2025-12-31T23:59:59Z",
  "message": "EXPIRED"
}
```

---

### POST `/coupons/issue`
Emitir un nuevo cupón para un cliente y promoción (crea lead histórico).

**Autenticación:** Requerida  
**Permisos:** `@perm.can('coupon', 'create')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `promotionId` | Long | Query | ID de la promoción | ✅ |
| `customerId` | Long | Query | ID del cliente | ✅ |

**Respuesta exitosa (200):**
```json
{
  "couponCode": "NEW987ZYX",
  "valid": true,
  "status": "VALID",
  "redeemedAt": null,
  "expiresAt": "2025-12-31T23:59:59Z",
  "message": "VALID"
}
```

**Respuestas de error:**
- **400**: Cliente ya tiene cupón activo para esta promoción

---

## Advices API

### GET `/advices`
Listar todos los advices (avisos/consejos).

**Autenticación:** Requerida  
**Permisos:** `@perm.can('advice', 'read')`

**Parámetros:** Ninguno

**Respuesta exitosa (200):**
```json
[
  {
    "id": 1,
    "title": "Consejo de Seguridad",
    "content": "Recuerda cambiar tu contraseña regularmente",
    "visible": true,
    "startDate": "2025-01-01T00:00:00Z",
    "endDate": "2025-12-31T23:59:59Z"
  }
]
```

---

### GET `/advices/visibles`
Obtener advices visibles ahora según zona horaria.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('advice', 'read')`

**Descripción:** Filtra advices por rango de fechas usando la zona horaria indicada por headers `X-Timezone` (IANA) o `X-Timezone-Offset` (minutos al ESTE de UTC).

**Parámetros (Headers):**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `X-Timezone` | String | Header | Zona horaria IANA (ej: "Europe/Madrid") | ❌ |
| `X-Timezone-Offset` | String | Header | Minutos al ESTE de UTC (ej: "120") | ❌ |

**Respuesta exitosa (200):**
```json
[
  {
    "id": 1,
    "title": "Oferta del Día",
    "content": "20% descuento en productos seleccionados",
    "visible": true
  }
]
```

---

### GET `/advices/{id}`
Obtener un advice por ID.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('advice', 'read')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del advice | ✅ |

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "title": "Consejo de Seguridad",
  "content": "Recuerda cambiar tu contraseña regularmente",
  "visible": true
}
```

---

### POST `/advices`
Crear un nuevo advice.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('advice', 'create')`

**Body:**
```json
{
  "title": "Nuevo Consejo",
  "content": "Este es el contenido del consejo",
  "visible": true,
  "startDate": "2025-01-01T00:00:00Z",
  "endDate": "2025-12-31T23:59:59Z"
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 2,
  "title": "Nuevo Consejo",
  "content": "Este es el contenido del consejo",
  "visible": true
}
```

---

### PUT `/advices/{id}`
Actualizar un advice existente.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('advice', 'update')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del advice | ✅ |

**Body:**
```json
{
  "title": "Consejo Actualizado",
  "content": "Contenido actualizado del consejo",
  "visible": false
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "title": "Consejo Actualizado",
  "content": "Contenido actualizado del consejo",
  "visible": false
}
```

---

### DELETE `/advices/{id}`
Eliminar un advice.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('advice', 'delete')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID del advice | ✅ |

**Respuesta exitosa (204):** Sin contenido

---

## Billing API

### POST `/api/billing/checkout-session/{companyId}`
Crear sesión de checkout de Stripe.

**Autenticación:** Requerida  
**Permisos:** `hasRole('admin')` o `hasRole('company_admin')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `companyId` | Long | Path | ID de la compañía | ✅ |

**Respuesta exitosa (200):**
```json
{
  "id": "cs_test_a1b2c3d4e5f6g7h8i9j0",
  "url": "https://checkout.stripe.com/pay/cs_test_..."
}
```

---

### POST `/api/billing/portal-session/{companyId}`
Crear sesión del portal de facturación de Stripe.

**Autenticación:** Requerida  
**Permisos:** `hasRole('admin')` o `hasRole('company_admin')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `companyId` | Long | Path | ID de la compañía | ✅ |

**Respuesta exitosa (200):**
```json
{
  "url": "https://billing.stripe.com/session/test_xxx"
}
```

---

## App Versions API

### GET `/app-versions`
Listar todas las versiones de la aplicación.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('appversion', 'read')`

**Parámetros:** Ninguno

**Respuesta exitosa (200):**
```json
[
  {
    "id": 1,
    "platform": "android",
    "version": "1.0.0",
    "minVersion": "1.0.0",
    "forceUpdate": false
  },
  {
    "id": 2,
    "platform": "ios",
    "version": "1.0.0",
    "minVersion": "1.0.0",
    "forceUpdate": false
  }
]
```

---

### GET `/app-versions/{id}`
Obtener una versión por ID.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('appversion', 'read')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la versión | ✅ |

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "platform": "android",
  "version": "1.0.0",
  "minVersion": "1.0.0",
  "forceUpdate": false
}
```

---

### GET `/app-versions/latest/{platform}`
Obtener la última versión por plataforma.

**Autenticación:** Requerida  
**Permisos:** `@perm.can('appversion', 'read')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `platform` | String | Path | Plataforma (android/ios) | ✅ |

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "platform": "android",
  "version": "2.5.0",
  "minVersion": "2.0.0",
  "forceUpdate": false
}
```

---

### POST `/app-versions`
Crear una nueva versión de la aplicación.

**Autenticación:** Requerida  
**Permisos:** `hasRole('ROLE_ADMIN')` o `@perm.can('appversion', 'create')`

**Body:**
```json
{
  "platform": "android",
  "version": "1.1.0",
  "minVersion": "1.0.0",
  "forceUpdate": false
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 3,
  "platform": "android",
  "version": "1.1.0",
  "minVersion": "1.0.0",
  "forceUpdate": false
}
```

---

### PUT `/app-versions/{id}`
Actualizar una versión existente.

**Autenticación:** Requerida  
**Permisos:** `hasRole('ROLE_ADMIN')` o `@perm.can('appversion', 'update')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la versión | ✅ |

**Body:**
```json
{
  "platform": "android",
  "version": "1.2.0",
  "minVersion": "1.1.0",
  "forceUpdate": true
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "platform": "android",
  "version": "1.2.0",
  "minVersion": "1.1.0",
  "forceUpdate": true
}
```

---

### DELETE `/app-versions/{id}`
Eliminar una versión.

**Autenticación:** Requerida  
**Permisos:** `hasRole('ROLE_ADMIN')` o `@perm.can('appversion', 'delete')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la versión | ✅ |

**Respuesta exitosa (200):** Sin contenido

---

## App Entities API

### GET `/entities`
Listar todas las entidades de la aplicación.

**Autenticación:** Requerida  
**Permisos:** `hasRole('ROLE_ADMIN')` o `@perm.can('appentity', 'read')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `withCount` | boolean | Query | Incluir conteo de atributos | ❌ (default: false) |

**Respuesta exitosa (200):**
```json
[
  {
    "id": 1,
    "resource": "device",
    "displayName": "Dispositivo",
    "attributeCount": 5
  },
  {
    "id": 2,
    "resource": "customer",
    "displayName": "Cliente",
    "attributeCount": 3
  }
]
```

---

### GET `/entities/{id}`
Obtener una entidad por ID.

**Autenticación:** Requerida  
**Permisos:** `hasRole('ROLE_ADMIN')` o `@perm.can('appentity', 'read')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la entidad | ✅ |
| `withCount` | boolean | Query | Incluir conteo de atributos | ❌ |

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "resource": "device",
  "displayName": "Dispositivo",
  "attributes": []
}
```

---

### GET `/entities/by-resource/{resource}`
Obtener una entidad por nombre de recurso.

**Autenticación:** Requerida  
**Permisos:** `hasRole('ROLE_ADMIN')` o `@perm.can('appentity', 'read')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `resource` | String | Path | Nombre del recurso | ✅ |
| `withCount` | boolean | Query | Incluir conteo de atributos | ❌ |

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "resource": "device",
  "displayName": "Dispositivo"
}
```

---

### PUT `/entities`
Crear o actualizar entidad (upsert).

**Autenticación:** Requerida  
**Permisos:** `hasRole('ROLE_ADMIN')` o `@perm.can('appentity', 'update')`

**Body:**
```json
{
  "resource": "device",
  "displayName": "Dispositivo",
  "attributes": []
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "resource": "device",
  "displayName": "Dispositivo"
}
```

---

### PUT `/entities/{id}`
Actualizar una entidad por ID.

**Autenticación:** Requerida  
**Permisos:** `hasRole('ROLE_ADMIN')` o `@perm.can('appentity', 'update')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la entidad | ✅ |

**Body:**
```json
{
  "resource": "device",
  "displayName": "Dispositivo Actualizado"
}
```

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "resource": "device",
  "displayName": "Dispositivo Actualizado"
}
```

**Respuestas de error:**
- **400**: ID no coincide con el del body

---

### DELETE `/entities/{id}`
Eliminar una entidad.

**Autenticación:** Requerida  
**Permisos:** `hasRole('ROLE_ADMIN')` o `@perm.can('appentity', 'delete')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la entidad | ✅ |

**Respuesta exitosa (204):** Sin contenido

---

### PUT `/entities/reorder`
Reordenar entidades (drag & drop).

**Autenticación:** Requerida  
**Permisos:** `hasRole('ROLE_ADMIN')` o `@perm.can('appentity', 'update')`

**Body:**
```json
{
  "ids": [3, 1, 2]
}
```

**Respuesta exitosa (204):** Sin contenido

---

### PUT `/entities/{id}/attributes/reorder`
Reordenar atributos de una entidad (drag & drop).

**Autenticación:** Requerida  
**Permisos:** `hasRole('ROLE_ADMIN')` o `@perm.can('appentity', 'update')`

**Parámetros:**
| Nombre | Tipo | Ubicación | Descripción | Requerido |
|--------|------|-----------|-------------|-----------|
| `id` | Long | Path | ID de la entidad | ✅ |

**Body:**
```json
{
  "ids": [5, 3, 1, 2, 4]
}
```

**Respuesta exitosa (204):** Sin contenido

---

## Códigos de Estado HTTP

| Código | Descripción |
|--------|-------------|
| **200** | OK - Operación exitosa |
| **201** | Created - Recurso creado exitosamente |
| **204** | No Content - Eliminación exitosa (sin cuerpo) |
| **400** | Bad Request - Datos inválidos o error de negocio |
| **401** | Unauthorized - Autenticación requerida o inválida |
| **403** | Forbidden - Sin permisos suficientes |
| **404** | Not Found - Recurso no encontrado |
| **409** | Conflict - Conflicto (recurso duplicado) |
| **413** | Payload Too Large - Archivo demasiado grande |
| **500** | Internal Server Error - Error del servidor |

---

## Notas Importantes

### Autenticación Híbrida
La mayoría de endpoints soportan tanto JWT como API Key. Para API Key, usar headers:
```http
X-API-KEY: {api_key}
client-id: {client_id}
```

### Sistema de Permisos
Los permisos se validan con `@perm.can(resource, action)`:
- **Recursos**: user, company, device, media, customer, promotion, coupon, lead
- **Acciones**: read, create, update, delete

### Filtros Multi-Tenant
Los datos se filtran automáticamente por compañía cuando el contexto de seguridad lo requiere (filtros de Hibernate).

### Formatos de Fecha
- **ISO 8601**: `2025-12-23T10:00:00Z`
- **Formato simple**: `2025-12-23` o `YYYY-MM-DD`

### Paginación
Actualmente no implementada. Se devuelven todos los resultados.

### Rate Limiting
No implementado actualmente.

---

**Versión de la API:** 1.0  
**Última actualización:** 23 de Diciembre de 2025
