package com.screenleads.backend.app.application.service;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.screenleads.backend.app.web.dto.AdviceDTO;
import com.screenleads.backend.app.web.dto.DeviceDTO;

public interface AdviceService {
    List<AdviceDTO> getAllAdvices();

    /** Devuelve los advices visibles "ahora" en la zoneId indicada (si null, systemDefault). */
    List<AdviceDTO> getVisibleAdvicesNow(ZoneId zoneId);

    Optional<AdviceDTO> getAdviceById(Long id);

    AdviceDTO saveAdvice(AdviceDTO dto);

    AdviceDTO updateAdvice(Long id, AdviceDTO dto);

    void deleteAdvice(Long id);

    /** Gestión de asignación de dispositivos a advices */
    List<DeviceDTO> getDevicesForAdvice(Long adviceId);

    void assignDeviceToAdvice(Long adviceId, Long deviceId);

    void unassignDeviceFromAdvice(Long adviceId, Long deviceId);
}
