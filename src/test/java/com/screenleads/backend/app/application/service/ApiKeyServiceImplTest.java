package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.domain.model.ApiKey;
import com.screenleads.backend.app.domain.model.Client;
import com.screenleads.backend.app.domain.repositories.ApiKeyRepository;
import com.screenleads.backend.app.domain.repositories.ClientRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyServiceImpl Unit Tests")
class ApiKeyServiceImplTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ApiKeyServiceImpl apiKeyService;

    @Nested
    @DisplayName("createApiKey() tests")
    class CreateApiKeyTests {

        @Test
        @DisplayName("Should create API key successfully for active client")
        void whenCreateApiKey_thenSuccess() {
            Client client = new Client();
            client.setClientId("client123");
            client.setActive(true);
            when(clientRepository.findByClientIdAndActiveTrue("client123")).thenReturn(Optional.of(client));

            ApiKey savedKey = new ApiKey();
            savedKey.setId(1L);
            savedKey.setKey("generated-key");
            savedKey.setActive(true);
            when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(savedKey);

            ApiKey result = apiKeyService.createApiKey("client123", "READ,WRITE", 30);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.isActive()).isTrue();
            
            ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
            verify(apiKeyRepository).save(captor.capture());
            ApiKey captured = captor.getValue();
            assertThat(captured.getClient()).isEqualTo(client);
            assertThat(captured.getPermissions()).isEqualTo("READ,WRITE");
            assertThat(captured.getKey()).isNotNull();
            assertThat(captured.getCreatedAt()).isNotNull();
            assertThat(captured.getExpiresAt()).isAfter(captured.getCreatedAt());
        }

        @Test
        @DisplayName("Should throw exception when client not found")
        void whenClientNotFound_thenThrowException() {
            when(clientRepository.findByClientIdAndActiveTrue("invalid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> apiKeyService.createApiKey("invalid", "READ", 30))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cliente no encontrado o inactivo");
        }

        @Test
        @DisplayName("Should set expiration date based on daysValid parameter")
        void whenCreateApiKey_thenSetExpirationCorrectly() {
            Client client = new Client();
            client.setClientId("client123");
            when(clientRepository.findByClientIdAndActiveTrue("client123")).thenReturn(Optional.of(client));
            when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

            LocalDateTime beforeCreation = LocalDateTime.now();
            ApiKey result = apiKeyService.createApiKey("client123", "READ", 90);
            LocalDateTime afterCreation = LocalDateTime.now();

            assertThat(result.getCreatedAt()).isBetween(beforeCreation, afterCreation);
            assertThat(result.getExpiresAt()).isAfter(result.getCreatedAt().plusDays(89));
            assertThat(result.getExpiresAt()).isBefore(result.getCreatedAt().plusDays(91));
        }
    }

    @Nested
    @DisplayName("createApiKeyByDbId() tests")
    class CreateApiKeyByDbIdTests {

        @Test
        @DisplayName("Should create API key by database ID successfully")
        void whenCreateByDbId_thenSuccess() {
            Client client = new Client();
            client.setId(1L);
            client.setClientId("client123");
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

            ApiKey savedKey = new ApiKey();
            savedKey.setId(10L);
            when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(savedKey);

            ApiKey result = apiKeyService.createApiKeyByDbId(1L, "ADMIN", 365);

            assertThat(result).isNotNull();
            verify(clientRepository).findById(1L);
            verify(apiKeyRepository).save(any(ApiKey.class));
        }

        @Test
        @DisplayName("Should throw exception when client DB ID not found")
        void whenClientDbIdNotFound_thenThrowException() {
            when(clientRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> apiKeyService.createApiKeyByDbId(999L, "READ", 30))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cliente no encontrado por id");
        }
    }

    @Nested
    @DisplayName("activate/deactivate API key tests")
    class ActivateDeactivateTests {

        @Test
        @DisplayName("Should deactivate API key successfully")
        void whenDeactivateApiKey_thenSetActiveFalse() {
            ApiKey key = new ApiKey();
            key.setId(1L);
            key.setActive(true);
            when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(key));
            when(apiKeyRepository.save(key)).thenReturn(key);

            apiKeyService.deactivateApiKey(1L);

            assertThat(key.isActive()).isFalse();
            verify(apiKeyRepository).save(key);
        }

        @Test
        @DisplayName("Should activate API key successfully")
        void whenActivateApiKey_thenSetActiveTrue() {
            ApiKey key = new ApiKey();
            key.setId(1L);
            key.setActive(false);
            when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(key));
            when(apiKeyRepository.save(key)).thenReturn(key);

            apiKeyService.activateApiKey(1L);

            assertThat(key.isActive()).isTrue();
            verify(apiKeyRepository).save(key);
        }

        @Test
        @DisplayName("Should handle non-existent ID gracefully on deactivate")
        void whenDeactivateNonExistent_thenNoException() {
            when(apiKeyRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatCode(() -> apiKeyService.deactivateApiKey(999L))
                    .doesNotThrowAnyException();
            verify(apiKeyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete and query API key tests")
    class DeleteAndQueryTests {

        @Test
        @DisplayName("Should delete API key by ID")
        void whenDeleteApiKey_thenCallRepository() {
            doNothing().when(apiKeyRepository).deleteById(1L);

            apiKeyService.deleteApiKey(1L);

            verify(apiKeyRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should retrieve all API keys for client")
        void whenGetApiKeysByClientDbId_thenReturnList() {
            ApiKey key1 = new ApiKey();
            key1.setId(1L);
            ApiKey key2 = new ApiKey();
            key2.setId(2L);
            when(apiKeyRepository.findAllByClient_Id(10L)).thenReturn(List.of(key1, key2));

            List<ApiKey> result = apiKeyService.getApiKeysByClientDbId(10L);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ApiKey::getId).containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("Should return empty list when no keys exist for client")
        void whenNoKeysForClient_thenReturnEmptyList() {
            when(apiKeyRepository.findAllByClient_Id(99L)).thenReturn(List.of());

            List<ApiKey> result = apiKeyService.getApiKeysByClientDbId(99L);

            assertThat(result).isEmpty();
        }
    }
}
