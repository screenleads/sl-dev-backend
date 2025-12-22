# 📬 ScreenLeads - Colecciones Postman

Colecciones completas y exhaustivas de Postman para todos los endpoints de la API de ScreenLeads.

**Última actualización**: 22 de diciembre de 2025 ✨
**Versión API**: v0.0.1-SNAPSHOT
**Spring Boot**: 3.5.9
**Java**: 17

## 📋 Índice de Colecciones

### 🔐 Autenticación y Seguridad
1. **ScreenLeads-Auth.postman_collection.json** - Autenticación JWT
2. **ScreenLeads-APIKeys.postman_collection.json** - API Keys y Clients

### 📊 Gestión de Contenido
3. **ScreenLeads-Advices.postman_collection.json** - Anuncios (Advices)
4. **ScreenLeads-Media.postman_collection.json** - Multimedia ⭐ **ACTUALIZADO 22/12/2025**
5. **ScreenLeads-Promotions.postman_collection.json** - Promociones y Leads
6. **ScreenLeads-MediaTypes.postman_collection.json** - Tipos de medios

### 🏢 Gestión Empresarial
7. **ScreenLeads-Companies.postman_collection.json** - Compañías
8. **ScreenLeads-Customers.postman_collection.json** - Clientes

### 🖥️ Dispositivos
9. **ScreenLeads-Devices.postman_collection.json** - Dispositivos (pantallas LED)
10. **ScreenLeads-DeviceTypes.postman_collection.json** - Tipos de dispositivos

### 🎟️ Cupones
11. **ScreenLeads-Coupons.postman_collection.json** - Validación y canje de cupones

### 👥 Administración
12. **ScreenLeads-Users-Roles.postman_collection.json** - Usuarios y Roles
13. **ScreenLeads-AppVersions-Entities.postman_collection.json** - Versiones y Entidades

### 💳 Facturación
14. **ScreenLeads-Billing.postman_collection.json** - Integración con Stripe

## 🌍 Entornos

Disponemos de 3 entornos preconfigurados:

- **ScreenLeads-Environment-Dev.postman_environment.json** - Desarrollo local (`http://localhost:3000`)
- **ScreenLeads-Environment-Pre.postman_environment.json** - Pre-producción ⭐ **ACTUALIZADO** (`https://sl-dev-backend-pre.herokuapp.com`)
- **ScreenLeads-Environment-Pro.postman_environment.json** - Producción (`https://api.screenleads.com`)

## 🚀 Configuración Inicial

### 1. Importar Colecciones y Entornos

1. Abre Postman
2. Click en **Import**
3. Arrastra todos los archivos `.json` de esta carpeta
4. Selecciona el entorno apropiado (Dev/Pre/Pro) en el selector superior derecho

### 2. Variables de Entorno Necesarias

Cada entorno debe configurar las siguientes variables:

```javascript
base_url         // URL base del API (ej: http://localhost:8080 o https://api.screenleads.com)
jwt_token        // Se autocompleta al hacer login
api_key          // Tu API Key (se obtiene desde /clients)
client_id        // Tu Client ID (se obtiene al crear un client)
company_id       // ID de la compañía (ej: 1)
```

### 3. Flujo de Trabajo Recomendado

#### Opción A: Autenticación con JWT

1. **Login** → Colección: `01. Authentication` → `Login`
   - El token JWT se guarda automáticamente en `{{jwt_token}}`
   - Todas las demás llamadas con JWT Auth usarán este token

2. **Usar cualquier endpoint** con autenticación Bearer Token

#### Opción B: Autenticación con API Key

1. **Crear Client** → Colección: `06. API Keys & Clients` → `Clients` → `Create Client`
   - Guarda el `clientId` y `apiKey` devueltos

2. **Configurar permisos** → Colección: `06. API Keys & Clients` → `API Keys` → `Update API Key Permissions`

3. **Usar endpoints con API Key** → Todas las colecciones tienen carpetas "API Key Authentication"

## 📚 Detalles de las Colecciones

### 🔐 01. Authentication (JWT)

Endpoints de autenticación con tokens JWT:

- ✅ **POST** `/auth/login` - Iniciar sesión
- ✅ **POST** `/auth/register` - Registro de usuarios
- ✅ **GET** `/auth/me` - Usuario actual
- ✅ **POST** `/auth/change-password` - Cambiar contraseña
- ✅ **POST** `/auth/refresh` - Renovar token

### � 04. Media (Multimedia) ⭐ **ACTUALIZADO 22/12/2025**

Gestión de archivos multimedia con **procesamiento síncrono**.

#### 🚀 Cambios Importantes:

**Antes (Asíncrono con polling):**
1. POST /medias/upload → `status: "processing"` + `jobId`
2. GET /medias/status/{filename} (múltiples llamadas hasta `status: "ready"`)
3. Obtener URLs finales

**Ahora (Síncrono - respuesta inmediata):**
1. POST /medias/upload → `status: "ready"` + URLs + thumbnails (en una sola llamada)

#### Endpoints Disponibles:

**JWT + API Key Authentication:**
- ✅ **GET** `/medias` - Listar todos los archivos multimedia
- ✅ **POST** `/medias/upload` - **Subida síncrona** (NUEVO)
- ⚠️ **GET** `/medias/status/{filename}` - Verificación de estado (DEPRECATED - ya no necesario)

#### 📤 POST /medias/upload - Detalles Completos

**Parámetros:**
- `file` (multipart/form-data) - **ÚNICO parámetro requerido**
- ❌ Ya NO requiere `companyId` ni `mediaTypeId`

**Formatos soportados:**
- 🖼️ **Imágenes**: JPG, JPEG, PNG, GIF, WebP (max 50MB)
- 🎬 **Videos**: MP4, AVI, MOV, MKV, WebM (max 100MB)

**Procesamiento automático:**
- ✅ Compresión inteligente (H.264 @ 1Mbps para videos)
- ✅ Redimensionado automático (máx 1920x1080)
- ✅ Generación de thumbnails (320px y 640px)
- ✅ Subida a Firebase Storage
- ✅ URLs públicas generadas automáticamente

**Timeouts:**
- ⏱️ Connection timeout: 5 minutos
- ⏱️ Read timeout: 5 minutos
- ✅ Suficiente para videos grandes

**Respuesta exitosa (200 OK):**
```json
{
  "status": "ready",
  "type": "image" | "video",
  "url": "https://storage.googleapis.com/.../compressed-uuid-file.jpg",
  "thumbnails": [
    "https://storage.googleapis.com/.../thumb-320-uuid-file.jpg",
    "https://storage.googleapis.com/.../thumb-640-uuid-file.jpg"
  ],
  "processingTimeMs": 2500
}
```

**Respuestas de error:**
- `400 Bad Request` - Archivo vacío
- `413 Payload Too Large` - Archivo demasiado grande
- `500 Internal Server Error` - Error procesando archivo

**Ejemplo de uso en Postman:**
```
POST {{base_url}}/medias/upload
Authorization: Bearer {{jwt_token}}
Content-Type: multipart/form-data

Body:
- file: [seleccionar archivo imagen o video]
```

**Ejemplo con API Key:**
```
POST {{base_url}}/medias/upload
X-API-KEY: {{api_key}}
client-id: {{client_id}}
Content-Type: multipart/form-data

Body:
- file: [seleccionar archivo imagen o video]
```

**Permisos requeridos:**
- JWT: `@PreAuthorize("@perm.can('media', 'create')")`
- API Key: Permiso `media:create`

### �🔑 06. API Keys & Clients

Gestión de clientes y API Keys para autenticación programática:

**Clients:**
- ✅ **GET** `/clients` - Listar clients
- ✅ **GET** `/clients/{id}` - Obtener client por ID
- ✅ **POST** `/clients` - Crear client (genera API Key automáticamente)
- ✅ **DELETE** `/clients/{id}` - Eliminar client

**API Keys:**
- ✅ **GET** `/api-keys/client/{clientId}` - Listar API Keys de un client
- ✅ **POST** `/api-keys/client/{clientId}` - Generar nueva API Key
- ✅ **PUT** `/api-keys/{apiKeyId}/permissions` - Actualizar permisos
- ✅ **DELETE** `/api-keys/{apiKeyId}` - Revocar API Key

### 📢 07. Advices (Anuncios) ⭐ NUEVO

Gestión completa de anuncios con horarios y programación:

**JWT Authentication:**
- ✅ **GET** `/advices` - Listar todos los advices
- ✅ **GET** `/advices/visibles` - Advices visibles ahora (con zona horaria)
- ✅ **GET** `/advices/{id}` - Obtener advice por ID
- ✅ **POST** `/advices` - Crear advice con schedules
- ✅ **PUT** `/advices/{id}` - Actualizar advice
- ✅ **DELETE** `/advices/{id}` - Eliminar advice

**API Key Authentication:**
- ✅ Todos los endpoints anteriores también con API Key

**Headers especiales para `/advices/visibles`:**
- `X-Timezone`: Zona horaria IANA (ej: "Europe/Madrid")
- `X-Timezone-Offset`: Offset en minutos (ej: "120")

### 🏢 08. Companies ⭐ NUEVO

CRUD completo de compañías:

**JWT + API Key Auth:**
- ✅ **GET** `/companies` - Listar compañías
- ✅ **GET** `/companies/{id}` - Obtener por ID
- ✅ **POST** `/companies` - Crear compañía
- ✅ **PUT** `/companies/{id}` - Actualizar
- ✅ **DELETE** `/companies/{id}` - Eliminar

**Permisos requeridos:** `ROLE_ADMIN` o `company:read/create/update/delete`

### 🎟️ 09. Coupons (Cupones) ⭐ NUEVO

Validación, canje y emisión de cupones:

**JWT + API Key Auth:**
- ✅ **GET** `/coupons/{code}` - Validar cupón
- ✅ **POST** `/coupons/{code}/redeem` - Canjear cupón
- ✅ **POST** `/coupons/{code}/expire` - Caducar cupón
- ✅ **POST** `/coupons/issue?promotionId=&customerId=` - Emitir cupón

### 👥 10. Users & Roles ⭐ NUEVO

Gestión de usuarios y roles del sistema:

**Users:**
- ✅ **GET** `/users` - Listar usuarios
- ✅ **GET** `/users/{id}` - Obtener usuario por ID
- ✅ **POST** `/users` - Crear usuario (devuelve contraseña temporal)
- ✅ **PUT** `/users/{id}` - Actualizar usuario
- ✅ **DELETE** `/users/{id}` - Eliminar usuario

**Roles:**
- ✅ **GET** `/roles` - Listar roles
- ✅ **GET** `/roles/{id}` - Obtener rol por ID
- ✅ **GET** `/roles/assignable` - Roles asignables según nivel del usuario
- ✅ **POST** `/roles` - Crear rol
- ✅ **PUT** `/roles/{id}` - Actualizar rol
- ✅ **DELETE** `/roles/{id}` - Eliminar rol

### 🖥️ 02. Devices (Actualizado)

Gestión de dispositivos (pantallas LED):

**CRUD Básico (JWT + API Key):**
- ✅ **GET** `/devices` - Listar dispositivos
- ✅ **GET** `/devices/{id}` - Obtener por ID
- ✅ **GET** `/devices/uuid/{uuid}` - Obtener por UUID
- ✅ **HEAD** `/devices/uuid/{uuid}` - Comprobar existencia ⭐ NUEVO
- ✅ **POST** `/devices` - Crear dispositivo
- ✅ **PUT** `/devices/{id}` - Actualizar
- ✅ **DELETE** `/devices/{id}` - Eliminar

**Gestión de Advices por Dispositivo:** ⭐ NUEVO
- ✅ **GET** `/devices/{deviceId}/advices` - Listar advices del dispositivo
- ✅ **POST** `/devices/{deviceId}/advices/{adviceId}` - Asignar advice
- ✅ **DELETE** `/devices/{deviceId}/advices/{adviceId}` - Quitar advice

### 📺 03. Promotions & Leads (Actualizado)

Gestión de promociones y leads (captación de clientes):

**Promotions (JWT):**
- ✅ **GET** `/promotions` - Listar promociones
- ✅ **GET** `/promotions/{id}` - Obtener por ID
- ✅ **POST** `/promotions` - Crear promoción
- ✅ **PUT** `/promotions/{id}` - Actualizar
- ✅ **DELETE** `/promotions/{id}` - Eliminar

**Leads:** ⭐ NUEVO
- ✅ **POST** `/promotions/{id}/leads` - Registrar lead
- ✅ **GET** `/promotions/{id}/leads` - Listar leads
- ✅ **POST** `/promotions/{id}/leads/test` - Crear lead de prueba
- ✅ **GET** `/promotions/{id}/leads/export.csv?from=&to=` - Exportar CSV
- ✅ **GET** `/promotions/{id}/leads/summary?from=&to=` - Resumen estadístico

### 👤 04. Customers

Gestión de clientes que participan en promociones:

- ✅ **GET** `/customers` - Listar clientes
- ✅ **GET** `/customers/{id}` - Obtener por ID
- ✅ **POST** `/customers` - Crear cliente
- ✅ **PUT** `/customers/{id}` - Actualizar
- ✅ **DELETE** `/customers/{id}` - Eliminar

### 📱 11. App Versions & Entities ⭐ NUEVO

**App Versions:**
- ✅ **GET** `/app-versions` - Listar versiones
- ✅ **GET** `/app-versions/{id}` - Obtener por ID
- ✅ **GET** `/app-versions/latest/{platform}` - Última versión (android/ios)
- ✅ **POST** `/app-versions` - Crear versión
- ✅ **PUT** `/app-versions/{id}` - Actualizar
- ✅ **DELETE** `/app-versions/{id}` - Eliminar

**App Entities:**
- ✅ **GET** `/entities?withCount=true` - Listar entidades
- ✅ **GET** `/entities/{id}?withCount=true` - Obtener por ID
- ✅ **GET** `/entities/by-resource/{resource}?withCount=true` - Por nombre
- ✅ **PUT** `/entities` - Crear/Actualizar (upsert)
- ✅ **PUT** `/entities/{id}` - Actualizar por ID
- ✅ **DELETE** `/entities/{id}` - Eliminar
- ✅ **PUT** `/entities/reorder` - Reordenar entidades
- ✅ **PUT** `/entities/{id}/attributes/reorder` - Reordenar atributos

### 💳 12. Billing (Stripe) ⭐ NUEVO

Integración con Stripe para facturación:

- ✅ **POST** `/api/billing/checkout-session/{companyId}` - Crear sesión de pago
- ✅ **POST** `/api/billing/portal-session/{companyId}` - Portal de facturación

**Requiere:** `ROLE_ADMIN` o `ROLE_COMPANY_ADMIN`

### 📸 05. Media

Gestión de archivos multimedia:

- **GET** `/medias` - Listar media
- **GET** `/medias/{id}` - Obtener por ID
- **POST** `/medias` - Crear media
- **PUT** `/medias/{id}` - Actualizar
- **DELETE** `/medias/{id}` - Eliminar
- **GET** `/medias/types` - Tipos de media
- **POST** `/medias/types` - Crear tipo

### 🔧 05. Admin

Endpoints administrativos del sistema:

- **GET** `/admin/health` - Estado del sistema
- Otros endpoints de administración

## 🔒 Sistema de Permisos

### Permisos disponibles por recurso

Cada recurso tiene 4 operaciones básicas: `read`, `create`, `update`, `delete`

**Recursos disponibles:**
- `advice` - Anuncios
- `company` - Compañías
- `device` - Dispositivos
- `devicetype` - Tipos de dispositivo
- `media` - Multimedia
- `mediatype` - Tipos de media
- `promotion` - Promociones
- `lead` - Leads de promociones
- `coupon` - Cupones
- `customer` - Clientes
- `user` - Usuarios
- `appversion` - Versiones de app
- `appentity` - Entidades del sistema

### Roles especiales

- `ROLE_ADMIN` - Acceso total al sistema
- `ROLE_COMPANY_ADMIN` - Administrador de compañía
- `ROLE_USER` - Usuario estándar

### Configuración de permisos en API Keys

Para configurar permisos en una API Key:

```json
{
  "permissions": [
    {
      "resource": "device",
      "actions": ["read", "create", "update"]
    },
    {
      "resource": "advice",
      "actions": ["read"]
    }
  ],
  "companyScope": [1, 2, 3],  // IDs de compañías permitidas
  "globalAccess": false        // true = acceso a todas las compañías
}
```

## 📝 Ejemplos de Uso

### Ejemplo 1: Crear un Advice con Horarios

```json
POST /advices
{
  "company": {"id": 1},
  "customInterval": false,
  "description": "Promoción de fin de semana",
  "interval": "",
  "media": {"id": 1},
  "promotion": null,
  "schedules": [
    {
      "startDate": "2025-12-01T00:00:00.000Z",
      "endDate": "2025-12-31T23:59:59.999Z",
      "dayWindows": [
        {
          "weekday": "SATURDAY",
          "ranges": [{"fromTime": "10:00", "toTime": "22:00"}]
        },
        {
          "weekday": "SUNDAY",
          "ranges": [{"fromTime": "10:00", "toTime": "22:00"}]
        }
      ]
    }
  ]
}
```

### Ejemplo 2: Obtener Advices Visibles Ahora

```bash
GET /advices/visibles
Headers:
  Authorization: Bearer {{jwt_token}}
  X-Timezone: Europe/Madrid
  X-Timezone-Offset: 120
```

### Ejemplo 3: Validar y Canjear un Cupón

```bash
# 1. Validar
GET /coupons/PROMO2025ABC

# 2. Canjear si es válido
POST /coupons/PROMO2025ABC/redeem
```

### Ejemplo 4: Exportar Leads de una Promoción

```bash
GET /promotions/1/leads/export.csv?from=2025-12-01&to=2025-12-31

Headers:
  Authorization: Bearer {{jwt_token}}
```

## 🐛 Troubleshooting

### Error 401 Unauthorized

**JWT:**
- Verifica que el token esté guardado en `{{jwt_token}}`
- Ejecuta `Login` de nuevo para obtener un token fresco
- Los tokens JWT expiran después de cierto tiempo

**API Key:**
- Verifica que los headers `X-API-KEY` y `client-id` estén configurados
- Comprueba que la API Key tenga los permisos necesarios
- Verifica que la API Key no haya sido revocada

### Error 403 Forbidden

- Tu usuario/API Key no tiene permisos para este recurso
- Para API Keys: actualiza permisos en `/api-keys/{id}/permissions`
- Para JWT: contacta con un administrador para ajustar roles

### Error 404 Not Found

- Verifica que el ID del recurso existe
- Comprueba que estás usando el entorno correcto (Dev/Pre/Pro)
- Algunos recursos pueden estar filtrados por `companyScope`

### Error 400 Bad Request

- Revisa el JSON del body (sintaxis correcta)
- Verifica que todos los campos requeridos estén presentes
- Comprueba que los tipos de datos sean correctos

## 🆕 Novedades en esta versión

### Colecciones Nuevas
- ✨ **Advices** - Gestión completa de anuncios con horarios
- ✨ **Companies** - CRUD completo de compañías
- ✨ **Coupons** - Sistema de cupones separado de Promotions
- ✨ **Users & Roles** - Administración de usuarios y permisos
- ✨ **App Versions & Entities** - Versionado y entidades del sistema
- ✨ **Billing** - Integración con Stripe

### Mejoras en Colecciones Existentes
- ✅ **Devices** - Añadidos endpoints de gestión de advices por dispositivo
- ✅ **Promotions** - Añadidos endpoints de leads (registro, listado, export, summary)
- ✅ **Auth** - Añadido endpoint de cambio de contraseña
- ✅ **API Keys** - Reestructurada con separación clara de Clients y API Keys

### Estructura Mejorada
- 📁 Todas las colecciones tienen carpetas "JWT Authentication" y "API Key Authentication"
- 📝 Descripciones detalladas en cada endpoint
- 🔄 Scripts de test para autocompletar variables de entorno
- 🎯 Ejemplos de request body más completos y realistas

## 📞 Soporte

Para cualquier duda o problema con las colecciones:

1. Revisa la sección de Troubleshooting
2. Consulta la documentación Swagger en `/swagger-ui/index.html`
3. Contacta al equipo de desarrollo

---

**Última actualización:** Diciembre 2025  
**Versión de las colecciones:** 2.0.0  
**Total de endpoints:** 100+
