package com.screenleads.backend.app.web.controller;

import com.screenleads.backend.app.application.service.MetadataService;
import com.screenleads.backend.app.web.dto.EntityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/metadata")
// @PreAuthorize("hasAnyRole('admin','company_admin')")
public class MetaDataController {

    private static final Logger log = LoggerFactory.getLogger(MetaDataController.class);

    private final MetadataService metadataService;

    // ✅ Inyección por constructor (sin @Autowired necesario en Spring 4.3+)
    public MetaDataController(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @GetMapping("/entities")
    public ResponseEntity<List<EntityInfo>> getEntities(
            @RequestParam(name = "withCount", defaultValue = "false") boolean withCount) {
        try {
            return ResponseEntity.ok(metadataService.getAllEntities(withCount));
        } catch (Exception ex) {
            log.error("Error en /metadata/entities", ex);
            return ResponseEntity.ok(List.of()); // nunca tumbes el endpoint
        }
    }
}
