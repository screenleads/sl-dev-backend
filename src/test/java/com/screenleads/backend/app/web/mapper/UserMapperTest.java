package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.domain.model.MediaType;
import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.web.dto.UserDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserMapper Unit Tests")
class UserMapperTest {

    @Test
    @DisplayName("toDto should convert User entity to UserDto with all fields")
    void whenToDto_thenConvertAllFields() {
        // Arrange
        MediaType mediaType = new MediaType();
        mediaType.setId(1L);
        mediaType.setExtension("jpg");
        mediaType.setType("image");
        mediaType.setEnabled(true);

        Media profileImage = new Media();
        profileImage.setId(10L);
        profileImage.setSrc("profile.jpg");
        profileImage.setType(mediaType);
        profileImage.setCreatedAt(java.time.Instant.parse("2025-01-01T10:00:00Z"));
        profileImage.setUpdatedAt(java.time.Instant.parse("2025-01-02T10:00:00Z"));

        Role role = Role.builder()
                .id(5L)
                .role("ADMIN")
                .description("Administrator")
                .level(10)
                .build();

        Company company = new Company();
        company.setId(20L);
        company.setName("Test Company");

        User user = new User();
        user.setId(100L);
        user.setUsername("johndoe");
        user.setEmail("john@example.com");
        user.setName("John");
        user.setLastName("Doe");
        user.setCompany(company);
        user.setProfileImage(profileImage);
        user.setRole(role);

        // Act
        UserDto result = UserMapper.toDto(user);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getUsername()).isEqualTo("johndoe");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(result.getName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        
        assertThat(result.getCompanyId()).isEqualTo(20L);
        assertThat(result.getCompany()).isNotNull();
        assertThat(result.getCompany().id()).isEqualTo(20L);
        assertThat(result.getCompany().name()).isEqualTo("Test Company");
        
        assertThat(result.getProfileImage()).isNotNull();
        assertThat(result.getProfileImage().id()).isEqualTo(10L);
        assertThat(result.getProfileImage().src()).isEqualTo("profile.jpg");
        assertThat(result.getProfileImage().type()).isNotNull();
        assertThat(result.getProfileImage().type().id()).isEqualTo(1L);
        
        assertThat(result.getRole()).isNotNull();
        assertThat(result.getRole().id()).isEqualTo(5L);
        assertThat(result.getRole().role()).isEqualTo("ADMIN");
        assertThat(result.getRole().description()).isEqualTo("Administrator");
        assertThat(result.getRole().level()).isEqualTo(10);
    }

    @Test
    @DisplayName("toDto should return null when User is null")
    void whenToDtoWithNull_thenReturnNull() {
        // Act
        UserDto result = UserMapper.toDto(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toDto should handle User with null Company")
    void whenToDtoWithNullCompany_thenCompanyFieldsAreNull() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setCompany(null);

        // Act
        UserDto result = UserMapper.toDto(user);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCompanyId()).isNull();
        assertThat(result.getCompany()).isNull();
    }

    @Test
    @DisplayName("toDto should handle User with null ProfileImage")
    void whenToDtoWithNullProfileImage_thenProfileImageIsNull() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setProfileImage(null);

        // Act
        UserDto result = UserMapper.toDto(user);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProfileImage()).isNull();
    }

    @Test
    @DisplayName("toDto should handle User with null Role")
    void whenToDtoWithNullRole_thenRoleIsNull() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setRole(null);

        // Act
        UserDto result = UserMapper.toDto(user);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getRole()).isNull();
    }

    @Test
    @DisplayName("toDto should handle ProfileImage with null MediaType")
    void whenToDtoWithProfileImageNoType_thenMediaTypeIsNull() {
        // Arrange
        Media profileImage = new Media();
        profileImage.setId(10L);
        profileImage.setSrc("profile.jpg");
        profileImage.setType(null);

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setProfileImage(profileImage);

        // Act
        UserDto result = UserMapper.toDto(user);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProfileImage()).isNotNull();
        assertThat(result.getProfileImage().type()).isNull();
    }

    @Test
    @DisplayName("toDto should handle User with minimal fields")
    void whenToDtoWithMinimalFields_thenConvertSuccessfully() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setUsername("minimal");

        // Act
        UserDto result = UserMapper.toDto(user);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("minimal");
        assertThat(result.getEmail()).isNull();
        assertThat(result.getName()).isNull();
        assertThat(result.getLastName()).isNull();
        assertThat(result.getCompanyId()).isNull();
        assertThat(result.getCompany()).isNull();
        assertThat(result.getProfileImage()).isNull();
        assertThat(result.getRole()).isNull();
    }
}
