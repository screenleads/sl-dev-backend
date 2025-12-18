package com.screenleads.backend.app.application.security;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityUtilsTest {

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUser_WhenUserAuthenticated_ShouldReturnUser() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        // When
        Optional<User> result = SecurityUtils.getCurrentUser();
        
        // Then
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    void getCurrentUser_WhenNotAuthenticated_ShouldReturnEmpty() {
        // Given - No authentication set
        
        // When
        Optional<User> result = SecurityUtils.getCurrentUser();
        
        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void getCurrentUser_WhenPrincipalNotUser_ShouldReturnEmpty() {
        // Given
        Authentication auth = new UsernamePasswordAuthenticationToken("stringPrincipal", null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        // When
        Optional<User> result = SecurityUtils.getCurrentUser();
        
        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void isAdmin_WhenUserIsAdmin_ShouldReturnTrue() {
        // Given
        Role adminRole = new Role();
        adminRole.setRole("ROLE_ADMIN");
        
        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setRole(adminRole);
        
        Authentication auth = new UsernamePasswordAuthenticationToken(adminUser, null, adminUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        // When
        boolean result = SecurityUtils.isAdmin();
        
        // Then
        assertTrue(result);
    }

    @Test
    void isAdmin_WhenUserIsNotAdmin_ShouldReturnFalse() {
        // Given
        Role userRole = new Role();
        userRole.setRole("ROLE_USER");
        
        User regularUser = new User();
        regularUser.setUsername("user");
        regularUser.setRole(userRole);
        
        Authentication auth = new UsernamePasswordAuthenticationToken(regularUser, null, regularUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        // When
        boolean result = SecurityUtils.isAdmin();
        
        // Then
        assertFalse(result);
    }

    @Test
    void isAdmin_WhenNotAuthenticated_ShouldReturnFalse() {
        // Given - No authentication
        
        // When
        boolean result = SecurityUtils.isAdmin();
        
        // Then
        assertFalse(result);
    }

    @Test
    void currentCompanyId_WhenUserHasCompany_ShouldReturnCompanyId() {
        // Given
        Company company = new Company();
        company.setId(123L);
        
        User user = new User();
        user.setUsername("testuser");
        user.setCompany(company);
        
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        // When
        Optional<Long> result = SecurityUtils.currentCompanyId();
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(123L, result.get());
    }

    @Test
    void currentCompanyId_WhenUserHasNoCompany_ShouldReturnEmpty() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setCompany(null);
        
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        // When
        Optional<Long> result = SecurityUtils.currentCompanyId();
        
        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void currentCompanyId_WhenNotAuthenticated_ShouldReturnEmpty() {
        // Given - No authentication
        
        // When
        Optional<Long> result = SecurityUtils.currentCompanyId();
        
        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void getCurrentUser_WhenAuthenticationIsNull_ShouldReturnEmpty() {
        // Given
        SecurityContextHolder.getContext().setAuthentication(null);
        
        // When
        Optional<User> result = SecurityUtils.getCurrentUser();
        
        // Then
        assertTrue(result.isEmpty());
    }
}
