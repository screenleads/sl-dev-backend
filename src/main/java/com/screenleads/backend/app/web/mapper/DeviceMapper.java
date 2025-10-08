package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Device;
import com.screenleads.backend.app.domain.model.DeviceType;
import com.screenleads.backend.app.web.dto.CompanyRefDTO;
import com.screenleads.backend.app.web.dto.DeviceDTO;
import com.screenleads.backend.app.web.dto.DeviceTypeDTO;

public class DeviceMapper {
    public static DeviceDTO toDTO(Device device) {
        return new DeviceDTO(
            device.getId(),
            device.getUuid(),
            device.getDescriptionName(),
            device.getWidth(),
            device.getHeight(),
            toDeviceTypeDTO(device.getType()),
            toCompanyRefDTO(device.getCompany())
        );
    }

    public static DeviceTypeDTO toDeviceTypeDTO(DeviceType type) {
        if (type == null) return null;
        return new DeviceTypeDTO(type.getId(), type.getType(), type.getEnabled());
    }

    public static CompanyRefDTO toCompanyRefDTO(Company company) {
        if (company == null) return null;
        return new CompanyRefDTO(company.getId(), company.getName());
    }
}
