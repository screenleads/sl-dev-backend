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

import com.screenleads.backend.app.application.service.DeviceService;
import com.screenleads.backend.app.web.dto.DeviceDTO;

@Controller
public class DevicesController {
    @Autowired
    private DeviceService deviceService;

    public DevicesController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @CrossOrigin
    @GetMapping("/devices")
    public ResponseEntity<List<DeviceDTO>> getAllDevices() {
        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    @CrossOrigin
    @GetMapping("/devices/code/{uuid}")
    public ResponseEntity<List<DeviceDTO>> createConnectionCodeForDevice(@PathVariable Long id) {
        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    @CrossOrigin
    @GetMapping("/devices/{id}")
    public ResponseEntity<DeviceDTO> getDeviceById(@PathVariable Long id) {
        Optional<DeviceDTO> device = deviceService.getDeviceById(id);
        return device.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @CrossOrigin
    @PostMapping("/devices")
    public ResponseEntity<DeviceDTO> createDevice(@RequestBody DeviceDTO deviceDTO) {
        return ResponseEntity.ok(deviceService.saveDevice(deviceDTO));
    }

    @CrossOrigin
    @PutMapping("/devices/{id}")
    public ResponseEntity<DeviceDTO> updateDevice(@PathVariable Long id, @RequestBody DeviceDTO deviceDTO) {
        try {
            DeviceDTO updatedDevice = deviceService.updateDevice(id, deviceDTO);
            return ResponseEntity.ok(updatedDevice);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @CrossOrigin
    @DeleteMapping("/devices/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }
}