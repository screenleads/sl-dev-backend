# Sistema Híbrido de Autenticación - JWT y API Keys

## 🎯 Objetivo

Permitir que los endpoints funcionen **tanto con usuarios JWT (roles tradicionales) como con API Keys (permisos granulares)** de forma transparente.

## 🔐 Arquitectura Dual

```
┌─────────────────────────────────────────────────────────────┐
│                  SISTEMA DE AUTENTICACIÓN                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  👤 JWT (Usuario con Token)          🔑 API Key            │
│  ├─ Header: Authorization: Bearer    ├─ Header: X-API-KEY  │
│  ├─ Principal: User                  ├─ Header: client-id  │
│  ├─ Authorities: ROLE_ADMIN, etc.    ├─ Principal: ApiKeyPrincipal │
│  └─ Permisos: Basados en Role.level └─ Authorities: API_CLIENT │
│                                                             │
│  Ambos convergen en @perm.can()                            │
│                    ↓                                        │
│            PermissionServiceImpl                            │
│                    ↓                                        │
│     ┌──────────────┴──────────────┐                        │
│     ↓                              ↓                        │
│  Usuario JWT              →    API Key                      │
│  (AppEntity + Role)            (ApiKeyPermissionService)    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## ✅ Controladores Actualizados

### 1. **PromotionsController** - Gestión de Promociones

```java
// ✅ Solo permisos (funciona con JWT y API Keys)
@PreAuthorize("@perm.can('promotion', 'read')")
@GetMapping
public List<PromotionDTO> getAllPromotions() { ... }

@PreAuthorize("@perm.can('promotion', 'create')")
@PostMapping
public PromotionDTO createPromotion(@RequestBody PromotionDTO dto) { ... }

@PreAuthorize("@perm.can('promotion', 'update')")
@PutMapping("/{id}")
public PromotionDTO updatePromotion(...) { ... }

@PreAuthorize("@perm.can('promotion', 'delete')")
@DeleteMapping("/{id}")
public void deletePromotion(@PathVariable Long id) { ... }

// Leads también con permisos
@PreAuthorize("@perm.can('lead', 'create')")
@PostMapping("/{id}/leads")
public PromotionLeadDTO registerLead(...) { ... }

@PreAuthorize("@perm.can('lead', 'read')")
@GetMapping("/{id}/leads")
public List<PromotionLeadDTO> listLeads(...) { ... }
```

**Acceso:**
- ✅ Usuario JWT con role adecuado
- ✅ API Key con `promotion:read`, `promotion:create`, etc.

---

### 2. **AdvicesController** - Gestión de Avisos

```java
@PreAuthorize("@perm.can('advice', 'read')")
@GetMapping
public ResponseEntity<List<AdviceDTO>> getAllAdvices() { ... }

@PreAuthorize("@perm.can('advice', 'read')")
@GetMapping("/visibles")
public ResponseEntity<List<AdviceDTO>> getVisibleAdvicesNow(...) { ... }

@PreAuthorize("@perm.can('advice', 'create')")
@PostMapping
public ResponseEntity<AdviceDTO> createAdvice(...) { ... }

@PreAuthorize("@perm.can('advice', 'update')")
@PutMapping("/{id}")
public ResponseEntity<AdviceDTO> updateAdvice(...) { ... }

@PreAuthorize("@perm.can('advice', 'delete')")
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteAdvice(@PathVariable Long id) { ... }
```

**Acceso:**
- ✅ Usuario JWT con role adecuado
- ✅ API Key con `advice:read`, `advice:create`, etc.

---

### 3. **CompanyController** - Gestión de Compañías (HÍBRIDO)

```java
// ✅ HÍBRIDO: ROLE_ADMIN O permisos de API Key
@PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('company', 'read')")
@GetMapping
public ResponseEntity<List<CompanyDTO>> getAllCompanies() { ... }

@PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('company', 'create')")
@PostMapping
public ResponseEntity<CompanyDTO> createCompany(...) { ... }

@PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('company', 'update')")
@PutMapping("/{id}")
public ResponseEntity<CompanyDTO> updateCompany(...) { ... }

@PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('company', 'delete')")
@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteCompany(...) { ... }
```

**Acceso:**
- ✅ Usuario JWT con `ROLE_ADMIN`
- ✅ API Key con `company:read`, `company:create`, etc.

---

## 🔄 Cómo Funciona el Sistema Dual

### En PermissionServiceImpl

```java
@Override
public boolean can(String resource, String action) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    
    // 1️⃣ Si es API_CLIENT, delega en ApiKeyPermissionService
    if (auth.getAuthorities().stream().anyMatch(a -> "API_CLIENT".equals(a.getAuthority()))) {
        ApiKeyPermissionService apiKeyPerm = SpringContext.getBean(ApiKeyPermissionService.class);
        return apiKeyPerm.can(resource, action);
    }
    
    // 2️⃣ Si es Usuario JWT, verifica contra AppEntity + Role.level
    User user = userRepository.findByUsername(auth.getName()).orElse(null);
    AppEntity permission = permissionRepository.findByResource(resource).orElse(null);
    // ... lógica de niveles
    return myLevel <= required;
}
```

### Ventajas del Sistema Híbrido

✅ **Flexibilidad**: Un mismo endpoint funciona con ambos sistemas  
✅ **Seguridad**: Cada sistema valida de forma independiente  
✅ **Escalabilidad**: Fácil agregar más tipos de autenticación  
✅ **Transparencia**: El código del controlador es simple y legible  

---

## 📊 Patrones de Uso Recomendados

### Patrón 1: Solo Permisos (Recomendado para nuevos endpoints)

```java
@PreAuthorize("@perm.can('recurso', 'accion')")
```

**Pros:**
- ✅ Funciona con JWT y API Keys
- ✅ Código más limpio
- ✅ Más flexible

**Usar en:**
- Nuevos endpoints
- APIs públicas/externas
- Recursos de negocio (promotions, advices, leads)

---

### Patrón 2: Híbrido (Para endpoints administrativos)

```java
@PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('recurso', 'accion')")
```

**Pros:**
- ✅ Mantiene compatibilidad con sistema de roles existente
- ✅ Permite acceso gradual vía API Keys
- ✅ Transición suave

**Usar en:**
- Endpoints administrativos (companies, users, roles)
- Recursos sensibles
- Durante migración de sistema antiguo

---

### Patrón 3: Solo Roles (Deprecated)

```java
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
```

**Contras:**
- ❌ No funciona con API Keys
- ❌ Menos flexible

**Usar solo en:**
- Endpoints internos muy sensibles
- Autenticación de usuarios (login, registro)

---

## 🧪 Ejemplos de Uso

### Con Usuario JWT

```bash
# Login para obtener token
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'

# Response: {"accessToken": "eyJhbGc..."}

# Usar el token para acceder
curl -X GET http://localhost:8080/promotions \
  -H "Authorization: Bearer eyJhbGc..."
```

### Con API Key

```bash
# Usar API Key directamente
curl -X GET http://localhost:8080/promotions \
  -H "X-API-KEY: sk_test_abc123" \
  -H "client-id: 550e8400-e29b-41d4-a716-446655440000"

# Crear promotion
curl -X POST http://localhost:8080/promotions \
  -H "X-API-KEY: sk_test_abc123" \
  -H "client-id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "Content-Type: application/json" \
  -d '{"name": "Black Friday", ...}'
```

---

## 🔐 Configuración de Permisos

### Para Usuario JWT

1. **Crear Role** con level adecuado
2. **Configurar AppEntity** con niveles de acceso
3. **Asignar Role al User**

```sql
-- El usuario con role.level=1 puede acceder a recursos que requieren level >= 1
INSERT INTO role (role, description, level) VALUES ('ROLE_ADMIN', 'Admin', 1);
INSERT INTO app_entity (resource, read_level, create_level, update_level, delete_level)
VALUES ('promotion', 2, 2, 2, 1);  -- Solo ADMIN puede delete
```

### Para API Key

1. **Crear Client**
2. **Crear API Key** con permisos
3. **Configurar company_scope** (opcional)

```sql
INSERT INTO api_key (key, client, active, permissions, company_scope, description)
VALUES (
    'sk_promotion_manager',
    1,
    true,
    'promotion:read,promotion:create,promotion:update,lead:read,lead:create',
    42,  -- Solo compañía 42
    'Promotion management integration'
);
```

---

## 📈 Próximos Pasos

### Controladores Pendientes de Actualizar

Puedes aplicar el mismo patrón a:

- [ ] **CustomerController** → `customer:read`, `customer:create`, etc.
- [ ] **DevicesController** → `device:read`, `device:create`, etc.
- [ ] **MediaController** → `media:read`, `media:create`, etc.
- [ ] **CouponController** → `coupon:read`, `coupon:create`, etc.
- [ ] **ApiKeyController** → `apikey:read`, `apikey:create` (solo admin)
- [ ] **ClientController** → `client:read`, `client:create` (solo admin)
- [ ] **UserController** → Mantener solo ROLE_ADMIN

### Patrón Recomendado

```java
// Para recursos de negocio
@PreAuthorize("@perm.can('recurso', 'accion')")

// Para recursos administrativos
@PreAuthorize("hasAuthority('ROLE_ADMIN') or @perm.can('recurso', 'accion')")

// Para recursos muy sensibles
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
```

---

## 🎯 Resumen

**Sistema Implementado:**
- ✅ Autenticación dual JWT + API Keys
- ✅ Permisos granulares por recurso y acción  
- ✅ Company scope para filtrado automático de datos
- ✅ 3 controladores actualizados (Promotions, Advices, Company)
- ✅ Sistema híbrido totalmente funcional

**Ventajas:**
- 🚀 Mayor flexibilidad de integración
- 🔐 Seguridad granular
- 🌍 Soporte multi-tenant (company scope)
- 📊 Auditoría detallada de accesos
- ⚡ Escalable y mantenible

**Listo para:**
- Crear API Keys de testing
- Integrar con sistemas externos
- Migrar endpoints restantes
