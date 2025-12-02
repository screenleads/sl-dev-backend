# Recursos y Permisos - ScreenLeads Backend

## 📦 Recursos Disponibles

Esta es la lista completa de recursos sobre los que se pueden definir permisos de API Keys.

---

## **Recursos de Negocio**

### `advice` - Consejos/Avisos
Gestión de consejos mostrados a los usuarios.

**Acciones:**
- `advice:read` - Consultar consejos
- `advice:create` - Crear nuevos consejos
- `advice:update` - Actualizar consejos existentes
- `advice:delete` - Eliminar consejos

**Ejemplo de uso:**
```java
@PreAuthorize("@apiKeyPerm.can('advice', 'read')")
@GetMapping("/advices")
public List<Advice> getAllAdvices() { ... }
```

---

### `promotion` - Promociones
Gestión de promociones y campañas.

**Acciones:**
- `promotion:read` - Consultar promociones
- `promotion:create` - Crear nuevas promociones
- `promotion:update` - Actualizar promociones
- `promotion:delete` - Eliminar promociones

**Ejemplo de uso:**
```java
@PreAuthorize("@apiKeyPerm.can('promotion', 'create')")
@PostMapping("/promotions")
public Promotion createPromotion(@RequestBody PromotionDTO dto) { ... }
```

---

### `lead` - Leads/Contactos
Gestión de leads generados por las promociones.

**Acciones:**
- `lead:read` - Consultar leads
- `lead:create` - Crear nuevos leads (típico para webhooks)
- `lead:update` - Actualizar leads
- `lead:delete` - Eliminar leads

**Ejemplo de uso:**
```java
@PreAuthorize("@apiKeyPerm.can('lead', 'create')")
@PostMapping("/leads")
public PromotionLead createLead(@RequestBody LeadDTO dto) { ... }
```

---

### `customer` - Clientes
Gestión de clientes/consumidores.

**Acciones:**
- `customer:read` - Consultar clientes
- `customer:create` - Crear nuevos clientes
- `customer:update` - Actualizar clientes
- `customer:delete` - Eliminar clientes

---

## **Recursos de Configuración**

### `company` - Compañías
Gestión de compañías/organizaciones.

**Acciones:**
- `company:read` - Consultar compañías
- `company:create` - Crear nuevas compañías (típicamente solo admin)
- `company:update` - Actualizar compañías
- `company:delete` - Eliminar compañías

**Nota:** El acceso está típicamente restringido por `companyScope`.

---

### `device` - Dispositivos
Gestión de dispositivos (pantallas, tablets, etc.).

**Acciones:**
- `device:read` - Consultar dispositivos
- `device:create` - Registrar nuevos dispositivos
- `device:update` - Actualizar configuración de dispositivos
- `device:delete` - Eliminar dispositivos

---

### `media` - Archivos Multimedia
Gestión de imágenes, videos, etc.

**Acciones:**
- `media:read` - Consultar/descargar archivos
- `media:create` - Subir nuevos archivos
- `media:update` - Actualizar metadata de archivos
- `media:delete` - Eliminar archivos

---

## **Recursos de Administración**

### `user` - Usuarios
Gestión de usuarios del sistema.

**Acciones:**
- `user:read` - Consultar usuarios
- `user:create` - Crear nuevos usuarios
- `user:update` - Actualizar usuarios
- `user:delete` - Eliminar usuarios

**Advertencia:** ⚠️ Típicamente restringido a ROLE_ADMIN.

---

### `role` - Roles
Gestión de roles y permisos de usuarios.

**Acciones:**
- `role:read` - Consultar roles
- `role:create` - Crear nuevos roles
- `role:update` - Actualizar roles
- `role:delete` - Eliminar roles

**Advertencia:** ⚠️ Típicamente restringido a ROLE_ADMIN.

---

### `client` - Clientes API
Gestión de clientes que usan las API Keys.

**Acciones:**
- `client:read` - Consultar clientes API
- `client:create` - Crear nuevos clientes API
- `client:update` - Actualizar clientes API
- `client:delete` - Eliminar clientes API

**Advertencia:** ⚠️ Típicamente restringido a ROLE_ADMIN.

---

### `apikey` - API Keys
Gestión de las propias API Keys.

**Acciones:**
- `apikey:read` - Consultar API Keys
- `apikey:create` - Generar nuevas API Keys
- `apikey:update` - Actualizar API Keys (cambiar permisos, scope)
- `apikey:delete` - Revocar API Keys

**Advertencia:** ⚠️ Típicamente restringido a ROLE_ADMIN o self-management.

---

## **Recursos del Sistema**

### `appversion` - Versiones de la Aplicación
Control de versiones de la aplicación móvil/web.

**Acciones:**
- `appversion:read` - Consultar versiones disponibles
- `appversion:create` - Publicar nuevas versiones
- `appversion:update` - Actualizar metadata de versiones
- `appversion:delete` - Eliminar versiones

---

### `mediatype` - Tipos de Medios
Catálogo de tipos de archivos multimedia aceptados.

**Acciones:**
- `mediatype:read` - Consultar tipos disponibles
- `mediatype:create` - Agregar nuevos tipos
- `mediatype:update` - Actualizar tipos
- `mediatype:delete` - Eliminar tipos

---

### `devicetype` - Tipos de Dispositivos
Catálogo de tipos de dispositivos soportados.

**Acciones:**
- `devicetype:read` - Consultar tipos disponibles
- `devicetype:create` - Agregar nuevos tipos
- `devicetype:update` - Actualizar tipos
- `devicetype:delete` - Eliminar tipos

---

## 🎯 **Configuraciones Típicas por Tipo de Integración**

### **Webhook Receptor de Leads**
```sql
permissions = 'lead:create,promotion:read'
company_scope = 42  -- Restringido a una compañía
```

### **Dashboard de Reporting**
```sql
permissions = 'advice:read,promotion:read,lead:read,customer:read,device:read'
company_scope = NULL  -- Acceso global para reportes
```

### **Integración de Gestión de Promociones**
```sql
permissions = 'promotion:*,media:read,media:create'
company_scope = 10  -- Restringido a una compañía
```

### **API de Sincronización**
```sql
permissions = 'advice:read,promotion:read,lead:read,lead:update,customer:*'
company_scope = 5  -- Restringido a una compañía
```

### **Administración Completa (Solo Dev/Testing)**
```sql
permissions = '*:*'
company_scope = NULL  -- ⚠️ PELIGROSO en producción
```

### **Solo Lectura Global (Analytics/BI)**
```sql
permissions = '*:read'
company_scope = NULL
```

---

## 🔐 **Recomendaciones de Seguridad por Entorno**

### Desarrollo
```sql
-- Más permisivo para facilitar desarrollo
permissions = 'advice:*,promotion:*,lead:*,customer:*,device:*,media:*'
company_scope = NULL  -- OK para desarrollo
```

### Preproducción
```sql
-- Permisos realistas de producción
permissions = 'promotion:read,promotion:create,lead:read,lead:create'
company_scope = 1  -- Datos de prueba
```

### Producción
```sql
-- Mínimos permisos necesarios
permissions = 'promotion:read,lead:create'
company_scope = 42  -- Compañía real específica
expires_at = '2026-01-01'  -- Con fecha de expiración
```

---

## 📝 **Wildcards y Patrones**

### Todas las acciones sobre un recurso
```
promotion:*     # create, read, update, delete sobre promotions
```

### Una acción sobre todos los recursos
```
*:read          # read sobre advice, promotion, lead, etc.
```

### Super admin (úsalo con precaución)
```
*:*             # TODAS las acciones sobre TODOS los recursos
*               # Equivalente a *:*
```

### Múltiples permisos específicos
```
advice:read,advice:create,promotion:read,promotion:create,lead:read
```

---

## 🧪 **Testing de Permisos**

Usa el controlador de pruebas:

```bash
# Verificar permisos actuales
curl -X GET http://localhost:8080/api/test-permissions/info \
  -H "X-API-KEY: tu-api-key" \
  -H "client-id: tu-client-id"

# Verificar permiso específico
curl -X GET "http://localhost:8080/api/test-permissions/check-multiple?resource=promotion&action=create" \
  -H "X-API-KEY: tu-api-key" \
  -H "client-id: tu-client-id"
```

---

## 📊 **Matriz de Recursos vs Casos de Uso**

| Recurso | Webhook | Dashboard | CRM Sync | Admin |
|---------|---------|-----------|----------|-------|
| advice | - | read | read | * |
| promotion | read | read | * | * |
| lead | create | read | * | * |
| customer | - | read | * | * |
| device | - | read | read | * |
| media | - | read | read/create | * |
| company | - | read | - | * |
| user | - | - | - | * |
| role | - | - | - | read |

`*` = Todas las acciones (read, create, update, delete)
`-` = Sin acceso

---

## ⚙️ **Implementación en Código**

Ver `docs/API_KEY_PERMISSIONS.md` para ejemplos completos de implementación.
