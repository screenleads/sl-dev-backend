package com.screenleads.backend.app.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.RoleRepository;
import com.screenleads.backend.app.domain.repositories.UserRepository;
import com.screenleads.backend.app.web.dto.CompanyDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Company CRUD operations.
 * Tests the complete flow through Controller -> Service -> Repository layers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Company CRUD Integration Tests")
public class CompanyCRUDIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private Company testCompany;

    @BeforeEach
    void setUp() throws Exception {
        // Create admin role
        Role adminRole = Role.builder()
                .role("ROLE_ADMIN")
                .level(1)
                .description("Administrator")
                .build();
        adminRole = roleRepository.save(adminRole);

        // Create admin user
        Company adminCompany = Company.builder()
                .name("Admin Company")
                .billingStatus(Company.BillingStatus.ACTIVE)
                .build();
        adminCompany = companyRepository.save(adminCompany);

        User adminUser = new User();
        adminUser.setUsername("admin@test.com");
        adminUser.setEmail("admin@test.com");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setCompany(adminCompany);
        adminUser.setRole(adminRole);
        userRepository.save(adminUser);

        // Login to get admin token
        String loginRequest = """
                {
                    "username": "admin@test.com",
                    "password": "admin123"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        adminToken = objectMapper.readTree(loginResponse).get("accessToken").asText();

        // Create test company
        testCompany = Company.builder()
                .name("Test Company")
                .observations("Test observations")
                .primaryColor("#FF0000")
                .secondaryColor("#00FF00")
                .billingStatus(Company.BillingStatus.ACTIVE)
                .build();
        testCompany = companyRepository.save(testCompany);
    }

    @Nested
    @DisplayName("Company Creation Tests")
    class CompanyCreationTests {

        @Test
        @DisplayName("Admin can create a new company with valid data")
        void whenCreateCompanyWithValidData_thenSuccess() throws Exception {
            String createRequest = """
                    {
                        "name": "New Company",
                        "observations": "New company observations",
                        "primaryColor": "#0000FF",
                        "secondaryColor": "#FFFF00",
                        "billingStatus": "ACTIVE"
                    }
                    """;

            MvcResult result = mockMvc.perform(post("/companies")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("New Company"))
                    .andExpect(jsonPath("$.observations").value("New company observations"))
                    .andExpect(jsonPath("$.primaryColor").value("#0000FF"))
                    .andExpect(jsonPath("$.secondaryColor").value("#FFFF00"))
                    .andExpect(jsonPath("$.billingStatus").value("ACTIVE"))
                    .andExpect(jsonPath("$.id").exists())
                    .andReturn();

            // Verify company was persisted
            String response = result.getResponse().getContentAsString();
            Long companyId = objectMapper.readTree(response).get("id").asLong();
            
            Company savedCompany = companyRepository.findById(companyId).orElseThrow();
            assertThat(savedCompany.getName()).isEqualTo("New Company");
            assertThat(savedCompany.getPrimaryColor()).isEqualTo("#0000FF");
        }

        @Test
        @DisplayName("Company creation requires authentication")
        void whenCreateCompanyWithoutAuth_thenUnauthorized() throws Exception {
            String createRequest = """
                    {
                        "name": "Unauthorized Company",
                        "billingStatus": "ACTIVE"
                    }
                    """;

            mockMvc.perform(post("/companies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createRequest))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Create company with minimum required fields")
        void whenCreateCompanyWithMinimumFields_thenSuccess() throws Exception {
            String createRequest = """
                    {
                        "name": "Minimal Company",
                        "billingStatus": "INCOMPLETE"
                    }
                    """;

            mockMvc.perform(post("/companies")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Minimal Company"))
                    .andExpect(jsonPath("$.billingStatus").value("INCOMPLETE"))
                    .andExpect(jsonPath("$.id").exists());
        }
    }

    @Nested
    @DisplayName("Company Read Tests")
    class CompanyReadTests {

        @Test
        @DisplayName("Get all companies returns list")
        void whenGetAllCompanies_thenReturnList() throws Exception {
            mockMvc.perform(get("/companies")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2)); // Admin company + test company
        }

        @Test
        @DisplayName("Get company by ID returns correct company")
        void whenGetCompanyById_thenReturnCompany() throws Exception {
            mockMvc.perform(get("/companies/{id}", testCompany.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testCompany.getId()))
                    .andExpect(jsonPath("$.name").value("Test Company"))
                    .andExpect(jsonPath("$.observations").value("Test observations"))
                    .andExpect(jsonPath("$.primaryColor").value("#FF0000"))
                    .andExpect(jsonPath("$.secondaryColor").value("#00FF00"));
        }

        @Test
        @DisplayName("Get non-existent company returns 404")
        void whenGetNonExistentCompany_thenNotFound() throws Exception {
            mockMvc.perform(get("/companies/{id}", 999999L)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Get company without auth returns 401")
        void whenGetCompanyWithoutAuth_thenUnauthorized() throws Exception {
            mockMvc.perform(get("/companies/{id}", testCompany.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Company Update Tests")
    class CompanyUpdateTests {

        @Test
        @DisplayName("Update company with valid data")
        void whenUpdateCompanyWithValidData_thenSuccess() throws Exception {
            String updateRequest = """
                    {
                        "name": "Updated Company Name",
                        "observations": "Updated observations",
                        "primaryColor": "#AAAAAA",
                        "secondaryColor": "#BBBBBB",
                        "billingStatus": "PAST_DUE"
                    }
                    """;

            mockMvc.perform(put("/companies/{id}", testCompany.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testCompany.getId()))
                    .andExpect(jsonPath("$.name").value("Updated Company Name"))
                    .andExpect(jsonPath("$.observations").value("Updated observations"))
                    .andExpect(jsonPath("$.primaryColor").value("#AAAAAA"))
                    .andExpect(jsonPath("$.secondaryColor").value("#BBBBBB"))
                    .andExpect(jsonPath("$.billingStatus").value("PAST_DUE"));

            // Verify changes were persisted
            Company updatedCompany = companyRepository.findById(testCompany.getId()).orElseThrow();
            assertThat(updatedCompany.getName()).isEqualTo("Updated Company Name");
            assertThat(updatedCompany.getBillingStatus()).isEqualTo(Company.BillingStatus.PAST_DUE);
        }

        @Test
        @DisplayName("Update Stripe fields")
        void whenUpdateStripeFields_thenSuccess() throws Exception {
            String updateRequest = """
                    {
                        "name": "Test Company",
                        "stripeCustomerId": "cus_test123",
                        "stripeSubscriptionId": "sub_test456",
                        "stripeSubscriptionItemId": "si_test789",
                        "billingStatus": "ACTIVE"
                    }
                    """;

            mockMvc.perform(put("/companies/{id}", testCompany.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stripeCustomerId").value("cus_test123"))
                    .andExpect(jsonPath("$.stripeSubscriptionId").value("sub_test456"))
                    .andExpect(jsonPath("$.stripeSubscriptionItemId").value("si_test789"));

            // Verify Stripe fields persisted
            Company updatedCompany = companyRepository.findById(testCompany.getId()).orElseThrow();
            assertThat(updatedCompany.getStripeCustomerId()).isEqualTo("cus_test123");
            assertThat(updatedCompany.getStripeSubscriptionId()).isEqualTo("sub_test456");
        }

        @Test
        @DisplayName("Update non-existent company returns 404")
        void whenUpdateNonExistentCompany_thenNotFound() throws Exception {
            String updateRequest = """
                    {
                        "name": "Non Existent",
                        "billingStatus": "ACTIVE"
                    }
                    """;

            mockMvc.perform(put("/companies/{id}", 999999L)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateRequest))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Company Delete Tests")
    class CompanyDeleteTests {

        @Test
        @DisplayName("Delete company successfully")
        void whenDeleteCompany_thenSuccess() throws Exception {
            // Create company to delete
            Company companyToDelete = Company.builder()
                    .name("Company To Delete")
                    .billingStatus(Company.BillingStatus.CANCELED)
                    .build();
            companyToDelete = companyRepository.save(companyToDelete);
            Long deleteId = companyToDelete.getId();

            // Delete the company
            mockMvc.perform(delete("/companies/{id}", deleteId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());

            // Verify company was deleted
            assertThat(companyRepository.findById(deleteId)).isEmpty();
        }

        @Test
        @DisplayName("Delete non-existent company succeeds (idempotent)")
        void whenDeleteNonExistentCompany_thenNoContent() throws Exception {
            mockMvc.perform(delete("/companies/{id}", 999999L)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Delete without auth returns 401")
        void whenDeleteWithoutAuth_thenUnauthorized() throws Exception {
            mockMvc.perform(delete("/companies/{id}", testCompany.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Complete Company Lifecycle Tests")
    class CompleteLifecycleTests {

        @Test
        @DisplayName("Complete CRUD lifecycle: Create -> Read -> Update -> Delete")
        void whenCompleteCRUDLifecycle_thenAllOperationsSucceed() throws Exception {
            // 1. CREATE
            String createRequest = """
                    {
                        "name": "Lifecycle Company",
                        "observations": "Initial state",
                        "primaryColor": "#111111",
                        "billingStatus": "INCOMPLETE"
                    }
                    """;

            MvcResult createResult = mockMvc.perform(post("/companies")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Lifecycle Company"))
                    .andReturn();

            String createResponse = createResult.getResponse().getContentAsString();
            Long companyId = objectMapper.readTree(createResponse).get("id").asLong();

            // 2. READ
            mockMvc.perform(get("/companies/{id}", companyId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(companyId))
                    .andExpect(jsonPath("$.name").value("Lifecycle Company"))
                    .andExpect(jsonPath("$.observations").value("Initial state"));

            // 3. UPDATE
            String updateRequest = """
                    {
                        "name": "Lifecycle Company Updated",
                        "observations": "Updated state",
                        "primaryColor": "#222222",
                        "billingStatus": "ACTIVE",
                        "stripeCustomerId": "cus_lifecycle123"
                    }
                    """;

            mockMvc.perform(put("/companies/{id}", companyId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Lifecycle Company Updated"))
                    .andExpect(jsonPath("$.observations").value("Updated state"))
                    .andExpect(jsonPath("$.billingStatus").value("ACTIVE"))
                    .andExpect(jsonPath("$.stripeCustomerId").value("cus_lifecycle123"));

            // Verify persistence after update
            Company updatedCompany = companyRepository.findById(companyId).orElseThrow();
            assertThat(updatedCompany.getName()).isEqualTo("Lifecycle Company Updated");
            assertThat(updatedCompany.getBillingStatus()).isEqualTo(Company.BillingStatus.ACTIVE);

            // 4. DELETE
            mockMvc.perform(delete("/companies/{id}", companyId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());

            // Verify deletion
            assertThat(companyRepository.findById(companyId)).isEmpty();
        }

        @Test
        @DisplayName("Billing status transitions correctly")
        void whenTransitionBillingStatus_thenCorrectFlow() throws Exception {
            // Create with INCOMPLETE
            String createRequest = """
                    {
                        "name": "Billing Flow Company",
                        "billingStatus": "INCOMPLETE"
                    }
                    """;

            MvcResult createResult = mockMvc.perform(post("/companies")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.billingStatus").value("INCOMPLETE"))
                    .andReturn();

            Long companyId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                    .get("id").asLong();

            // Transition to ACTIVE
            String activateRequest = """
                    {
                        "name": "Billing Flow Company",
                        "billingStatus": "ACTIVE",
                        "stripeCustomerId": "cus_active123",
                        "stripeSubscriptionId": "sub_active456"
                    }
                    """;

            mockMvc.perform(put("/companies/{id}", companyId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(activateRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.billingStatus").value("ACTIVE"));

            // Transition to PAST_DUE
            String pastDueRequest = """
                    {
                        "name": "Billing Flow Company",
                        "billingStatus": "PAST_DUE"
                    }
                    """;

            mockMvc.perform(put("/companies/{id}", companyId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(pastDueRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.billingStatus").value("PAST_DUE"));

            // Transition to CANCELED
            String cancelRequest = """
                    {
                        "name": "Billing Flow Company",
                        "billingStatus": "CANCELED"
                    }
                    """;

            mockMvc.perform(put("/companies/{id}", companyId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cancelRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.billingStatus").value("CANCELED"));

            // Verify final state in database
            Company finalCompany = companyRepository.findById(companyId).orElseThrow();
            assertThat(finalCompany.getBillingStatus()).isEqualTo(Company.BillingStatus.CANCELED);
        }
    }
}
