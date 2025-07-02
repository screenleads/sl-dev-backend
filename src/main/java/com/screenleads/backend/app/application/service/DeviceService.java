package com.screenleads.backend.app.application.service;

import java.util.List;
import java.util.Optional;

import com.screenleads.backend.app.web.dto.DeviceDTO;

public interface DeviceService {
    List<DeviceDTO> getAllDevices();

    Optional<DeviceDTO> getDeviceById(Long id);

    DeviceDTO saveDevice(DeviceDTO DeviceDTO);

    DeviceDTO updateDevice(Long id, DeviceDTO DeviceDTO);

    void deleteDevice(Long id);
}
