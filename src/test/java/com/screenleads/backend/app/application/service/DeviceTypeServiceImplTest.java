package com.screenleads.backend.app.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.screenleads.backend.app.domain.model.DeviceType;
import com.screenleads.backend.app.domain.repositories.DeviceTypeRepository;
import com.screenleads.backend.app.web.dto.DeviceTypeDTO;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceTypeServiceImpl Unit Tests")
class DeviceTypeServiceImplTest {

    @Mock
    private DeviceTypeRepository deviceTypeRepository;

    @InjectMocks
    private DeviceTypeServiceImpl deviceTypeService;

    private DeviceType testDeviceType;

    @BeforeEach
    void setUp() {
        testDeviceType = new DeviceType();
        testDeviceType.setId(1L);
        testDeviceType.setType("TABLET");
        testDeviceType.setEnabled(true);

        testDeviceTypeDTO = new DeviceTypeDTO(1L, "TABLET", true);
    }

    @Test
    @DisplayName("getAllDeviceTypes should return all device types sorted by id")
    void whenGetAllDeviceTypes_thenReturnsAllSorted() {
        // Arrange
        DeviceType type2 = new DeviceType();
        type2.setId(3L);
        type2.setType("SMARTPHONE");
        type2.setEnabled(true);

        DeviceType type3 = new DeviceType();
        type3.setId(2L);
        type3.setType("DESKTOP");
        type3.setEnabled(true);

        when(deviceTypeRepository.findAll()).thenReturn(Arrays.asList(type2, testDeviceType, type3));

        // Act
        List<DeviceTypeDTO> result = deviceTypeService.getAllDeviceTypes();

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(2).id()).isEqualTo(3L);
        verify(deviceTypeRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllDeviceTypes should return empty list when no types exist")
    void whenGetAllDeviceTypesEmpty_thenReturnsEmptyList() {
        // Arrange
        when(deviceTypeRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<DeviceTypeDTO> result = deviceTypeService.getAllDeviceTypes();

        // Assert
        assertThat(result).isEmpty();
        verify(deviceTypeRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getDeviceTypeById should return device type when found")
    void whenGetDeviceTypeByIdExists_thenReturnsDeviceType() {
        // Arrange
        when(deviceTypeRepository.findById(1L)).thenReturn(Optional.of(testDeviceType));

        // Act
        Optional<DeviceTypeDTO> result = deviceTypeService.getDeviceTypeById(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(1L);
        assertThat(result.get().type()).isEqualTo("TABLET");
        assertThat(result.get().enabled()).isTrue();
        verify(deviceTypeRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getDeviceTypeById should return empty when not found")
    void whenGetDeviceTypeByIdNotExists_thenReturnsEmpty() {
        // Arrange
        when(deviceTypeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<DeviceTypeDTO> result = deviceTypeService.getDeviceTypeById(999L);

        // Assert
        assertThat(result).isEmpty();
        verify(deviceTypeRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("saveDeviceType should create new device type when type doesn't exist")
    void whenSaveDeviceTypeNew_thenCreatesNew() {
        // Arrange
        DeviceTypeDTO inputDTO = new DeviceTypeDTO(null, "LAPTOP", true);
        DeviceType savedType = new DeviceType();
        savedType.setId(10L);
        savedType.setType("LAPTOP");
        savedType.setEnabled(true);

        when(deviceTypeRepository.findByType("LAPTOP")).thenReturn(Optional.empty());
        when(deviceTypeRepository.save(any(DeviceType.class))).thenReturn(savedType);

        // Act
        DeviceTypeDTO result = deviceTypeService.saveDeviceType(inputDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.type()).isEqualTo("LAPTOP");
        verify(deviceTypeRepository, times(1)).findByType("LAPTOP");
        verify(deviceTypeRepository, times(1)).save(any(DeviceType.class));
    }

    @Test
    @DisplayName("saveDeviceType should return existing when type already exists")
    void whenSaveDeviceTypeWithExistingType_thenReturnsExisting() {
        // Arrange
        DeviceTypeDTO inputDTO = new DeviceTypeDTO(null, "TABLET", true);

        when(deviceTypeRepository.findByType("TABLET")).thenReturn(Optional.of(testDeviceType));

        // Act
        DeviceTypeDTO result = deviceTypeService.saveDeviceType(inputDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.type()).isEqualTo("TABLET");
        verify(deviceTypeRepository, times(1)).findByType("TABLET");
        verify(deviceTypeRepository, never()).save(any(DeviceType.class));
    }

    @Test
    @DisplayName("updateDeviceType should update device type successfully")
    void whenUpdateDeviceTypeExists_thenUpdatesSuccessfully() {
        // Arrange
        DeviceTypeDTO updateDTO = new DeviceTypeDTO(1L, "SMARTPHONE", false);
        DeviceType updatedType = new DeviceType();
        updatedType.setId(1L);
        updatedType.setType("SMARTPHONE");
        updatedType.setEnabled(false);

        when(deviceTypeRepository.findById(1L)).thenReturn(Optional.of(testDeviceType));
        when(deviceTypeRepository.save(any(DeviceType.class))).thenReturn(updatedType);

        // Act
        DeviceTypeDTO result = deviceTypeService.updateDeviceType(1L, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.type()).isEqualTo("SMARTPHONE");
        assertThat(result.enabled()).isFalse();
        verify(deviceTypeRepository, times(1)).findById(1L);
        verify(deviceTypeRepository, times(1)).save(any(DeviceType.class));
    }

    @Test
    @DisplayName("updateDeviceType should throw exception when device type not found")
    void whenUpdateDeviceTypeNotExists_thenThrowsException() {
        // Arrange
        DeviceTypeDTO updateDTO = new DeviceTypeDTO(999L, "TABLET", true);
        when(deviceTypeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> deviceTypeService.updateDeviceType(999L, updateDTO))
                .isInstanceOf(NoSuchElementException.class);

        verify(deviceTypeRepository, times(1)).findById(999L);
        verify(deviceTypeRepository, never()).save(any(DeviceType.class));
    }

    @Test
    @DisplayName("deleteDeviceType should delete device type by id")
    void whenDeleteDeviceType_thenDeletesSuccessfully() {
        // Act
        deviceTypeService.deleteDeviceType(1L);

        // Assert
        verify(deviceTypeRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("deleteDeviceType should call repository even if type doesn't exist")
    void whenDeleteNonExistentDeviceType_thenCallsRepository() {
        // Act
        deviceTypeService.deleteDeviceType(999L);

        // Assert
        verify(deviceTypeRepository, times(1)).deleteById(999L);
    }
}
