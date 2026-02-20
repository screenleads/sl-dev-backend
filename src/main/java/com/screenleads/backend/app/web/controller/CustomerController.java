package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.web.dto.SSOLoginRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.FirebaseAuthException;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.screenleads.backend.app.application.service.CustomerService;
import com.screenleads.backend.app.domain.model.UserSegment;
import com.screenleads.backend.app.web.dto.CreateCustomerRequest;
import com.screenleads.backend.app.web.dto.CustomerDTO;
import com.screenleads.backend.app.web.dto.CustomerSearchCriteria;
import com.screenleads.backend.app.web.dto.CustomerStatsDTO;
import com.screenleads.backend.app.web.dto.UpdateCustomerRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller para gestión de Customers (clientes finales/consumidores)
 */
@Slf4j
@RestController
@RequestMapping("/api/customers")
@CrossOrigin
@Tag(name = "Customers", description = "Gestión de clientes finales que canjean promociones")
@RequiredArgsConstructor
public class CustomerController {

    /**
     * Registro/login SSO con Firebase
     */
    @PostMapping("/sso-login")
    @Operation(summary = "Registro/login SSO con Firebase", description = "Permite registrar o loguear un customer usando SSO (Google, Facebook, etc) validando el token de Firebase")
    public ResponseEntity<?> ssoLogin(@RequestBody SSOLoginRequest request) {
        try {
            // Validar el token de Firebase
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(request.getFirebaseToken());
            // Comprobar que el email del token coincide con el del request
            if (request.getEmail() == null || !request.getEmail().equalsIgnoreCase(decodedToken.getEmail())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email no coincide con el token");
            }
            // Buscar o crear el customer
            CustomerDTO customer = customerService.findByEmail(request.getEmail());
            if (customer == null) {
                // Crear nuevo customer
                CreateCustomerRequest createReq = new CreateCustomerRequest();
                createReq.setEmail(request.getEmail());
                createReq.setFirstName(request.getDisplayName());
                createReq.setLastName("");
                createReq.setPhone(null);
                customer = customerService.createCustomer(createReq);
            }
            // Aquí podrías emitir tu propio token JWT si lo necesitas
            return ResponseEntity.ok(customer);
        } catch (FirebaseAuthException e) {
            log.error("Error validando token Firebase", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token Firebase inválido: " + e.getMessage());
        }
    }

    private final CustomerService customerService;

    // ========== CRUD básico ==========

    @PostMapping
    @Operation(summary = "Crear nuevo customer", description = "Público para que dispositivos registren customers al canjear")
    public ResponseEntity<CustomerDTO> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        log.info("POST /api/customers - Creating customer");
        CustomerDTO created = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PreAuthorize("@perm.can('customer', 'read')")
    @GetMapping("/{id}")
    @Operation(summary = "Obtener customer por ID")
    public ResponseEntity<CustomerDTO> getCustomerById(@PathVariable Long id) {
        log.info("GET /api/customers/{} - Getting customer", id);
        CustomerDTO customer = customerService.findById(id);
        return ResponseEntity.ok(customer);
    }

    @PreAuthorize("@perm.can('customer', 'update')")
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar customer existente")
    public ResponseEntity<CustomerDTO> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        log.info("PUT /api/customers/{} - Updating customer", id);
        CustomerDTO updated = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("@perm.can('customer', 'delete')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar customer")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        log.info("DELETE /api/customers/{} - Deleting customer", id);
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    // ========== Búsqueda ==========

    @PreAuthorize("@perm.can('customer', 'read')")
    @GetMapping
    @Operation(summary = "Listar todos los customers")
    public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
        log.info("GET /api/customers - Listing all customers");
        List<CustomerDTO> customers = customerService.findAll();
        return ResponseEntity.ok(customers);
    }

    @PreAuthorize("@perm.can('customer', 'read')")
    @GetMapping("/email/{email}")
    @Operation(summary = "Buscar customer por email")
    public ResponseEntity<CustomerDTO> getCustomerByEmail(@PathVariable String email) {
        log.info("GET /api/customers/email/{} - Finding customer by email", email);
        CustomerDTO customer = customerService.findByEmail(email);
        if (customer == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(customer);
    }

    @PreAuthorize("@perm.can('customer', 'read')")
    @GetMapping("/phone/{phone}")
    @Operation(summary = "Buscar customer por teléfono")
    public ResponseEntity<CustomerDTO> getCustomerByPhone(@PathVariable String phone) {
        log.info("GET /api/customers/phone/{} - Finding customer by phone", phone);
        CustomerDTO customer = customerService.findByPhone(phone);
        if (customer == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(customer);
    }

    @PreAuthorize("@perm.can('customer', 'read')")
    @PostMapping("/search")
    @Operation(summary = "Buscar customers con criterios y paginación")
    public ResponseEntity<Page<CustomerDTO>> searchCustomers(
            @RequestBody CustomerSearchCriteria criteria,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.info("POST /api/customers/search - Searching customers");
        Page<CustomerDTO> customers = customerService.searchCustomers(criteria, pageable);
        return ResponseEntity.ok(customers);
    }

    // ========== Verificación ==========

    @PostMapping("/{id}/verify-email")
    @Operation(summary = "Verificar email de customer", description = "Público para que el customer verifique su email")
    public ResponseEntity<Void> verifyEmail(
            @PathVariable Long id,
            @RequestParam String token) {
        log.info("POST /api/customers/{}/verify-email - Verifying email", id);
        customerService.verifyEmail(id, token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/verify-phone")
    @Operation(summary = "Verificar teléfono de customer", description = "Público para que el customer verifique su teléfono")
    public ResponseEntity<Void> verifyPhone(
            @PathVariable Long id,
            @RequestParam String code) {
        log.info("POST /api/customers/{}/verify-phone - Verifying phone", id);
        customerService.verifyPhone(id, code);
        return ResponseEntity.ok().build();
    }

    // ========== Segmentación y Tags ==========

    @PreAuthorize("@perm.can('customer', 'update')")
    @PutMapping("/{id}/segment")
    @Operation(summary = "Actualizar segmento de customer")
    public ResponseEntity<Void> updateSegment(
            @PathVariable Long id,
            @RequestParam UserSegment segment) {
        log.info("PUT /api/customers/{}/segment - Updating segment to {}", id, segment);
        customerService.updateSegment(id, segment);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@perm.can('customer', 'update')")
    @PostMapping("/{id}/tags")
    @Operation(summary = "Añadir tags a customer")
    public ResponseEntity<Void> addTags(
            @PathVariable Long id,
            @RequestBody Set<String> tags) {
        log.info("POST /api/customers/{}/tags - Adding tags: {}", id, tags);
        customerService.addTags(id, tags);
        return ResponseEntity.ok().build();
    }

    // ========== Estadísticas ==========

    @PreAuthorize("@perm.can('customer', 'read')")
    @GetMapping("/{id}/stats")
    @Operation(summary = "Obtener estadísticas de customer")
    public ResponseEntity<CustomerStatsDTO> getCustomerStats(@PathVariable Long id) {
        log.info("GET /api/customers/{}/stats - Getting customer stats", id);
        CustomerStatsDTO stats = customerService.getCustomerStats(id);
        return ResponseEntity.ok(stats);
    }

    @PreAuthorize("@perm.can('customer', 'update')")
    @PostMapping("/{id}/recalculate-engagement")
    @Operation(summary = "Recalcular engagement score de customer")
    public ResponseEntity<Void> recalculateEngagementScore(@PathVariable Long id) {
        log.info("POST /api/customers/{}/recalculate-engagement - Recalculating engagement score", id);
        customerService.recalculateEngagementScore(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("@perm.can('customer', 'update')")
    @PostMapping("/{id}/update-lifetime-value")
    @Operation(summary = "Actualizar lifetime value de customer")
    public ResponseEntity<Void> updateLifetimeValue(@PathVariable Long id) {
        log.info("POST /api/customers/{}/update-lifetime-value - Updating lifetime value", id);
        customerService.updateLifetimeValue(id);
        return ResponseEntity.ok().build();
    }
}
