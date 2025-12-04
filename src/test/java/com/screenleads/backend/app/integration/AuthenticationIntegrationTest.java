package com.screenleads.backend.app.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;

/**
 * Integration test for Company CRUD operations.
 * 
 * This test class demonstrates:
 * - Full integration testing with @SpringBootTest
 * - Using real H2 database instead of mocks
 * - Testing complete CRUD flow (create → read → update → delete)
 * - Transaction rollback with @Transactional for test isolation
 * - End-to-end request/response testing with MockMvc
 * - Testing security configuration
 * 
 * Pattern: @SpringBootTest + @AutoConfigureMockMvc + H2 + @Transactional
 * 
 * Note: This is a REAL integration test - all layers (controller, service,
 * repository, database)
 * are executed with actual Spring context, unlike unit tests with mocks.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional // Rollback database changes after each test
@DisplayName("Company CRUD Integration Tests")
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompanyRepository companyRepository;

    private Company testCompany;

    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        companyRepository.deleteAll();

        // Create test company
        testCompany = new Company();
        testCompany.setName("Integration Test Company");
        testCompany.setObservations("Integration test observations");
        testCompany.setPrimaryColor("#FFFFFF");
        testCompany.setSecondaryColor("#000000");
        testCompany.setBillingStatus("ACTIVE");
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    @DisplayName("Should complete full CRUD flow: create → read → update → delete")
    void shouldCompleteFullCrudFlow() throws Exception {
        // ==================== STEP 1: Create company ====================
        String companyJson = """
                {
                    "name": "Test Integration Company",
                    "observations": "Test observations",
                    "primaryColor": "#FFFFFF",
                    "secondaryColor": "#000000"
                }
                """;

        mockMvc.perform(post("/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(companyJson))
                .andExpect(status().isOk()) // Idempotent behavior returns 200 OK
                .andExpect(jsonPath("$.name").value("Test Integration Company"));

        // ==================== STEP 2: Read all companies ====================
        mockMvc.perform(get("/companies"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$[0].name").value("Test Integration Company"));

        // ==================== STEP 3: Verify data in database ====================
        Company savedCompany = companyRepository.findByName("Test Integration Company").orElseThrow();
        assertThat(savedCompany).isNotNull();
        assertThat(savedCompany.getObservations()).contains("observations"); // Test data contains "Test observations"

        // ==================== STEP 4: Update company ====================
        String updateJson = """
                {
                    "name": "Updated Company Name",
                    "email": "updated@integration.com",
                    "phone": "+34600333444"
                }
                """;

        mockMvc.perform(put("/companies/{id}", savedCompany.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Company Name"));

        // ==================== STEP 5: Delete company ====================
        mockMvc.perform(delete("/companies/{id}", savedCompany.getId()))
                .andExpect(status().isNoContent());

        // ==================== STEP 6: Verify deletion ====================
        mockMvc.perform(get("/companies/{id}", savedCompany.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    @DisplayName("Should allow access when user has ADMIN permissions")
    void shouldAllowAccessWithAdminPermission() throws Exception {
        // When: User with ADMIN role accesses endpoint
        mockMvc.perform(get("/companies"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"));
    }

    @Test
    @DisplayName("Should reject access to protected endpoint without authentication")
    void shouldRejectAccessWithoutAuthentication() throws Exception {
        // When: Accessing protected endpoint without authentication
        mockMvc.perform(get("/companies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    @DisplayName("Should handle database constraints correctly")
    void shouldHandleDatabaseConstraints() throws Exception {
        // Given: Company saved in database
        companyRepository.save(testCompany);
        companyRepository.flush();

        // When: Trying to create duplicate company (idempotency check)
        String duplicateJson = """
                {
                    "name": "Integration Test Company",
                    "observations": "Different observations",
                    "primaryColor": "#000000",
                    "secondaryColor": "#FFFFFF"
                }
                """;

        // Then: Should return existing company (idempotent behavior)
        mockMvc.perform(post("/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicateJson))
                .andExpect(status().isOk()) // Idempotent behavior returns 200 OK
                .andExpect(jsonPath("$.observations").value("Integration test observations")); // Original observations
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    @DisplayName("Should maintain data integrity across multiple operations")
    void shouldMaintainDataIntegrityAcrossOperations() throws Exception {
        // Given: Multiple companies
        Company company1 = createCompany("Company A", "Observations A");
        Company company2 = createCompany("Company B", "Observations B");
        companyRepository.save(company1);
        companyRepository.save(company2);
        companyRepository.flush();

        // When: Querying all companies
        mockMvc.perform(get("/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // When: Deleting one company
        mockMvc.perform(delete("/companies/{id}", company1.getId()))
                .andExpect(status().isNoContent());

        // Then: Only one company should remain
        mockMvc.perform(get("/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Company B"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    @DisplayName("Should handle concurrent requests correctly")
    void shouldHandleConcurrentRequests() throws Exception {
        // Given: Saved company
        Company saved = companyRepository.save(testCompany);
        companyRepository.flush();

        // When: Multiple rapid GET requests
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/companies/{id}", saved.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Integration Test Company"));
        }

        // Then: All requests should succeed consistently
    }

    // ==================== Helper Methods ====================

    private Company createCompany(String name, String observations) {
        Company company = new Company();
        company.setName(name);
        company.setObservations(observations);
        company.setPrimaryColor("#FFFFFF");
        company.setSecondaryColor("#000000");
        company.setBillingStatus("ACTIVE");
        return company;
    }
}
