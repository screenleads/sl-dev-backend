# 🔑 Quick Start: API Key Permissions

## 🚀 Configuración Rápida

### 1. Ejecutar Migración de Base de Datos

```bash
psql -U postgres -d tu_database -f scripts/add_api_key_permissions_and_scope.sql
```

### 2. Crear tu Primera API Key

```sql
-- API Key con permisos de lectura y creación de snapshots
-- Restringida a la compañía ID 1
INSERT INTO api_key (key, client, active, permissions, company_scope, description, created_at)
VALUES (
    'sk_test_abc123xyz789',
    1,  -- Reemplaza con tu Client ID
    true,
    'snapshot:read,snapshot:create,lead:read',
    1,  -- Reemplaza con tu Company ID (o NULL para acceso global)
    'Test API key',
    NOW()
);
```

### 3. Probar la API Key

```bash
# Obtener información de la API Key
curl -X GET http://localhost:8080/api/test-permissions/info \
  -H "X-API-KEY: sk_test_abc123xyz789" \
  -H "client-id: tu-client-id-uuid"

# Probar permiso de lectura
curl -X GET http://localhost:8080/api/test-permissions/snapshot/read \
  -H "X-API-KEY: sk_test_abc123xyz789" \
  -H "client-id: tu-client-id-uuid"
```

## 📝 Formato de Permisos

```
# Sintaxis básica
resource:action,resource:action,...

# Ejemplos
snapshot:read                           # Solo leer snapshots
snapshot:read,snapshot:create           # Leer y crear snapshots
snapshot:*                              # Todas las acciones sobre snapshots
*:read                                  # Leer cualquier recurso
*:*                                     # Super admin (todo)
```

## 🌍 Company Scope

```sql
-- Acceso global (todas las compañías)
company_scope = NULL

-- Acceso restringido (solo compañía específica)
company_scope = 42
```

## 💻 Uso en Código

### En Controladores

```java
@PreAuthorize("@apiKeyPerm.can('snapshot', 'read')")
@GetMapping("/snapshots")
public List<Snapshot> getAll() {
    // Los datos ya están filtrados automáticamente por company_scope
    return snapshotService.findAll();
}
```

### En Servicios

```java
if (!apiKeyPermissionService.can("snapshot", "update")) {
    throw new AccessDeniedException("No tienes permiso");
}
```

## 🧪 Endpoints de Testing

- `GET /api/test-permissions/info` - Info de la API Key
- `GET /api/test-permissions/snapshot/read` - Probar lectura
- `POST /api/test-permissions/snapshot/create` - Probar creación
- `PUT /api/test-permissions/lead/update` - Probar actualización
- `DELETE /api/test-permissions/lead/delete` - Probar eliminación
- `GET /api/test-permissions/can-access-company/{id}` - Verificar acceso a compañía

## 📚 Documentación Completa

Ver `docs/API_KEY_PERMISSIONS.md` para documentación detallada.

## ⚡ Ejemplos de Configuraciones Comunes

### Solo Lectura (Global)
```sql
permissions = 'snapshot:read,lead:read,company:read'
company_scope = NULL
```

### Integración de Webhook (Restringida)
```sql
permissions = 'snapshot:create,lead:create'
company_scope = 10
```

### Full Access (Compañía Específica)
```sql
permissions = 'snapshot:*,lead:*,company:read'
company_scope = 42
```

### Super Admin
```sql
permissions = '*:*'
company_scope = NULL
```
