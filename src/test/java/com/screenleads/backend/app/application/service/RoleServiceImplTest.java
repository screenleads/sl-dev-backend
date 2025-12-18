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

import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.repositories.RoleRepository;
import com.screenleads.backend.app.web.dto.RoleDTO;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleServiceImpl Unit Tests")
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role testRole;

    @BeforeEach
    void setUp() {
        testRole = new Role();
        testRole.setId(1L);
        testRole.setRole("ADMIN");
        testRole.setDescription("Administrator role");
        testRole.setLevel(1);
    }

    @Test
    @DisplayName("getAll should return all roles")
    void whenGetAll_thenReturnsAllRoles() {
        // Arrange
        Role role2 = new Role();
        role2.setId(2L);
        role2.setRole("USER");
        role2.setDescription("User role");
        role2.setLevel(5);

        when(roleRepository.findAll()).thenReturn(Arrays.asList(testRole, role2));

        // Act
        List<RoleDTO> result = roleService.getAll();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).role()).isEqualTo("ADMIN");
        assertThat(result.get(1).role()).isEqualTo("USER");
        verify(roleRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAll should return empty list when no roles exist")
    void whenGetAllEmpty_thenReturnsEmptyList() {
        // Arrange
        when(roleRepository.findAll()).thenReturn(List.of());

        // Act
        List<RoleDTO> result = roleService.getAll();

        // Assert
        assertThat(result).isEmpty();
        verify(roleRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getById should return role when found")
    void whenGetByIdExists_thenReturnsRole() {
        // Arrange
        when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

        // Act
        RoleDTO result = roleService.getById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.role()).isEqualTo("ADMIN");
        assertThat(result.description()).isEqualTo("Administrator role");
        assertThat(result.level()).isEqualTo(1);
        verify(roleRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getById should return null when not found")
    void whenGetByIdNotExists_thenReturnsNull() {
        // Arrange
        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        RoleDTO result = roleService.getById(999L);

        // Assert
        assertThat(result).isNull();
        verify(roleRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("create should save and return new role")
    void whenCreate_thenSavesAndReturnsRole() {
        // Arrange
        RoleDTO inputDTO = new RoleDTO(null, "MODERATOR", "Moderator role", 3);
        Role savedRole = new Role();
        savedRole.setId(10L);
        savedRole.setRole("MODERATOR");
        savedRole.setDescription("Moderator role");
        savedRole.setLevel(3);

        when(roleRepository.save(any(Role.class))).thenReturn(savedRole);

        // Act
        RoleDTO result = roleService.create(inputDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.role()).isEqualTo("MODERATOR");
        assertThat(result.level()).isEqualTo(3);
        verify(roleRepository, times(1)).save(any(Role.class));
    }

    @Test
    @DisplayName("update should update existing role successfully")
    void whenUpdateExists_thenUpdatesSuccessfully() {
        // Arrange
        RoleDTO updateDTO = new RoleDTO(1L, "SUPER_ADMIN", "Super Administrator", 0);
        Role updatedRole = new Role();
        updatedRole.setId(1L);
        updatedRole.setRole("SUPER_ADMIN");
        updatedRole.setDescription("Super Administrator");
        updatedRole.setLevel(0);

        when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
        when(roleRepository.save(any(Role.class))).thenReturn(updatedRole);

        // Act
        RoleDTO result = roleService.update(1L, updateDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.role()).isEqualTo("SUPER_ADMIN");
        assertThat(result.description()).isEqualTo("Super Administrator");
        assertThat(result.level()).isEqualTo(0);
        verify(roleRepository, times(1)).findById(1L);
        verify(roleRepository, times(1)).save(any(Role.class));
    }

    @Test
    @DisplayName("update should throw exception when role not found")
    void whenUpdateNotExists_thenThrowsException() {
        // Arrange
        RoleDTO updateDTO = new RoleDTO(999L, "ADMIN", "Administrator", 1);
        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> roleService.update(999L, updateDTO))
                .isInstanceOf(NoSuchElementException.class);

        verify(roleRepository, times(1)).findById(999L);
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("delete should delete role by id")
    void whenDelete_thenDeletesSuccessfully() {
        // Act
        roleService.delete(1L);

        // Assert
        verify(roleRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("delete should call repository even if role doesn't exist")
    void whenDeleteNonExistent_thenCallsRepository() {
        // Act
        roleService.delete(999L);

        // Assert
        verify(roleRepository, times(1)).deleteById(999L);
    }
}
