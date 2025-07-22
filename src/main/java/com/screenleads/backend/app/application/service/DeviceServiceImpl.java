package com.screenleads.backend.app.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

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
import com.screenleads.backend.app.web.mapper.AdviceMapper;

@Service
public class DeviceServiceImpl implements DeviceService {

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
    public List<DeviceDTO> getAllDevices() {
        return deviceRepository.findAll().stream()
                .map(this::convertToDTO)
                .sorted(Comparator.comparing(DeviceDTO::id))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<DeviceDTO> getDeviceById(Long id) {
        return deviceRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    public DeviceDTO saveDevice(DeviceDTO deviceDTO) {
        Device device = convertToEntity(deviceDTO);
        if (deviceRepository.existsByUuid(device.getUuid()))
            return convertToDTO(deviceRepository.findByUuid(device.getUuid()));
        Device savedDevice = deviceRepository.save(device);
        return convertToDTO(savedDevice);
    }

    @Override
    public DeviceDTO updateDevice(Long id, DeviceDTO deviceDTO) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        device.setUuid(deviceDTO.uuid());
        device.setDescriptionName(deviceDTO.descriptionName());
        device.setWidth(deviceDTO.width());
        device.setHeight(deviceDTO.height());
        device.setType(deviceTypeRepository.findById(deviceDTO.type().getId())
                .orElseThrow(() -> new RuntimeException("Device type not found")));

        if (deviceDTO.company() != null && deviceDTO.company().getId() != null) {
            Company company = companyRepository.findById(deviceDTO.company().getId())
                    .orElseThrow(() -> new RuntimeException("Company not found"));
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
    public List<AdviceDTO> getAdvicesForDevice(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        return device.getAdvices().stream()
                .sorted(Comparator.comparing(Advice::getId))
                .map(AdviceMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void assignAdviceToDevice(Long deviceId, Long adviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        Advice advice = adviceRepository.findById(adviceId)
                .orElseThrow(() -> new RuntimeException("Advice not found"));
        device.getAdvices().add(advice);
        deviceRepository.save(device);
    }

    @Override
    public void removeAdviceFromDevice(Long deviceId, Long adviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        Advice advice = adviceRepository.findById(adviceId)
                .orElseThrow(() -> new RuntimeException("Advice not found"));
        device.getAdvices().remove(advice);
        deviceRepository.save(device);
    }

    private DeviceDTO convertToDTO(Device device) {
        return new DeviceDTO(
                device.getId(),
                device.getUuid(),
                device.getDescriptionName(),
                device.getWidth(),
                device.getHeight(),
                device.getType(),
                device.getCompany()
        );
    }

    private Device convertToEntity(DeviceDTO deviceDTO) {
        Device device = new Device();
        device.setUuid(deviceDTO.uuid());
        device.setDescriptionName(deviceDTO.descriptionName());
        device.setWidth(deviceDTO.width());
        device.setHeight(deviceDTO.height());
        device.setType(deviceTypeRepository.findById(deviceDTO.type().getId())
                .orElseThrow(() -> new RuntimeException("Device type not found")));

        if (deviceDTO.company() != null && deviceDTO.company().getId() != null) {
            Company company = companyRepository.findById(deviceDTO.company().getId())
                    .orElseThrow(() -> new RuntimeException("Company not found"));
            device.setCompany(company);
        }

        return device;
    }
}
