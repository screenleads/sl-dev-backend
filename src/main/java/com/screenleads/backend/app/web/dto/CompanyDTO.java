package com.screenleads.backend.app.web.dto;

import java.util.List;

import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.domain.model.Device;
import com.screenleads.backend.app.domain.model.Media;

public record CompanyDTO(Long id, String name, String observations, Media logo, List<Device> devices,
        List<Advice> advices) {
}
