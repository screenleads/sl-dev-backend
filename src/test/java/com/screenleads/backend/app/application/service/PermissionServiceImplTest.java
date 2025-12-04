package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.AppEntity;
import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.repositories.AppEntityRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionServiceImpl Unit Tests")
class PermissionServiceImplTest {

    @Mock
    private AppEntityRepository permissionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    @Nested
    @DisplayName("can() method tests")
    class CanPermissionTests {

        @Test
        @DisplayName("Should deny when no authentication context exists")
        void whenNoAuthentication_thenDenied() {
            try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                SecurityContext securityContext = mock(SecurityContext.class);
                when(securityContext.getAuthentication()).thenReturn(null);
                mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                boolean result = permissionService.can("devices", "read");

                assertThat(result).isFalse();
            }
        }

        @Test
        @DisplayName("Should deny when authentication is not authenticated")
        void whenNotAuthenticated_thenDenied() {
            try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                SecurityContext securityContext = mock(SecurityContext.class);
                Authentication authentication = mock(Authentication.class);
                when(authentication.isAuthenticated()).thenReturn(false);
                when(securityContext.getAuthentication()).thenReturn(authentication);
                mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                boolean result = permissionService.can("devices", "read");

                assertThat(result).isFalse();
            }
        }

        @Test
        @DisplayName("Should deny when user not found in repository")
        void whenUserNotFound_thenDenied() {
            try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                SecurityContext securityContext = mock(SecurityContext.class);
                Authentication authentication = mock(Authentication.class);
                when(authentication.isAuthenticated()).thenReturn(true);
                when(authentication.getName()).thenReturn("testuser");
                when(authentication.getAuthorities()).thenReturn((Collection) List.of(new SimpleGrantedAuthority("USER")));
                when(securityContext.getAuthentication()).thenReturn(authentication);
                mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

                boolean result = permissionService.can("devices", "read");

                assertThat(result).isFalse();
            }
        }

        @Test
        @DisplayName("Should deny when user has no role")
        void whenUserHasNoRole_thenDenied() {
            try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                SecurityContext securityContext = mock(SecurityContext.class);
                Authentication authentication = mock(Authentication.class);
                when(authentication.isAuthenticated()).thenReturn(true);
                when(authentication.getName()).thenReturn("testuser");
                when(authentication.getAuthorities()).thenReturn((Collection) List.of(new SimpleGrantedAuthority("USER")));
                when(securityContext.getAuthentication()).thenReturn(authentication);
                mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                User user = new User();
                user.setUsername("testuser");
                user.setRole(null);
                when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

                boolean result = permissionService.can("devices", "read");

                assertThat(result).isFalse();
            }
        }

        @Test
        @DisplayName("Should deny when resource not found")
        void whenResourceNotFound_thenDenied() {
            try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                SecurityContext securityContext = mock(SecurityContext.class);
                Authentication authentication = mock(Authentication.class);
                when(authentication.isAuthenticated()).thenReturn(true);
                when(authentication.getName()).thenReturn("testuser");
                when(authentication.getAuthorities()).thenReturn((Collection) List.of(new SimpleGrantedAuthority("USER")));
                when(securityContext.getAuthentication()).thenReturn(authentication);
                mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                Role role = new Role();
                role.setLevel(5);
                User user = new User();
                user.setUsername("testuser");
                user.setRole(role);
                when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
                when(permissionRepository.findByResource("unknown")).thenReturn(Optional.empty());

                boolean result = permissionService.can("unknown", "read");

                assertThat(result).isFalse();
            }
        }

        @Test
        @DisplayName("Should allow when user level is sufficient for read action")
        void whenUserLevelSufficient_thenAllowed() {
            try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                SecurityContext securityContext = mock(SecurityContext.class);
                Authentication authentication = mock(Authentication.class);
                when(authentication.isAuthenticated()).thenReturn(true);
                when(authentication.getName()).thenReturn("testuser");
                when(authentication.getAuthorities()).thenReturn((Collection) List.of(new SimpleGrantedAuthority("USER")));
                when(securityContext.getAuthentication()).thenReturn(authentication);
                mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                Role role = new Role();
                role.setLevel(3);
                User user = new User();
                user.setUsername("testuser");
                user.setRole(role);
                when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

                AppEntity permission = new AppEntity();
                permission.setResource("devices");
                permission.setReadLevel(5);
                when(permissionRepository.findByResource("devices")).thenReturn(Optional.of(permission));

                boolean result = permissionService.can("devices", "read");

                assertThat(result).isTrue();
            }
        }

        @Test
        @DisplayName("Should deny when user level is insufficient")
        void whenUserLevelInsufficient_thenDenied() {
            try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                SecurityContext securityContext = mock(SecurityContext.class);
                Authentication authentication = mock(Authentication.class);
                when(authentication.isAuthenticated()).thenReturn(true);
                when(authentication.getName()).thenReturn("testuser");
                when(authentication.getAuthorities()).thenReturn((Collection) List.of(new SimpleGrantedAuthority("USER")));
                when(securityContext.getAuthentication()).thenReturn(authentication);
                mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                Role role = new Role();
                role.setLevel(6);
                User user = new User();
                user.setUsername("testuser");
                user.setRole(role);
                when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

                AppEntity permission = new AppEntity();
                permission.setResource("devices");
                permission.setReadLevel(5);
                when(permissionRepository.findByResource("devices")).thenReturn(Optional.of(permission));

                boolean result = permissionService.can("devices", "read");

                assertThat(result).isFalse();
            }
        }

        @Test
        @DisplayName("Should handle create action correctly")
        void whenCreateAction_thenCheckCreateLevel() {
            try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                SecurityContext securityContext = mock(SecurityContext.class);
                Authentication authentication = mock(Authentication.class);
                when(authentication.isAuthenticated()).thenReturn(true);
                when(authentication.getName()).thenReturn("testuser");
                when(authentication.getAuthorities()).thenReturn((Collection) List.of(new SimpleGrantedAuthority("USER")));
                when(securityContext.getAuthentication()).thenReturn(authentication);
                mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                Role role = new Role();
                role.setLevel(2);
                User user = new User();
                user.setUsername("testuser");
                user.setRole(role);
                when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

                AppEntity permission = new AppEntity();
                permission.setResource("devices");
                permission.setCreateLevel(3);
                when(permissionRepository.findByResource("devices")).thenReturn(Optional.of(permission));

                boolean result = permissionService.can("devices", "create");

                assertThat(result).isTrue();
            }
        }

        @Test
        @DisplayName("Should deny when action is invalid")
        void whenInvalidAction_thenDenied() {
            try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                SecurityContext securityContext = mock(SecurityContext.class);
                Authentication authentication = mock(Authentication.class);
                when(authentication.isAuthenticated()).thenReturn(true);
                when(authentication.getName()).thenReturn("testuser");
                when(authentication.getAuthorities()).thenReturn((Collection) List.of(new SimpleGrantedAuthority("USER")));
                when(securityContext.getAuthentication()).thenReturn(authentication);
                mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                Role role = new Role();
                role.setLevel(3);
                User user = new User();
                user.setUsername("testuser");
                user.setRole(role);
                when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

                AppEntity permission = new AppEntity();
                permission.setResource("devices");
                when(permissionRepository.findByResource("devices")).thenReturn(Optional.of(permission));

                boolean result = permissionService.can("devices", "invalidAction");

                assertThat(result).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("effectiveLevel() method tests")
    class EffectiveLevelTests {

        @Test
        @DisplayName("Should return MAX_VALUE when no authentication")
        void whenNoAuth_thenReturnMaxValue() {
            try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                SecurityContext securityContext = mock(SecurityContext.class);
                when(securityContext.getAuthentication()).thenReturn(null);
                mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                int level = permissionService.effectiveLevel();

                assertThat(level).isEqualTo(Integer.MAX_VALUE);
            }
        }

        @Test
        @DisplayName("Should return MAX_VALUE when not authenticated")
        void whenNotAuthenticated_thenReturnMaxValue() {
            try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                SecurityContext securityContext = mock(SecurityContext.class);
                Authentication authentication = mock(Authentication.class);
                when(authentication.isAuthenticated()).thenReturn(false);
                when(securityContext.getAuthentication()).thenReturn(authentication);
                mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                int level = permissionService.effectiveLevel();

                assertThat(level).isEqualTo(Integer.MAX_VALUE);
            }
        }

        @Test
        @DisplayName("Should return user role level when authenticated")
        void whenAuthenticated_thenReturnUserLevel() {
            try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                SecurityContext securityContext = mock(SecurityContext.class);
                Authentication authentication = mock(Authentication.class);
                when(authentication.isAuthenticated()).thenReturn(true);
                when(authentication.getName()).thenReturn("testuser");
                when(securityContext.getAuthentication()).thenReturn(authentication);
                mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                Role role = new Role();
                role.setLevel(5);
                User user = new User();
                user.setUsername("testuser");
                user.setRole(role);
                when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

                int level = permissionService.effectiveLevel();

                assertThat(level).isEqualTo(5);
            }
        }

        @Test
        @DisplayName("Should return MAX_VALUE when user has no role")
        void whenNoRole_thenReturnMaxValue() {
            try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                SecurityContext securityContext = mock(SecurityContext.class);
                Authentication authentication = mock(Authentication.class);
                when(authentication.isAuthenticated()).thenReturn(true);
                when(authentication.getName()).thenReturn("testuser");
                when(securityContext.getAuthentication()).thenReturn(authentication);
                mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                User user = new User();
                user.setUsername("testuser");
                user.setRole(null);
                when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

                int level = permissionService.effectiveLevel();

                assertThat(level).isEqualTo(Integer.MAX_VALUE);
            }
        }

        @Test
        @DisplayName("Should return MAX_VALUE when user not found")
        void whenUserNotFound_thenReturnMaxValue() {
            try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
                SecurityContext securityContext = mock(SecurityContext.class);
                Authentication authentication = mock(Authentication.class);
                when(authentication.isAuthenticated()).thenReturn(true);
                when(authentication.getName()).thenReturn("testuser");
                when(securityContext.getAuthentication()).thenReturn(authentication);
                mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

                int level = permissionService.effectiveLevel();

                assertThat(level).isEqualTo(Integer.MAX_VALUE);
            }
        }
    }
}
