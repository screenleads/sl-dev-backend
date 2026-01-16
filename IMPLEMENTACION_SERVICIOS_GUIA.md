# Guía de Implementación de Servicios - Backend ScreenLeads

**Fecha**: 13 de enero de 2026  
**Estado**: Repositorios y DTOs completados, Servicios e Implementaciones pendientes

---

## ✅ COMPLETADO

### 1. **Repositorios** (7 nuevos + 1 actualizado)
- ✅ `PromotionRedemptionRepository.java`
- ✅ `UserActionRepository.java`
- ✅ `CompanyBillingRepository.java`
- ✅ `InvoiceRepository.java`
- ✅ `InvoiceItemRepository.java`
- ✅ `BillingEventRepository.java`
- ✅ `DataExportRepository.java`
- ✅ `CustomerRepository.java` (actualizado)

### 2. **DTOs** (8 nuevos records)
- ✅ `CustomerDTO.java`
- ✅ `PromotionRedemptionDTO.java`
- ✅ `UserActionDTO.java`
- ✅ `CompanyBillingDTO.java`
- ✅ `InvoiceDTO.java`
- ✅ `InvoiceItemDTO.java`
- ✅ `BillingEventDTO.java`
- ✅ `DataExportDTO.java`

### 3. **Interfaces de Servicio** (6 nuevas + 1 actualizada)
- ✅ `PromotionRedemptionService.java`
- ✅ `UserActionService.java`
- ✅ `CompanyBillingService.java`
- ✅ `InvoiceService.java`
- ✅ `BillingEventService.java`
- ✅ `DataExportService.java`
- ✅ `CustomerService.java` (actualizada)

### 4. **Controladores** (2 ejemplos completos)
- ✅ `PromotionRedemptionController.java`
- ✅ `CompanyBillingController.java`

---

## 📝 PENDIENTE: Implementaciones de Servicios

### Patrón de Implementación

Todos los servicios deben seguir esta estructura estándar:

```java
package com.screenleads.backend.app.application.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.screenleads.backend.app.domain.model.NombreEntidad;
import com.screenleads.backend.app.domain.repositories.NombreEntidadRepository;
import com.screenleads.backend.app.web.dto.NombreEntidadDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NombreEntidadServiceImpl implements NombreEntidadService {

    private static final String NOT_FOUND = "NombreEntidad not found: ";

    private final NombreEntidadRepository repository;
    // Inyectar otros repositorios/servicios necesarios

    // =========================================
    // CRUD Básico
    // =========================================

    @Override
    @Transactional(readOnly = true)
    public List<NombreEntidadDTO> getAllEntities() {
        return repository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NombreEntidadDTO> getEntityById(Long id) {
        return repository.findById(id)
                .map(this::convertToDTO);
    }

    @Override
    public NombreEntidadDTO createEntity(NombreEntidadDTO dto) {
        NombreEntidad entity = convertToEntity(dto);
        NombreEntidad saved = repository.save(entity);
        return convertToDTO(saved);
    }

    @Override
    public NombreEntidadDTO updateEntity(Long id, NombreEntidadDTO dto) {
        NombreEntidad existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND + id));

        // Actualizar campos desde DTO
        updateEntityFields(existing, dto);

        // JPA persiste automáticamente en transacción
        return convertToDTO(existing);
    }

    @Override
    public void deleteEntity(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException(NOT_FOUND + id);
        }
        repository.deleteById(id);
    }

    // =========================================
    // Métodos Helper Privados
    // =========================================

    private NombreEntidadDTO convertToDTO(NombreEntidad entity) {
        return new NombreEntidadDTO(
            entity.getId(),
            entity.getCampo1(),
            entity.getCampo2(),
            // ... todos los campos
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private NombreEntidad convertToEntity(NombreEntidadDTO dto) {
        NombreEntidad entity = new NombreEntidad();
        updateEntityFields(entity, dto);
        return entity;
    }

    private void updateEntityFields(NombreEntidad entity, NombreEntidadDTO dto) {
        // Actualizar solo campos no nulos
        if (dto.campo1() != null) entity.setCampo1(dto.campo1());
        if (dto.campo2() != null) entity.setCampo2(dto.campo2());
        // ... resto de campos
    }
}
```

---

## 🔨 IMPLEMENTACIONES A CREAR

### 1. **CustomerServiceImpl.java**

```java
package com.screenleads.backend.app.application.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;
    private final UserActionService userActionService; // Para tracking
    
    // Implementar todos los métodos de CustomerService
    // - CRUD básico de customers
    // - Búsqueda por email/phone
    // - Verificación de email/phone
    // - Gestión de authMethods
    // - Incremento de métricas (totalRedemptions)
}
```

**Campos importantes a mapear**:
- Customer tiene relaciones con PromotionRedemption y UserAction (lazy load)
- authMethods es un Set<AuthMethod> (ElementCollection)
- socialProfiles, tags son JSON (String en BD)
- Métricas: totalRedemptions, lifetimeValue se actualizan con helpers

---

### 2. **PromotionRedemptionServiceImpl.java**

```java
package com.screenleads.backend.app.application.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PromotionRedemptionServiceImpl implements PromotionRedemptionService {
    private final PromotionRedemptionRepository redemptionRepository;
    private final CustomerRepository customerRepository;
    private final PromotionRepository promotionRepository;
    private final DeviceRepository deviceRepository;
    private final UserActionService userActionService;
    private final BillingEventService billingEventService;
    
    // Implementar métodos:
    // - CRUD de redemptions
    // - Generación de cupones únicos (UUID o prefijo + random)
    // - Verificación de límites de canje (redemptionLimitType de Promotion)
    // - Marcado de verificación/fraud/billing
    // - Tracking de acciones (usar userActionService)
    // - Registro de eventos de billing
}
```

**Lógica importante**:
- Al crear redemption:
  1. Validar límites de la promoción (maxRedemptionsPerUser, etc.)
  2. Generar couponCode único
  3. Incrementar contador de promotion (promotion.incrementRedemption())
  4. Incrementar contador de customer (customer.incrementRedemptions())
  5. Crear UserAction de tipo REDEEM_PROMOTION
  6. Crear BillingEvent de tipo LEAD_REGISTERED
  7. Reportar uso a Stripe (usar StripeBillingService existente)

---

### 3. **UserActionServiceImpl.java**

```java
package com.screenleads.backend.app.application.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserActionServiceImpl implements UserActionService {
    private final UserActionRepository actionRepository;
    private final CustomerRepository customerRepository;
    private final DeviceRepository deviceRepository;
    
    // Implementar:
    // - CRUD básico
    // - trackAction() - método helper para crear acciones rápidamente
    // - Búsquedas por customer, device, tipo, rango de fechas
}
```

**Uso típico**:
```java
userActionService.trackAction(
    customerId, 
    deviceId, 
    UserActionType.REDEEM_PROMOTION,
    "Promotion",
    promotionId,
    "{\"couponCode\":\"" + coupon + "\"}"
);
```

---

### 4. **CompanyBillingServiceImpl.java**

```java
package com.screenleads.backend.app.application.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompanyBillingServiceImpl implements CompanyBillingService {
    private final CompanyBillingRepository billingRepository;
    private final CompanyRepository companyRepository;
    private final StripeBillingService stripeBillingService; // Usar existente
    
    // Implementar:
    // - CRUD básico
    // - incrementCurrentPeriodUsage() - llamado al registrar lead
    // - resetCurrentPeriod() - llamado al generar invoice mensual
    // - hasReachedDeviceLimit() - validación antes de crear device
    // - hasReachedPromotionLimit() - validación antes de crear promotion
    // - Integración con Stripe (crear customer, subscription, etc.)
}
```

**Nota**: CompanyBilling tiene relación One-to-One con Company

---

### 5. **InvoiceServiceImpl.java**

```java
package com.screenleads.backend.app.application.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final CompanyBillingRepository billingRepository;
    private final PromotionRedemptionRepository redemptionRepository;
    private final BillingEventService billingEventService;
    
    // Implementar:
    // - CRUD básico
    // - generateMonthlyInvoice() - método principal
    //   1. Obtener todos los redemptions del periodo con billing PENDING
    //   2. Agrupar por promoción
    //   3. Crear InvoiceItems (uno por promoción + base fee)
    //   4. Calcular totales (invoice.calculateTotals())
    //   5. Crear BillingEvent de INVOICE_CREATED
    //   6. Marcar redemptions como BILLED
    //   7. Actualizar CompanyBilling (resetCurrentPeriod)
    // - finalizeInvoice() - cambiar status a FINALIZED
    // - markAsPaid() - registrar pago
}
```

**Flujo de facturación mensual**:
```java
// Ejecutar vía Scheduled Task cada mes
@Scheduled(cron = "0 0 1 1 * ?") // 1 de cada mes a las 00:00
public void generateMonthlyInvoices() {
    List<CompanyBilling> activeCompanies = billingRepository.findByActive(true);
    
    for (CompanyBilling billing : activeCompanies) {
        Instant periodStart = billing.getCurrentPeriodStart();
        Instant periodEnd = billing.getCurrentPeriodEnd();
        
        invoiceService.generateMonthlyInvoice(
            billing.getId(), 
            periodStart, 
            periodEnd
        );
    }
}
```

---

### 6. **BillingEventServiceImpl.java**

```java
package com.screenleads.backend.app.application.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BillingEventServiceImpl implements BillingEventService {
    private final BillingEventRepository eventRepository;
    private final CompanyBillingRepository billingRepository;
    
    // Implementar:
    // - CRUD básico
    // - Métodos helper para tracking:
    //   * trackLeadRegistered()
    //   * trackUsageReported()
    //   * trackInvoiceCreated()
    //   * trackPaymentSucceeded()
    //   * trackPaymentFailed()
    // - Cada método crea un BillingEvent con factory method
}
```

**Uso**:
```java
billingEventService.trackLeadRegistered(
    companyBillingId, 
    redemptionId, 
    1 // quantity
);
```

---

### 7. **DataExportServiceImpl.java**

```java
package com.screenleads.backend.app.application.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DataExportServiceImpl implements DataExportService {
    private final DataExportRepository exportRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PromotionRedemptionRepository redemptionRepository;
    
    // Implementar:
    // - CRUD básico
    // - requestExport() - crear export en status PENDING
    // - markAsStarted() - cambiar status a PROCESSING
    // - markAsCompleted() - status COMPLETED + fileUrl
    // - markAsFailed() - status FAILED + errorMessage
    // - recordDownload() - incrementar downloadCount
    // - cleanupExpiredExports() - eliminar exports expirados (Scheduled)
    
    // Generación de exports (puede ser async):
    // 1. Leer filtros JSON
    // 2. Consultar datos según exportType
    // 3. Generar archivo CSV/JSON/XLSX
    // 4. Subir a Firebase Storage (usar FirebaseStorageService existente)
    // 5. Obtener URL temporal
    // 6. Actualizar export con fileUrl y status COMPLETED
}
```

---

## 🎯 CONTROLADORES RESTANTES

Siguiendo el patrón de `PromotionRedemptionController` y `CompanyBillingController`, crear:

### 1. **CustomerController.java**
```java
@RestController
@RequestMapping("/customers")
public class CustomerController {
    // GET /customers - lista
    // GET /customers/{id} - por ID
    // GET /customers/email/{email} - por email
    // GET /customers/search?q={term} - búsqueda
    // POST /customers - crear
    // PUT /customers/{id} - actualizar
    // PUT /customers/{id}/verify-email - verificar email
    // PUT /customers/{id}/verify-phone - verificar phone
    // DELETE /customers/{id} - eliminar
}
```

### 2. **InvoiceController.java**
```java
@RestController
@RequestMapping("/invoices")
public class InvoiceController {
    // GET /invoices - lista
    // GET /invoices/{id} - por ID
    // GET /invoices/company-billing/{id} - por company billing
    // POST /invoices - crear
    // PUT /invoices/{id} - actualizar
    // PUT /invoices/{id}/finalize - finalizar
    // PUT /invoices/{id}/mark-paid - marcar como pagada
    // GET /invoices/overdue - facturas vencidas
    // DELETE /invoices/{id} - eliminar
}
```

### 3. **UserActionController.java**
```java
@RestController
@RequestMapping("/user-actions")
public class UserActionController {
    // GET /user-actions - lista (admin only)
    // GET /user-actions/customer/{id} - por customer
    // GET /user-actions/device/{id} - por device
    // POST /user-actions - crear (tracking)
}
```

### 4. **BillingEventController.java**
```java
@RestController
@RequestMapping("/billing-events")
public class BillingEventController {
    // GET /billing-events - lista (admin only)
    // GET /billing-events/company-billing/{id} - por company
    // GET /billing-events/invoice/{id} - por invoice
}
```

### 5. **DataExportController.java**
```java
@RestController
@RequestMapping("/data-exports")
public class DataExportController {
    // GET /data-exports - lista
    // GET /data-exports/{id} - por ID
    // GET /data-exports/company/{id} - por company
    // POST /data-exports/request - solicitar export
    // GET /data-exports/{id}/download - descargar (redirige a fileUrl)
    // DELETE /data-exports/{id} - eliminar
}
```

---

## ⚙️ TAREAS SCHEDULED

Crear clase `ScheduledTasks.java`:

```java
package com.screenleads.backend.app.application.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final InvoiceService invoiceService;
    private final CompanyBillingRepository billingRepository;
    private final DataExportService exportService;

    /**
     * Generar facturas mensuales (día 1 de cada mes a las 00:00)
     */
    @Scheduled(cron = "0 0 1 1 * ?")
    public void generateMonthlyInvoices() {
        log.info("Starting monthly invoice generation...");
        
        List<CompanyBilling> activeCompanies = billingRepository.findByActive(true);
        
        for (CompanyBilling billing : activeCompanies) {
            try {
                Instant periodStart = billing.getCurrentPeriodStart();
                Instant periodEnd = billing.getCurrentPeriodEnd();
                
                invoiceService.generateMonthlyInvoice(
                    billing.getId(), 
                    periodStart, 
                    periodEnd
                );
                
                log.info("Invoice generated for company billing {}", billing.getId());
            } catch (Exception e) {
                log.error("Error generating invoice for company billing {}: {}", 
                    billing.getId(), e.getMessage());
            }
        }
        
        log.info("Monthly invoice generation completed");
    }

    /**
     * Limpiar exports expirados (todos los días a las 02:00)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredExports() {
        log.info("Starting cleanup of expired exports...");
        
        try {
            exportService.cleanupExpiredExports();
            log.info("Expired exports cleanup completed");
        } catch (Exception e) {
            log.error("Error cleaning up expired exports: {}", e.getMessage());
        }
    }
}
```

---

## 🔧 INTEGRACIÓN CON SERVICIOS EXISTENTES

### StripeBillingService (existente)

Actualizar para usar nuevas entidades:

```java
// En PromotionRedemptionServiceImpl, al crear redemption:
try {
    Promotion promo = redemption.getPromotion();
    Company company = promo.getCompany();
    
    // Buscar CompanyBilling
    CompanyBilling billing = billingRepository.findByCompanyId(company.getId())
        .orElseThrow(() -> new IllegalStateException("CompanyBilling not found"));
    
    // Reportar uso a Stripe
    stripeBillingService.reportLeadUsage(
        billing.getStripeSubscriptionItemId(),
        1L, // quantity
        Instant.now().getEpochSecond()
    );
    
    // Registrar evento
    billingEventService.trackUsageReported(
        billing.getId(),
        stripeUsageRecordId,
        1
    );
    
    // Incrementar uso del periodo
    companyBillingService.incrementCurrentPeriodUsage(billing.getId(), 1);
    
} catch (Exception e) {
    log.error("Error reporting usage to Stripe: {}", e.getMessage());
}
```

---

## 📦 ORDEN DE IMPLEMENTACIÓN RECOMENDADO

1. **CustomerServiceImpl** (base fundamental)
2. **UserActionServiceImpl** (tracking básico)
3. **PromotionRedemptionServiceImpl** (core business logic)
4. **BillingEventServiceImpl** (auditoría)
5. **CompanyBillingServiceImpl** (configuración)
6. **InvoiceServiceImpl** (facturación)
7. **DataExportServiceImpl** (remarketing)
8. **Controladores** (uno por uno, testeando con Postman)
9. **ScheduledTasks** (automatización)

---

## ✅ CHECKLIST DE VALIDACIÓN

- [ ] Todas las implementaciones de servicios compiladas sin errores
- [ ] Todos los controladores creados y anotados correctamente
- [ ] Tests unitarios básicos para servicios críticos
- [ ] Probar crear Customer nuevo
- [ ] Probar crear PromotionRedemption (flujo completo)
- [ ] Verificar que se registran UserActions
- [ ] Verificar que se registran BillingEvents
- [ ] Probar generar factura mensual
- [ ] Probar solicitar DataExport
- [ ] Verificar integraciones con Stripe

---

**Siguiente paso**: Implementar CustomerServiceImpl siguiendo el patrón documentado.
