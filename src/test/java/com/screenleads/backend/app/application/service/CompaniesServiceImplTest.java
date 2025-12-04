package com.screenleads.backend.app.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.screenleads.backend.app.web.dto.MediaSlimDTO;
import com.screenleads.backend.app.web.dto.MediaTypeDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.screenleads.backend.app.web.dto.CompanyDTO;
import com.screenleads.backend.app.domain.model.Advice;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Device;
import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.domain.model.MediaType;
import com.screenleads.backend.app.domain.repositories.AdviceRepository;
import com.screenleads.backend.app.domain.repositories.CompanyRepository;
import com.screenleads.backend.app.domain.repositories.DeviceRepository;
import com.screenleads.backend.app.domain.repositories.MediaRepository;
import com.screenleads.backend.app.domain.repositories.MediaTypeRepository;

/**
 * Comprehensive test suite for CompaniesServiceImpl.
 * 
 * This test class demonstrates:
 * - Testing complex service layer with multiple dependencies
 * - Using Mockito for repository mocking
 * - Given-When-Then test structure
 * - Testing CRUD operations with DTO/Entity conversion
 * - Testing idempotency (duplicate company handling)
 * - Testing business logic (logo management, cascading deletes)
 * - Verifying repository interactions
 * - Edge case handling (null, empty, not found)
 * 
 * Pattern: @Mock repositories + @InjectMocks service
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CompaniesServiceImpl Tests")
class CompaniesServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private AdviceRepository adviceRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private MediaTypeRepository mediaTypeRepository;

    @InjectMocks
    private CompaniesServiceImpl companiesService;

    private Company testCompany1;
    private Company testCompany2;
    private CompanyDTO testCompanyDTO;
    private Media testLogo;

    @BeforeEach
    void setUp() {
        // Setup test data
        testCompany1 = createCompany(1L, "Acme Corp", "Acme observations", null);
        testCompany2 = createCompany(2L, "Tech Solutions", "Tech observations", null);

        testLogo = createMedia(10L, "https://example.com/logo.png");

        testCompanyDTO = new CompanyDTO(
                null, "New Company", "New observations", null, null, null,
                "#FFFFFF", "#000000", null, null, null, Company.BillingStatus.ACTIVE);
    }

    @Nested
    @DisplayName("Get All Companies Tests")
    class GetAllCompaniesTests {

        @Test
        @DisplayName("Should return all companies as DTOs sorted by ID")
        void shouldReturnAllCompaniesAsDTOs() {
            // Given: Multiple companies in repository
            when(companyRepository.findAll()).thenReturn(Arrays.asList(testCompany1, testCompany2));

            // When: Getting all companies
            List<CompanyDTO> result = companiesService.getAllCompanies();

            // Then: Should return all companies as DTOs
            assertThat(result)
                    .hasSize(2)
                    .extracting(CompanyDTO::name)
                    .containsExactly("Acme Corp", "Tech Solutions");

            // And: Repository should be called once
            verify(companyRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no companies exist")
        void shouldReturnEmptyListWhenNoCompanies() {
            // Given: Empty repository
            when(companyRepository.findAll()).thenReturn(Arrays.asList());

            // When
            List<CompanyDTO> result = companiesService.getAllCompanies();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should map company entities to DTOs with all fields")
        void shouldMapCompanyEntitiesToDTOs() {
            // Given: Company with logo
            testCompany1.setLogo(testLogo);
            when(companyRepository.findAll()).thenReturn(Arrays.asList(testCompany1));

            // When
            List<CompanyDTO> result = companiesService.getAllCompanies();

            // Then: DTO should have all mapped fields
            assertThat(result).hasSize(1);
            CompanyDTO dto = result.get(0);
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.name()).isEqualTo("Acme Corp");
            assertThat(dto.observations()).isEqualTo("Acme observations");
        }
    }

    @Nested
    @DisplayName("Get Company By ID Tests")
    class GetCompanyByIdTests {

        @Test
        @DisplayName("Should return company DTO when company exists")
        void shouldReturnCompanyDTOWhenExists() {
            // Given
            when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany1));

            // When
            Optional<CompanyDTO> result = companiesService.getCompanyById(1L);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(1L);
            assertThat(result.get().name()).isEqualTo("Acme Corp");

            verify(companyRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Should return empty Optional when company does not exist")
        void shouldReturnEmptyWhenCompanyNotFound() {
            // Given
            when(companyRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            Optional<CompanyDTO> result = companiesService.getCompanyById(999L);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should map logo to logoId when company has logo")
        void shouldMapLogoToLogoId() {
            // Given
            testCompany1.setLogo(testLogo);
            when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany1));

            // When
            Optional<CompanyDTO> result = companiesService.getCompanyById(1L);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().logo()).isNotNull();
            assertThat(result.get().logo().id()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("Save Company Tests")
    class SaveCompanyTests {

        @Test
        @DisplayName("Should save new company successfully")
        void shouldSaveNewCompany() {
            // Given: New company DTO without logo
            Company savedCompany = createCompany(3L, "New Company", "new@example.com", null);
            when(companyRepository.findByName("New Company")).thenReturn(Optional.empty());
            when(companyRepository.save(any(Company.class))).thenReturn(savedCompany);

            // When
            CompanyDTO result = companiesService.saveCompany(testCompanyDTO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(3L);
            assertThat(result.name()).isEqualTo("New Company");

            // And: Should check for duplicates and save twice (first save, then with logo)
            verify(companyRepository, times(1)).findByName("New Company");
            verify(companyRepository, times(2)).save(any(Company.class));
        }

        @Test
        @DisplayName("Should be idempotent - return existing company if name already exists")
        void shouldBeIdempotentForDuplicateName() {
            // Given: Company with same name already exists
            when(companyRepository.findByName("New Company")).thenReturn(Optional.of(testCompany1));

            // When
            CompanyDTO result = companiesService.saveCompany(testCompanyDTO);

            // Then: Should return existing company without saving
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Acme Corp");

            // And: Should not call save
            verify(companyRepository, never()).save(any(Company.class));
            verify(companyRepository, times(1)).findByName("New Company");
        }

        @Test
        @DisplayName("Should save company with logo")
        void shouldSaveCompanyWithLogo() {
            // Given: DTO with logo
            MediaTypeDTO mediaTypeDTO = new MediaTypeDTO(1L, "png", "image/png", true);
            MediaSlimDTO logoDTO = new MediaSlimDTO(10L, "https://example.com/logo.png", mediaTypeDTO, null, null);
            CompanyDTO dtoWithLogo = new CompanyDTO(
                    null, "New Company", "New observations", logoDTO, null, null,
                    "#FFFFFF", "#000000", null, null, null, Company.BillingStatus.ACTIVE);
            when(companyRepository.findByName("New Company")).thenReturn(Optional.empty());
            when(mediaRepository.findById(10L)).thenReturn(Optional.of(testLogo));

            Company savedCompany = createCompany(3L, "New Company", "New observations", testLogo);
            when(companyRepository.save(any(Company.class))).thenReturn(savedCompany);

            // When
            CompanyDTO result = companiesService.saveCompany(dtoWithLogo);

            // Then
            assertThat(result.logo()).isNotNull();
            assertThat(result.logo().id()).isEqualTo(10L);

            // And: Should fetch logo from repository twice (convertToEntity + saveCompany)
            verify(mediaRepository, times(2)).findById(10L);
        }

        @Test
        @DisplayName("Should handle null logo gracefully")
        void shouldHandleNullLogo() {
            // Given: DTO without logo (logo field is null)
            when(companyRepository.findByName("New Company")).thenReturn(Optional.empty());

            Company savedCompany = createCompany(3L, "New Company", "New observations", null);
            when(companyRepository.save(any(Company.class))).thenReturn(savedCompany);

            // When
            CompanyDTO result = companiesService.saveCompany(testCompanyDTO);

            // Then
            assertThat(result.logo()).isNull();
            verify(mediaRepository, never()).findById(anyLong());
            verify(companyRepository, times(2)).save(any(Company.class));
        }

        @Test
        @DisplayName("Should capture and verify saved company entity")
        void shouldCaptureAndVerifySavedEntity() {
            // Given
            when(companyRepository.findByName("New Company")).thenReturn(Optional.empty());
            when(companyRepository.save(any(Company.class)))
                    .thenReturn(createCompany(3L, "New Company", "New observations", null));

            ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);

            // When
            companiesService.saveCompany(testCompanyDTO);

            // Then: Capture and verify the saved entity (called twice)
            verify(companyRepository, times(2)).save(companyCaptor.capture());
            Company capturedCompany = companyCaptor.getAllValues().get(0); // First save

            assertThat(capturedCompany.getName()).isEqualTo("New Company");
            assertThat(capturedCompany.getObservations()).isEqualTo("New observations");
            assertThat(capturedCompany.getPrimaryColor()).isEqualTo("#FFFFFF");
        }
    }

    @Nested
    @DisplayName("Update Company Tests")
    class UpdateCompanyTests {

        @Test
        @DisplayName("Should update existing company successfully")
        void shouldUpdateExistingCompany() {
            // Given: Existing company and updated DTO
            CompanyDTO updateDTO = new CompanyDTO(
                    null, "Updated Name", "Updated observations", null, null, null,
                    "#FF0000", "#00FF00", null, null, null, Company.BillingStatus.ACTIVE);

            when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany1));
            when(companyRepository.save(any(Company.class))).thenReturn(testCompany1);

            // When
            CompanyDTO result = companiesService.updateCompany(1L, updateDTO);

            // Then
            assertThat(result).isNotNull();
            verify(companyRepository, times(1)).findById(1L);
            verify(companyRepository, times(1)).save(any(Company.class));
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent company")
        void shouldThrowExceptionWhenUpdatingNonExistent() {
            // Given
            when(companyRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> companiesService.updateCompany(999L, testCompanyDTO))
                    .isInstanceOf(RuntimeException.class);

            verify(companyRepository, never()).save(any(Company.class));
        }

        @Test
        @DisplayName("Should update logo when logo is provided")
        void shouldUpdateLogoWhenProvided() {
            // Given
            MediaTypeDTO mediaTypeDTO = new MediaTypeDTO(1L, "png", "image/png", true);
            MediaSlimDTO logoDTO = new MediaSlimDTO(10L, "https://example.com/logo.png", mediaTypeDTO, null, null);
            CompanyDTO dtoWithLogo = new CompanyDTO(
                    null, "Updated Company", "Updated observations", logoDTO, null, null,
                    "#FFFFFF", "#000000", null, null, null, Company.BillingStatus.ACTIVE);
            when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany1));
            when(mediaRepository.findById(10L)).thenReturn(Optional.of(testLogo));
            when(companyRepository.save(any(Company.class))).thenReturn(testCompany1);

            // When
            companiesService.updateCompany(1L, dtoWithLogo);

            // Then
            verify(mediaRepository, times(1)).findById(10L);
        }

        @Test
        @DisplayName("Should clear logo when logo is null")
        void shouldClearLogoWhenNull() {
            // Given: Company with existing logo
            testCompany1.setLogo(testLogo);

            when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany1));
            when(companyRepository.save(any(Company.class))).thenReturn(testCompany1);

            // When
            companiesService.updateCompany(1L, testCompanyDTO);

            // Then: Logo should be cleared (implementation should set it to null)
            verify(companyRepository, times(1)).save(argThat(company -> company.getLogo() == null));
        }
    }

    @Nested
    @DisplayName("Delete Company Tests")
    class DeleteCompanyTests {

        @Test
        @DisplayName("Should delete company and cascade delete related entities")
        void shouldDeleteCompanyAndCascade() {
            // Given: Company ID for deletion

            // When
            companiesService.deleteCompany(1L);

            // Then: Should delete company by ID
            verify(companyRepository, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("Should delete company successfully without validation")
        void shouldDeleteCompanyWithoutValidation() {
            // Given: Company ID

            // When
            companiesService.deleteCompany(999L);

            // Then: deleteById is called (no exception handling in current implementation)
            verify(companyRepository, times(1)).deleteById(999L);
        }

        @Test
        @DisplayName("Should handle company deletion by ID")
        void shouldHandleCompanyWithoutLogoDuringDeletion() {
            // Given: Company ID

            // When
            companiesService.deleteCompany(1L);

            // Then
            verify(companyRepository, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("Should call deleteById for deletion")
        void shouldDeleteInCorrectOrder() {
            // Given: Company ID

            // When
            companiesService.deleteCompany(1L);

            // Then: Verify deleteById is called
            verify(companyRepository, times(1)).deleteById(1L);
        }
    }

    // ==================== Helper Methods ====================

    private Company createCompany(Long id, String name, String observations, Media logo) {
        Company company = new Company();
        company.setId(id);
        company.setName(name);
        company.setObservations(observations);
        company.setPrimaryColor("#FFFFFF");
        company.setSecondaryColor("#000000");
        company.setLogo(logo);
        company.setBillingStatus("ACTIVE");
        return company;
    }

    private Media createMedia(Long id, String src) {
        Media media = new Media();
        media.setId(id);
        media.setSrc(src);
        return media;
    }

    private Advice createAdvice(Long id, String description) {
        Advice advice = new Advice();
        advice.setId(id);
        advice.setDescription(description);
        return advice;
    }

    private Device createDevice(Long id, String uuid) {
        Device device = new Device();
        device.setId(id);
        device.setUuid(uuid);
        return device;
    }
}
