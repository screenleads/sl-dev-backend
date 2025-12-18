package com.screenleads.backend.app.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
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
import com.screenleads.backend.app.web.dto.CompanyRefDTO;
import com.screenleads.backend.app.web.dto.DeviceDTO;
import com.screenleads.backend.app.web.dto.DeviceTypeDTO;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceServiceImpl Unit Tests")
class DeviceServiceImplTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceTypeRepository deviceTypeRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private AdviceRepository adviceRepository;

    @InjectMocks
    private DeviceServiceImpl deviceService;

    private Device testDevice;
    private DeviceType testDeviceType;
    private Company testCompany;

    @BeforeEach
    void setUp() {
        // Setup test DeviceType
        testDeviceType = new DeviceType();
        testDeviceType.setId(1L);
        testDeviceType.setType("TABLET");

        // Setup test Company
        testCompany = new Company();
        testCompany.setId(100L);
        testCompany.setName("Test Company");

        // Setup test Device
        testDevice = new Device();
        testDevice.setId(1L);
        testDevice.setUuid("device-uuid-123");
        testDevice.setDescriptionName("Test Device");
        testDevice.setWidth(1920);
        testDevice.setHeight(1080);
        testDevice.setType(testDeviceType);
        testDevice.setCompany(testCompany);
        testDevice.setAdvices(new HashSet<>());
    }

    @Nested
    @DisplayName("Get Operations")
    class GetOperations {

        @Test
        @DisplayName("getAllDevices should return all devices sorted by id")
        void whenGetAllDevices_thenReturnsAllSorted() {
            // Arrange
            Device device2 = new Device();
            device2.setId(2L);
            device2.setUuid("device-uuid-456");
            device2.setDescriptionName("Device 2");
            device2.setType(testDeviceType);
            device2.setAdvices(new HashSet<>());

            when(deviceRepository.findAll()).thenReturn(Arrays.asList(device2, testDevice));

            // Act
            List<DeviceDTO> result = deviceService.getAllDevices();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0).id()).isEqualTo(1L);
            assertThat(result.get(1).id()).isEqualTo(2L);
            verify(deviceRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("getAllDevices should return empty list when no devices exist")
        void whenGetAllDevicesEmpty_thenReturnsEmptyList() {
            // Arrange
            when(deviceRepository.findAll()).thenReturn(List.of());

            // Act
            List<DeviceDTO> result = deviceService.getAllDevices();

            // Assert
            assertThat(result).isEmpty();
            verify(deviceRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("getDeviceById should return device when found")
        void whenGetDeviceByIdExists_thenReturnsDevice() {
            // Arrange
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));

            // Act
            Optional<DeviceDTO> result = deviceService.getDeviceById(1L);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(1L);
            assertThat(result.get().uuid()).isEqualTo("device-uuid-123");
            verify(deviceRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("getDeviceById should return empty when not found")
        void whenGetDeviceByIdNotExists_thenReturnsEmpty() {
            // Arrange
            when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

            // Act
            Optional<DeviceDTO> result = deviceService.getDeviceById(999L);

            // Assert
            assertThat(result).isEmpty();
            verify(deviceRepository, times(1)).findById(999L);
        }

        @Test
        @DisplayName("getDeviceByUuid should return device when found")
        void whenGetDeviceByUuidExists_thenReturnsDevice() {
            // Arrange
            when(deviceRepository.findOptionalByUuid("device-uuid-123")).thenReturn(Optional.of(testDevice));

            // Act
            Optional<DeviceDTO> result = deviceService.getDeviceByUuid("device-uuid-123");

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().uuid()).isEqualTo("device-uuid-123");
            verify(deviceRepository, times(1)).findOptionalByUuid("device-uuid-123");
        }

        @Test
        @DisplayName("getDeviceByUuid should return empty when not found")
        void whenGetDeviceByUuidNotExists_thenReturnsEmpty() {
            // Arrange
            when(deviceRepository.findOptionalByUuid("non-existent")).thenReturn(Optional.empty());

            // Act
            Optional<DeviceDTO> result = deviceService.getDeviceByUuid("non-existent");

            // Assert
            assertThat(result).isEmpty();
            verify(deviceRepository, times(1)).findOptionalByUuid("non-existent");
        }
    }

    @Nested
    @DisplayName("Save Operations")
    class SaveOperations {

        @Test
        @DisplayName("saveDevice should create new device when UUID doesn't exist")
        void whenSaveDeviceNewUuid_thenCreatesDevice() {
            // Arrange
            DeviceTypeDTO typeDTO = new DeviceTypeDTO(1L, "TABLET", true);
            CompanyRefDTO companyDTO = new CompanyRefDTO(100L, "Test Company");
            DeviceDTO inputDTO = new DeviceDTO(null, "new-uuid", "New Device", 1920.0, 1080.0, typeDTO, companyDTO);

            when(deviceRepository.findOptionalByUuid("new-uuid")).thenReturn(Optional.empty());
            when(deviceTypeRepository.findById(1L)).thenReturn(Optional.of(testDeviceType));
            when(companyRepository.findById(100L)).thenReturn(Optional.of(testCompany));
            when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

            // Act
            DeviceDTO result = deviceService.saveDevice(inputDTO);

            // Assert
            assertThat(result).isNotNull();
            verify(deviceRepository, times(1)).findOptionalByUuid("new-uuid");
            verify(deviceTypeRepository, times(1)).findById(1L);
            verify(companyRepository, times(1)).findById(100L);
            verify(deviceRepository, times(1)).save(any(Device.class));
        }

        @Test
        @DisplayName("saveDevice should update existing device when UUID exists (upsert)")
        void whenSaveDeviceExistingUuid_thenUpdatesDevice() {
            // Arrange
            DeviceTypeDTO typeDTO = new DeviceTypeDTO(1L, "TABLET", true);
            CompanyRefDTO companyDTO = new CompanyRefDTO(100L, "Test Company");
            DeviceDTO inputDTO = new DeviceDTO(null, "device-uuid-123", "Updated Device", 2560.0, 1440.0, typeDTO, companyDTO);

            when(deviceRepository.findOptionalByUuid("device-uuid-123")).thenReturn(Optional.of(testDevice));
            when(deviceTypeRepository.findById(1L)).thenReturn(Optional.of(testDeviceType));
            when(companyRepository.findById(100L)).thenReturn(Optional.of(testCompany));
            when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

            // Act
            DeviceDTO result = deviceService.saveDevice(inputDTO);

            // Assert
            assertThat(result).isNotNull();
            verify(deviceRepository, times(1)).findOptionalByUuid("device-uuid-123");
            verify(deviceTypeRepository, times(1)).findById(1L);
            verify(deviceRepository, times(1)).save(any(Device.class));
        }

        @Test
        @DisplayName("saveDevice should throw exception when DeviceType not found")
        void whenSaveDeviceWithInvalidType_thenThrowsException() {
            // Arrange
            DeviceTypeDTO typeDTO = new DeviceTypeDTO(999L, "INVALID", true);
            DeviceDTO inputDTO = new DeviceDTO(null, "new-uuid", "New Device", 1920.0, 1080.0, typeDTO, null);

            when(deviceTypeRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> deviceService.saveDevice(inputDTO))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Device type not found");

            verify(deviceRepository, never()).save(any(Device.class));
        }

        @Test
        @DisplayName("saveDevice should throw exception when Company not found")
        void whenSaveDeviceWithInvalidCompany_thenThrowsException() {
            // Arrange
            DeviceTypeDTO typeDTO = new DeviceTypeDTO(1L, "TABLET", true);
            CompanyRefDTO companyDTO = new CompanyRefDTO(999L, "Invalid");
            DeviceDTO inputDTO = new DeviceDTO(null, "new-uuid", "New Device", 1920.0, 1080.0, typeDTO, companyDTO);

            when(deviceRepository.findOptionalByUuid("new-uuid")).thenReturn(Optional.empty());
            when(deviceTypeRepository.findById(1L)).thenReturn(Optional.of(testDeviceType));
            when(companyRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> deviceService.saveDevice(inputDTO))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Company not found");

            verify(deviceRepository, never()).save(any(Device.class));
        }

        @Test
        @DisplayName("saveDevice should set company to null when not provided")
        void whenSaveDeviceWithoutCompany_thenCompanyIsNull() {
            // Arrange
            DeviceTypeDTO typeDTO = new DeviceTypeDTO(1L, "TABLET", true);
            DeviceDTO inputDTO = new DeviceDTO(null, "new-uuid", "New Device", 1920.0, 1080.0, typeDTO, null);

            when(deviceRepository.findOptionalByUuid("new-uuid")).thenReturn(Optional.empty());
            when(deviceTypeRepository.findById(1L)).thenReturn(Optional.of(testDeviceType));
            when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

            // Act
            DeviceDTO result = deviceService.saveDevice(inputDTO);

            // Assert
            assertThat(result).isNotNull();
            verify(companyRepository, never()).findById(anyLong());
            verify(deviceRepository, times(1)).save(any(Device.class));
        }
    }

    @Nested
    @DisplayName("Update Operations")
    class UpdateOperations {

        @Test
        @DisplayName("updateDevice should update device successfully")
        void whenUpdateDeviceExists_thenUpdatesSuccessfully() {
            // Arrange
            DeviceTypeDTO typeDTO = new DeviceTypeDTO(1L, "TABLET", true);
            CompanyRefDTO companyDTO = new CompanyRefDTO(100L, "Updated Company");
            DeviceDTO updateDTO = new DeviceDTO(1L, "updated-uuid", "Updated Device", 2560.0, 1440.0, typeDTO, companyDTO);

            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
            when(deviceTypeRepository.findById(1L)).thenReturn(Optional.of(testDeviceType));
            when(companyRepository.findById(100L)).thenReturn(Optional.of(testCompany));
            when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

            // Act
            DeviceDTO result = deviceService.updateDevice(1L, updateDTO);

            // Assert
            assertThat(result).isNotNull();
            verify(deviceRepository, times(1)).findById(1L);
            verify(deviceRepository, times(1)).save(any(Device.class));
        }

        @Test
        @DisplayName("updateDevice should throw exception when device not found")
        void whenUpdateDeviceNotExists_thenThrowsException() {
            // Arrange
            DeviceTypeDTO typeDTO = new DeviceTypeDTO(1L, "TABLET", true);
            DeviceDTO updateDTO = new DeviceDTO(999L, "uuid", "Device", 1920.0, 1080.0, typeDTO, null);

            when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> deviceService.updateDevice(999L, updateDTO))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Device not found")
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);

            verify(deviceRepository, never()).save(any(Device.class));
        }

        @Test
        @DisplayName("updateDevice should throw exception when DeviceType is null")
        void whenUpdateDeviceWithNullType_thenThrowsException() {
            // Arrange
            DeviceDTO updateDTO = new DeviceDTO(1L, "uuid", "Device", 1920.0, 1080.0, null, null);

            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));

            // Act & Assert
            assertThatThrownBy(() -> deviceService.updateDevice(1L, updateDTO))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Device type is required")
                    .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

            verify(deviceRepository, never()).save(any(Device.class));
        }

        @Test
        @DisplayName("updateDevice should throw exception when DeviceType ID is null")
        void whenUpdateDeviceWithNullTypeId_thenThrowsException() {
            // Arrange
            DeviceTypeDTO typeDTO = new DeviceTypeDTO(null, "TABLET", true);
            DeviceDTO updateDTO = new DeviceDTO(1L, "uuid", "Device", 1920.0, 1080.0, typeDTO, null);

            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));

            // Act & Assert
            assertThatThrownBy(() -> deviceService.updateDevice(1L, updateDTO))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Device type is required");

            verify(deviceRepository, never()).save(any(Device.class));
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperations {

        @Test
        @DisplayName("deleteDevice should delete device by id")
        void whenDeleteDevice_thenDeletesSuccessfully() {
            // Act
            deviceService.deleteDevice(1L);

            // Assert
            verify(deviceRepository, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("deleteByUuid should delete device when found")
        void whenDeleteByUuidExists_thenDeletesDevice() {
            // Arrange
            when(deviceRepository.findOptionalByUuid("device-uuid-123")).thenReturn(Optional.of(testDevice));

            // Act
            deviceService.deleteByUuid("device-uuid-123");

            // Assert
            verify(deviceRepository, times(1)).findOptionalByUuid("device-uuid-123");
            verify(deviceRepository, times(1)).delete(testDevice);
        }

        @Test
        @DisplayName("deleteByUuid should do nothing when device not found")
        void whenDeleteByUuidNotExists_thenDoesNothing() {
            // Arrange
            when(deviceRepository.findOptionalByUuid("non-existent")).thenReturn(Optional.empty());

            // Act
            deviceService.deleteByUuid("non-existent");

            // Assert
            verify(deviceRepository, times(1)).findOptionalByUuid("non-existent");
            verify(deviceRepository, never()).delete(any(Device.class));
        }
    }

    @Nested
    @DisplayName("Advice Management")
    class AdviceManagement {

        @Test
        @DisplayName("getAdvicesForDevice should return all advices for device")
        void whenGetAdvicesForDeviceExists_thenReturnsAdvices() {
            // Arrange
            Advice advice1 = new Advice();
            advice1.setId(1L);
            Advice advice2 = new Advice();
            advice2.setId(2L);
            
            testDevice.getAdvices().add(advice1);
            testDevice.getAdvices().add(advice2);

            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));

            // Act
            List<AdviceDTO> result = deviceService.getAdvicesForDevice(1L);

            // Assert
            assertThat(result).hasSize(2);
            verify(deviceRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("getAdvicesForDevice should throw exception when device not found")
        void whenGetAdvicesForDeviceNotExists_thenThrowsException() {
            // Arrange
            when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> deviceService.getAdvicesForDevice(999L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Device not found");
        }

        @Test
        @DisplayName("assignAdviceToDevice should add advice to device")
        void whenAssignAdviceToDevice_thenAddsAdvice() {
            // Arrange
            Advice advice = new Advice();
            advice.setId(10L);

            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
            when(adviceRepository.findById(10L)).thenReturn(Optional.of(advice));
            when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

            // Act
            deviceService.assignAdviceToDevice(1L, 10L);

            // Assert
            verify(deviceRepository, times(1)).findById(1L);
            verify(adviceRepository, times(1)).findById(10L);
            verify(deviceRepository, times(1)).save(testDevice);
        }

        @Test
        @DisplayName("assignAdviceToDevice should throw exception when device not found")
        void whenAssignAdviceToDeviceNotExists_thenThrowsException() {
            // Arrange
            when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> deviceService.assignAdviceToDevice(999L, 10L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Device not found");

            verify(adviceRepository, never()).findById(anyLong());
            verify(deviceRepository, never()).save(any(Device.class));
        }

        @Test
        @DisplayName("assignAdviceToDevice should throw exception when advice not found")
        void whenAssignAdviceToDeviceAdviceNotExists_thenThrowsException() {
            // Arrange
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
            when(adviceRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> deviceService.assignAdviceToDevice(1L, 999L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Advice not found");

            verify(deviceRepository, never()).save(any(Device.class));
        }

        @Test
        @DisplayName("removeAdviceFromDevice should remove advice from device")
        void whenRemoveAdviceFromDevice_thenRemovesAdvice() {
            // Arrange
            Advice advice = new Advice();
            advice.setId(10L);
            testDevice.getAdvices().add(advice);

            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
            when(adviceRepository.findById(10L)).thenReturn(Optional.of(advice));
            when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

            // Act
            deviceService.removeAdviceFromDevice(1L, 10L);

            // Assert
            verify(deviceRepository, times(1)).findById(1L);
            verify(adviceRepository, times(1)).findById(10L);
            verify(deviceRepository, times(1)).save(testDevice);
        }

        @Test
        @DisplayName("removeAdviceFromDevice should throw exception when device not found")
        void whenRemoveAdviceFromDeviceNotExists_thenThrowsException() {
            // Arrange
            when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> deviceService.removeAdviceFromDevice(999L, 10L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Device not found");

            verify(deviceRepository, never()).save(any(Device.class));
        }

        @Test
        @DisplayName("removeAdviceFromDevice should throw exception when advice not found")
        void whenRemoveAdviceFromDeviceAdviceNotExists_thenThrowsException() {
            // Arrange
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice));
            when(adviceRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> deviceService.removeAdviceFromDevice(1L, 999L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Advice not found");

            verify(deviceRepository, never()).save(any(Device.class));
        }
    }
}
