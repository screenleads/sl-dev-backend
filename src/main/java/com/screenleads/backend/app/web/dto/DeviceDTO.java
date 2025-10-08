
package com.screenleads.backend.app.web.dto;

public record DeviceDTO(Long id, String uuid, String descriptionName, Number width, Number height, DeviceTypeDTO type,
        CompanyRefDTO company) {
}
