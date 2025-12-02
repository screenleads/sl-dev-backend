// src/main/java/com/screenleads/backend/app/web/controller/DeviceTypesController.java
package com.screenleads.backend.app.web.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.screenleads.backend.app.application.service.DeviceTypeService;
import com.screenleads.backend.app.web.dto.DeviceTypeDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController // <-- era @Controller; @RestController no cambia las rutas existentes
@Tag(name = "Device Types", description = "CRUD de tipos de dispositivo")
public class DeviceTypesController {

    @Autowired
    private DeviceTypeService deviceTypeService;

    public DeviceTypesController(DeviceTypeService deviceTypeService) {
        this.deviceTypeService = deviceTypeService;
    }

    @CrossOrigin
    @GetMapping("/devices/types")
    @PreAuthorize("@perm.can('devicetype', 'read')")
    @Operation(summary = "Listar tipos de dispositivo")
    public ResponseEntity<List<DeviceTypeDTO>> getAllDeviceTypes() {
        return ResponseEntity.ok(deviceTypeService.getAllDeviceTypes());
    }

    @CrossOrigin
    @GetMapping("/devices/types/{id}")
    @PreAuthorize("@perm.can('devicetype', 'read')")
    @Operation(summary = "Obtener tipo de dispositivo por id")
    public ResponseEntity<DeviceTypeDTO> getDeviceTypeById(@PathVariable Long id) {
        Optional<DeviceTypeDTO> device = deviceTypeService.getDeviceTypeById(id);
        return device.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @CrossOrigin
    @PostMapping("/devices/types")
    @PreAuthorize("@perm.can('devicetype', 'create')")
    @Operation(summary = "Crear tipo de dispositivo")
    public ResponseEntity<DeviceTypeDTO> createDeviceType(@RequestBody DeviceTypeDTO deviceDTO) {
        return ResponseEntity.ok(deviceTypeService.saveDeviceType(deviceDTO));
    }

    @CrossOrigin
    @PutMapping("/devices/types/{id}")
    @PreAuthorize("@perm.can('devicetype', 'update')")
    @Operation(summary = "Actualizar tipo de dispositivo")
    public ResponseEntity<DeviceTypeDTO> updateDeviceType(@PathVariable Long id, @RequestBody DeviceTypeDTO deviceDTO) {
        DeviceTypeDTO updatedDevice = deviceTypeService.updateDeviceType(id, deviceDTO);
        return ResponseEntity.ok(updatedDevice);
    }

    @CrossOrigin
    @DeleteMapping("/devices/types/{id}")
    @PreAuthorize("@perm.can('devicetype', 'delete')")
    @Operation(summary = "Eliminar tipo de dispositivo")
    public ResponseEntity<String> deleteDeviceType(@PathVariable Long id) {
        deviceTypeService.deleteDeviceType(id);
        return ResponseEntity.ok("Media Type (" + id + ") deleted succesfully");
    }
}
