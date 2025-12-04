package com.screenleads.backend.app.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

/**
 * Unit tests for DeviceServiceImpl.
 * 
 * Testing:
 * - CRUD operations for devices
 * - UUID-based operations (upsert idempotency)
 * - Device-Advice relationship management
 * - Error handling and validation
 */
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

    private DeviceType testDeviceType;
    private Company testCompany;
    private Device testDevice1;
    private Device testDevice2;
    private Advice testAdvice;

    @BeforeEach
    void setUp() {
        // Setup device type
        testDeviceType = new DeviceType();
        testDeviceType.setId(1L);
        testDeviceType.setType("TABLET");
        testDeviceType.setEnabled(true);

        // Setup company
        testCompany = new Company();
        testCompany.setId(1L);
        testCompany.setName("Test Company");

        // Setup devices
        testDevice1 = createDevice(1L, "uuid-123", "Device 1", 1920, 1080, testDeviceType, testCompany);
        testDevice2 = createDevice(2L, "uuid-456", "Device 2", 1280, 720, testDeviceType, testCompany);

        // Setup advice
        testAdvice = new Advice();
        testAdvice.setId(1L);
        testAdvice.setDescription("Test Advice");
    }

    @Nested
    @DisplayName("Get All Devices Tests")
    class GetAllDevicesTests {

        @Test
        @DisplayName("Should return all devices sorted by ID")
        void whenGetAllDevices_thenReturnSortedList() {
            // Given
            when(deviceRepository.findAll()).thenReturn(Arrays.asList(testDevice2, testDevice1));

            // When
            List<DeviceDTO> result = deviceService.getAllDevices();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).id()).isEqualTo(1L);
            assertThat(result.get(1).id()).isEqualTo(2L);
            verify(deviceRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no devices exist")
        void whenGetAllDevices_withNoDevices_thenReturnEmptyList() {
            // Given
            when(deviceRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<DeviceDTO> result = deviceService.getAllDevices();

            // Then
            assertThat(result).isEmpty();
            verify(deviceRepository).findAll();
        }
    }

    @Nested
    @DisplayName("Get Device By ID Tests")
    class GetDeviceByIdTests {

        @Test
        @DisplayName("Should return device when ID exists")
        void whenGetDeviceById_withValidId_thenReturnDevice() {
            // Given
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice1));

            // When
            Optional<DeviceDTO> result = deviceService.getDeviceById(1L);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(1L);
            assertThat(result.get().uuid()).isEqualTo("uuid-123");
            verify(deviceRepository).findById(1L);
        }

        @Test
        @DisplayName("Should return empty when ID does not exist")
        void whenGetDeviceById_withInvalidId_thenReturnEmpty() {
            // Given
            when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            Optional<DeviceDTO> result = deviceService.getDeviceById(999L);

            // Then
            assertThat(result).isEmpty();
            verify(deviceRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("Get Device By UUID Tests")
    class GetDeviceByUuidTests {

        @Test
        @DisplayName("Should return device when UUID exists")
        void whenGetDeviceByUuid_withValidUuid_thenReturnDevice() {
            // Given
            when(deviceRepository.findOptionalByUuid("uuid-123")).thenReturn(Optional.of(testDevice1));

            // When
            Optional<DeviceDTO> result = deviceService.getDeviceByUuid("uuid-123");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().uuid()).isEqualTo("uuid-123");
            verify(deviceRepository).findOptionalByUuid("uuid-123");
        }

        @Test
        @DisplayName("Should return empty when UUID does not exist")
        void whenGetDeviceByUuid_withInvalidUuid_thenReturnEmpty() {
            // Given
            when(deviceRepository.findOptionalByUuid("invalid-uuid")).thenReturn(Optional.empty());

            // When
            Optional<DeviceDTO> result = deviceService.getDeviceByUuid("invalid-uuid");

            // Then
            assertThat(result).isEmpty();
            verify(deviceRepository).findOptionalByUuid("invalid-uuid");
        }
    }

    @Nested
    @DisplayName("Save Device Tests")
    class SaveDeviceTests {

        @Test
        @DisplayName("Should save new device (insert)")
        void whenSaveDevice_withNewUuid_thenInsertDevice() {
            // Given
            DeviceTypeDTO typeDTO = new DeviceTypeDTO(1L, "TABLET", true);
            CompanyRefDTO companyDTO = new CompanyRefDTO(1L, "Test Company");
            DeviceDTO inputDTO = new DeviceDTO(null, "new-uuid", "New Device", 1920L, 1080L, typeDTO, companyDTO);

            Device savedDevice = createDevice(3L, "new-uuid", "New Device", 1920, 1080, testDeviceType, testCompany);

            when(deviceRepository.findOptionalByUuid("new-uuid")).thenReturn(Optional.empty());
            when(deviceTypeRepository.findById(1L)).thenReturn(Optional.of(testDeviceType));
            when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
            when(deviceRepository.save(any(Device.class))).thenReturn(savedDevice);

            // When
            DeviceDTO result = deviceService.saveDevice(inputDTO);

            // Then
            assertThat(result.id()).isEqualTo(3L);
            assertThat(result.uuid()).isEqualTo("new-uuid");
            verify(deviceRepository).findOptionalByUuid("new-uuid");
            verify(deviceTypeRepository).findById(1L);
            verify(companyRepository).findById(1L);
            verify(deviceRepository).save(any(Device.class));
        }

        @Test
        @DisplayName("Should update existing device (upsert)")
        void whenSaveDevice_withExistingUuid_thenUpdateDevice() {
            // Given
            DeviceTypeDTO typeDTO = new DeviceTypeDTO(1L, "TABLET", true);
            CompanyRefDTO companyDTO = new CompanyRefDTO(1L, "Test Company");
            DeviceDTO inputDTO = new DeviceDTO(null, "uuid-123", "Updated Name", 2560L, 1440L, typeDTO, companyDTO);

            Device updatedDevice = createDevice(1L, "uuid-123", "Updated Name", 2560, 1440, testDeviceType, testCompany);

            when(deviceRepository.findOptionalByUuid("uuid-123")).thenReturn(Optional.of(testDevice1));
            when(deviceTypeRepository.findById(1L)).thenReturn(Optional.of(testDeviceType));
            when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
            when(deviceRepository.save(any(Device.class))).thenReturn(updatedDevice);

            // When
            DeviceDTO result = deviceService.saveDevice(inputDTO);

            // Then
            assertThat(result.uuid()).isEqualTo("uuid-123");
            assertThat(result.descriptionName()).isEqualTo("Updated Name");
            verify(deviceRepository).findOptionalByUuid("uuid-123");
            verify(deviceTypeRepository).findById(1L);
            verify(deviceRepository).save(any(Device.class));
        }

        @Test
        @DisplayName("Should throw exception when device type not found")
        void whenSaveDevice_withInvalidDeviceType_thenThrowException() {
            // Given
            DeviceTypeDTO typeDTO = new DeviceTypeDTO(999L, "INVALID", true);
            DeviceDTO inputDTO = new DeviceDTO(null, "new-uuid", "New Device", 1920L, 1080L, typeDTO, null);

            lenient().when(deviceRepository.findOptionalByUuid("new-uuid")).thenReturn(Optional.empty());
            lenient().when(deviceTypeRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> deviceService.saveDevice(inputDTO))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Device type not found");
        }

        @Test
        @DisplayName("Should throw exception when company not found")
        void whenSaveDevice_withInvalidCompany_thenThrowException() {
            // Given
            DeviceTypeDTO typeDTO = new DeviceTypeDTO(1L, "TABLET", true);
            CompanyRefDTO companyDTO = new CompanyRefDTO(999L, "Invalid");
            DeviceDTO inputDTO = new DeviceDTO(null, "new-uuid", "New Device", 1920L, 1080L, typeDTO, companyDTO);

            when(deviceRepository.findOptionalByUuid("new-uuid")).thenReturn(Optional.empty());
            when(deviceTypeRepository.findById(1L)).thenReturn(Optional.of(testDeviceType));
            when(companyRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> deviceService.saveDevice(inputDTO))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Company not found");

            verify(deviceRepository).findOptionalByUuid("new-uuid");
            verify(deviceTypeRepository).findById(1L);
            verify(companyRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("Update Device Tests")
    class UpdateDeviceTests {

        @Test
        @DisplayName("Should update device with valid data")
        void whenUpdateDevice_withValidData_thenReturnUpdatedDevice() {
            // Given
            DeviceTypeDTO typeDTO = new DeviceTypeDTO(1L, "TABLET", true);
            DeviceDTO updateDTO = new DeviceDTO(1L, "uuid-123", "Updated", 2560L, 1440L, typeDTO, null);

            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice1));
            when(deviceTypeRepository.findById(1L)).thenReturn(Optional.of(testDeviceType));
            when(deviceRepository.save(any(Device.class))).thenReturn(testDevice1);

            // When
            DeviceDTO result = deviceService.updateDevice(1L, updateDTO);

            // Then
            assertThat(result).isNotNull();
            verify(deviceRepository).findById(1L);
            verify(deviceTypeRepository).findById(1L);
            verify(deviceRepository).save(any(Device.class));
        }

        @Test
        @DisplayName("Should throw exception when device not found")
        void whenUpdateDevice_withInvalidId_thenThrowException() {
            // Given
            DeviceTypeDTO typeDTO = new DeviceTypeDTO(1L, "TABLET", true);
            DeviceDTO updateDTO = new DeviceDTO(999L, "uuid-999", "Updated", 1920L, 1080L, typeDTO, null);

            when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> deviceService.updateDevice(999L, updateDTO))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Device not found");

            verify(deviceRepository).findById(999L);
        }

        @Test
        @DisplayName("Should throw exception when device type is null")
        void whenUpdateDevice_withNullDeviceType_thenThrowException() {
            // Given
            DeviceDTO updateDTO = new DeviceDTO(1L, "uuid-123", "Updated", 1920L, 1080L, null, null);

            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice1));

            // When/Then
            assertThatThrownBy(() -> deviceService.updateDevice(1L, updateDTO))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Device type is required");

            verify(deviceRepository).findById(1L);
        }
    }

    @Nested
    @DisplayName("Delete Device Tests")
    class DeleteDeviceTests {

        @Test
        @DisplayName("Should delete device by ID")
        void whenDeleteDevice_withValidId_thenDeviceIsDeleted() {
            // Given
            doNothing().when(deviceRepository).deleteById(1L);

            // When
            deviceService.deleteDevice(1L);

            // Then
            verify(deviceRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should delete device by UUID")
        void whenDeleteByUuid_withValidUuid_thenDeviceIsDeleted() {
            // Given
            when(deviceRepository.findOptionalByUuid("uuid-123")).thenReturn(Optional.of(testDevice1));
            doNothing().when(deviceRepository).delete(testDevice1);

            // When
            deviceService.deleteByUuid("uuid-123");

            // Then
            verify(deviceRepository).findOptionalByUuid("uuid-123");
            verify(deviceRepository).delete(testDevice1);
        }

        @Test
        @DisplayName("Should not throw when deleting non-existent UUID")
        void whenDeleteByUuid_withInvalidUuid_thenNoException() {
            // Given
            when(deviceRepository.findOptionalByUuid("invalid-uuid")).thenReturn(Optional.empty());

            // When
            deviceService.deleteByUuid("invalid-uuid");

            // Then
            verify(deviceRepository).findOptionalByUuid("invalid-uuid");
            verify(deviceRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Device-Advice Relationship Tests")
    class DeviceAdviceRelationshipTests {

        @Test
        @DisplayName("Should return advices for device")
        void whenGetAdvicesForDevice_thenReturnAdvicesList() {
            // Given
            testDevice1.setAdvices(new HashSet<>(Arrays.asList(testAdvice)));
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice1));

            // When
            List<AdviceDTO> result = deviceService.getAdvicesForDevice(1L);

            // Then
            assertThat(result).hasSize(1);
            verify(deviceRepository).findById(1L);
        }

        @Test
        @DisplayName("Should assign advice to device")
        void whenAssignAdviceToDevice_thenAdviceIsAssigned() {
            // Given
            testDevice1.setAdvices(new HashSet<>());
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice1));
            when(adviceRepository.findById(1L)).thenReturn(Optional.of(testAdvice));
            when(deviceRepository.save(any(Device.class))).thenReturn(testDevice1);

            // When
            deviceService.assignAdviceToDevice(1L, 1L);

            // Then
            assertThat(testDevice1.getAdvices()).contains(testAdvice);
            verify(deviceRepository).findById(1L);
            verify(adviceRepository).findById(1L);
            verify(deviceRepository).save(testDevice1);
        }

        @Test
        @DisplayName("Should remove advice from device")
        void whenRemoveAdviceFromDevice_thenAdviceIsRemoved() {
            // Given
            testDevice1.setAdvices(new HashSet<>(Arrays.asList(testAdvice)));
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice1));
            when(adviceRepository.findById(1L)).thenReturn(Optional.of(testAdvice));
            when(deviceRepository.save(any(Device.class))).thenReturn(testDevice1);

            // When
            deviceService.removeAdviceFromDevice(1L, 1L);

            // Then
            assertThat(testDevice1.getAdvices()).doesNotContain(testAdvice);
            verify(deviceRepository).findById(1L);
            verify(adviceRepository).findById(1L);
            verify(deviceRepository).save(testDevice1);
        }

        @Test
        @DisplayName("Should throw exception when device not found for advice assignment")
        void whenAssignAdviceToDevice_withInvalidDeviceId_thenThrowException() {
            // Given
            when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> deviceService.assignAdviceToDevice(999L, 1L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Device not found");

            verify(deviceRepository).findById(999L);
        }

        @Test
        @DisplayName("Should throw exception when advice not found for assignment")
        void whenAssignAdviceToDevice_withInvalidAdviceId_thenThrowException() {
            // Given
            testDevice1.setAdvices(new HashSet<>());
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(testDevice1));
            when(adviceRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> deviceService.assignAdviceToDevice(1L, 999L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Advice not found");

            verify(deviceRepository).findById(1L);
            verify(adviceRepository).findById(999L);
        }
    }

    // Helper methods
    private Device createDevice(Long id, String uuid, String name, Integer width, Integer height,
            DeviceType type, Company company) {
        Device device = new Device();
        device.setId(id);
        device.setUuid(uuid);
        device.setDescriptionName(name);
        device.setWidth(width);
        device.setHeight(height);
        device.setType(type);
        device.setCompany(company);
        device.setAdvices(new HashSet<>());
        return device;
    }
}
