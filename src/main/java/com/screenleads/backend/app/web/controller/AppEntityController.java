package com.screenleads.backend.app.web.controller;


import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.screenleads.backend.app.application.service.AppEntityService;
import com.screenleads.backend.app.web.dto.AppEntityDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/entities")
@RequiredArgsConstructor
@Validated
public class AppEntityController {

    private final AppEntityService service;

    // ---- LISTAR ----
    @GetMapping
    public ResponseEntity<List<AppEntityDTO>> list(
            @RequestParam(name = "withCount", defaultValue = "false") boolean withCount) {
        return ResponseEntity.ok(service.findAll(withCount));
    }

    // ---- OBTENER POR ID ----
    @GetMapping("/{id}")
    public ResponseEntity<AppEntityDTO> getById(
            @PathVariable Long id,
            @RequestParam(name = "withCount", defaultValue = "false") boolean withCount) {
        return ResponseEntity.ok(service.findById(id, withCount));
    }

    // ---- OBTENER POR RESOURCE ----
    @GetMapping("/by-resource/{resource}")
    public ResponseEntity<AppEntityDTO> getByResource(
            @PathVariable String resource,
            @RequestParam(name = "withCount", defaultValue = "false") boolean withCount) {
        return ResponseEntity.ok(service.findByResource(resource, withCount));
    }

    // ---- UPSERT (CREAR/ACTUALIZAR) ----
    @PutMapping
    public ResponseEntity<AppEntityDTO> upsert(@RequestBody AppEntityDTO dto) {
        AppEntityDTO saved = service.upsert(dto);
        return new ResponseEntity<>(saved, HttpStatus.OK);
    }

    // ---- BORRAR ----
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.deleteById(id);
    }
}
