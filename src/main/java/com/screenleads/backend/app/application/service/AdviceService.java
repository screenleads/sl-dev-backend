package com.screenleads.backend.app.application.service;

import java.util.List;
import java.util.Optional;

import com.screenleads.backend.app.web.dto.AdviceDTO;

public interface AdviceService {
    List<AdviceDTO> getAllAdvices();

    Optional<AdviceDTO> getAdviceById(Long id);

    AdviceDTO saveAdvice(AdviceDTO AdviceDTO);

    AdviceDTO updateAdvice(Long id, AdviceDTO AdviceDTO);

    void deleteAdvice(Long id);
}
