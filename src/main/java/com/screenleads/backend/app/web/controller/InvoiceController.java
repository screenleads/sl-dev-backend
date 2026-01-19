package com.screenleads.backend.app.web.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.screenleads.backend.app.application.service.InvoiceService;
import com.screenleads.backend.app.web.dto.InvoiceDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/invoices")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Gestión de facturas")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PreAuthorize("@perm.can('invoice', 'read')")
    @GetMapping
    @Operation(summary = "Listar todas las facturas")
    public ResponseEntity<List<InvoiceDTO>> getAllInvoices() {
        log.info("GET /invoices - Listing all invoices");
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    @PreAuthorize("@perm.can('invoice', 'read')")
    @GetMapping("/{id}")
    @Operation(summary = "Obtener factura por ID")
    public ResponseEntity<InvoiceDTO> getInvoiceById(@PathVariable Long id) {
        log.info("GET /invoices/{} - Getting invoice", id);
        return invoiceService.getInvoiceById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("@perm.can('invoice', 'read')")
    @GetMapping("/number/{invoiceNumber}")
    @Operation(summary = "Obtener factura por número")
    public ResponseEntity<InvoiceDTO> getInvoiceByNumber(@PathVariable String invoiceNumber) {
        log.info("GET /invoices/number/{} - Getting invoice by number", invoiceNumber);
        return invoiceService.getInvoiceByNumber(invoiceNumber)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("@perm.can('invoice', 'read')")
    @GetMapping("/billing/{companyBillingId}")
    @Operation(summary = "Obtener facturas por configuración de facturación")
    public ResponseEntity<List<InvoiceDTO>> getInvoicesByCompanyBilling(@PathVariable Long companyBillingId) {
        log.info("GET /invoices/billing/{} - Getting invoices by company billing", companyBillingId);
        return ResponseEntity.ok(invoiceService.getInvoicesByCompanyBilling(companyBillingId));
    }

    @PreAuthorize("@perm.can('invoice', 'read')")
    @GetMapping("/overdue")
    @Operation(summary = "Obtener facturas vencidas")
    public ResponseEntity<List<InvoiceDTO>> getOverdueInvoices() {
        log.info("GET /invoices/overdue - Getting overdue invoices");
        return ResponseEntity.ok(invoiceService.getOverdueInvoices());
    }

    @PreAuthorize("@perm.can('invoice', 'write')")
    @PostMapping
    @Operation(summary = "Crear nueva factura")
    public ResponseEntity<InvoiceDTO> createInvoice(@RequestBody InvoiceDTO dto) {
        log.info("POST /invoices - Creating invoice");
        InvoiceDTO created = invoiceService.createInvoice(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PreAuthorize("@perm.can('invoice', 'write')")
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar factura")
    public ResponseEntity<InvoiceDTO> updateInvoice(@PathVariable Long id, @RequestBody InvoiceDTO dto) {
        log.info("PUT /invoices/{} - Updating invoice", id);
        InvoiceDTO updated = invoiceService.updateInvoice(id, dto);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("@perm.can('invoice', 'write')")
    @PostMapping("/{id}/finalize")
    @Operation(summary = "Finalizar factura")
    public ResponseEntity<InvoiceDTO> finalizeInvoice(@PathVariable Long id) {
        log.info("POST /invoices/{}/finalize - Finalizing invoice", id);
        InvoiceDTO finalized = invoiceService.finalizeInvoice(id);
        return ResponseEntity.ok(finalized);
    }

    @PreAuthorize("@perm.can('invoice', 'write')")
    @PostMapping("/{id}/mark-paid")
    @Operation(summary = "Marcar factura como pagada")
    public ResponseEntity<InvoiceDTO> markAsPaid(@PathVariable Long id) {
        log.info("POST /invoices/{}/mark-paid - Marking invoice as paid", id);
        InvoiceDTO paid = invoiceService.markAsPaid(id);
        return ResponseEntity.ok(paid);
    }

    @PreAuthorize("@perm.can('invoice', 'delete')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar factura")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        log.info("DELETE /invoices/{} - Deleting invoice", id);
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }
}
