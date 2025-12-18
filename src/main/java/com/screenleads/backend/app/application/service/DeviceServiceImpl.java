package com.screenleads.backend.app.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Device;
import com.screenleads.backend.app.domain.model.DeviceType;
import com.screenleads.backend.app.domain.repositories.AdviceRepository;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.DeviceRepository;
import com.screenleads.backend.app.domain.repositories.DeviceTypeRepository;
import com.screenleads.backend.app.web.dto.AdviceDTO;
import com.screenleads.backend.app.web.dto.DeviceDTO;
import com.screenleads.backend.app.web.mapper.DeviceMapper;
import com.screenleads.backend.app.web.mapper.AdviceMapper;

@Service
@Transactional
public class DeviceServiceImpl implements DeviceService {

    private static final String DEVICE_NOT_FOUND = "Device not found";
    private static final String DEVICE_TYPE_NOT_FOUND = "Device type not found";
    private static final String COMPANY_NOT_FOUND = "Company not found";

    private final DeviceRepository deviceRepository;
    private final DeviceTypeRepository deviceTypeRepository;
    private final CompanyRepository companyRepository;
    private final AdviceRepository adviceRepository;

    public DeviceServiceImpl(
            DeviceRepository deviceRepository,
            DeviceTypeRepository deviceTypeRepository,
            CompanyRepository companyRepository,
            AdviceRepository adviceRepository) {
        this.deviceRepository = deviceRepository;
        this.deviceTypeRepository = deviceTypeRepository;
        this.companyRepository = companyRepository;
        this.adviceRepository = adviceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceDTO> getAllDevices() {
        return deviceRepository.findAll().stream()
                .map(this::convertToDTO)
                .sorted(Comparator.comparing(DeviceDTO::id))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeviceDTO> getDeviceById(Long id) {
        return deviceRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeviceDTO> getDeviceByUuid(String uuid) {
        return deviceRepository.findOptionalByUuid(uuid).map(this::convertToDTO);
    }

    @Override
    public DeviceDTO saveDevice(DeviceDTO dto) {
        // Upsert idempotente por UUID
        DeviceType type = deviceTypeRepository.findById(dto.type().id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, DEVICE_TYPE_NOT_FOUND));

        Device device = deviceRepository.findOptionalByUuid(dto.uuid()).orElseGet(Device::new);
        Integer width = dto.width() != null ? dto.width().intValue() : null;
        Integer height = dto.height() != null ? dto.height().intValue() : null;

        device.setUuid(dto.uuid());
        device.setDescriptionName(dto.descriptionName());
        device.setWidth(width);
        device.setHeight(height);
        device.setType(type);

        if (dto.company() != null && dto.company().id() != null) {
            Company company = companyRepository.findById(dto.company().id())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, COMPANY_NOT_FOUND));
            device.setCompany(company);
        } else {
            device.setCompany(null);
        }

        return convertToDTO(deviceRepository.save(device));
    }

    @Override
    public DeviceDTO updateDevice(Long id, DeviceDTO deviceDTO) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, DEVICE_NOT_FOUND));

        if (deviceDTO.type() == null || deviceDTO.type().id() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Device type is required");
        }

        DeviceType type = deviceTypeRepository.findById(deviceDTO.type().id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, DEVICE_TYPE_NOT_FOUND));

        device.setUuid(deviceDTO.uuid());
        device.setDescriptionName(deviceDTO.descriptionName());
        Integer width = deviceDTO.width() != null ? deviceDTO.width().intValue() : null;
        Integer height = deviceDTO.height() != null ? deviceDTO.height().intValue() : null;
        device.setWidth(width);
        device.setHeight(height);
        device.setType(type);

        if (deviceDTO.company() != null && deviceDTO.company().id() != null) {
            Company company = companyRepository.findById(deviceDTO.company().id())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, COMPANY_NOT_FOUND));
            device.setCompany(company);
        } else {
            device.setCompany(null);
        }

        Device updatedDevice = deviceRepository.save(device);
        return convertToDTO(updatedDevice);
    }

    @Override
    public void deleteDevice(Long id) {
        deviceRepository.deleteById(id);
    }

    @Override
    public void deleteByUuid(String uuid) {
        deviceRepository.findOptionalByUuid(uuid).ifPresent(deviceRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdviceDTO> getAdvicesForDevice(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, DEVICE_NOT_FOUND));
        return device.getAdvices().stream()
                .sorted(Comparator.comparing(Advice::getId))
                .map(AdviceMapper::toDTO)
                .toList();
    }

    @Override
    public void assignAdviceToDevice(Long deviceId, Long adviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, DEVICE_NOT_FOUND));
        Advice advice = adviceRepository.findById(adviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Advice not found"));
        device.getAdvices().add(advice);
        deviceRepository.save(device);
    }

    @Override
    public void removeAdviceFromDevice(Long deviceId, Long adviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, DEVICE_NOT_FOUND));
        Advice advice = adviceRepository.findById(adviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Advice not found"));
        device.getAdvices().remove(advice);
        deviceRepository.save(device);
    }

    private DeviceDTO convertToDTO(Device device) {
        return DeviceMapper.toDTO(device);
    }
}
