# 🔍 Sistema de Permisos Estandarizado

## ✅ COMPLETADO - Estandarización a 3 Acciones

**Fecha**: 10 de enero de 2026

### 📊 Estándar Implementado

El backend ahora usa **únicamente 3 acciones** para todos los recursos:

```
✅ read   - Para consultar/listar (GET)
✅ write  - Para crear Y actualizar (POST, PUT, PATCH)
✅ delete - Para eliminar (DELETE)
```

## 🎯 Recursos Estandarizados

Todos los controladores ahora usan el mismo estándar:

### Recursos de Negocio
- `advice:read`, `advice:write`, `advice:delete`
- `promotion:read`, `promotion:write`, `promotion:delete`
- `lead:read`, `lead:write`, `lead:delete`
- `customer:read`, `customer:write`, `customer:delete`

### Recursos de Infraestructura
- `device:read`, `device:write`, `device:delete`
- `media:read`, `media:write`, `media:delete`
- `media-type:read`, `media-type:write`, `media-type:delete`

### Recursos de Sistema
- `company:read`, `company:write`, `company:delete`
- `user:read`, `user:write`, `user:delete`
- `appentity:read`, `appentity:write`, `appentity:delete`

## 📝 Formato de Permisos para API Keys

```
# Ejemplo básico
advice:read,advice:write,advice:delete

# Múltiples recursos
advice:read,advice:write,
promotion:read,promotion:write,
media:read,media:write,media:delete

# Con wildcards
advice:*           # Todas las acciones sobre advice
*:read             # Solo lectura en todos los recursos
*:*                # Super admin (todo)
```

## 🔧 Controladores Actualizados

Se han estandarizado **10 controladores**:

1. ✅ **AdvicesController** - `advice:write` para POST y PUT
2. ✅ **PromotionsController** - `promotion:write` y `lead:write`
3. ✅ **DevicesController** - `device:write` para POST y PUT
4. ✅ **DeviceTypesController** - `device:write` para POST y PUT
5. ✅ **CustomerController** - `customer:write` para POST y PUT
6. ✅ **CouponController** - `promotion:write` para POST
7. ✅ **CompanyController** - `company:write` para POST y PUT
8. ✅ **RoleController** - `user:write` para POST y PUT
9. ✅ **AppEntityController** - `appentity:write` para PUT
10. ✅ **MediaController** - `media:write` (ya estaba correcto)
11. ✅ **MediaTypesController** - `media-type:write` (ya estaba correcto)
12. ✅ **ApiKeyPermissionTestController** - Actualizado para testing

## 📖 Ejemplos de Uso

### API Key de Solo Lectura
```
company:read,
device:read,
media:read,
advice:read,
promotion:read,
customer:read
```

### API Key de Gestión Completa
```
advice:read,advice:write,advice:delete,
promotion:read,promotion:write,promotion:delete,
media:read,media:write,media:delete,
customer:read,customer:write,customer:delete
```

### API Key Simplificada con Wildcards
```
advice:*,
promotion:*,
media:*,
customer:read
```

## ⚠️ Cambios Importantes

### Antes (Sistema Antiguo)
- `create` - Para crear (POST)
- `update` - Para actualizar (PUT)
- `read` - Para consultar (GET)
- `delete` - Para eliminar (DELETE)

### Ahora (Sistema Nuevo - Simplificado)
- `write` - Para crear Y actualizar (POST, PUT, PATCH)
- `read` - Para consultar (GET)
- `delete` - Para eliminar (DELETE)

## 🔑 Actualización de API Keys Existentes

Las API Keys que usaban permisos antiguos deben actualizarse:

```
# Antes
advice:create,advice:update,advice:delete

# Ahora
advice:write,advice:delete
```

```
# Antes
device:create,device:update,device:read

# Ahora  
device:write,device:read
```

## 📚 Documentación Actualizada

Revisar y actualizar:
- ✅ Código fuente de controladores
- ⚠️ `docs/API_KEY_PERMISSIONS.md` - Necesita actualización
- ⚠️ `docs/API_KEY_RESOURCES.md` - Necesita actualización
- ⚠️ Colecciones de Postman
- ⚠️ Tests de integración

---

**Resumen**: Sistema completamente estandarizado a 3 acciones (read, write, delete) en todos los controladores.
