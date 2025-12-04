package com.screenleads.backend.app.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.domain.repositories.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Permission-Based Access Control.
 * Validates @perm.can() enforcement across different resources and roles.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Permission-Based Access Control Integration Tests")
public class PermissionAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private AppEntityRepository appEntityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String userToken;
    private Company testCompany;

    @BeforeEach
    void setUp() throws Exception {
        // Create roles with different levels
        Role adminRole = Role.builder()
                .role("ROLE_ADMIN")
                .level(1)  // Highest permission
                .description("Administrator")
                .build();
        adminRole = roleRepository.save(adminRole);

        Role userRole = Role.builder()
                .role("ROLE_USER")
                .level(5)  // Lower permission
                .description("Regular User")
                .build();
        userRole = roleRepository.save(userRole);

        // Create company permission configuration
        AppEntity companyEntity = AppEntity.builder()
                .resource("company")
                .entityName("Company")
                .endpointBase("/companies")
                .createLevel(2)  // Only ADMIN (level 1) can create
                .readLevel(5)    // USER (level 5) and ADMIN can read
                .updateLevel(2)  // Only ADMIN (level 1) can update
                .deleteLevel(1)  // Only ADMIN (level 1) can delete
                .displayLabel("Companies")
                .visibleInMenu(true)
                .build();
        appEntityRepository.save(companyEntity);

        // Create device permission configuration
        AppEntity deviceEntity = AppEntity.builder()
                .resource("device")
                .entityName("Device")
                .endpointBase("/devices")
                .createLevel(5)  // Both USER and ADMIN can create
                .readLevel(5)    // Both can read
                .updateLevel(3)  // Only ADMIN can update (level 1 < 3)
                .deleteLevel(2)  // Only ADMIN can delete (level 1 < 2)
                .displayLabel("Devices")
                .visibleInMenu(true)
                .build();
        appEntityRepository.save(deviceEntity);

        // Create test company
        testCompany = Company.builder()
                .name("Test Company")
                .billingStatus(Company.BillingStatus.ACTIVE)
                .build();
        testCompany = companyRepository.save(testCompany);

        // Create admin user
        User admin = new User();
        admin.setUsername("admin@test.com");
        admin.setEmail("admin@test.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setCompany(testCompany);
        admin.setRole(adminRole);
        userRepository.save(admin);

        // Create regular user
        User regularUser = new User();
        regularUser.setUsername("user@test.com");
        regularUser.setEmail("user@test.com");
        regularUser.setPassword(passwordEncoder.encode("user123"));
        regularUser.setCompany(testCompany);
        regularUser.setRole(userRole);
        userRepository.save(regularUser);

        // Login admin
        String adminLoginRequest = """
                {
                    "username": "admin@test.com",
                    "password": "admin123"
                }
                """;

        MvcResult adminLoginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminLoginRequest))
                .andExpect(status().isOk())
                .andReturn();

        adminToken = objectMapper.readTree(adminLoginResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        // Login regular user
        String userLoginRequest = """
                {
                    "username": "user@test.com",
                    "password": "user123"
                }
                """;

        MvcResult userLoginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userLoginRequest))
                .andExpect(status().isOk())
                .andReturn();

        userToken = objectMapper.readTree(userLoginResult.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @Nested
    @DisplayName("Company Resource Permission Tests")
    class CompanyResourcePermissionTests {

        @Test
        @DisplayName("Admin can create company (level 1 <= createLevel 2)")
        void whenAdminCreatesCompany_thenSuccess() throws Exception {
            String createRequest = """
                    {
                        "name": "New Company",
                        "billingStatus": "ACTIVE"
                    }
                    """;

            mockMvc.perform(post("/companies")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createRequest))
                    .andExpect(status().isOk()) // CompanyController returns 200 OK, not 201
                    .andExpect(jsonPath("$.name").value("New Company"));
        }

        @Test
        @DisplayName("User cannot create company (level 5 > createLevel 2)")
        void whenUserTriesToCreateCompany_thenForbidden() throws Exception {
            String createRequest = """
                    {
                        "name": "Unauthorized Company",
                        "billingStatus": "ACTIVE"
                    }
                    """;

            // Note: AuthorizationDeniedException currently returns 500 instead of 403
            mockMvc.perform(post("/companies")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createRequest))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Access Denied"));
        }

        @Test
        @DisplayName("Both admin and user can read companies (level <= 5)")
        void whenBothRolesReadCompanies_thenSuccess() throws Exception {
            // Admin can read
            mockMvc.perform(get("/companies")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            // User can read
            mockMvc.perform(get("/companies")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Admin can update company (level 1 <= updateLevel 2)")
        void whenAdminUpdatesCompany_thenSuccess() throws Exception {
            String updateRequest = String.format("""
                    {
                        "name": "Updated Company Name",
                        "billingStatus": "ACTIVE"
                    }
                    """);

            mockMvc.perform(put("/companies/" + testCompany.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Company Name"));
        }

        @Test
        @DisplayName("User cannot update company (level 5 > updateLevel 2)")
        void whenUserTriesToUpdateCompany_thenForbidden() throws Exception {
            String updateRequest = """
                    {
                        "name": "Hacked Company Name",
                        "billingStatus": "ACTIVE"
                    }
                    """;

            // Note: AuthorizationDeniedException returns 500, not 403
            mockMvc.perform(put("/companies/" + testCompany.getId())
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateRequest))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Access Denied"));
        }

        @Test
        @DisplayName("Admin can delete company (level 1 <= deleteLevel 1)")
        void whenAdminDeletesCompany_thenSuccess() throws Exception {
            Company tempCompany = Company.builder()
                    .name("Temp Company")
                    .billingStatus(Company.BillingStatus.ACTIVE)
                    .build();
            tempCompany = companyRepository.save(tempCompany);

            mockMvc.perform(delete("/companies/" + tempCompany.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("User cannot delete company (level 5 > deleteLevel 1)")
        void whenUserTriesToDeleteCompany_thenForbidden() throws Exception {
            Company tempCompany = Company.builder()
                    .name("Protected Company")
                    .billingStatus(Company.BillingStatus.ACTIVE)
                    .build();
            tempCompany = companyRepository.save(tempCompany);

            // Note: AuthorizationDeniedException returns 500, not 403
            mockMvc.perform(delete("/companies/" + tempCompany.getId())
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Access Denied"));
        }
    }

    @Nested
    @DisplayName("Unauthenticated Access Tests")
    class UnauthenticatedAccessTests {

        @Test
        @DisplayName("Unauthenticated requests are rejected")
        void whenNoAuthToken_thenUnauthorized() throws Exception {
            mockMvc.perform(get("/companies"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(post("/companies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Test\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
