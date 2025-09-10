package com.screenleads.backend.app.web.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.screenleads.backend.app.application.service.DeviceService;
import com.screenleads.backend.app.web.dto.AdviceDTO;
import com.screenleads.backend.app.web.dto.DeviceDTO;

@RestController
@RequestMapping("/devices")
@CrossOrigin
public class DevicesController {

    private final DeviceService deviceService;

    public DevicesController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    // -------------------------------------------------------------------------
    // CRUD básico
    // -------------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<List<DeviceDTO>> getAllDevices() {
        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceDTO> getDeviceById(@PathVariable Long id) {
        Optional<DeviceDTO> device = deviceService.getDeviceById(id);
        return device.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<DeviceDTO> createDevice(@RequestBody DeviceDTO deviceDTO) {
        DeviceDTO saved = deviceService.saveDevice(deviceDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceDTO> updateDevice(@PathVariable Long id, @RequestBody DeviceDTO deviceDTO) {
        DeviceDTO updatedDevice = deviceService.updateDevice(id, deviceDTO);
        return ResponseEntity.ok(updatedDevice);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Búsqueda / existencia por UUID (para que el frontend se "autocure")
    // -------------------------------------------------------------------------

    @GetMapping("/uuid/{uuid}")
    public ResponseEntity<DeviceDTO> getDeviceByUuid(@PathVariable String uuid) {
        return deviceService.getDeviceByUuid(uuid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @RequestMapping(value = "/uuid/{uuid}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> headDeviceByUuid(@PathVariable String uuid) {
        return deviceService.getDeviceByUuid(uuid).isPresent()
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    // -------------------------------------------------------------------------
    // Gestión de advices por dispositivo
    // -------------------------------------------------------------------------

    @GetMapping("/{deviceId}/advices")
    public ResponseEntity<List<AdviceDTO>> getAdvicesForDevice(@PathVariable Long deviceId) {
        return ResponseEntity.ok(deviceService.getAdvicesForDevice(deviceId));
    }

    @PostMapping("/{deviceId}/advices/{adviceId}")
    public ResponseEntity<Void> assignAdviceToDevice(@PathVariable Long deviceId, @PathVariable Long adviceId) {
        deviceService.assignAdviceToDevice(deviceId, adviceId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{deviceId}/advices/{adviceId}")
    public ResponseEntity<Void> removeAdviceFromDevice(@PathVariable Long deviceId, @PathVariable Long adviceId) {
        deviceService.removeAdviceFromDevice(deviceId, adviceId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // (Opcional) Endpoint placeholder para /code/{uuid} si realmente lo necesitas
    // -------------------------------------------------------------------------

    /**
     * TODO: Implementar generación/lectura de código de conexión para el
     * dispositivo.
     * La firma original no coincidía con el path variable y devolvía la lista
     * completa.
     * De momento respondemos 501 Not Implemented para evitar confusión.
     */
    @GetMapping("/code/{uuid}")
    public ResponseEntity<Void> createConnectionCodeForDevice(@PathVariable String uuid) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
