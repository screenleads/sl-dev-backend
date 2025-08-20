package com.screenleads.backend.app.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// Si usas seguridad por roles, descomenta @PreAuthorize y añade dependencia spring-boot-starter-security
// import org.springframework.security.access.prepost.PreAuthorize;

import com.screenleads.backend.app.application.service.MetadataService;
import com.screenleads.backend.app.web.dto.EntityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/metadata")
// @PreAuthorize("hasAnyRole('admin','company_admin')") // opcional: proteger el
// recurso
public class MetaDataController {
    private static final Logger log = LoggerFactory.getLogger(MetaDataController.class);
    private MetadataService metadataService;

    public void MetadataController(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @GetMapping("/entities")
    public ResponseEntity<List<EntityInfo>> getEntities(
            @RequestParam(name = "withCount", defaultValue = "false") boolean withCount) {
        try {
            return ResponseEntity.ok(metadataService.getAllEntities(withCount));
        } catch (Exception ex) {
            // Blindaje extra: nunca tumbes el endpoint
            log.error("Error en /metadata/entities", ex);
            return ResponseEntity.ok(List.of());
        }
    }
}
