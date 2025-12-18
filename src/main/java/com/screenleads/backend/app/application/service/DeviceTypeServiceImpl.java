package com.screenleads.backend.app.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.screenleads.backend.app.domain.model.DeviceType;
import com.screenleads.backend.app.domain.repositories.DeviceTypeRepository;

import com.screenleads.backend.app.web.dto.DeviceTypeDTO;

@Service
public class DeviceTypeServiceImpl implements DeviceTypeService {

    private final DeviceTypeRepository deviceTypeRepository;

    public DeviceTypeServiceImpl(DeviceTypeRepository deviceTypeRepository) {
        this.deviceTypeRepository = deviceTypeRepository;
    }

    @Override
    public List<DeviceTypeDTO> getAllDeviceTypes() {
        return deviceTypeRepository.findAll().stream()
                .map(this::convertToDTO)
                .sorted(Comparator.comparing(DeviceTypeDTO::id))
                .toList();
    }

    @Override
    public Optional<DeviceTypeDTO> getDeviceTypeById(Long id) {
        return deviceTypeRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    public DeviceTypeDTO saveDeviceType(DeviceTypeDTO deviceTypeDTO) {
        DeviceType deviceType = convertToEntity(deviceTypeDTO);
        Optional<DeviceType> exist = deviceTypeRepository.findByType(deviceType.getType());
        if (exist.isPresent())
            return convertToDTO(exist.get());
        DeviceType savedDeviceType = deviceTypeRepository.save(deviceType);
        return convertToDTO(savedDeviceType);
    }

    @Override
    public DeviceTypeDTO updateDeviceType(Long id, DeviceTypeDTO deviceTypeDTO) {
        DeviceType deviceType = deviceTypeRepository.findById(id).orElseThrow();
        deviceType.setType(deviceTypeDTO.type());
        deviceType.setEnabled(deviceTypeDTO.enabled());

        DeviceType updatedDeviceType = deviceTypeRepository.save(deviceType);
        return convertToDTO(updatedDeviceType);
    }

    @Override
    public void deleteDeviceType(Long id) {
        deviceTypeRepository.deleteById(id);
    }

    // Convert DeviceType Entity to DeviceTypeDTO
    private DeviceTypeDTO convertToDTO(DeviceType deviceType) {
        return new DeviceTypeDTO(deviceType.getId(), deviceType.getType(), deviceType.getEnabled());
    }

    // Convert DeviceTypeDTO to DeviceType Entity
    private DeviceType convertToEntity(DeviceTypeDTO deviceTypeDTO) {
        DeviceType deviceType = new DeviceType();
        deviceType.setId(deviceTypeDTO.id());
        deviceType.setType(deviceTypeDTO.type());
        deviceType.setEnabled(deviceTypeDTO.enabled());
        return deviceType;
    }
}
