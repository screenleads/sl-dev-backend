package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Device;
import com.screenleads.backend.app.domain.model.DeviceType;
import com.screenleads.backend.app.web.dto.CompanyRefDTO;
import com.screenleads.backend.app.web.dto.DeviceDTO;
import com.screenleads.backend.app.web.dto.DeviceTypeDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DeviceMapper Unit Tests")
class DeviceMapperTest {

    @Test
    @DisplayName("toDTO should convert Device entity to DeviceDTO with all fields")
    void whenToDTO_thenConvertAllFields() {
        // Arrange
        DeviceType deviceType = new DeviceType();
        deviceType.setId(1L);
        deviceType.setType("Screen");
        deviceType.setEnabled(true);

        Company company = new Company();
        company.setId(10L);
        company.setName("Test Company");

        Device device = new Device();
        device.setId(100L);
        device.setUuid("uuid-123-456");
        device.setDescriptionName("Main Display");
        device.setWidth(1920);
        device.setHeight(1080);
        device.setType(deviceType);
        device.setCompany(company);

        // Act
        DeviceDTO result = DeviceMapper.toDTO(device);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.uuid()).isEqualTo("uuid-123-456");
        assertThat(result.descriptionName()).isEqualTo("Main Display");
        assertThat(result.width()).isEqualTo(1920);
        assertThat(result.height()).isEqualTo(1080);
        
        assertThat(result.type()).isNotNull();
        assertThat(result.type().id()).isEqualTo(1L);
        assertThat(result.type().type()).isEqualTo("Screen");
        assertThat(result.type().enabled()).isTrue();
        
        assertThat(result.company()).isNotNull();
        assertThat(result.company().id()).isEqualTo(10L);
        assertThat(result.company().name()).isEqualTo("Test Company");
    }

    @Test
    @DisplayName("toDTO should handle null DeviceType")
    void whenToDTOWithNullType_thenTypeIsNull() {
        // Arrange
        Device device = new Device();
        device.setId(1L);
        device.setUuid("test-uuid");
        device.setDescriptionName("Test Device");
        device.setType(null);

        // Act
        DeviceDTO result = DeviceMapper.toDTO(device);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.type()).isNull();
    }

    @Test
    @DisplayName("toDTO should handle null Company")
    void whenToDTOWithNullCompany_thenCompanyIsNull() {
        // Arrange
        Device device = new Device();
        device.setId(1L);
        device.setUuid("test-uuid");
        device.setDescriptionName("Test Device");
        device.setCompany(null);

        // Act
        DeviceDTO result = DeviceMapper.toDTO(device);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.company()).isNull();
    }

    @Test
    @DisplayName("toDeviceTypeDTO should convert DeviceType to DeviceTypeDTO")
    void whenToDeviceTypeDTO_thenConvertCorrectly() {
        // Arrange
        DeviceType deviceType = new DeviceType();
        deviceType.setId(5L);
        deviceType.setType("Tablet");
        deviceType.setEnabled(false);

        // Act
        DeviceTypeDTO result = DeviceMapper.toDeviceTypeDTO(deviceType);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.type()).isEqualTo("Tablet");
        assertThat(result.enabled()).isFalse();
    }

    @Test
    @DisplayName("toDeviceTypeDTO should return null when input is null")
    void whenToDeviceTypeDTOWithNull_thenReturnNull() {
        // Act
        DeviceTypeDTO result = DeviceMapper.toDeviceTypeDTO(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toCompanyRefDTO should convert Company to CompanyRefDTO")
    void whenToCompanyRefDTO_thenConvertCorrectly() {
        // Arrange
        Company company = new Company();
        company.setId(20L);
        company.setName("Acme Corporation");

        // Act
        CompanyRefDTO result = DeviceMapper.toCompanyRefDTO(company);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(20L);
        assertThat(result.name()).isEqualTo("Acme Corporation");
    }

    @Test
    @DisplayName("toCompanyRefDTO should return null when input is null")
    void whenToCompanyRefDTOWithNull_thenReturnNull() {
        // Act
        CompanyRefDTO result = DeviceMapper.toCompanyRefDTO(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toDTO should handle Device with minimal fields")
    void whenToDTOWithMinimalFields_thenConvertSuccessfully() {
        // Arrange
        Device device = new Device();
        device.setId(1L);
        device.setUuid("minimal-uuid");
        device.setDescriptionName("Minimal Device");

        // Act
        DeviceDTO result = DeviceMapper.toDTO(device);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.uuid()).isEqualTo("minimal-uuid");
        assertThat(result.descriptionName()).isEqualTo("Minimal Device");
        assertThat(result.width()).isNull();
        assertThat(result.height()).isNull();
        assertThat(result.type()).isNull();
        assertThat(result.company()).isNull();
    }
}
