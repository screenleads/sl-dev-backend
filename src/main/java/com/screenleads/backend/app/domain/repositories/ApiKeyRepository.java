package com.screenleads.backend.app.domain.repositories;

import com.screenleads.backend.app.domain.model.ApiKey;
import com.screenleads.backend.app.domain.model.ApiClient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    // Obtener todas las ApiKeys por id de cliente
    List<ApiKey> findAllByApiClient_Id(Long clientId);

    // Buscar por prefijo de key y cliente (para luego verificar el hash)
    Optional<ApiKey> findByKeyPrefixAndApiClientAndActiveTrue(String keyPrefix, ApiClient apiClient);

    // Buscar por prefijo solamente
    Optional<ApiKey> findByKeyPrefix(String keyPrefix);

    // Buscar por hash de key (uso interno si necesario)
    Optional<ApiKey> findByKeyHash(String keyHash);
}
