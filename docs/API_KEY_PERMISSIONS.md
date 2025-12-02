# Sistema de Permisos Granulares para API Keys

## 📋 Descripción General

Este sistema permite que las API Keys tengan permisos granulares y alcance de datos configurable:

- **Permisos por recurso y acción**: Control fino sobre qué puede hacer cada API Key (read, create, update, delete)
- **Alcance de datos**: Acceso global a todas las compañías o restringido a una compañía específica

## 🏗️ Arquitectura

### 1. Modelo de Datos (`ApiKey`)

```java
@Entity
public class ApiKey {
    private Long id;
    private String key;                    // API Key única
    private Client client;                 // Cliente propietario
    private boolean active;                // Estado activo/inactivo
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;       // Fecha de expiración
    private String permissions;            // Permisos en formato "resource:action"
    private Long companyScope;             // NULL = global, ID = compañía específica
    private String description;            // Descripción legible
}
```

### 2. Principal de Autenticación (`ApiKeyPrincipal`)

Objeto que representa la autenticación de una API Key:

```java
public class ApiKeyPrincipal {
    private Long apiKeyId;
    private String clientId;
    private Long clientDbId;
    private Set<String> permissions;
    private Long companyScope;
    
    public boolean hasPermission(String resource, String action);
    public boolean hasGlobalAccess();
    public boolean hasRestrictedAccess();
}
```

### 3. Servicio de Permisos (`ApiKeyPermissionService`)

Métodos disponibles:

```java
@Service("apiKeyPerm")
public class ApiKeyPermissionService {
    boolean can(String resource, String action);
    Long getCompanyScope();
    boolean hasGlobalAccess();
    boolean canAccessCompany(Long companyId);
}
```

## 🔐 Formato de Permisos

Los permisos se almacenan como string separado por comas en formato `resource:action`:

### Ejemplos de Permisos

```
# Permisos específicos
"snapshot:read,snapshot:create,lead:read,lead:update"

# Todas las acciones sobre un recurso
"snapshot:*,lead:*"

# Una acción sobre todos los recursos
"*:read"

# Super admin (todos los recursos, todas las acciones)
"*:*" o simplemente "*"
```

### Recursos Disponibles

- `snapshot` - Snapshots/capturas
- `lead` - Leads/contactos
- `company` - Compañías
- `user` - Usuarios
- `client` - Clientes API
- `apikey` - API Keys
- Cualquier otro recurso de tu dominio

### Acciones Disponibles

- `read` - Lectura/consulta
- `create` - Creación
- `update` - Actualización
- `delete` - Eliminación

## 🌍 Alcance de Datos (Company Scope)

### Acceso Global

```java
companyScope = null
```

- ✅ Puede acceder a datos de **todas las compañías**
- ✅ No se aplica ningún filtro de Hibernate
- ⚠️ Usar con precaución en producción

### Acceso Restringido

```java
companyScope = 42  // ID de la compañía
```

- ✅ Solo accede a datos de la compañía con ID = 42
- ✅ Se aplica automáticamente el filtro de Hibernate `companyFilter`
- ✅ Más seguro para integraciones específicas

## 💻 Uso en Controladores

### Con @PreAuthorize (SpEL)

```java
@RestController
@RequestMapping("/snapshots")
public class SnapshotController {
    
    // Solo si tiene permiso de lectura
    @PreAuthorize("@apiKeyPerm.can('snapshot', 'read')")
    @GetMapping
    public List<Snapshot> getAll() {
        // Los datos ya están filtrados por company_scope si aplica
        return snapshotService.findAll();
    }
    
    // Solo si tiene permiso de creación
    @PreAuthorize("@apiKeyPerm.can('snapshot', 'create')")
    @PostMapping
    public Snapshot create(@RequestBody SnapshotDTO dto) {
        return snapshotService.create(dto);
    }
    
    // Solo si tiene permiso de actualización
    @PreAuthorize("@apiKeyPerm.can('snapshot', 'update')")
    @PutMapping("/{id}")
    public Snapshot update(@PathVariable Long id, @RequestBody SnapshotDTO dto) {
        return snapshotService.update(id, dto);
    }
    
    // Solo si tiene permiso de eliminación
    @PreAuthorize("@apiKeyPerm.can('snapshot', 'delete')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        snapshotService.delete(id);
    }
}
```

### Verificación Manual en Servicios

```java
@Service
public class SnapshotService {
    
    @Autowired
    private ApiKeyPermissionService apiKeyPerm;
    
    public void processSnapshot(Long id) {
        // Verificar permiso manualmente
        if (!apiKeyPerm.can("snapshot", "update")) {
            throw new AccessDeniedException("No tiene permiso para actualizar snapshots");
        }
        
        // Verificar acceso a compañía específica
        Long companyId = snapshot.getCompanyId();
        if (!apiKeyPerm.canAccessCompany(companyId)) {
            throw new AccessDeniedException("No tiene acceso a esta compañía");
        }
        
        // Continuar procesamiento...
    }
}
```

## 🔧 Configuración de API Keys

### Ejemplo 1: API Key de Solo Lectura (Global)

```sql
INSERT INTO api_key (key, client, active, permissions, company_scope, description)
VALUES (
    'sk_readonly_abc123xyz789',
    1,  -- ID del cliente
    true,
    'snapshot:read,lead:read,company:read',
    NULL,  -- Acceso global
    'Read-only API key for reporting dashboard'
);
```

### Ejemplo 2: API Key de Compañía Específica (Full Access)

```sql
INSERT INTO api_key (key, client, active, permissions, company_scope, description)
VALUES (
    'sk_company42_full_xyz789',
    1,
    true,
    'snapshot:*,lead:*,company:read',
    42,  -- Solo compañía ID 42
    'Full access key for Company ABC'
);
```

### Ejemplo 3: API Key Super Admin

```sql
INSERT INTO api_key (key, client, active, permissions, company_scope, description)
VALUES (
    'sk_admin_super_secret',
    1,
    true,
    '*:*',  -- Todos los permisos
    NULL,   -- Acceso global
    'Super admin key - USE WITH CAUTION'
);
```

### Ejemplo 4: API Key de Integración Específica

```sql
INSERT INTO api_key (key, client, active, permissions, company_scope, description, expires_at)
VALUES (
    'sk_webhook_integration',
    2,
    true,
    'snapshot:create,lead:create',
    10,  -- Solo compañía ID 10
    'Webhook integration for external CRM',
    '2026-12-31 23:59:59'  -- Expira en 1 año
);
```

## 🧪 Testing con Postman/cURL

### Request con API Key

```bash
# Acceso global con permisos de lectura
curl -X GET https://api.example.com/snapshots \
  -H "X-API-KEY: sk_readonly_abc123xyz789" \
  -H "client-id: 550e8400-e29b-41d4-a716-446655440000"

# Crear snapshot (requiere permiso create)
curl -X POST https://api.example.com/snapshots \
  -H "X-API-KEY: sk_company42_full_xyz789" \
  -H "client-id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com", "companyId": 42}'
```

### Respuestas Esperadas

```json
// ✅ Success (200/201)
{
  "id": 123,
  "url": "https://example.com",
  "companyId": 42
}

// ❌ Sin permisos (403 Forbidden)
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied"
}

// ❌ API Key inválida (401 Unauthorized)
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication Failed"
}
```

## 🔄 Migración de Base de Datos

Ejecutar el script SQL:

```bash
psql -U postgres -d screenleads_db -f scripts/add_api_key_permissions_and_scope.sql
```

O en tu herramienta favorita de gestión de BD.

## 📊 Matriz de Permisos Recomendada

| Tipo de API Key | Permisos | Company Scope | Uso |
|-----------------|----------|---------------|-----|
| **Read-only** | `*:read` | Específica | Dashboards, reportes |
| **Integration** | `snapshot:create,lead:create` | Específica | Webhooks externos |
| **Full Company** | `snapshot:*,lead:*` | Específica | Gestión completa de una compañía |
| **Global Read** | `*:read` | NULL (global) | Analytics, BI tools |
| **Super Admin** | `*:*` | NULL (global) | Administración, testing |

## 🛡️ Seguridad

### Buenas Prácticas

1. ✅ **Principio de mínimo privilegio**: Da solo los permisos necesarios
2. ✅ **Usa company_scope**: Restringe a compañías específicas siempre que sea posible
3. ✅ **Establece expiración**: Usa `expires_at` para API Keys temporales
4. ✅ **Audita el uso**: Registra todas las peticiones de API Keys
5. ✅ **Rota las keys**: Cambia regularmente las API Keys
6. ✅ **Descripción clara**: Documenta el propósito de cada key en `description`

### ⚠️ Advertencias

- Las API Keys con `*:*` y `company_scope = NULL` tienen **acceso total**
- Nunca expongas las API Keys en código frontend o repositorios públicos
- Revoca inmediatamente cualquier key comprometida (`active = false`)

## 🔍 Debugging

### Ver información del principal actual

```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
if (auth.getPrincipal() instanceof ApiKeyPrincipal) {
    ApiKeyPrincipal principal = (ApiKeyPrincipal) auth.getPrincipal();
    log.info("API Key: {}", principal.getClientId());
    log.info("Permissions: {}", principal.getPermissions());
    log.info("Company Scope: {}", principal.getCompanyScope());
    log.info("Has global access: {}", principal.hasGlobalAccess());
}
```

### Verificar filtro de Hibernate

```java
Session session = entityManager.unwrap(Session.class);
Filter filter = session.getEnabledFilter("companyFilter");
if (filter != null) {
    Long companyId = (Long) filter.getParameter("companyId");
    log.info("Filtro de compañía activo: {}", companyId);
}
```

## 📝 Changelog

### v1.0.0 (2025-12-02)

- ✨ Sistema de permisos granulares `resource:action`
- ✨ Soporte para company scope (global vs específica)
- ✨ `ApiKeyPrincipal` con información completa
- ✨ `ApiKeyPermissionService` con métodos de verificación
- ✨ Filtro automático de Hibernate para company scope
- ✨ Verificación de expiración de API Keys
- 📝 Documentación completa y ejemplos

## 🤝 Contribuir

Al agregar nuevos recursos o acciones, recuerda:

1. Documentar los nuevos permisos en este archivo
2. Agregar ejemplos de uso
3. Actualizar la matriz de permisos recomendada
