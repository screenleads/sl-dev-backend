package com.screenleads.backend.app.application.service;

import java.util.List;
import java.util.Optional;
import com.screenleads.backend.app.web.dto.AdviceDTO;
import com.screenleads.backend.app.web.dto.DeviceDTO;

public interface DeviceService {
    List<DeviceDTO> getAllDevices();

    Optional<DeviceDTO> getDeviceById(Long id);

    Optional<DeviceDTO> getDeviceByUuid(String uuid); // NUEVO

    DeviceDTO saveDevice(DeviceDTO deviceDTO);

    DeviceDTO updateDevice(Long id, DeviceDTO deviceDTO);

    void deleteDevice(Long id);

    void deleteByUuid(String uuid); // opcional

    List<AdviceDTO> getAdvicesForDevice(Long deviceId);

    void assignAdviceToDevice(Long deviceId, Long adviceId);

    void removeAdviceFromDevice(Long deviceId, Long adviceId);
}