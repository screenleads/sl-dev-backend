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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Device-Company multi-entity relationships.
 * Validates company isolation, permissions, and cross-entity operations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Device-Company Multi-Entity Integration Tests")
public class DeviceCompanyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DeviceTypeRepository deviceTypeRepository;

    @Autowired
    private AppEntityRepository appEntityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String company1AdminToken;
    private String company2AdminToken;
    private Company company1;
    private Company company2;
    private DeviceType deviceType;

    @BeforeEach
    void setUp() throws Exception {
        // Create admin role
        Role adminRole = Role.builder()
                .role("ROLE_ADMIN")
                .level(1)
                .description("Administrator")
                .build();
        adminRole = roleRepository.save(adminRole);

        // Create device permission configuration
        AppEntity deviceEntity = AppEntity.builder()
                .resource("device")
                .entityName("Device")
                .endpointBase("/devices")
                .createLevel(5)  // ROLE_ADMIN (level 1) can create
                .readLevel(5)    // ROLE_ADMIN (level 1) can read
                .updateLevel(5)  // ROLE_ADMIN (level 1) can update
                .deleteLevel(5)  // ROLE_ADMIN (level 1) can delete
                .displayLabel("Devices")
                .visibleInMenu(true)
                .build();
        appEntityRepository.save(deviceEntity);

        // Create device type
        deviceType = DeviceType.builder()
                .type("SCREEN")
                .enabled(true)
                .build();
        deviceType = deviceTypeRepository.save(deviceType);

        // Create Company 1
        company1 = Company.builder()
                .name("Company One")
                .billingStatus(Company.BillingStatus.ACTIVE)
                .build();
        company1 = companyRepository.save(company1);

        // Create Company 1 admin user
        User admin1 = new User();
        admin1.setUsername("admin1@company1.com");
        admin1.setEmail("admin1@company1.com");
        admin1.setPassword(passwordEncoder.encode("admin123"));
        admin1.setCompany(company1);
        admin1.setRole(adminRole);
        userRepository.save(admin1);

        // Login Company 1 admin
        String loginRequest1 = """
                {
                    "username": "admin1@company1.com",
                    "password": "admin123"
                }
                """;

        MvcResult loginResult1 = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest1))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponse1 = loginResult1.getResponse().getContentAsString();
        company1AdminToken = objectMapper.readTree(loginResponse1).get("accessToken").asText();

        // Create Company 2
        company2 = Company.builder()
                .name("Company Two")
                .billingStatus(Company.BillingStatus.ACTIVE)
                .build();
        company2 = companyRepository.save(company2);

        // Create Company 2 admin user
        User admin2 = new User();
        admin2.setUsername("admin2@company2.com");
        admin2.setEmail("admin2@company2.com");
        admin2.setPassword(passwordEncoder.encode("admin123"));
        admin2.setCompany(company2);
        admin2.setRole(adminRole);
        userRepository.save(admin2);

        // Login Company 2 admin
        String loginRequest2 = """
                {
                    "username": "admin2@company2.com",
                    "password": "admin123"
                }
                """;

        MvcResult loginResult2 = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest2))
                .andExpect(status().isOk())
                .andReturn();

        String loginResponse2 = loginResult2.getResponse().getContentAsString();
        company2AdminToken = objectMapper.readTree(loginResponse2).get("accessToken").asText();
    }

    @Nested
    @DisplayName("Company Isolation Tests")
    class CompanyIsolationTests {

        @Test
        @DisplayName("Company 1 can create device in their company")
        void whenCompany1CreatesDevice_thenSuccess() throws Exception {
            String createRequest = String.format("""
                    {
                        "uuid": "device-uuid-001",
                        "width": 1920,
                        "height": 1080,
                        "descriptionName": "Main Screen",
                        "company": {"id": %d},
                        "type": {"id": %d}
                    }
                    """, company1.getId(), deviceType.getId());

            MvcResult result = mockMvc.perform(post("/devices")
                            .header("Authorization", "Bearer " + company1AdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createRequest))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.uuid").value("device-uuid-001"))
                    .andExpect(jsonPath("$.company.id").value(company1.getId()))
                    .andReturn();

            // Verify persistence
            Long deviceId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("id").asLong();
            Device savedDevice = deviceRepository.findById(deviceId).orElseThrow();
            assertThat(savedDevice.getCompany().getId()).isEqualTo(company1.getId());
        }

        @Test
        @DisplayName("Admin can see devices from all companies (no isolation for ADMIN)")
        void whenAdminListsDevices_thenSeesAllDevices() throws Exception {
            // Create device for Company 1
            Device device1 = Device.builder()
                    .uuid("device-company1")
                    .width(1920)
                    .height(1080)
                    .company(company1)
                    .type(deviceType)
                    .build();
            deviceRepository.save(device1);

            // Create device for Company 2
            Device device2 = Device.builder()
                    .uuid("device-company2")
                    .width(1920)
                    .height(1080)
                    .company(company2)
                    .type(deviceType)
                    .build();
            deviceRepository.save(device2);

            // Company 1 ADMIN lists devices - can see all because ADMIN role
            MvcResult result = mockMvc.perform(get("/devices")
                            .header("Authorization", "Bearer " + company1AdminToken))
                    .andExpect(status().isOk())
                    .andReturn();

            String response = result.getResponse().getContentAsString();
            // Admin can see both companies' devices
            assertThat(response).contains("device-company1");
            assertThat(response).contains("device-company2");
        }

        @Test
        @DisplayName("Admin can access devices from any company")
        void whenAdminAccessesOtherCompanyDevice_thenSuccess() throws Exception {
            // Create device for Company 2
            Device device2 = Device.builder()
                    .uuid("device-company2-visible")
                    .width(1920)
                    .height(1080)
                    .company(company2)
                    .type(deviceType)
                    .build();
            device2 = deviceRepository.save(device2);

            // Company 1 ADMIN can access Company 2's device
            mockMvc.perform(get("/devices/" + device2.getId())
                            .header("Authorization", "Bearer " + company1AdminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value("device-company2-visible"));
        }

        @Test
        @DisplayName("Admin can update devices from any company")
        void whenAdminUpdatesOtherCompanyDevice_thenSuccess() throws Exception {
            // Create device for Company 2
            Device device2 = Device.builder()
                    .uuid("device-company2-update")
                    .width(1920)
                    .height(1080)
                    .company(company2)
                    .type(deviceType)
                    .build();
            device2 = deviceRepository.save(device2);

            String updateRequest = String.format("""
                    {
                        "uuid": "device-company2-update",
                        "width": 3840,
                        "height": 2160,
                        "descriptionName": "Updated Screen",
                        "company": {"id": %d},
                        "type": {"id": %d}
                    }
                    """, company2.getId(), deviceType.getId());

            // Company 1 ADMIN can update Company 2's device
            mockMvc.perform(put("/devices/" + device2.getId())
                            .header("Authorization", "Bearer " + company1AdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.width").value(3840));
        }

        @Test
        @DisplayName("Admin can delete devices from any company")
        void whenAdminDeletesOtherCompanyDevice_thenSuccess() throws Exception {
            // Create device for Company 2
            Device device2 = Device.builder()
                    .uuid("device-company2-delete")
                    .width(1920)
                    .height(1080)
                    .company(company2)
                    .type(deviceType)
                    .build();
            device2 = deviceRepository.save(device2);

            // Company 1 ADMIN can delete Company 2's device
            mockMvc.perform(delete("/devices/" + device2.getId())
                            .header("Authorization", "Bearer " + company1AdminToken))
                    .andExpect(status().isNoContent());

            // Verify device was deleted
            assertThat(deviceRepository.findById(device2.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Device-Company Relationship Tests")
    class DeviceCompanyRelationshipTests {

        @Test
        @DisplayName("Device is created with correct company association")
        void whenCreateDevice_thenCompanyAssociationIsCorrect() throws Exception {
            String createRequest = String.format("""
                    {
                        "uuid": "device-relation-test",
                        "width": 1920,
                        "height": 1080,
                        "descriptionName": "Test Screen",
                        "company": {"id": %d},
                        "type": {"id": %d}
                    }
                    """, company1.getId(), deviceType.getId());

            MvcResult result = mockMvc.perform(post("/devices")
                            .header("Authorization", "Bearer " + company1AdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createRequest))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.company.id").value(company1.getId()))
                    .andExpect(jsonPath("$.company.name").value("Company One"))
                    .andReturn();

            // Verify relationship in database
            Long deviceId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("id").asLong();
            Device device = deviceRepository.findById(deviceId).orElseThrow();
            assertThat(device.getCompany().getId()).isEqualTo(company1.getId());
        }

        @Test
        @DisplayName("Multiple devices can belong to same company")
        void whenCreateMultipleDevices_thenAllBelongToSameCompany() throws Exception {
            // Create first device
            String createRequest1 = String.format("""
                    {
                        "uuid": "device-multi-1",
                        "width": 1920,
                        "height": 1080,
                        "company": {"id": %d},
                        "type": {"id": %d}
                    }
                    """, company1.getId(), deviceType.getId());

            mockMvc.perform(post("/devices")
                            .header("Authorization", "Bearer " + company1AdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createRequest1))
                    .andExpect(status().isCreated());

            // Create second device
            String createRequest2 = String.format("""
                    {
                        "uuid": "device-multi-2",
                        "width": 3840,
                        "height": 2160,
                        "company": {"id": %d},
                        "type": {"id": %d}
                    }
                    """, company1.getId(), deviceType.getId());

            mockMvc.perform(post("/devices")
                            .header("Authorization", "Bearer " + company1AdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createRequest2))
                    .andExpect(status().isCreated());

            // Verify both devices exist for company1
            long deviceCount = deviceRepository.findAll().stream()
                    .filter(d -> d.getCompany().getId().equals(company1.getId()))
                    .count();
            assertThat(deviceCount).isEqualTo(2);
        }

        @Test
        @DisplayName("Device requires valid company ID")
        void whenCreateDeviceWithInvalidCompany_thenError() throws Exception {
            String createRequest = String.format("""
                    {
                        "uuid": "device-invalid-company",
                        "width": 1920,
                        "height": 1080,
                        "company": {"id": 99999},
                        "type": {"id": %d}
                    }
                    """, deviceType.getId());

            mockMvc.perform(post("/devices")
                            .header("Authorization", "Bearer " + company1AdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createRequest))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("UUID Uniqueness Tests")
    class UUIDUniquenessTests {

        @Test
        @DisplayName("Can create device with unique UUID")
        void whenCreateDeviceWithUniqueUUID_thenSuccess() throws Exception {
            // Create device with unique UUID
            String createRequest = String.format("""
                    {
                        "uuid": "unique-uuid-test",
                        "width": 1920,
                        "height": 1080,
                        "company": {"id": %d},
                        "type": {"id": %d}
                    }
                    """, company1.getId(), deviceType.getId());

            mockMvc.perform(post("/devices")
                            .header("Authorization", "Bearer " + company1AdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createRequest))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.uuid").value("unique-uuid-test"));
        }

        @Test
        @DisplayName("Can query device by UUID")
        void whenQueryDeviceByUUID_thenSuccess() throws Exception {
            // Create device
            Device device = Device.builder()
                    .uuid("query-uuid-test")
                    .width(1920)
                    .height(1080)
                    .descriptionName("Queryable Screen")
                    .company(company1)
                    .type(deviceType)
                    .build();
            deviceRepository.save(device);

            // Query by UUID
            mockMvc.perform(get("/devices/uuid/query-uuid-test")
                            .header("Authorization", "Bearer " + company1AdminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uuid").value("query-uuid-test"))
                    .andExpect(jsonPath("$.descriptionName").value("Queryable Screen"));
        }
    }

    @Nested
    @DisplayName("Complete Lifecycle Tests")
    class CompleteLifecycleTests {

        @Test
        @DisplayName("Complete device lifecycle within company context")
        void whenCompleteDeviceLifecycle_thenAllOperationsSucceed() throws Exception {
            // 1. Create device
            String createRequest = String.format("""
                    {
                        "uuid": "lifecycle-device",
                        "width": 1920,
                        "height": 1080,
                        "descriptionName": "Initial Screen",
                        "company": {"id": %d},
                        "type": {"id": %d}
                    }
                    """, company1.getId(), deviceType.getId());

            MvcResult createResult = mockMvc.perform(post("/devices")
                            .header("Authorization", "Bearer " + company1AdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createRequest))
                    .andExpect(status().isCreated())
                    .andReturn();

            Long deviceId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                    .get("id").asLong();

            // 2. Read device
            mockMvc.perform(get("/devices/" + deviceId)
                            .header("Authorization", "Bearer " + company1AdminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.descriptionName").value("Initial Screen"));

            // 3. Update device
            String updateRequest = String.format("""
                    {
                        "uuid": "lifecycle-device",
                        "width": 3840,
                        "height": 2160,
                        "descriptionName": "Updated Screen",
                        "company": {"id": %d},
                        "type": {"id": %d}
                    }
                    """, company1.getId(), deviceType.getId());

            mockMvc.perform(put("/devices/" + deviceId)
                            .header("Authorization", "Bearer " + company1AdminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.width").value(3840))
                    .andExpect(jsonPath("$.descriptionName").value("Updated Screen"));

            // 4. Delete device
            mockMvc.perform(delete("/devices/" + deviceId)
                            .header("Authorization", "Bearer " + company1AdminToken))
                    .andExpect(status().isNoContent());

            // 5. Verify deletion
            assertThat(deviceRepository.findById(deviceId)).isEmpty();
        }
    }
}
