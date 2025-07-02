package com.screenleads.backend.app.application.service;

import java.util.List;
import java.util.Optional;

import com.screenleads.backend.app.web.dto.DeviceTypeDTO;

public interface DeviceTypeService {
    List<DeviceTypeDTO> getAllDeviceTypes();

    Optional<DeviceTypeDTO> getDeviceTypeById(Long id);

    DeviceTypeDTO saveDeviceType(DeviceTypeDTO DeviceTypeDTO);

    DeviceTypeDTO updateDeviceType(Long id, DeviceTypeDTO DeviceTypeDTO);

    void deleteDeviceType(Long id);
}
