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

import com.screenleads.backend.app.application.service.DeviceTypeService;
import com.screenleads.backend.app.web.dto.DeviceTypeDTO;

@Controller
public class DeviceTypesController {
    @Autowired
    private DeviceTypeService deviceTypeService;

    public DeviceTypesController(DeviceTypeService deviceTypeService) {
        this.deviceTypeService = deviceTypeService;
    }

    @CrossOrigin
    @GetMapping("/devices/types")
    public ResponseEntity<List<DeviceTypeDTO>> getAllDeviceTypes() {
        return ResponseEntity.ok(deviceTypeService.getAllDeviceTypes());
    }

    @CrossOrigin
    @GetMapping("/devices/types/{id}")
    public ResponseEntity<DeviceTypeDTO> getDeviceTypeById(@PathVariable Long id) {
        Optional<DeviceTypeDTO> device = deviceTypeService.getDeviceTypeById(id);
        return device.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @CrossOrigin
    @PostMapping("/devices/types")
    public ResponseEntity<DeviceTypeDTO> createDeviceType(@RequestBody DeviceTypeDTO deviceDTO) {
        return ResponseEntity.ok(deviceTypeService.saveDeviceType(deviceDTO));
    }

    @CrossOrigin
    @PutMapping("/devices/types/{id}")
    public ResponseEntity<DeviceTypeDTO> updateDeviceType(@PathVariable Long id, @RequestBody DeviceTypeDTO deviceDTO) {

        DeviceTypeDTO updatedDevice = deviceTypeService.updateDeviceType(id, deviceDTO);
        return ResponseEntity.ok(updatedDevice);

    }

    @CrossOrigin
    @DeleteMapping("/devices/types/{id}")
    public ResponseEntity<String> deleteDeviceType(@PathVariable Long id) {
        deviceTypeService.deleteDeviceType(id);
        return ResponseEntity.ok("Media Type (" + id + ") deleted succesfully");
    }
}