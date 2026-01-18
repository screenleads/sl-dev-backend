# 🧪 Validación Completa de Endpoints - Backend v115

**Fecha:** 18 de Enero de 2026  
**Version:** v115 (37 repositorios, 306 source files)  
**URL Base:** https://sl-dev-backend-7ab91220ba93.herokuapp.com

---

## 🎯 Objetivo

Validar que **TODOS** los endpoints (viejos + nuevos) funcionan correctamente con:
- ✅ **JWT Token** (autenticación de usuario con roles)
- ✅ **API Key** (autenticación de sistema con permisos)
- ❌ **NO ambos simultáneamente** (se usa JWT O API Key, el que llegue primero)

---

## 📋 Checklist de Controladores (29 controladores)

### ✅ Módulos Base (Ya existentes)

#### 1. AuthController
- `POST /auth/login` - ✅ Público
- `POST /auth/refresh` - ✅ Público
- `GET /auth/me` - 🔐 JWT/API Key
- `POST /auth/forgot-password` - ✅ Público
- `POST /auth/reset-password` - ✅ Público

#### 2. UserController
- `GET /users` - 🔐 JWT/API Key + `@PreAuthorize("hasRole('ADMIN')")`
- `POST /users` - 🔐 JWT/API Key + Admin
- `GET /users/{id}` - 🔐 JWT/API Key
- `PUT /users/{id}` - 🔐 JWT/API Key
- `DELETE /users/{id}` - 🔐 JWT/API Key + Admin

#### 3. RoleController
- `GET /roles` - 🔐 JWT/API Key
- `POST /roles` - 🔐 JWT/API Key + Admin
- `GET /roles/assignable` - 🔐 JWT/API Key

#### 4. CompanyController
- `GET /companies` - 🔐 JWT/API Key + Admin
- `POST /companies` - 🔐 JWT/API Key + Admin
- `GET /companies/{id}` - 🔐 JWT/API Key
- `PUT /companies/{id}` - 🔐 JWT/API Key
- `DELETE /companies/{id}` - 🔐 JWT/API Key + Admin

#### 5. CompanyTokenController (API Keys)
- `GET /company-tokens` - 🔐 JWT only (no API Key para gestionar API Keys)
- `POST /company-tokens` - 🔐 JWT only
- `PUT /company-tokens/{token}/renew` - 🔐 JWT only
- `DELETE /company-tokens/{token}` - 🔐 JWT only

#### 6. DevicesController
- `POST /devices` - ✅ Público (auto-registro)
- `GET /devices` - 🔐 JWT/API Key
- `GET /devices/{id}` - 🔐 JWT/API Key
- `PUT /devices/{id}` - 🔐 JWT/API Key
- `DELETE /devices/{id}` - 🔐 JWT/API Key

#### 7. PromotionsController
- `GET /promotions` - 🔐 JWT/API Key
- `POST /promotions` - 🔐 JWT/API Key
- `GET /promotions/{id}` - 🔐 JWT/API Key
- `PUT /promotions/{id}` - 🔐 JWT/API Key
- `DELETE /promotions/{id}` - 🔐 JWT/API Key

#### 8. PromotionRedemptionController (Coupons)
- `GET /promotion-redemptions` - 🔐 JWT/API Key
- `POST /promotion-redemptions` - 🔐 JWT/API Key
- `GET /promotion-redemptions/{id}` - 🔐 JWT/API Key
- `PUT /promotion-redemptions/{id}/verify` - 🔐 JWT/API Key

#### 9. CustomerController
- `GET /customers` - 🔐 JWT/API Key
- `POST /customers` - 🔐 JWT/API Key
- `GET /customers/{id}` - 🔐 JWT/API Key
- `PUT /customers/{id}` - 🔐 JWT/API Key
- `GET /customers/{id}/stats` - 🔐 JWT/API Key
- `GET /customers/email/{email}` - 🔐 JWT/API Key
- `POST /customers/search` - 🔐 JWT/API Key

#### 10. MediaController
- `POST /media/upload` - 🔐 JWT/API Key
- `GET /media` - 🔐 JWT/API Key
- `GET /media/{id}` - 🔐 JWT/API Key
- `DELETE /media/{id}` - 🔐 JWT/API Key

---

### 🆕 Módulos Nuevos (Week 2-4)

#### 11. AdviceImpressionController (v107)
- `POST /api/analytics/impressions` - 🔐 JWT/API Key
- `GET /api/analytics/impressions/device/{deviceId}` - 🔐 JWT/API Key
- `GET /api/analytics/impressions/advice/{adviceId}` - 🔐 JWT/API Key

#### 12. AdviceInteractionController (v108)
- `POST /api/analytics/interactions` - 🔐 JWT/API Key
- `GET /api/analytics/interactions/impression/{impressionId}` - 🔐 JWT/API Key
- `GET /api/analytics/interactions/customer/{customerId}` - 🔐 JWT/API Key

#### 13. AnalyticsDashboardController (v109)
- `GET /api/analytics/dashboard` - 🔐 JWT/API Key
- `GET /api/analytics/promotions/{id}/metrics` - 🔐 JWT/API Key
- `GET /api/analytics/promotions/top-performers` - 🔐 JWT/API Key

#### 14. AudienceSegmentController (v111)
- `GET /api/audience-segments` - 🔐 JWT/API Key
- `POST /api/audience-segments` - 🔐 JWT/API Key
- `GET /api/audience-segments/{id}` - 🔐 JWT/API Key
- `PUT /api/audience-segments/{id}` - 🔐 JWT/API Key
- `DELETE /api/audience-segments/{id}` - 🔐 JWT/API Key
- `GET /api/audience-segments/{id}/customers` - 🔐 JWT/API Key
- `GET /api/audience-segments/{id}/customers/count` - 🔐 JWT/API Key

#### 15. NotificationTemplateController (v111)
- `GET /api/notification-templates` - 🔐 JWT/API Key
- `POST /api/notification-templates` - 🔐 JWT/API Key
- `GET /api/notification-templates/{id}` - 🔐 JWT/API Key
- `PUT /api/notification-templates/{id}` - 🔐 JWT/API Key
- `DELETE /api/notification-templates/{id}` - 🔐 JWT/API Key

#### 16. MarketingCampaignController (v112)
- `GET /api/marketing-campaigns` - 🔐 JWT/API Key
- `POST /api/marketing-campaigns` - 🔐 JWT/API Key
- `GET /api/marketing-campaigns/{id}` - 🔐 JWT/API Key
- `PUT /api/marketing-campaigns/{id}` - 🔐 JWT/API Key
- `DELETE /api/marketing-campaigns/{id}` - 🔐 JWT/API Key
- `POST /api/marketing-campaigns/{id}/execute` - 🔐 JWT/API Key
- `POST /api/marketing-campaigns/{id}/pause` - 🔐 JWT/API Key
- `POST /api/marketing-campaigns/{id}/resume` - 🔐 JWT/API Key
- `POST /api/marketing-campaigns/{id}/cancel` - 🔐 JWT/API Key
- `GET /api/marketing-campaigns/{id}/stats` - 🔐 JWT/API Key

#### 17. GeofenceController (v114)
- `POST /api/geofence/zones` - 🔐 JWT/API Key
- `PUT /api/geofence/zones/{zoneId}` - 🔐 JWT/API Key
- `DELETE /api/geofence/zones/{zoneId}` - 🔐 JWT/API Key
- `GET /api/geofence/zones/{zoneId}` - 🔐 JWT/API Key
- `GET /api/geofence/zones/company/{companyId}` - 🔐 JWT/API Key
- `POST /api/geofence/rules` - 🔐 JWT/API Key
- `PUT /api/geofence/rules/{ruleId}` - 🔐 JWT/API Key
- `DELETE /api/geofence/rules/{ruleId}` - 🔐 JWT/API Key
- `GET /api/geofence/rules/promotion/{promotionId}` - 🔐 JWT/API Key
- `GET /api/geofence/rules/zone/{zoneId}` - 🔐 JWT/API Key
- `GET /api/geofence/rules/company/{companyId}` - 🔐 JWT/API Key
- `POST /api/geofence/check` - 🔐 JWT/API Key (usado por dispositivos)
- `GET /api/geofence/zones/containing` - 🔐 JWT/API Key
- `GET /api/geofence/check/inside` - 🔐 JWT/API Key
- `POST /api/geofence/events` - 🔐 JWT/API Key
- `GET /api/geofence/events/device/{deviceId}` - 🔐 JWT/API Key
- `GET /api/geofence/events/zone/{zoneId}` - 🔐 JWT/API Key
- `GET /api/geofence/stats/zone/{zoneId}` - 🔐 JWT/API Key
- `GET /api/geofence/stats/company/{companyId}` - 🔐 JWT/API Key

#### 18. FraudDetectionController (v115)
- `POST /api/fraud/rules` - 🔐 JWT/API Key
- `PUT /api/fraud/rules/{ruleId}` - 🔐 JWT/API Key
- `DELETE /api/fraud/rules/{ruleId}` - 🔐 JWT/API Key
- `GET /api/fraud/rules/{ruleId}` - 🔐 JWT/API Key
- `GET /api/fraud/rules/company/{companyId}` - 🔐 JWT/API Key
- `POST /api/fraud/alerts` - 🔐 JWT/API Key
- `PUT /api/fraud/alerts/{alertId}/status` - 🔐 JWT/API Key
- `GET /api/fraud/alerts/{alertId}` - 🔐 JWT/API Key
- `GET /api/fraud/alerts/company/{companyId}` - 🔐 JWT/API Key
- `GET /api/fraud/alerts/company/{companyId}/pending` - 🔐 JWT/API Key
- `GET /api/fraud/alerts/company/{companyId}/stats` - 🔐 JWT/API Key
- `POST /api/fraud/blacklist` - 🔐 JWT/API Key
- `DELETE /api/fraud/blacklist/{blacklistId}` - 🔐 JWT/API Key
- `GET /api/fraud/blacklist/check` - 🔐 JWT/API Key
- `GET /api/fraud/blacklist/company/{companyId}` - 🔐 JWT/API Key
- `POST /api/fraud/check` - 🔐 JWT/API Key (detección en tiempo real)

---

## 🧪 Scripts de Prueba

### Setup Variables
```powershell
$baseUrl = "https://sl-dev-backend-7ab91220ba93.herokuapp.com"

# Para JWT
$jwtToken = "eyJhbGciOiJIUzI1NiJ9..." # Obtener con POST /auth/login

# Para API Key
$apiKey = "sk_test_xxxxx"
$clientId = "client_xxxxx"
```

### Test 1: Obtener JWT Token
```powershell
$loginBody = @{
    email = "admin@screenleads.com"
    password = "admin123"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "$baseUrl/auth/login" `
    -Method POST `
    -ContentType "application/json" `
    -Body $loginBody

$jwtToken = $response.accessToken
Write-Host "JWT Token: $jwtToken"
```

### Test 2: Health Check (Público)
```powershell
$health = Invoke-RestMethod -Uri "$baseUrl/actuator/health"
Write-Host "Health Status: $($health.status)"
# Expected: UP
```

### Test 3: GET /auth/me con JWT
```powershell
$headers = @{
    "Authorization" = "Bearer $jwtToken"
}

$me = Invoke-RestMethod -Uri "$baseUrl/auth/me" `
    -Method GET `
    -Headers $headers

Write-Host "Logged user: $($me.email)"
```

### Test 4: GET /auth/me con API Key
```powershell
$headers = @{
    "X-API-KEY" = $apiKey
    "client-id" = $clientId
}

$me = Invoke-RestMethod -Uri "$baseUrl/auth/me" `
    -Method GET `
    -Headers $headers

# Should fail if API Key doesn't have permission for 'user:read'
```

### Test 5: GET /customers con JWT
```powershell
$headers = @{
    "Authorization" = "Bearer $jwtToken"
}

$customers = Invoke-RestMethod -Uri "$baseUrl/customers" `
    -Method GET `
    -Headers $headers

Write-Host "Total customers: $($customers.totalElements)"
```

### Test 6: GET /customers con API Key
```powershell
$headers = @{
    "X-API-KEY" = $apiKey
    "client-id" = $clientId
}

$customers = Invoke-RestMethod -Uri "$baseUrl/customers" `
    -Method GET `
    -Headers $headers

Write-Host "Total customers: $($customers.totalElements)"
# Debe funcionar si API Key tiene 'customer:read'
```

### Test 7: Nuevos endpoints Analytics
```powershell
# Con JWT
$headers = @{
    "Authorization" = "Bearer $jwtToken"
}

# Dashboard
$dashboard = Invoke-RestMethod -Uri "$baseUrl/api/analytics/dashboard?companyId=1" `
    -Method GET `
    -Headers $headers

Write-Host "Analytics Dashboard: $($dashboard | ConvertTo-Json -Depth 3)"

# Top Performers
$topPromotions = Invoke-RestMethod -Uri "$baseUrl/api/analytics/promotions/top-performers?companyId=1&limit=10" `
    -Method GET `
    -Headers $headers

Write-Host "Top 10 Promotions: $($topPromotions.Count)"
```

### Test 8: Geofencing
```powershell
$headers = @{
    "Authorization" = "Bearer $jwtToken"
}

# Listar zonas
$zones = Invoke-RestMethod -Uri "$baseUrl/api/geofence/zones/company/1" `
    -Method GET `
    -Headers $headers

Write-Host "Total zones: $($zones.Count)"

# Crear zona
$zoneBody = @{
    company = @{ id = 1 }
    name = "Zona Test"
    description = "Zona de prueba"
    type = "CIRCLE"
    geometry = @{
        center = @{
            lat = 40.416775
            lon = -3.703790
        }
        radius = 1000
    }
    isActive = $true
    color = "#FF5733"
} | ConvertTo-Json -Depth 5

$newZone = Invoke-RestMethod -Uri "$baseUrl/api/geofence/zones" `
    -Method POST `
    -Headers $headers `
    -ContentType "application/json" `
    -Body $zoneBody

Write-Host "Created zone: $($newZone.id)"
```

### Test 9: Fraud Detection
```powershell
$headers = @{
    "Authorization" = "Bearer $jwtToken"
}

# Alertas pendientes
$alerts = Invoke-RestMethod -Uri "$baseUrl/api/fraud/alerts/company/1/pending" `
    -Method GET `
    -Headers $headers

Write-Host "Pending alerts: $($alerts.Count)"

# Estadísticas
$stats = Invoke-RestMethod -Uri "$baseUrl/api/fraud/alerts/company/1/stats" `
    -Method GET `
    -Headers $headers

Write-Host "Fraud Stats: $($stats | ConvertTo-Json)"
```

### Test 10: Marketing Campaigns
```powershell
$headers = @{
    "Authorization" = "Bearer $jwtToken"
}

# Listar campañas
$campaigns = Invoke-RestMethod -Uri "$baseUrl/api/marketing-campaigns?companyId=1" `
    -Method GET `
    -Headers $headers

Write-Host "Total campaigns: $($campaigns.totalElements)"

# Stats de una campaña
if ($campaigns.content.Count -gt 0) {
    $campaignId = $campaigns.content[0].id
    $campaignStats = Invoke-RestMethod -Uri "$baseUrl/api/marketing-campaigns/$campaignId/stats" `
        -Method GET `
        -Headers $headers
    
    Write-Host "Campaign stats: $($campaignStats | ConvertTo-Json)"
}
```

---

## ✅ Checklist de Validación

### Seguridad
- [ ] JWT funciona en todos los endpoints protegidos
- [ ] API Key funciona en endpoints permitidos
- [ ] No se puede usar JWT + API Key simultáneamente
- [ ] Endpoints públicos no requieren autenticación
- [ ] Roles se validan correctamente (ADMIN, USER, etc.)

### Endpoints Base
- [ ] Auth (login, me, refresh) - OK
- [ ] Users CRUD - OK
- [ ] Roles - OK
- [ ] Companies - OK
- [ ] Devices - OK
- [ ] Promotions - OK
- [ ] Customers - OK

### Endpoints Week 2 (Analytics)
- [ ] AdviceImpression - OK
- [ ] AdviceInteraction - OK
- [ ] Analytics Dashboard - OK
- [ ] Promotion Metrics - OK

### Endpoints Week 3 (Remarketing)
- [ ] Audience Segments - OK
- [ ] Notification Templates - OK
- [ ] Marketing Campaigns - OK
- [ ] Campaign execution - OK

### Endpoints Week 4 (Geofencing + Fraud)
- [ ] Geofence Zones - OK
- [ ] Geofence Rules - OK
- [ ] Geofence Events - OK
- [ ] Fraud Rules - OK
- [ ] Fraud Alerts - OK
- [ ] Blacklist - OK

---

## 🐛 Problemas Detectados

_(Completar durante las pruebas)_

### Críticos
- [ ] Ninguno detectado

### Menores
- [ ] Ninguno detectado

---

## 📊 Resumen Final

- **Total Endpoints:** ~150+
- **Controladores:** 29
- **Autenticación:** JWT ✅ | API Key ✅
- **Estado:** ⏳ Pendiente de validación

---

**Siguiente paso:** Ejecutar script completo de pruebas y marcar checkboxes
