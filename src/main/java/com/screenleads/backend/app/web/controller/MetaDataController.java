package com.screenleads.backend.app.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// Si usas seguridad por roles, descomenta @PreAuthorize y añade dependencia spring-boot-starter-security
// import org.springframework.security.access.prepost.PreAuthorize;

import com.screenleads.backend.app.application.service.MetadataService;
import com.screenleads.backend.app.web.dto.EntityInfo;

import java.util.List;

@RestController
@RequestMapping("/metadata")
// @PreAuthorize("hasAnyRole('admin','company_admin')") // opcional: proteger el
// recurso
public class MetaDataController {
    private MetadataService metadataService;

    public void MetadataController(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @GetMapping("/entities")
    public ResponseEntity<List<EntityInfo>> getEntities(
            @RequestParam(name = "withCount", defaultValue = "false") boolean withCount) {
        return ResponseEntity.ok(metadataService.getAllEntities(withCount));
    }
}
