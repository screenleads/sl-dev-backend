package com.screenleads.backend.app.web.dto;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.DeviceType;

public record DeviceDTO(Long id, String uuid, String descriptionName, Number width, Number height, DeviceType type,Company company) {
}
