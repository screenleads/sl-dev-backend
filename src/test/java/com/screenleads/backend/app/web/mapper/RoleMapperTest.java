package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.web.dto.RoleDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RoleMapper Unit Tests")
class RoleMapperTest {

    @Test
    @DisplayName("toDTO should convert Role entity to RoleDTO")
    void whenToDTO_thenConvertAllFields() {
        // Arrange
        Role role = Role.builder()
                .id(1L)
                .role("ADMIN")
                .description("Administrator role")
                .level(10)
                .build();

        // Act
        RoleDTO result = RoleMapper.toDTO(role);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.role()).isEqualTo("ADMIN");
        assertThat(result.description()).isEqualTo("Administrator role");
        assertThat(result.level()).isEqualTo(10);
    }

    @Test
    @DisplayName("toDTO should return null when Role is null")
    void whenToDTOWithNull_thenReturnNull() {
        // Act
        RoleDTO result = RoleMapper.toDTO(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toEntity should convert RoleDTO to Role entity")
    void whenToEntity_thenConvertAllFields() {
        // Arrange
        RoleDTO dto = new RoleDTO(2L, "USER", "Regular user role", 5);

        // Act
        Role result = RoleMapper.toEntity(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getRole()).isEqualTo("USER");
        assertThat(result.getDescription()).isEqualTo("Regular user role");
        assertThat(result.getLevel()).isEqualTo(5);
    }

    @Test
    @DisplayName("toEntity should return null when RoleDTO is null")
    void whenToEntityWithNull_thenReturnNull() {
        // Act
        Role result = RoleMapper.toEntity(null);

        // Assert
        assertThat(result).isNull();
    }
}
