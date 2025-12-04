package com.screenleads.backend.app.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.RoleRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import com.screenleads.backend.app.web.dto.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for User Login and Authentication endpoints.
 * 
 * These tests validate:
 * - User login with valid credentials
 * - Error handling for invalid credentials
 * - JWT token generation
 * - Access to protected endpoints with JWT
 * - Password change functionality
 * 
 * Pattern: @SpringBootTest + @AutoConfigureMockMvc + H2 + @Transactional
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
@DisplayName("User Login and Authentication Integration Tests")
class UserLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Company testCompany;
    private Role testRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Create test company
        testCompany = new Company();
        testCompany.setName("Test Company Login");
        testCompany = companyRepository.save(testCompany);

        // Create test role
        testRole = new Role();
        testRole.setRole("ROLE_USER");
        testRole.setLevel(5);
        testRole.setDescription("Standard User");
        testRole = roleRepository.save(testRole);

        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("testuser@example.com");
        testUser.setPassword(passwordEncoder.encode("testpassword"));
        testUser.setName("Test");
        testUser.setLastName("User");
        testUser.setCompany(testCompany);
        testUser.setRole(testRole);
        testUser = userRepository.save(testUser);
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials and return refreshToken")
        void whenLoginWithValidCredentials_thenReturnJwtTokens() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("testpassword");

            MvcResult result = mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andExpect(jsonPath("$.user.username").value("testuser"))
                    .andExpect(jsonPath("$.user.email").value("testuser@example.com"))
                    .andReturn();

            JwtResponse response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    JwtResponse.class);

            assertThat(response.getAccessToken()).isNotBlank();
            assertThat(response.getRefreshToken()).isNotBlank();
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Should return 401 with invalid password")
        void whenLoginWithInvalidPassword_thenUnauthorized() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("wrongpassword");

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }

        @Test
        @DisplayName("Should return 401 with non-existent username")
        void whenLoginWithNonExistentUser_thenUnauthorized() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setUsername("nonexistent");
            request.setPassword("anypassword");

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }
    }

    @Nested
    @DisplayName("Get Current User Tests")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should get current user with valid JWT token")
        void whenGetCurrentUserWithValidToken_thenReturnUserData() throws Exception {
            // First login to get token
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername("testuser");
            loginRequest.setPassword("testpassword");

            MvcResult loginResult = mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            JwtResponse jwtResponse = objectMapper.readValue(
                    loginResult.getResponse().getContentAsString(),
                    JwtResponse.class);

            // Use token to get current user
            mockMvc.perform(get("/auth/me")
                    .header("Authorization", "Bearer " + jwtResponse.getAccessToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.email").value("testuser@example.com"))
                    .andExpect(jsonPath("$.name").value("Test"))
                    .andExpect(jsonPath("$.lastName").value("User"));
        }

        @Test
        @DisplayName("Should return 401 when accessing protected endpoint without token")
        void whenGetCurrentUserWithoutToken_thenUnauthorized() throws Exception {
            mockMvc.perform(get("/auth/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 with invalid token")
        void whenGetCurrentUserWithInvalidToken_thenUnauthorized() throws Exception {
            mockMvc.perform(get("/auth/me")
                    .header("Authorization", "Bearer invalid.jwt.token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Password Change Tests")
    class PasswordChangeTests {

        @Test
        @DisplayName("Should change password successfully")
        void whenChangePasswordWithCorrectOldPassword_thenSuccess() throws Exception {
            // Login to get token
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername("testuser");
            loginRequest.setPassword("testpassword");

            MvcResult loginResult = mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            JwtResponse jwtResponse = objectMapper.readValue(
                    loginResult.getResponse().getContentAsString(),
                    JwtResponse.class);

            // Change password
            PasswordChangeRequest changeRequest = new PasswordChangeRequest();
            changeRequest.setCurrentPassword("testpassword");
            changeRequest.setNewPassword("newpassword123");

            mockMvc.perform(post("/auth/change-password")
                    .header("Authorization", "Bearer " + jwtResponse.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(changeRequest)))
                    .andExpect(status().isOk());

            // Verify password was changed in database
            User updatedUser = userRepository.findByUsername("testuser").orElseThrow();
            assertThat(passwordEncoder.matches("newpassword123", updatedUser.getPassword())).isTrue();
            assertThat(passwordEncoder.matches("testpassword", updatedUser.getPassword())).isFalse();

            // Verify can login with new password
            LoginRequest newLoginRequest = new LoginRequest();
            newLoginRequest.setUsername("testuser");
            newLoginRequest.setPassword("newpassword123");

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newLoginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.username").value("testuser"));

            // Verify old password no longer works
            LoginRequest oldPasswordRequest = new LoginRequest();
            oldPasswordRequest.setUsername("testuser");
            oldPasswordRequest.setPassword("testpassword");

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(oldPasswordRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("Should fail password change without authentication")
        void whenChangePasswordWithoutAuth_thenUnauthorized() throws Exception {
            PasswordChangeRequest changeRequest = new PasswordChangeRequest();
            changeRequest.setCurrentPassword("testpassword");
            changeRequest.setNewPassword("newpassword123");

            mockMvc.perform(post("/auth/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(changeRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Complete Authentication Flow Tests")
    class CompleteFlowTests {

        @Test
        @DisplayName("Should complete full flow: login → get user → change password → re-login")
        void whenCompleteAuthenticationFlow_thenAllStepsSucceed() throws Exception {
            // Step 1: Login
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername("testuser");
            loginRequest.setPassword("testpassword");

            MvcResult loginResult = mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andReturn();

            JwtResponse loginResponse = objectMapper.readValue(
                    loginResult.getResponse().getContentAsString(),
                    JwtResponse.class);

            String accessToken = loginResponse.getAccessToken();
            assertThat(accessToken).isNotBlank();

            // Step 2: Get current user data
            mockMvc.perform(get("/auth/me")
                    .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.email").value("testuser@example.com"));

            // Step 3: Change password
            PasswordChangeRequest changeRequest = new PasswordChangeRequest();
            changeRequest.setCurrentPassword("testpassword");
            changeRequest.setNewPassword("updatedpassword");

            mockMvc.perform(post("/auth/change-password")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(changeRequest)))
                    .andExpect(status().isOk());

            // Step 4: Login with new password
            LoginRequest newLoginRequest = new LoginRequest();
            newLoginRequest.setUsername("testuser");
            newLoginRequest.setPassword("updatedpassword");

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newLoginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.username").value("testuser"));

            // Step 5: Verify old password no longer works
            LoginRequest oldPasswordRequest = new LoginRequest();
            oldPasswordRequest.setUsername("testuser");
            oldPasswordRequest.setPassword("testpassword");

            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(oldPasswordRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }
    }
}
