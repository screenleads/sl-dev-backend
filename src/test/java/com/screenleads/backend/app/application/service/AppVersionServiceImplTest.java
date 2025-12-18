package com.screenleads.backend.app.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.screenleads.backend.app.domain.model.AppVersion;
import com.screenleads.backend.app.domain.repositories.AppVersionRepository;
import com.screenleads.backend.app.web.dto.AppVersionDTO;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppVersionServiceImpl Unit Tests")
class AppVersionServiceImplTest {

    @Mock
    private AppVersionRepository repository;

    @InjectMocks
    private AppVersionServiceImpl appVersionService;

    private AppVersion testAppVersion;

    @BeforeEach
    void setUp() {
        testAppVersion = AppVersion.builder()
                .id(1L)
                .platform("Android")
                .version("1.2.3")
                .message("Update available")
                .url("https://example.com/update")
                .forceUpdate(false)
                .build();

        testAppVersionDTO = AppVersionDTO.builder()
                .id(1L)
                .platform("Android")
                .version("1.2.3")
                .message("Update available")
                .url("https://example.com/update")
                .forceUpdate(false)
                .build();
    }

    @Test
    @DisplayName("save should create new app version")
    void whenSave_thenReturnsAppVersionDTO() {
        // Arrange
        AppVersionDTO inputDTO = AppVersionDTO.builder()
                .platform("iOS")
                .version("2.0.0")
                .message("Major update")
                .url("https://example.com/ios-update")
                .forceUpdate(true)
                .build();

        AppVersion savedEntity = AppVersion.builder()
                .id(10L)
                .platform("iOS")
                .version("2.0.0")
                .message("Major update")
                .url("https://example.com/ios-update")
                .forceUpdate(true)
                .build();

        when(repository.save(any(AppVersion.class))).thenReturn(savedEntity);

        // Act
        AppVersionDTO result = appVersionService.save(inputDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getPlatform()).isEqualTo("iOS");
        assertThat(result.getVersion()).isEqualTo("2.0.0");
        assertThat(result.isForceUpdate()).isTrue();
        verify(repository, times(1)).save(any(AppVersion.class));
    }

    @Test
    @DisplayName("findAll should return all app versions")
    void whenFindAll_thenReturnsAllVersions() {
        // Arrange
        AppVersion version2 = AppVersion.builder()
                .id(2L)
                .platform("iOS")
                .version("1.5.0")
                .message("iOS update")
                .url("https://example.com/ios")
                .forceUpdate(false)
                .build();

        when(repository.findAll()).thenReturn(Arrays.asList(testAppVersion, version2));

        // Act
        List<AppVersionDTO> result = appVersionService.findAll();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPlatform()).isEqualTo("Android");
        assertThat(result.get(1).getPlatform()).isEqualTo("iOS");
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("findAll should return empty list when no versions exist")
    void whenFindAllEmpty_thenReturnsEmptyList() {
        // Arrange
        when(repository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<AppVersionDTO> result = appVersionService.findAll();

        // Assert
        assertThat(result).isEmpty();
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("findById should return app version when found")
    void whenFindByIdExists_thenReturnsAppVersion() {
        // Arrange
        when(repository.findById(1L)).thenReturn(Optional.of(testAppVersion));

        // Act
        AppVersionDTO result = appVersionService.findById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getPlatform()).isEqualTo("Android");
        assertThat(result.getVersion()).isEqualTo("1.2.3");
        verify(repository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("findById should throw exception when not found")
    void whenFindByIdNotExists_thenThrowsException() {
        // Arrange
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appVersionService.findById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("AppVersion not found with id 999");

        verify(repository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("deleteById should delete app version")
    void whenDeleteById_thenDeletesSuccessfully() {
        // Act
        appVersionService.deleteById(1L);

        // Assert
        verify(repository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("getLatestVersion should return latest version for platform")
    void whenGetLatestVersionExists_thenReturnsLatestVersion() {
        // Arrange
        when(repository.findTopByPlatformOrderByIdDesc("Android"))
                .thenReturn(Optional.of(testAppVersion));

        // Act
        AppVersionDTO result = appVersionService.getLatestVersion("Android");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPlatform()).isEqualTo("Android");
        assertThat(result.getVersion()).isEqualTo("1.2.3");
        verify(repository, times(1)).findTopByPlatformOrderByIdDesc("Android");
    }

    @Test
    @DisplayName("getLatestVersion should throw exception when no version for platform")
    void whenGetLatestVersionNotExists_thenThrowsException() {
        // Arrange
        when(repository.findTopByPlatformOrderByIdDesc("Windows"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appVersionService.getLatestVersion("Windows"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No version found for platform Windows");

        verify(repository, times(1)).findTopByPlatformOrderByIdDesc("Windows");
    }

    @Test
    @DisplayName("save should handle force update flag correctly")
    void whenSaveWithForceUpdateTrue_thenSavesCorrectly() {
        // Arrange
        AppVersionDTO inputDTO = AppVersionDTO.builder()
                .platform("Android")
                .version("3.0.0")
                .message("Critical update - forced")
                .url("https://example.com/critical")
                .forceUpdate(true)
                .build();

        AppVersion savedEntity = AppVersion.builder()
                .id(20L)
                .platform("Android")
                .version("3.0.0")
                .message("Critical update - forced")
                .url("https://example.com/critical")
                .forceUpdate(true)
                .build();

        when(repository.save(any(AppVersion.class))).thenReturn(savedEntity);

        // Act
        AppVersionDTO result = appVersionService.save(inputDTO);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isForceUpdate()).isTrue();
        assertThat(result.getMessage()).contains("Critical");
        verify(repository, times(1)).save(any(AppVersion.class));
    }

    @Test
    @DisplayName("findAll should correctly convert all entity fields to DTO")
    void whenFindAll_thenConvertsAllFieldsCorrectly() {
        // Arrange
        when(repository.findAll()).thenReturn(Collections.singletonList(testAppVersion));

        // Act
        List<AppVersionDTO> result = appVersionService.findAll();

        // Assert
        assertThat(result).hasSize(1);
        AppVersionDTO dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(testAppVersion.getId());
        assertThat(dto.getPlatform()).isEqualTo(testAppVersion.getPlatform());
        assertThat(dto.getVersion()).isEqualTo(testAppVersion.getVersion());
        assertThat(dto.getMessage()).isEqualTo(testAppVersion.getMessage());
        assertThat(dto.getUrl()).isEqualTo(testAppVersion.getUrl());
        assertThat(dto.isForceUpdate()).isEqualTo(testAppVersion.isForceUpdate());
    }
}
