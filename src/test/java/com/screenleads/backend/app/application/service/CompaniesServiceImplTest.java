package com.screenleads.backend.app.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.screenleads.backend.app.web.dto.CompanyDTO;
import com.screenleads.backend.app.web.dto.MediaSlimDTO;
import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.Media;
import com.screenleads.backend.app.domain.model.MediaType;
import com.screenleads.backend.app.domain.repositories.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompaniesServiceImpl Unit Tests")
class CompaniesServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private MediaTypeRepository mediaTypeRepository;

    @Mock
    private AdviceRepository adviceRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private CompaniesServiceImpl companiesService;

    private Company testCompany;
    private Media testLogo;
    private MediaType testMediaType;

    @BeforeEach
    void setUp() {
        testMediaType = MediaType.builder()
                .id(1L)
                .extension("png")
                .type("image/png")
                .enabled(true)
                .build();

        testLogo = Media.builder()
                .id(1L)
                .src("https://example.com/logo.png")
                .type(testMediaType)
                .build();

        testCompany = Company.builder()
                .id(1L)
                .name("Test Company")
                .observations("Test observations")
                .logo(testLogo)
                .primaryColor("#FF0000")
                .secondaryColor("#00FF00")
                .billingStatus(null)
                .devices(List.of())
                .advices(List.of())
                .build();
    }

    @Test
    @DisplayName("getAllCompanies should return all companies as DTOs")
    void whenGetAllCompanies_thenReturnsAllCompanies() {
        // Arrange
        when(companyRepository.findAll()).thenReturn(List.of(testCompany));

        // Act
        List<CompanyDTO> result = companiesService.getAllCompanies();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Test Company");
        verify(companyRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getCompanyById should return company when found")
    void whenGetCompanyByIdExists_thenReturnsCompany() {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));

        // Act
        Optional<CompanyDTO> result = companiesService.getCompanyById(1L);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Test Company");
        assertThat(result.get().observations()).isEqualTo("Test observations");
    }

    @Test
    @DisplayName("getCompanyById should return empty when not found")
    void whenGetCompanyByIdNotExists_thenReturnsEmpty() {
        // Arrange
        when(companyRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<CompanyDTO> result = companiesService.getCompanyById(999L);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("saveCompany should create new company without logo")
    void whenSaveCompanyWithoutLogo_thenCreatesCompany() {
        // Arrange
        CompanyDTO dto = new CompanyDTO(
                null, "New Company", "Observations", null, null,
                List.of(), List.of(), "#000000", "#FFFFFF",
                null, null, null, null);
        
        Company newCompany = Company.builder()
                .id(2L)
                .name("New Company")
                .observations("Observations")
                .billingStatus(null)
                .devices(List.of())
                .advices(List.of())
                .build();

        when(companyRepository.findByName("New Company")).thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenReturn(newCompany);

        // Act
        CompanyDTO result = companiesService.saveCompany(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("New Company");
        verify(companyRepository, times(2)).save(any(Company.class)); // Initial + after logo processing
    }

    @Test
    @DisplayName("saveCompany should be idempotent by company name")
    void whenSaveCompanyWithExistingName_thenReturnsExisting() {
        // Arrange
        CompanyDTO dto = new CompanyDTO(
                null, "Test Company", "New observations", null, null,
                List.of(), List.of(), "#000000", "#FFFFFF",
                null, null, null, null);

        when(companyRepository.findByName("Test Company")).thenReturn(Optional.of(testCompany));

        // Act
        CompanyDTO result = companiesService.saveCompany(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Test Company");
        verify(companyRepository, times(0)).save(any(Company.class)); // No save because already exists
    }

    @Test
    @DisplayName("saveCompany should link existing logo by ID")
    void whenSaveCompanyWithExistingLogoId_thenLinksLogo() {
        // Arrange
        MediaSlimDTO logoDTO = new MediaSlimDTO(1L, "https://example.com/logo.png", null, null, null);
        CompanyDTO dto = new CompanyDTO(
                null, "New Company", "Observations", logoDTO, 1L,
                List.of(), List.of(), "#000000", "#FFFFFF",
                null, null, null, null);

        Company newCompany = Company.builder()
                .id(2L)
                .name("New Company")
                .logo(testLogo)
                .billingStatus(null)
                .devices(List.of())
                .advices(List.of())
                .build();

        when(companyRepository.findByName("New Company")).thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenReturn(newCompany);
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(testLogo));

        // Act
        CompanyDTO result = companiesService.saveCompany(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.logo()).isNotNull();
        verify(mediaRepository, times(2)).findById(1L);
    }

    @Test
    @DisplayName("saveCompany should create new logo from src")
    void whenSaveCompanyWithLogoSrc_thenCreatesNewLogo() {
        // Arrange
        MediaSlimDTO logoDTO = new MediaSlimDTO(null, "https://example.com/newlogo.jpg", null, null, null);
        CompanyDTO dto = new CompanyDTO(
                null, "New Company", "Observations", logoDTO, null,
                List.of(), List.of(), "#000000", "#FFFFFF",
                null, null, null, null);

        Media newLogo = Media.builder()
                .id(2L)
                .src("https://example.com/newlogo.jpg")
                .type(testMediaType)
                .build();

        Company newCompany = Company.builder()
                .id(2L)
                .name("New Company")
                .logo(newLogo)
                .billingStatus(null)
                .devices(List.of())
                .advices(List.of())
                .build();

        when(companyRepository.findByName("New Company")).thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenReturn(newCompany);
        when(mediaRepository.save(any(Media.class))).thenReturn(newLogo);
        when(mediaTypeRepository.findByExtension(anyString())).thenReturn(Optional.of(testMediaType));

        // Act
        CompanyDTO result = companiesService.saveCompany(dto);

        // Assert
        assertThat(result).isNotNull();
        verify(mediaRepository, times(1)).save(any(Media.class));
    }

    @Test
    @DisplayName("updateCompany should update all fields")
    void whenUpdateCompany_thenUpdatesAllFields() {
        // Arrange
        CompanyDTO dto = new CompanyDTO(
                1L, "Updated Company", "Updated observations", null, null,
                List.of(), List.of(), "#111111", "#222222",
                "stripe_cust_123", "stripe_sub_123", "stripe_item_123",
                Company.BillingStatus.ACTIVE);

        when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));
        when(companyRepository.save(any(Company.class))).thenReturn(testCompany);

        // Act
        CompanyDTO result = companiesService.updateCompany(1L, dto);

        // Assert
        assertThat(result).isNotNull();
        verify(companyRepository, times(1)).save(testCompany);
        assertThat(testCompany.getName()).isEqualTo("Updated Company");
    }

    @Test
    @DisplayName("updateCompany should throw exception when not found")
    void whenUpdateCompanyNotExists_thenThrowsException() {
        // Arrange
        CompanyDTO dto = new CompanyDTO(
                999L, "Updated", "Obs", null, null, List.of(), List.of(),
                "#000", "#FFF", null, null, null, null);

        when(companyRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> companiesService.updateCompany(999L, dto))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("deleteCompany should delete existing company")
    void whenDeleteCompany_thenDeletes() {
        // Act
        companiesService.deleteCompany(1L);

        // Assert
        verify(companyRepository, times(1)).deleteById(1L);
    }
}
