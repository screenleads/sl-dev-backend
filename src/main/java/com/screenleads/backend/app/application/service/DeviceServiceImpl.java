package com.screenleads.backend.app.application.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.screenleads.backend.app.domain.model.Device;
import com.screenleads.backend.app.domain.model.DeviceType;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.DeviceRepository;
import com.screenleads.backend.app.domain.repositories.DeviceTypeRepository;
import com.screenleads.backend.app.web.dto.DeviceDTO;

@Service
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceTypeRepository deviceTypeRepository;
    private final CompanyRepository companyRepository;

    public DeviceServiceImpl(DeviceRepository deviceRepository, DeviceTypeRepository deviceTypeRepository,
            CompanyRepository companyRepository) {
        this.deviceRepository = deviceRepository;
        this.deviceTypeRepository = deviceTypeRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    public List<DeviceDTO> getAllDevices() {
        return deviceRepository.findAll().stream()
                .map(this::convertToDTO)
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
        DeviceType type = null;
        Optional<DeviceType> aux = deviceTypeRepository.findById(deviceDTO.type().getId());
        if (aux.isPresent()) {
            type = aux.get();
        }
        Device device = deviceRepository.findById(id).orElseThrow();
        device.setUuid(deviceDTO.uuid());
        device.setDescriptionName(deviceDTO.descriptionName());
        device.setWidth(deviceDTO.width());
        device.setHeight(deviceDTO.height());
        device.setType(type);
        Device updatedDevice = deviceRepository.save(device);
        return convertToDTO(updatedDevice);
    }

    @Override
    public void deleteDevice(Long id) {
        deviceRepository.deleteById(id);
    }

    // Convert Device Entity to DeviceDTO
    private DeviceDTO convertToDTO(Device Device) {
        DeviceType type = null;
        Optional<DeviceType> aux = deviceTypeRepository.findById(Device.getType().getId());
        if (aux.isPresent()) {
            type = aux.get();
        }
        return new DeviceDTO(Device.getId(), Device.getUuid(), Device.getDescriptionName(), Device.getWidth(),
                Device.getHeight(), type);
    }

    // Convert DeviceDTO to Device Entity
    private Device convertToEntity(DeviceDTO DeviceDTO) {
        Device device = new Device();
        device.setUuid(DeviceDTO.uuid());
        device.setDescriptionName(DeviceDTO.descriptionName());
        device.setWidth(DeviceDTO.width());
        device.setHeight(DeviceDTO.height());
        device.setType(deviceTypeRepository.findById(DeviceDTO.type().getId()).get());

        return device;
    }
}
