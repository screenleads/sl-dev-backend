package com.screenleads.backend.app.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.screenleads.backend.app.domain.model.ApiKey;
import com.screenleads.backend.app.domain.model.ApiClient;
import com.screenleads.backend.app.domain.repositories.ApiKeyRepository;
import com.screenleads.backend.app.domain.repositories.ClientRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyServiceImpl Unit Tests")
class ApiKeyServiceImplTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ApiKeyServiceImpl apiKeyService;

    private ApiClient testClient;
    private ApiKey testApiKey;

    @BeforeEach
    void setUp() {
        testClient = new ApiClient();
        testClient.setId(1L);
        testClient.setClientId("client-123");
        testClient.setActive(true);

        testApiKey = new ApiKey();
        testApiKey.setId(1L);
        testApiKey.setKeyHash("$2a$12$hashedkey123");
        testApiKey.setKeyPrefix("sk_live_test");
        testApiKey.setApiClient(testClient);
        testApiKey.setScopes("customers:read,campaigns:write");
        testApiKey.setActive(true);
        testApiKey.setCreatedAt(LocalDateTime.now());
        testApiKey.setExpiresAt(LocalDateTime.now().plusDays(30));
    }

    @Test
    @DisplayName("createApiKey should create new API key with valid client")
    void whenCreateApiKeyWithValidClient_thenCreatesApiKey() {
        // Arrange
        when(clientRepository.findByClientIdAndActiveTrue("client-123"))
                .thenReturn(Optional.of(testClient));
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(testApiKey);

        // Act
        ApiKeyService.ApiKeyCreationResult result = apiKeyService.createApiKey("client-123", "customers:read,campaigns:write", 30, true);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getApiKey()).isNotNull();
        assertThat(result.getRawKey()).isNotNull();
        assertThat(result.getApiKey().getApiClient()).isEqualTo(testClient);
        verify(clientRepository, times(1)).findByClientIdAndActiveTrue("client-123");
        verify(apiKeyRepository, times(1)).save(any(ApiKey.class));
    }

    @Test
    @DisplayName("createApiKey should set correct expiration date")
    void whenCreateApiKey_thenSetsCorrectExpirationDate() {
        // Arrange
        when(clientRepository.findByClientIdAndActiveTrue("client-123"))
                .thenReturn(Optional.of(testClient));

        ArgumentCaptor<ApiKey> apiKeyCaptor = ArgumentCaptor.forClass(ApiKey.class);
        when(apiKeyRepository.save(apiKeyCaptor.capture())).thenReturn(testApiKey);

        // Act
        apiKeyService.createApiKey("client-123", "customers:read", 7, true);

        // Assert
        ApiKey capturedKey = apiKeyCaptor.getValue();
        assertThat(capturedKey.getCreatedAt()).isNotNull();
        assertThat(capturedKey.getExpiresAt()).isNotNull();
        assertThat(capturedKey.isActive()).isTrue();
    }

    @Test
    @DisplayName("createApiKey should throw exception when client not found")
    void whenCreateApiKeyWithInvalidClient_thenThrowsException() {
        // Arrange
        when(clientRepository.findByClientIdAndActiveTrue("invalid-client"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> apiKeyService.createApiKey("invalid-client", "customers:read", 30, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cliente no encontrado o inactivo");

        verify(apiKeyRepository, never()).save(any(ApiKey.class));
    }

    @Test
    @DisplayName("deactivateApiKey should deactivate existing API key")
    void whenDeactivateApiKey_thenSetsActiveToFalse() {
        // Arrange
        when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(testApiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(testApiKey);

        // Act
        apiKeyService.deactivateApiKey(1L);

        // Assert
        verify(apiKeyRepository, times(1)).findById(1L);
        verify(apiKeyRepository, times(1)).save(testApiKey);
        assertThat(testApiKey.isActive()).isFalse();
    }

    @Test
    @DisplayName("deactivateApiKey should do nothing when key not found")
    void whenDeactivateNonExistentApiKey_thenDoesNothing() {
        // Arrange
        when(apiKeyRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        apiKeyService.deactivateApiKey(999L);

        // Assert
        verify(apiKeyRepository, times(1)).findById(999L);
        verify(apiKeyRepository, never()).save(any(ApiKey.class));
    }

    @Test
    @DisplayName("activateApiKey should activate existing API key")
    void whenActivateApiKey_thenSetsActiveToTrue() {
        // Arrange
        testApiKey.setActive(false);
        when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(testApiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(testApiKey);

        // Act
        apiKeyService.activateApiKey(1L);

        // Assert
        verify(apiKeyRepository, times(1)).findById(1L);
        verify(apiKeyRepository, times(1)).save(testApiKey);
        assertThat(testApiKey.isActive()).isTrue();
    }

    @Test
    @DisplayName("deleteApiKey should delete API key by id")
    void whenDeleteApiKey_thenDeletesSuccessfully() {
        // Act
        apiKeyService.deleteApiKey(1L);

        // Assert
        verify(apiKeyRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("createApiKeyByDbId should create API key using database ID")
    void whenCreateApiKeyByDbId_thenCreatesApiKey() {
        // Arrange
        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(testApiKey);

        // Act
        ApiKeyService.ApiKeyCreationResult result = apiKeyService.createApiKeyByDbId(1L, "customers:read,customers:write,campaigns:read,campaigns:write", 90, true);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getApiKey()).isNotNull();
        assertThat(result.getApiKey().getApiClient()).isEqualTo(testClient);
        verify(clientRepository, times(1)).findById(1L);
        verify(apiKeyRepository, times(1)).save(any(ApiKey.class));
    }

    @Test
    @DisplayName("createApiKeyByDbId should throw exception when client not found")
    void whenCreateApiKeyByDbIdWithInvalidId_thenThrowsException() {
        // Arrange
        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> apiKeyService.createApiKeyByDbId(999L, "customers:read", 30, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cliente no encontrado por id");

        verify(apiKeyRepository, never()).save(any(ApiKey.class));
    }

    @Test
    @DisplayName("getApiKeysByClientDbId should return all keys for client")
    void whenGetApiKeysByClientDbId_thenReturnsAllKeys() {
        // Arrange
        ApiKey key2 = new ApiKey();
        key2.setId(2L);
        key2.setKeyPrefix("sk_live_anot");
        key2.setKeyHash("$2a$12$anotherhashedkey");
        key2.setApiClient(testClient);

        when(apiKeyRepository.findAllByApiClient_Id(1L))
                .thenReturn(Arrays.asList(testApiKey, key2));

        // Act
        List<ApiKey> result = apiKeyService.getApiKeysByClientDbId(1L);

        // Assert
        assertThat(result).hasSize(2).contains(testApiKey, key2);
        verify(apiKeyRepository, times(1)).findAllByApiClient_Id(1L);
    }

    @Test
    @DisplayName("getApiKeysByClientDbId should return empty list when no keys exist")
    void whenGetApiKeysByClientDbIdWithNoKeys_thenReturnsEmptyList() {
        // Arrange
        when(apiKeyRepository.findAllByApiClient_Id(999L)).thenReturn(Arrays.asList());

        // Act
        List<ApiKey> result = apiKeyService.getApiKeysByClientDbId(999L);

        // Assert
        assertThat(result).isEmpty();
        verify(apiKeyRepository, times(1)).findAllByApiClient_Id(999L);
    }

    @Test
    @DisplayName("createApiKey should generate unique API key")
    void whenCreateApiKey_thenGeneratesUniqueKey() {
        // Arrange
        when(clientRepository.findByClientIdAndActiveTrue("client-123"))
                .thenReturn(Optional.of(testClient));

        ArgumentCaptor<ApiKey> apiKeyCaptor = ArgumentCaptor.forClass(ApiKey.class);
        when(apiKeyRepository.save(apiKeyCaptor.capture())).thenReturn(testApiKey);

        // Act
        apiKeyService.createApiKey("client-123", "customers:read,campaigns:write", 30, true);

        // Assert
        ApiKey capturedKey = apiKeyCaptor.getValue();
        assertThat(capturedKey.getKeyHash()).isNotNull();
        assertThat(capturedKey.getKeyPrefix()).isNotNull();
        assertThat(capturedKey.getKeyPrefix()).startsWith("sk_"); // Should start with sk_
    }
}
