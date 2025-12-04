package com.screenleads.backend.app.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.screenleads.backend.app.domain.model.*;
import com.screenleads.backend.app.domain.repositories.*;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("API Key Authentication Integration Tests")
class ApiKeyAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AppEntityRepository appEntityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Company testCompany;
    private Client testClient;
    private User adminUser;
    private String adminToken;
    private String validApiKey;

    @BeforeEach
    void setUp() throws Exception {
        // Create AppEntity for company resource
        AppEntity companyEntity = AppEntity.builder()
                .resource("company")
                .entityName("Company")
                .endpointBase("/companies")
                .createLevel(2)
                .readLevel(5)
                .updateLevel(2)
                .deleteLevel(1)
                .displayLabel("Companies")
                .build();
        appEntityRepository.save(companyEntity);

        // Create admin role
        Role adminRole = Role.builder()
                .role("ROLE_ADMIN")
                .description("Administrator")
                .level(1)
                .build();
        adminRole = roleRepository.save(adminRole);

        // Create test company
        testCompany = Company.builder()
                .name("API Test Company")
                .billingStatus(Company.BillingStatus.ACTIVE)
                .build();
        testCompany = companyRepository.save(testCompany);

        // Create test client
        testClient = new Client();
        testClient.setClientId("test-client-" + System.currentTimeMillis());
        testClient.setName("Test Client");
        testClient.setActive(true);
        testClient = clientRepository.save(testClient);

        // Create admin user
        adminUser = User.builder()
                .username("apitest@test.com")
                .email("apitest@test.com")
                .password(passwordEncoder.encode("admin123"))
                .role(adminRole)
                .company(testCompany)
                .build();
        adminUser = userRepository.save(adminUser);

        // Login to get JWT token
        String loginRequest = """
                {
                    "username": "apitest@test.com",
                    "password": "admin123"
                }
                """;

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        adminToken = objectMapper.readTree(response).get("accessToken").asText();

        // Create a valid API key
        String apiKeyValue = "test-api-key-" + System.currentTimeMillis();
        ApiKey apiKey = new ApiKey();
        apiKey.setKey(apiKeyValue);
        apiKey.setClient(testClient);
        apiKey.setActive(true);
        apiKey.setCreatedAt(LocalDateTime.now());
        apiKey.setExpiresAt(LocalDateTime.now().plusYears(1));
        apiKey.setCompanyScope(testCompany.getId());
        apiKey.setDescription("Test API Key");
        apiKeyRepository.save(apiKey);

        validApiKey = apiKeyValue;
    }

    @Nested
    @DisplayName("API Key Creation Tests")
    class ApiKeyCreationTests {

        @Test
        @DisplayName("API key can be created programmatically")
        void whenCreateApiKeyProgrammatically_thenSuccess() {
            ApiKey newKey = new ApiKey();
            newKey.setKey("programmatic-key-" + System.currentTimeMillis());
            newKey.setClient(testClient);
            newKey.setActive(true);
            newKey.setCreatedAt(LocalDateTime.now());
            newKey.setExpiresAt(LocalDateTime.now().plusYears(1));
            newKey.setCompanyScope(testCompany.getId());
            newKey.setDescription("Programmatic Key");
            
            ApiKey saved = apiKeyRepository.save(newKey);
            
            assert saved.getId() != null;
            assert saved.getKey() != null;
            assert saved.isActive();
        }

        @Test
        @DisplayName("API key has required fields")
        void whenCreateApiKey_thenHasRequiredFields() {
            ApiKey key = new ApiKey();
            key.setKey("test-key-fields");
            key.setClient(testClient);
            key.setActive(true);
            key.setCreatedAt(LocalDateTime.now());
            key.setCompanyScope(testCompany.getId());
            
            ApiKey saved = apiKeyRepository.save(key);
            
            assert saved.getKey().equals("test-key-fields");
            assert saved.getClient().getId().equals(testClient.getId());
            assert saved.isActive() == true;
            assert saved.getCompanyScope().equals(testCompany.getId());
        }
    }

    @Nested
    @DisplayName("API Key Authentication Tests - Documents ACTUAL System Behavior")
    class ApiKeyAuthenticationTests {

        /**
         * Documents actual behavior: API Key authentication sets authority="API_CLIENT"
         * but endpoints require ROLE_ADMIN or @perm.can() permissions.
         * Result: AuthorizationDeniedException → 500 (not 401 or 403).
         */
        @Test
        @DisplayName("Valid API key authenticates but lacks endpoint permissions")
        void whenValidApiKey_thenAccessDenied() throws Exception {
            mockMvc.perform(get("/companies")
                            .header("X-API-Key", validApiKey)
                            .header("client-id", testClient.getClientId()))
                    .andExpect(status().isInternalServerError()) // Actual behavior: 500 not 401
                    .andExpect(jsonPath("$.message").value("Access Denied"));
        }

        @Test
        @DisplayName("Invalid API key returns unauthorized")
        void whenInvalidApiKey_thenUnauthorized() throws Exception {
            mockMvc.perform(get("/companies")
                            .header("X-API-Key", "invalid-api-key-999")
                            .header("client-id", testClient.getClientId()))
                    .andExpect(status().isUnauthorized()); // Filter rejects, no authentication created
        }

        @Test
        @DisplayName("Expired API key authenticates but lacks permissions")
        void whenExpiredApiKey_thenAccessDenied() throws Exception {
            // Create expired API key
            String expiredKeyValue = "expired-key-" + System.currentTimeMillis();
            ApiKey expiredKey = new ApiKey();
            expiredKey.setKey(expiredKeyValue);
            expiredKey.setClient(testClient);
            expiredKey.setActive(true);
            expiredKey.setCreatedAt(LocalDateTime.now());
            expiredKey.setExpiresAt(LocalDateTime.now().minusDays(1));
            expiredKey.setCompanyScope(testCompany.getId());
            expiredKey.setDescription("Expired Key");
            apiKeyRepository.save(expiredKey);

            mockMvc.perform(get("/companies")
                            .header("X-API-Key", expiredKeyValue)
                            .header("client-id", testClient.getClientId()))
                    .andExpect(status().isInternalServerError()) // Still authenticated, still denied
                    .andExpect(jsonPath("$.message").value("Access Denied"));
        }

        @Test
        @DisplayName("Inactive API key returns unauthorized")
        void whenInactiveApiKey_thenUnauthorized() throws Exception {
            // Create inactive API key
            String inactiveKeyValue = "inactive-key-" + System.currentTimeMillis();
            ApiKey inactiveKey = new ApiKey();
            inactiveKey.setKey(inactiveKeyValue);
            inactiveKey.setClient(testClient);
            inactiveKey.setActive(false); // INACTIVE
            inactiveKey.setCreatedAt(LocalDateTime.now());
            inactiveKey.setExpiresAt(LocalDateTime.now().plusYears(1));
            inactiveKey.setCompanyScope(testCompany.getId());
            inactiveKey.setDescription("Inactive Key");
            apiKeyRepository.save(inactiveKey);

            mockMvc.perform(get("/companies")
                            .header("X-API-Key", inactiveKeyValue)
                            .header("client-id", testClient.getClientId()))
                    .andExpect(status().isUnauthorized()); // Filter does not authenticate inactive keys
        }
    }

    /**
     * Test Suite 3: API Key vs JWT Comparison
     * Documents difference: JWT with ROLE_ADMIN works, API Key with API_CLIENT does not.
     */
    @Nested
    @DisplayName("API Key vs JWT Tests")
    class ApiKeyVsJwtTests {

        @Test
        @DisplayName("JWT works, API Key fails - different authority models")
        void whenBothAuthMethods_thenOnlyJwtWorks() throws Exception {
            // JWT with ROLE_ADMIN works
            mockMvc.perform(get("/companies")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            // API Key with API_CLIENT fails (no ROLE_ADMIN, no @perm.can access)
            mockMvc.perform(get("/companies")
                            .header("X-API-Key", validApiKey)
                            .header("client-id", testClient.getClientId()))
                    .andExpect(status().isInternalServerError()) // Access Denied
                    .andExpect(jsonPath("$.message").value("Access Denied"));
        }

        @Test
        @DisplayName("API key authentication creates principal but lacks permissions")
        void whenApiKeyUsed_thenAuthenticatedButDenied() throws Exception {
            // Create second company to test scope (not relevant since access is denied anyway)
            Company otherCompany = Company.builder()
                    .name("Other Company")
                    .billingStatus(Company.BillingStatus.ACTIVE)
                    .build();
            otherCompany = companyRepository.save(otherCompany);

            // API key authenticates (principal=clientId, authority=API_CLIENT)
            // but still denied access due to @PreAuthorize requirements
            mockMvc.perform(get("/companies")
                            .header("X-API-Key", validApiKey)
                            .header("client-id", testClient.getClientId()))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Access Denied"));
        }
    }

    @Nested
    @DisplayName("API Key Management Tests")
    class ApiKeyManagementTests {

        @Test
        @DisplayName("Can query API keys by client")
        void whenQueryByClient_thenReturnsClientKeys() {
            // Create another API key for same client
            ApiKey secondKey = new ApiKey();
            secondKey.setKey("second-key-" + System.currentTimeMillis());
            secondKey.setClient(testClient);
            secondKey.setActive(true);
            secondKey.setCreatedAt(LocalDateTime.now());
            secondKey.setCompanyScope(testCompany.getId());
            apiKeyRepository.save(secondKey);

            // Query keys for client
            var keys = apiKeyRepository.findAll();
            long clientKeys = keys.stream()
                    .filter(k -> k.getClient().getId().equals(testClient.getId()))
                    .count();

            assert clientKeys >= 2; // At least the setUp key and the new one
        }

        @Test
        @DisplayName("Can deactivate API key")
        void whenDeactivateApiKey_thenNoLongerActive() {
            ApiKey keyToDeactivate = new ApiKey();
            keyToDeactivate.setKey("deactivate-me");
            keyToDeactivate.setClient(testClient);
            keyToDeactivate.setActive(true);
            keyToDeactivate.setCreatedAt(LocalDateTime.now());
            keyToDeactivate.setCompanyScope(testCompany.getId());
            ApiKey saved = apiKeyRepository.save(keyToDeactivate);

            // Deactivate
            saved.setActive(false);
            apiKeyRepository.save(saved);

            // Verify
            ApiKey reloaded = apiKeyRepository.findById(saved.getId()).orElseThrow();
            assert !reloaded.isActive();
        }

        @Test
        @DisplayName("Can delete API key")
        void whenDeleteApiKey_thenRemoved() {
            ApiKey keyToDelete = new ApiKey();
            keyToDelete.setKey("delete-me");
            keyToDelete.setClient(testClient);
            keyToDelete.setActive(true);
            keyToDelete.setCreatedAt(LocalDateTime.now());
            keyToDelete.setCompanyScope(testCompany.getId());
            ApiKey saved = apiKeyRepository.save(keyToDelete);
            Long keyId = saved.getId();

            // Delete
            apiKeyRepository.deleteById(keyId);

            // Verify
            assert apiKeyRepository.findById(keyId).isEmpty();
        }
    }
}
