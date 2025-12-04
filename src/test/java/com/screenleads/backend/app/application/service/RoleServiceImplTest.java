package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.repositories.RoleRepository;
import com.screenleads.backend.app.web.dto.RoleDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleServiceImpl Unit Tests")
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role testRole;
    private RoleDTO testRoleDTO;

    @BeforeEach
    void setUp() {
        testRole = new Role();
        testRole.setId(1L);
        testRole.setRole("USER");
        testRole.setDescription("Standard User Role");
        testRole.setLevel(3);

        testRoleDTO = new RoleDTO(1L, "USER", "Standard User Role", 3);
    }

    @Nested
    @DisplayName("GetAll Roles Tests")
    class GetAllRolesTests {

        @Test
        @DisplayName("Should return all roles")
        void whenGetAll_thenReturnAllRoles() {
            // Given
            Role role2 = new Role();
            role2.setId(2L);
            role2.setRole("ADMIN");
            role2.setDescription("Administrator Role");
            role2.setLevel(1);

            when(roleRepository.findAll()).thenReturn(Arrays.asList(testRole, role2));

            // When
            List<RoleDTO> result = roleService.getAll();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).role()).isEqualTo("USER");
            assertThat(result.get(0).level()).isEqualTo(3);
            assertThat(result.get(1).role()).isEqualTo("ADMIN");
            assertThat(result.get(1).level()).isEqualTo(1);
            verify(roleRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no roles exist")
        void whenGetAll_withNoRoles_thenReturnEmptyList() {
            // Given
            when(roleRepository.findAll()).thenReturn(Arrays.asList());

            // When
            List<RoleDTO> result = roleService.getAll();

            // Then
            assertThat(result).isEmpty();
            verify(roleRepository).findAll();
        }

        @Test
        @DisplayName("Should return roles sorted by level")
        void whenGetAll_thenReturnSortedByLevel() {
            // Given
            Role admin = new Role();
            admin.setId(1L);
            admin.setRole("ADMIN");
            admin.setLevel(1);

            Role manager = new Role();
            manager.setId(2L);
            manager.setRole("MANAGER");
            manager.setLevel(2);

            Role user = new Role();
            user.setId(3L);
            user.setRole("USER");
            user.setLevel(3);

            when(roleRepository.findAll()).thenReturn(Arrays.asList(user, admin, manager));

            // When
            List<RoleDTO> result = roleService.getAll();

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).role()).isEqualTo("USER");
            assertThat(result.get(1).role()).isEqualTo("ADMIN");
            assertThat(result.get(2).role()).isEqualTo("MANAGER");
            verify(roleRepository).findAll();
        }
    }

    @Nested
    @DisplayName("GetById Tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should return role when found by id")
        void whenGetById_withValidId_thenReturnRole() {
            // Given
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

            // When
            RoleDTO result = roleService.getById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.role()).isEqualTo("USER");
            assertThat(result.description()).isEqualTo("Standard User Role");
            assertThat(result.level()).isEqualTo(3);
            verify(roleRepository).findById(1L);
        }

        @Test
        @DisplayName("Should return null when role not found")
        void whenGetById_withInvalidId_thenReturnNull() {
            // Given
            when(roleRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            RoleDTO result = roleService.getById(999L);

            // Then
            assertThat(result).isNull();
            verify(roleRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("Create Role Tests")
    class CreateRoleTests {

        @Test
        @DisplayName("Should create role successfully")
        void whenCreate_withValidData_thenReturnCreatedRole() {
            // Given
            RoleDTO inputDTO = new RoleDTO(null, "MODERATOR", "Moderator Role", 2);
            Role savedRole = new Role();
            savedRole.setId(10L);
            savedRole.setRole("MODERATOR");
            savedRole.setDescription("Moderator Role");
            savedRole.setLevel(2);

            when(roleRepository.save(any(Role.class))).thenReturn(savedRole);

            // When
            RoleDTO result = roleService.create(inputDTO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(10L);
            assertThat(result.role()).isEqualTo("MODERATOR");
            assertThat(result.description()).isEqualTo("Moderator Role");
            assertThat(result.level()).isEqualTo(2);
            verify(roleRepository).save(any(Role.class));
        }

        @Test
        @DisplayName("Should create role with null description")
        void whenCreate_withNullDescription_thenCreateSuccessfully() {
            // Given
            RoleDTO inputDTO = new RoleDTO(null, "GUEST", null, 4);
            Role savedRole = new Role();
            savedRole.setId(11L);
            savedRole.setRole("GUEST");
            savedRole.setDescription(null);
            savedRole.setLevel(4);

            when(roleRepository.save(any(Role.class))).thenReturn(savedRole);

            // When
            RoleDTO result = roleService.create(inputDTO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(11L);
            assertThat(result.role()).isEqualTo("GUEST");
            assertThat(result.description()).isNull();
            assertThat(result.level()).isEqualTo(4);
            verify(roleRepository).save(any(Role.class));
        }
    }

    @Nested
    @DisplayName("Update Role Tests")
    class UpdateRoleTests {

        @Test
        @DisplayName("Should update role successfully")
        void whenUpdate_withValidData_thenReturnUpdatedRole() {
            // Given
            RoleDTO updateDTO = new RoleDTO(1L, "POWER_USER", "Power User Role", 2);
            
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            RoleDTO result = roleService.update(1L, updateDTO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.role()).isEqualTo("POWER_USER");
            assertThat(result.description()).isEqualTo("Power User Role");
            assertThat(result.level()).isEqualTo(2);
            verify(roleRepository).findById(1L);
            verify(roleRepository).save(any(Role.class));
        }

        @Test
        @DisplayName("Should update only role name")
        void whenUpdate_withOnlyRoleName_thenUpdateOnlyName() {
            // Given
            RoleDTO updateDTO = new RoleDTO(1L, "BASIC_USER", "Standard User Role", 3);
            
            when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
            when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            RoleDTO result = roleService.update(1L, updateDTO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.role()).isEqualTo("BASIC_USER");
            assertThat(result.description()).isEqualTo("Standard User Role");
            assertThat(result.level()).isEqualTo(3);
            verify(roleRepository).save(any(Role.class));
        }

        @Test
        @DisplayName("Should throw exception when role not found")
        void whenUpdate_withInvalidId_thenThrowException() {
            // Given
            RoleDTO updateDTO = new RoleDTO(999L, "UPDATED", "Updated Role", 2);
            
            when(roleRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> roleService.update(999L, updateDTO))
                    .isInstanceOf(NoSuchElementException.class);
            verify(roleRepository).findById(999L);
            verify(roleRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Role Tests")
    class DeleteRoleTests {

        @Test
        @DisplayName("Should delete role successfully")
        void whenDelete_withValidId_thenDeleteSuccessfully() {
            // Given
            doNothing().when(roleRepository).deleteById(1L);

            // When
            roleService.delete(1L);

            // Then
            verify(roleRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should call delete even with non-existent id")
        void whenDelete_withInvalidId_thenStillCallDelete() {
            // Given
            doNothing().when(roleRepository).deleteById(999L);

            // When
            roleService.delete(999L);

            // Then
            verify(roleRepository).deleteById(999L);
        }
    }
}
