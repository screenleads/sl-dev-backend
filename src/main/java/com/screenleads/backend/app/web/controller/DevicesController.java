package com.screenleads.backend.app.web.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.screenleads.backend.app.application.service.DeviceService;
import com.screenleads.backend.app.web.dto.AdviceDTO;
import com.screenleads.backend.app.web.dto.DeviceDTO;

@RestController
@RequestMapping("/devices")
@CrossOrigin
public class DevicesController {

    @Autowired
    private final DeviceService deviceService;

    public DevicesController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @CrossOrigin
    @GetMapping
    public ResponseEntity<List<DeviceDTO>> getAllDevices() {
        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    @CrossOrigin
    @GetMapping("/code/{uuid}")
    public ResponseEntity<List<DeviceDTO>> createConnectionCodeForDevice(@PathVariable Long id) {
        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    @CrossOrigin
    @GetMapping("/{id}")
    public ResponseEntity<DeviceDTO> getDeviceById(@PathVariable Long id) {
        Optional<DeviceDTO> device = deviceService.getDeviceById(id);
        return device.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @CrossOrigin
    @PostMapping
    public ResponseEntity<DeviceDTO> createDevice(@RequestBody DeviceDTO deviceDTO) {
        return ResponseEntity.ok(deviceService.saveDevice(deviceDTO));
    }

    @CrossOrigin
    @PutMapping("/{id}")
    public ResponseEntity<DeviceDTO> updateDevice(@PathVariable Long id, @RequestBody DeviceDTO deviceDTO) {
        try {
            DeviceDTO updatedDevice = deviceService.updateDevice(id, deviceDTO);
            return ResponseEntity.ok(updatedDevice);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @CrossOrigin
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    @CrossOrigin
    @GetMapping("/{deviceId}/advices")
    public ResponseEntity<List<AdviceDTO>> getAdvicesForDevice(@PathVariable Long deviceId) {
        return ResponseEntity.ok(deviceService.getAdvicesForDevice(deviceId));
    }

    @CrossOrigin
    @PostMapping("/{deviceId}/advices/{adviceId}")
    public ResponseEntity<Void> assignAdviceToDevice(@PathVariable Long deviceId, @PathVariable Long adviceId) {
        deviceService.assignAdviceToDevice(deviceId, adviceId);
        return ResponseEntity.ok().build();
    }

    @CrossOrigin
    @DeleteMapping("/{deviceId}/advices/{adviceId}")
    public ResponseEntity<Void> removeAdviceFromDevice(@PathVariable Long deviceId, @PathVariable Long adviceId) {
        deviceService.removeAdviceFromDevice(deviceId, adviceId);
        return ResponseEntity.noContent().build();
    }
}
