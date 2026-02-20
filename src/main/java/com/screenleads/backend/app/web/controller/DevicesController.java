package com.screenleads.backend.app.web.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.screenleads.backend.app.application.service.DeviceService;
import com.screenleads.backend.app.web.dto.AdviceDTO;
import com.screenleads.backend.app.web.dto.DeviceDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/devices")
@CrossOrigin
@Tag(name = "Devices", description = "CRUD de dispositivos y gestión de advices por dispositivo")
public class DevicesController {

    private final DeviceService deviceService;

    public DevicesController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    // -------------------------------------------------------------------------
    // CRUD básico
    // -------------------------------------------------------------------------

    @PreAuthorize("@perm.can('device', 'read')")
    @GetMapping
    @Operation(summary = "Listar dispositivos")
    public ResponseEntity<List<DeviceDTO>> getAllDevices() {
        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    @PreAuthorize("@perm.can('device', 'read')")
    @GetMapping("/{id}")
    @Operation(summary = "Obtener dispositivo por id")
    public ResponseEntity<DeviceDTO> getDeviceById(@PathVariable Long id) {
        Optional<DeviceDTO> device = deviceService.getDeviceById(id);
        return device.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // @PreAuthorize removed - Public endpoint for device self-registration
    @PostMapping
    @Operation(summary = "Crear dispositivo (auto-registro público)")
    public ResponseEntity<DeviceDTO> createDevice(@RequestBody DeviceDTO deviceDTO) {
        DeviceDTO saved = deviceService.saveDevice(deviceDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // @PreAuthorize removed - Allow devices to update themselves (e.g., device
    // name)
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar dispositivo")
    public ResponseEntity<DeviceDTO> updateDevice(@PathVariable Long id, @RequestBody DeviceDTO deviceDTO) {
        DeviceDTO updatedDevice = deviceService.updateDevice(id, deviceDTO);
        return ResponseEntity.ok(updatedDevice);
    }

    @PreAuthorize("@perm.can('device', 'delete')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar dispositivo")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Búsqueda / existencia por UUID (para que el frontend se "autocure")
    // -------------------------------------------------------------------------

    // @PreAuthorize removed - Public endpoint for device lookup
    @GetMapping("/uuid/{uuid}")
    @Operation(summary = "Obtener dispositivo por UUID (público)")
    public ResponseEntity<DeviceDTO> getDeviceByUuid(@PathVariable String uuid) {
        return deviceService.getDeviceByUuid(uuid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @RequestMapping(value = "/uuid/{uuid}", method = RequestMethod.HEAD)
    @Operation(summary = "Comprobar existencia de dispositivo por UUID", description = "Devuelve 200 si existe, 404 si no")
    public ResponseEntity<Void> headDeviceByUuid(@PathVariable String uuid) {
        return deviceService.getDeviceByUuid(uuid).isPresent()
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    // -------------------------------------------------------------------------
    // Gestión de advices por dispositivo
    // -------------------------------------------------------------------------

    @GetMapping("/{deviceId}/advices")
    @Operation(summary = "Listar advices asignados a un dispositivo")
    public ResponseEntity<List<AdviceDTO>> getAdvicesForDevice(@PathVariable Long deviceId) {
        return ResponseEntity.ok(deviceService.getAdvicesForDevice(deviceId));
    }

    @PostMapping("/{deviceId}/advices/{adviceId}")
    @Operation(summary = "Asignar un advice a un dispositivo")
    public ResponseEntity<Void> assignAdviceToDevice(@PathVariable Long deviceId, @PathVariable Long adviceId) {
        deviceService.assignAdviceToDevice(deviceId, adviceId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{deviceId}/advices/{adviceId}")
    @Operation(summary = "Quitar un advice de un dispositivo")
    public ResponseEntity<Void> removeAdviceFromDevice(@PathVariable Long deviceId, @PathVariable Long adviceId) {
        deviceService.removeAdviceFromDevice(deviceId, adviceId);
        return ResponseEntity.noContent().build();
    }
}
