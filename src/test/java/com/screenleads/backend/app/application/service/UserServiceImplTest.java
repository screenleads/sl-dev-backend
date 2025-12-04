package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.domain.repositories.*;
import com.screenleads.backend.app.web.dto.*;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PermissionService permissionService;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private MediaTypeRepository mediaTypeRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Filter filter;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private Company testCompany;
    private Role testRole;

    @BeforeEach
    void setUp() {
        testCompany = new Company();
        testCompany.setId(1L);
        testCompany.setName("Test Company");

        testRole = new Role();
        testRole.setId(1L);
        testRole.setRole("USER");
        testRole.setDescription("User Role");
        testRole.setLevel(3);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setName("Test");
        testUser.setLastName("User");
        testUser.setPassword("encoded-password");
        testUser.setCompany(testCompany);
        testUser.setRole(testRole);
    }

    @Nested
    @DisplayName("GetAll Users Tests")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return all users")
        void whenGetAll_thenReturnAllUsers() {
            // Given
            User user2 = new User();
            user2.setId(2L);
            user2.setUsername("user2");
            user2.setEmail("user2@example.com");
            user2.setCompany(testCompany);
            user2.setRole(testRole);

            lenient().when(entityManager.unwrap(Session.class)).thenReturn(session);
            lenient().when(session.getEnabledFilter("companyFilter")).thenReturn(null);
            when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, user2));

            // When
            List<UserDto> result = userService.getAll();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getUsername()).isEqualTo("testuser");
            assertThat(result.get(1).getUsername()).isEqualTo("user2");
            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no users exist")
        void whenGetAll_withNoUsers_thenReturnEmptyList() {
            // Given
            lenient().when(entityManager.unwrap(Session.class)).thenReturn(session);
            lenient().when(session.getEnabledFilter("companyFilter")).thenReturn(null);
            when(userRepository.findAll()).thenReturn(Arrays.asList());

            // When
            List<UserDto> result = userService.getAll();

            // Then
            assertThat(result).isEmpty();
            verify(userRepository).findAll();
        }
    }

    @Nested
    @DisplayName("GetById Tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should return user when found by id")
        void whenGetById_withValidId_thenReturnUser() {
            // Given
            lenient().when(entityManager.unwrap(Session.class)).thenReturn(session);
            lenient().when(session.getEnabledFilter("companyFilter")).thenReturn(null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            UserDto result = userService.getById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("Should return null when user not found")
        void whenGetById_withInvalidId_thenReturnNull() {
            // Given
            lenient().when(entityManager.unwrap(Session.class)).thenReturn(session);
            lenient().when(session.getEnabledFilter("companyFilter")).thenReturn(null);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            UserDto result = userService.getById(999L);

            // Then
            assertThat(result).isNull();
            verify(userRepository).findById(999L);
        }
    }

    @Nested
    @DisplayName("Create User Tests")
    class CreateUserTests {

        @Test
        @DisplayName("Should throw exception when username already exists")
        void whenCreate_withDuplicateUsername_thenThrowException() {
            // Given
            RoleDTO roleDTO = new RoleDTO(1L, "USER", "User Role", 3);
            UserDto inputDto = UserDto.builder()
                    .username("existinguser")
                    .email("new@example.com")
                    .role(roleDTO)
                    .build();

            when(permissionService.can("user", "create")).thenReturn(true);
            when(permissionService.effectiveLevel()).thenReturn(2);
            when(userRepository.findByUsername("existinguser")).thenReturn(Optional.of(testUser));

            // When/Then
            assertThatThrownBy(() -> userService.create(inputDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("username ya existe");
        }

        @Test
        @DisplayName("Should throw exception when body is null")
        void whenCreate_withNullDto_thenThrowException() {
            // When/Then
            assertThatThrownBy(() -> userService.create(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Body requerido");
        }

        @Test
        @DisplayName("Should throw exception when username is blank")
        void whenCreate_withBlankUsername_thenThrowException() {
            // Given
            UserDto inputDto = UserDto.builder()
                    .username("")
                    .email("new@example.com")
                    .build();

            // When/Then
            assertThatThrownBy(() -> userService.create(inputDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("username requerido");
        }

        @Test
        @DisplayName("Should throw exception when user has no permission to create")
        void whenCreate_withoutPermission_thenThrowException() {
            // Given
            RoleDTO roleDTO = new RoleDTO(1L, "USER", "User Role", 3);
            UserDto inputDto = UserDto.builder()
                    .username("newuser")
                    .email("new@example.com")
                    .role(roleDTO)
                    .build();

            when(permissionService.can("user", "create")).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> userService.create(inputDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No autorizado a crear usuarios");
        }

        @Test
        @DisplayName("Should throw exception when user level is too high")
        void whenCreate_withHighLevel_thenThrowException() {
            // Given
            RoleDTO roleDTO = new RoleDTO(1L, "USER", "User Role", 3);
            UserDto inputDto = UserDto.builder()
                    .username("newuser")
                    .email("new@example.com")
                    .role(roleDTO)
                    .build();

            when(permissionService.can("user", "create")).thenReturn(true);
            when(permissionService.effectiveLevel()).thenReturn(3); // level > 2

            // When/Then
            assertThatThrownBy(() -> userService.create(inputDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solo roles de nivel 1 o 2 pueden crear usuarios");
        }
    }

    @Nested
    @DisplayName("Update User Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user successfully")
        void whenUpdate_withValidData_thenReturnUpdatedUser() {
            // Given
            UserDto updateDto = UserDto.builder()
                    .username("updateduser")
                    .email("updated@example.com")
                    .name("Updated")
                    .lastName("Name")
                    .build();

            lenient().when(entityManager.unwrap(Session.class)).thenReturn(session);
            lenient().when(session.getEnabledFilter("companyFilter")).thenReturn(null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            UserDto result = userService.update(1L, updateDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("updateduser");
            assertThat(result.getEmail()).isEqualTo("updated@example.com");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should update user password when provided")
        void whenUpdate_withPassword_thenEncodeAndUpdate() {
            // Given
            UserDto updateDto = UserDto.builder()
                    .password("newpassword")
                    .build();

            lenient().when(entityManager.unwrap(Session.class)).thenReturn(session);
            lenient().when(session.getEnabledFilter("companyFilter")).thenReturn(null);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode("newpassword")).thenReturn("encoded-newpassword");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            UserDto result = userService.update(1L, updateDto);

            // Then
            assertThat(result).isNotNull();
            verify(passwordEncoder).encode("newpassword");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should return null when user not found")
        void whenUpdate_withInvalidId_thenReturnNull() {
            // Given
            UserDto updateDto = UserDto.builder()
                    .username("updateduser")
                    .build();

            lenient().when(entityManager.unwrap(Session.class)).thenReturn(session);
            lenient().when(session.getEnabledFilter("companyFilter")).thenReturn(null);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            UserDto result = userService.update(999L, updateDto);

            // Then
            assertThat(result).isNull();
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete User Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should delete user when has permission")
        void whenDelete_withPermission_thenDeleteSuccessfully() {
            // Given
            when(permissionService.can("user", "delete")).thenReturn(true);
            lenient().when(entityManager.unwrap(Session.class)).thenReturn(session);
            lenient().when(session.getEnabledFilter("companyFilter")).thenReturn(null);
            doNothing().when(userRepository).deleteById(1L);

            // When
            userService.delete(1L);

            // Then
            verify(userRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should throw exception when user has no delete permission")
        void whenDelete_withoutPermission_thenThrowException() {
            // Given
            when(permissionService.can("user", "delete")).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> userService.delete(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No autorizado a borrar usuarios");
        }
    }
}
