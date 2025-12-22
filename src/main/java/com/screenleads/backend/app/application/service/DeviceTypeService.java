package com.screenleads.backend.app.application.service;

import java.util.List;
import java.util.Optional;

import com.screenleads.backend.app.web.dto.DeviceTypeDTO;

public interface DeviceTypeService {
    List<DeviceTypeDTO> getAllDeviceTypes();

    Optional<DeviceTypeDTO> getDeviceTypeById(Long id);

    DeviceTypeDTO saveDeviceType(DeviceTypeDTO dto);

    DeviceTypeDTO updateDeviceType(Long id, DeviceTypeDTO dto);

    void deleteDeviceType(Long id);
}
