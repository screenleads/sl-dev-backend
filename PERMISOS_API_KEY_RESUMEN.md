# 🔐 Resumen de Permisos API Key - Nuevos Controladores

**Fecha**: 13 de enero de 2026  
**Contexto**: Rediseño del modelo de dominio - Nuevos controladores de canjes y facturación

---

## ✅ CORRECCIONES REALIZADAS

### 1. Corrección de Referencias al Bean de Permisos

**Problema identificado**: Los controladores usaban referencias incorrectas al bean de permisos:
- ❌ `@perm.can()` - No existe
- ❌ `@perm.canCompany()` - No existe  
- ❌ `@perm.canCompanyBilling()` - No existe

**Solución aplicada**: Todos los `@PreAuthorize` ahora usan:
- ✅ `@apiKeyPerm.can('resource', 'action')` - Bean correcto definido en `ApiKeyPermissionService`

**Archivos corregidos**:
- [PromotionRedemptionController.java](src/main/java/com/screenleads/backend/app/web/controller/PromotionRedemptionController.java) - 9 endpoints corregidos
- [CompanyBillingController.java](src/main/java/com/screenleads/backend/app/web/controller/CompanyBillingController.java) - 4 endpoints corregidos

---

## 📋 PERMISOS POR CONTROLADOR

### PromotionRedemptionController

**Ruta base**: `/promotion-redemptions`

| Método | Endpoint | Autenticación | Permisos API Key |
|--------|----------|---------------|------------------|
| GET | `/` | ROLE_ADMIN o API Key | `redemption:read` |
| GET | `/{id}` | ROLE_ADMIN o API Key | `redemption:read` |
| GET | `/coupon/{code}` | ROLE_ADMIN o API Key | `redemption:read` |
| GET | `/customer/{customerId}` | ROLE_ADMIN o API Key | `redemption:read` |
| GET | `/promotion/{promotionId}` | ROLE_ADMIN o API Key | `redemption:read` |
| POST | `/` | ROLE_ADMIN o API Key | `redemption:write` |
| PUT | `/{id}` | ROLE_ADMIN o API Key | `redemption:write` |
| PUT | `/{id}/verify` | ROLE_ADMIN o API Key | `redemption:write` |
| PUT | `/{id}/redeem` | ROLE_ADMIN o API Key | `redemption:write` |
| DELETE | `/{id}` | ROLE_ADMIN o API Key | `redemption:delete` |

**Colección Postman**: `ScreenLeads-PromotionRedemptions.postman_collection.json`
- ✅ 9 endpoints con JWT
- ✅ 9 endpoints con API Key (10 total con variantes)
- ✅ Ejemplos de configuración SQL
- ✅ Descripciones detalladas

### CompanyBillingController

**Ruta base**: `/company-billing`

| Método | Endpoint | Autenticación | Permisos API Key | Notas |
|--------|----------|---------------|------------------|-------|
| GET | `/` | **ROLE_ADMIN ONLY** | ❌ No disponible | Admin-only |
| GET | `/{id}` | ROLE_ADMIN o API Key | `billing:read` | ✅ |
| GET | `/company/{companyId}` | ROLE_ADMIN o API Key | `billing:read` | ✅ |
| POST | `/` | **ROLE_ADMIN ONLY** | ❌ No disponible | Admin-only |
| PUT | `/{id}` | **ROLE_ADMIN ONLY** | ❌ No disponible | Admin-only |
| DELETE | `/{id}` | **ROLE_ADMIN ONLY** | ❌ No disponible | Admin-only |
| POST | `/{id}/reset-period` | **ROLE_ADMIN ONLY** | ❌ No disponible | Admin-only |
| GET | `/{id}/device-limit-reached` | ROLE_ADMIN o API Key | `billing:read` | ✅ |
| GET | `/{id}/promotion-limit-reached` | ROLE_ADMIN o API Key | `billing:read` | ✅ |

**⚠️ IMPORTANTE**: La mayoría de endpoints son admin-only por seguridad. Solo consultas y verificación de límites están disponibles para API Keys.

**Colección Postman**: `ScreenLeads-CompanyBilling.postman_collection.json`
- ✅ 9 endpoints con JWT
- ✅ 5 endpoints con API Key (solo los permitidos)
- ✅ Advertencias de seguridad documentadas
- ✅ Ejemplos de casos de uso

---

## 🗂️ MATRIZ COMPLETA DE RECURSOS Y PERMISOS

### Nuevos Recursos (Rediseño 2026)

| Recurso | Descripción | Permisos Disponibles |
|---------|-------------|---------------------|
| `redemption` | Canjes de promociones | `read`, `write`, `delete` |
| `billing` | Config. de facturación | `read` (write/delete admin-only) |
| `invoice` | Facturas mensuales | `read`, `write`, `delete` |
| `customer` | Consumidores finales | `read`, `write`, `delete` |
| `useraction` | Historial de acciones | `read`, `write` |
| `billingevent` | Eventos de auditoría | `read`, `write` |
| `dataexport` | Exportaciones GDPR | `read`, `write`, `delete` |

### Recursos Originales (Mantienen compatibilidad)

| Recurso | Descripción | Permisos Disponibles |
|---------|-------------|---------------------|
| `snapshot` | Capturas de pantalla | `read`, `create`, `update`, `delete` |
| `lead` | Leads/contactos | `read`, `create`, `update`, `delete` |
| `company` | Empresas | `read`, `create`, `update`, `delete` |
| `device` | Dispositivos | `read`, `create`, `update`, `delete` |
| `advice` | Avisos | `read`, `write`, `delete` |
| `promotion` | Promociones | `read`, `write`, `delete` |
| `user` | Usuarios | `read`, `write`, `delete` |
| `client` | API Clients | `read`, `write`, `delete` |
| `apikey` | API Keys | `read`, `write`, `delete` |

---

## 💡 EJEMPLOS DE CONFIGURACIÓN

### 1. Integración de Punto de Venta (POS)
```sql
INSERT INTO api_key (key, client, active, permissions, company_scope, description)
VALUES (
    'sk_pos_integration_abc123',
    1,
    true,
    'redemption:read,redemption:write',
    42,  -- Solo compañía 42
    'POS Integration - Redemption Management'
);
```

**Uso**: Validar y crear canjes en puntos de venta físicos.

### 2. Dashboard de Facturación (Solo Lectura)
```sql
INSERT INTO api_key (key, client, active, permissions, company_scope, description)
VALUES (
    'sk_billing_dashboard_xyz789',
    2,
    true,
    'billing:read,invoice:read',
    NULL,  -- Acceso global (para admin dashboards)
    'Billing Dashboard - Global Read Access'
);
```

**Uso**: Mostrar métricas de facturación en dashboards externos.

### 3. Integración de Remarketing
```sql
INSERT INTO api_key (key, client, active, permissions, company_scope, description)
VALUES (
    'sk_remarketing_def456',
    3,
    true,
    'customer:read,redemption:read,dataexport:write',
    15,  -- Solo compañía 15
    'Remarketing System - Customer Data Access'
);
```

**Uso**: Exportar datos de clientes para campañas de remarketing.

### 4. Webhook de Stripe
```sql
INSERT INTO api_key (key, client, active, permissions, company_scope, description)
VALUES (
    'sk_stripe_webhook_ghi789',
    4,
    true,
    'billingevent:write,invoice:read,invoice:write',
    NULL,  -- Global (para todos los eventos de Stripe)
    'Stripe Webhook Handler'
);
```

**Uso**: Recibir eventos de Stripe y actualizar facturas.

### 5. App Móvil de Cliente
```sql
INSERT INTO api_key (key, client, active, permissions, company_scope, description)
VALUES (
    'sk_mobile_customer_jkl012',
    5,
    true,
    'redemption:read,customer:read,useraction:write,dataexport:write',
    10,  -- Solo compañía 10
    'Mobile Customer App'
);
```

**Uso**: Clientes consultan sus canjes, solicitan exportaciones GDPR.

### 6. Sistema de Validación de Cupones
```sql
INSERT INTO api_key (key, client, active, permissions, company_scope, description)
VALUES (
    'sk_coupon_validator_mno345',
    6,
    true,
    'redemption:read',
    20,  -- Solo compañía 20
    'External Coupon Validation System'
);
```

**Uso**: Sistema externo valida cupones antes de aplicar descuentos.

---

## 🔒 CONSIDERACIONES DE SEGURIDAD

### Company Scope

**Acceso Global** (`company_scope = NULL`):
- ✅ Accede a datos de todas las compañías
- ⚠️ Solo para APIs de confianza (admin dashboards, webhooks globales)
- ⚠️ Se aplica validación extra en código de negocio

**Acceso Restringido** (`company_scope = <ID>`):
- ✅ Solo accede a datos de una compañía específica
- ✅ Filtro automático de Hibernate
- ✅ Más seguro para integraciones de clientes
- ✅ Recomendado por defecto

### Endpoints Admin-Only

Los siguientes endpoints de `CompanyBillingController` son **exclusivamente ROLE_ADMIN**:
- `POST /company-billing` - Crear configuración
- `PUT /company-billing/{id}` - Actualizar configuración
- `DELETE /company-billing/{id}` - Eliminar configuración
- `POST /company-billing/{id}/reset-period` - Resetear periodo
- `GET /company-billing` - Listar todas (global)

**Razón**: Modificar configuración de facturación tiene implicaciones financieras críticas.

### Permisos Recomendados por Rol

| Rol/Caso de Uso | Permisos Sugeridos |
|------------------|-------------------|
| **Lectura General** | `*:read` |
| **Integración POS** | `redemption:*` |
| **Dashboard Billing** | `billing:read,invoice:read,billingevent:read` |
| **Remarketing** | `customer:read,redemption:read,dataexport:*` |
| **Webhook Stripe** | `billingevent:write,invoice:*` |
| **App Cliente** | `redemption:read,customer:read,useraction:write,dataexport:write` |
| **Super Admin** | `*:*` (con company_scope = NULL) |

---

## 📚 DOCUMENTACIÓN ACTUALIZADA

### Archivos Modificados

1. **API_KEY_PERMISSIONS.md** ✅
   - Añadida sección "Nuevos Recursos (Rediseño 2026)"
   - 7 nuevos recursos documentados
   - 5 ejemplos de configuración por caso de uso
   - Actualizada matriz de permisos

2. **PromotionRedemptionController.java** ✅
   - Corregidos 9 `@PreAuthorize` de `@perm.can()` a `@apiKeyPerm.can()`
   - Sin errores de compilación

3. **CompanyBillingController.java** ✅
   - Corregidos 4 `@PreAuthorize` de `@perm.canCompany()` y `@perm.canCompanyBilling()` a `@apiKeyPerm.can()`
   - Sin errores de compilación

4. **ScreenLeads-PromotionRedemptions.postman_collection.json** ✅ NUEVO
   - 18 requests totales (9 JWT + 9 API Key)
   - Ejemplos completos de body
   - Descripciones detalladas
   - Ejemplos SQL de configuración

5. **ScreenLeads-CompanyBilling.postman_collection.json** ✅ NUEVO
   - 14 requests totales (9 JWT + 5 API Key)
   - Advertencias de seguridad admin-only
   - Casos de uso documentados
   - Restricciones explicadas

### Ubicación de Archivos

```
sl-dev-backend/
├── docs/
│   └── API_KEY_PERMISSIONS.md ✅ ACTUALIZADO
├── postman/
│   ├── ScreenLeads-PromotionRedemptions.postman_collection.json ✅ NUEVO
│   └── ScreenLeads-CompanyBilling.postman_collection.json ✅ NUEVO
└── src/main/java/com/screenleads/backend/app/web/controller/
    ├── PromotionRedemptionController.java ✅ CORREGIDO
    └── CompanyBillingController.java ✅ CORREGIDO
```

---

## 🧪 TESTING

### Con Postman

1. **Importar colecciones**:
   - `ScreenLeads-PromotionRedemptions.postman_collection.json`
   - `ScreenLeads-CompanyBilling.postman_collection.json`

2. **Configurar variables de entorno**:
   ```json
   {
     "base_url": "http://localhost:8080",
     "jwt_token": "eyJhbGci...",
     "api_key": "sk_redemption_readonly_abc123",
     "client_id": "screen_client_001"
   }
   ```

3. **Probar autenticación JWT**: Folder "JWT Authentication"
4. **Probar autenticación API Key**: Folder "API Key Authentication"

### Con cURL

#### Endpoint con JWT
```bash
curl -X GET http://localhost:8080/promotion-redemptions \
  -H "Authorization: Bearer eyJhbGci..."
```

#### Endpoint con API Key
```bash
curl -X GET http://localhost:8080/promotion-redemptions \
  -H "X-API-KEY: sk_redemption_readonly_abc123" \
  -H "X-CLIENT-ID: screen_client_001"
```

#### Crear Canje con API Key
```bash
curl -X POST http://localhost:8080/promotion-redemptions \
  -H "X-API-KEY: sk_redemption_write_xyz456" \
  -H "X-CLIENT-ID: screen_client_001" \
  -H "Content-Type: application/json" \
  -d '{
    "promotionId": 1,
    "customerId": 5,
    "deviceId": 3,
    "couponCode": "SCREEN2024-TEST123",
    "couponStatus": "VALID",
    "redemptionMethod": "API"
  }'
```

---

## ✅ CHECKLIST DE VALIDACIÓN

### Seguridad
- [x] Todas las referencias `@perm.can()` corregidas a `@apiKeyPerm.can()`
- [x] Endpoints admin-only claramente identificados
- [x] Company scope documentado en colecciones
- [x] Ejemplos de configuración incluyen company_scope

### Documentación
- [x] API_KEY_PERMISSIONS.md actualizado con nuevos recursos
- [x] Colecciones Postman creadas para ambos controladores
- [x] Cada endpoint tiene descripción y ejemplo
- [x] Permisos requeridos documentados por endpoint
- [x] Ejemplos SQL de configuración incluidos

### Funcionalidad
- [x] Sin errores de compilación en controladores
- [x] Permisos consistentes entre JWT y API Key
- [x] Colecciones incluyen variables reutilizables
- [x] Casos de uso reales documentados

### Testing
- [x] 18 requests de Promotion Redemptions (9 JWT + 9 API Key)
- [x] 14 requests de Company Billing (9 JWT + 5 API Key)
- [x] Ejemplos de body incluidos en POSTs/PUTs
- [x] Descripciones indican permisos requeridos

---

## 🎯 PRÓXIMOS PASOS

### Controladores Pendientes (de IMPLEMENTACION_SERVICIOS_GUIA.md)

Se recomienda crear colecciones similares para:

1. **CustomerController** (9 endpoints)
   - Permisos: `customer:read`, `customer:write`, `customer:delete`
   
2. **InvoiceController** (9 endpoints)
   - Permisos: `invoice:read`, `invoice:write`, `invoice:delete`
   - Varios endpoints admin-only (finalize, mark as paid)
   
3. **UserActionController** (4 endpoints)
   - Permisos: `useraction:read`, `useraction:write`
   
4. **BillingEventController** (3 endpoints)
   - Permisos: `billingevent:read`, `billingevent:write`
   
5. **DataExportController** (6 endpoints)
   - Permisos: `dataexport:read`, `dataexport:write`, `dataexport:delete`

### Validación en Producción

1. Verificar que los filtros de Hibernate funcionan correctamente con company_scope
2. Monitorear uso de API Keys con logging
3. Revisar regularmente permisos de API Keys activas
4. Configurar expiración para API Keys no permanentes

---

## 📞 SOPORTE

Para preguntas sobre:
- **Permisos**: Ver [API_KEY_PERMISSIONS.md](docs/API_KEY_PERMISSIONS.md)
- **Uso de colecciones**: Importar en Postman y revisar descripciones
- **Configuración**: Ver ejemplos SQL en este documento

**Fecha del resumen**: 13 de enero de 2026
