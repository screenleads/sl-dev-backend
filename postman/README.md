# 📦 Colecciones de Postman - ScreenLeads API

Esta carpeta contiene todas las colecciones de Postman para probar la API de ScreenLeads, incluyendo el nuevo sistema de autenticación híbrida (JWT + API Keys).

## 📋 Contenido

### 🌍 Entornos (3)

Las colecciones funcionan con **3 entornos** independientes:

1. **`ScreenLeads-Environment-Dev.postman_environment.json`** 
   - 🏠 **DEV (Local)** - `http://localhost:8080`
   - Para desarrollo local
   - Base de datos local

2. **`ScreenLeads-Environment-Pre.postman_environment.json`**
   - 🚧 **PRE (Preproducción)** - `https://pre-api.screenleads.com`
   - Entorno de staging/testing
   - Datos de prueba

3. **`ScreenLeads-Environment-Pro.postman_environment.json`**
   - 🚀 **PRO (Producción)** - `https://api.screenleads.com`
   - Entorno de producción
   - ⚠️ **¡Usar con precaución!**

**Cada entorno incluye:**
- `base_url` - URL del servidor
- `environment` - Identificador (dev/pre/pro)
- Variables auto-guardadas: `jwt_token`, `api_key`, `client_id`
- Variables de referencia: `company_id`, `device_id`, etc.

### 📚 Colecciones

1. **`ScreenLeads-Auth.postman_collection.json`**
   - ✅ Login
   - ✅ Register
   - ✅ Get Current User
   - ✅ Change Password
   - ✅ Refresh Token
   - 📌 Variables: `base_path=/auth`

2. **`ScreenLeads-Devices.postman_collection.json`**
   - ✅ CRUD de Dispositivos (JWT Auth)
   - ✅ CRUD de Dispositivos (API Key Auth)
   - Ejemplos de ambos métodos de autenticación
   - 📌 Variables: `base_path=/devices`

3. **`ScreenLeads-Promotions.postman_collection.json`**
   - ✅ CRUD de Promociones
   - ✅ Validación de Cupones
   - ✅ Canje de Cupones
   - ✅ Emisión de Cupones
   - 📌 Variables: `promotions_path=/promotions`, `coupons_path=/coupons`

4. **`ScreenLeads-Customers.postman_collection.json`**
   - ✅ CRUD de Clientes/Leads
   - 📌 Variables: `base_path=/customers`

5. **`ScreenLeads-Admin.postman_collection.json`**
   - ✅ Gestión de Usuarios
   - ✅ Gestión de Roles
   - ✅ Gestión de Compañías
   - Requiere `ROLE_ADMIN` o permisos específicos
   - 📌 Variables: `users_path=/users`, `roles_path=/roles`, `companies_path=/companies`

6. **`ScreenLeads-APIKeys.postman_collection.json`**
   - ✅ Gestión de Clients
   - ✅ Creación de API Keys con permisos granulares
   - ✅ Activación/Desactivación de API Keys
   - ✅ Endpoint de test de permisos
   - 📌 Variables: `clients_path=/clients`, `apikeys_path=/api-keys`, `test_path=/test`

7. **`ScreenLeads-Media.postman_collection.json`**
   - ✅ Gestión de Archivos Multimedia
   - ✅ Upload de archivos
   - ✅ Media Types
   - ✅ Avisos (Advices)
   - ✅ Device Types
   - 📌 Variables: `media_path=/medias`, `advices_path=/advices`, `devices_path=/devices`

**Todas las colecciones incluyen:**
- ✅ Variables a nivel de colección para paths
- ✅ Uso de variables de entorno para URLs y tokens
- ✅ Scripts de auto-guardado en requests clave

## 🚀 Cómo Usar

### 1. Importar en Postman

**Opción A: Importar todo**
1. Abre Postman
2. Click en `Import`
3. Arrastra toda la carpeta `postman/` o selecciona todos los archivos `.json`
4. Click en `Import`

**Opción B: Importar uno a uno**
1. Abre Postman
2. Click en `Import`
3. Selecciona un archivo `.json`
4. Repite para cada colección

### 2. Configurar el Entorno

1. En Postman, selecciona el entorno adecuado en el dropdown superior derecho:
   - **ScreenLeads - DEV (Local)** - Para desarrollo local
   - **ScreenLeads - PRE (Preproducción)** - Para testing en servidor de staging
   - **ScreenLeads - PRO (Producción)** - Para producción (⚠️ cuidado)

2. Verifica/Edita las URLs según tu configuración:
   
   **DEV:**
   ```
   base_url: http://localhost:8080
   environment: dev
   ```
   
   **PRE:**
   ```
   base_url: https://pre-api.screenleads.com
   environment: pre
   ```
   
   **PRO:**
   ```
   base_url: https://api.screenleads.com
   environment: pro
   ```

3. Ajusta IDs de referencia según el entorno:
   ```
   company_id: 1 (o el ID apropiado para cada entorno)
   ```

### 3. Autenticarse

**Método 1: Con JWT (Usuario)**
1. Ve a la colección **"01. Authentication"**
2. Ejecuta **"Login"** con tus credenciales
3. El script guardará automáticamente el `jwt_token` en las variables de entorno
4. Todas las requests con `Bearer Token` usarán automáticamente este token

**Método 2: Con API Key**
1. Ve a la colección **"06. API Keys & Clients"**
2. Ejecuta **"Create Client"** para crear un nuevo client
3. Ejecuta **"Create API Key"** con los permisos deseados
4. El script guardará automáticamente el `api_key` en las variables de entorno
5. Usa las requests en la carpeta "API Key Auth" que incluyen headers:
   - `X-API-KEY: {{api_key}}`
   - `client-id: {{client_id}}`

### 4. Flujo Típico de Pruebas

**Para cada entorno (DEV, PRE, PRO):**

```
1. Seleccionar entorno en Postman
2. Login → Guarda JWT Token automáticamente
3. Create Client → Guarda Client ID automáticamente
4. Create API Key → Guarda API Key automáticamente
5. Probar endpoints con JWT (carpetas normales)
6. Probar endpoints con API Key (carpetas "API Key Auth")
```

**⚠️ Recomendaciones por Entorno:**

- **DEV**: Experimenta libremente, crea/borra datos de prueba
- **PRE**: Testing controlado, valida cambios antes de producción
- **PRO**: Solo operaciones validadas, evita DELETE en datos importantes

## 🔐 Ejemplos de Permisos de API Keys

### API Key con Acceso Total
```
Permissions: *:*
Company Scope: NULL (global)
```

### API Key para Gestión de Dispositivos
```
Permissions: device:read,device:create,device:update,device:delete
Company Scope: NULL (todas las compañías) o ID específico
```

### API Key para Dashboard de Cliente
```
Permissions: customer:read,advice:read,promotion:read,device:read
Company Scope: 123 (solo datos de compañía 123)
```

### API Key para Integración Externa
```
Permissions: device:read,customer:create,promotion:read
Company Scope: NULL
```

## 📝 Variables de Entorno Disponibles

### Variables Globales (en todos los entornos)

| Variable | Descripción | Auto-guardada | Entorno |
|----------|-------------|---------------|---------|
| `base_url` | URL base de la API | No | DEV: `http://localhost:8080`<br>PRE: `https://pre-api.screenleads.com`<br>PRO: `https://api.screenleads.com` |
| `environment` | Identificador del entorno | No | dev / pre / pro |
| `jwt_token` | Token JWT del usuario | ✅ Sí (en Login) | Todos |
| `api_key` | API Key activa | ✅ Sí (en Create API Key) | Todos |
| `client_id` | ID del Client | ✅ Sí (en Create Client) | Todos |
| `company_id` | ID de Compañía de prueba | No | Configurar según entorno |
| `device_id` | ID de Dispositivo | ✅ Sí (en Get Devices) | Todos |
| `customer_id` | ID de Cliente | No | Manual |
| `promotion_id` | ID de Promoción | No | Manual |
| `user_id` | ID de Usuario | No | Manual |

### Variables a Nivel de Colección

Cada colección incluye variables para sus paths base:

| Colección | Variables |
|-----------|-----------|
| Auth | `base_path=/auth` |
| Devices | `base_path=/devices` |
| Promotions | `promotions_path=/promotions`, `coupons_path=/coupons` |
| Customers | `base_path=/customers` |
| Admin | `users_path=/users`, `roles_path=/roles`, `companies_path=/companies` |
| API Keys | `clients_path=/clients`, `apikeys_path=/api-keys`, `test_path=/test` |
| Media | `media_path=/medias`, `advices_path=/advices`, `devices_path=/devices` |

## 🎯 Endpoints con Autenticación Híbrida

Los siguientes endpoints aceptan **ambos** tipos de autenticación:

### Solo Permisos (`@perm.can()`)
- `/devices` - Requiere `device:read/create/update/delete`
- `/customers` - Requiere `customer:read/create/update/delete`
- `/promotions` - Requiere `promotion:read/create/update/delete`
- `/advices` - Requiere `advice:read/create/update/delete`
- `/coupons` - Requiere `coupon:read/create/update`
- `/medias` - Requiere `media:read/create`
- `/devices/types` - Requiere `devicetype:read/create/update/delete`
- `/medias/types` - Requiere `mediatype:read/create/update/delete`

### Híbrido (`ROLE_ADMIN or @perm.can()`)
- `/companies` - Requiere `ROLE_ADMIN` o `company:read/create/update/delete`
- `/users` - Requiere `ROLE_ADMIN` o `user:read/create/update/delete`
- `/roles` - Requiere `ROLE_ADMIN` o `user:read/update/delete`
- `/clients` - Requiere `ROLE_ADMIN` o `client:read/create/update/delete`
- `/api-keys` - Requiere `ROLE_ADMIN` o `apikey:read/create/update/delete`
- `/app-versions` - Requiere `ROLE_ADMIN` o `appversion:read/create/update/delete`
- `/company-tokens` - Requiere `ROLE_ADMIN` o `companytoken:read/create/update/delete`
- `/entities` - Requiere `ROLE_ADMIN` o `appentity:read/create/update/delete`

## 🧪 Testing de Permisos

Usa el endpoint de test incluido en la colección **API Keys**:

```http
GET /test/has-permission?resource=device&action=read
Headers:
  X-API-KEY: {{api_key}}
  client-id: {{client_id}}
```

Respuestas:
- ✅ `200 OK` - Permiso concedido
- ❌ `403 Forbidden` - Permiso denegado

## 📖 Recursos Disponibles

```
advice, promotion, lead, customer, company, device, media,
user, role, client, apikey, coupon, devicetype, mediatype,
appversion, companytoken, appentity
```

## 🎬 Acciones Disponibles

```
read, create, update, delete
```

## 🌟 Wildcards

- `*:read` - Leer cualquier recurso
- `device:*` - Cualquier acción sobre devices
- `*:*` - Acceso total

## 📞 Soporte

Para más información consulta:
- `docs/API_KEY_QUICK_START.md` - Guía rápida de API Keys
- `docs/API_KEY_PERMISSIONS.md` - Sistema de permisos detallado
- `docs/HYBRID_AUTHENTICATION.md` - Arquitectura del sistema híbrido

## 🔄 Gestión de Entornos

### Cambiar entre Entornos

1. Click en el dropdown de entornos (esquina superior derecha)
2. Selecciona el entorno deseado
3. Las requests usarán automáticamente la URL correcta

### Sincronizar Variables entre Entornos

Cuando creas recursos en un entorno:
1. Los IDs se guardan automáticamente en las variables del entorno activo
2. Al cambiar de entorno, necesitarás crear/obtener nuevos recursos
3. Cada entorno mantiene sus propias variables independientes

### URLs por Entorno

```bash
# DEV (Local)
http://localhost:8080/devices
http://localhost:8080/auth/login

# PRE (Preproducción)
https://pre-api.screenleads.com/devices
https://pre-api.screenleads.com/auth/login

# PRO (Producción)
https://api.screenleads.com/devices
https://api.screenleads.com/auth/login
```

### Configuración de CORS

Asegúrate de que tu backend permite CORS desde:
- **DEV**: `http://localhost:*` (cualquier puerto)
- **PRE**: Dominios de staging autorizados
- **PRO**: Solo dominios de producción autorizados

---

**Última actualización**: Diciembre 2024
**Versión**: 1.1 - Multi-entorno (DEV/PRE/PRO)

