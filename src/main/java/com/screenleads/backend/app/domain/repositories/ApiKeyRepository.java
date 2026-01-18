package com.screenleads.backend.app.domain.repositories;

import com.screenleads.backend.app.domain.model.ApiKey;
import com.screenleads.backend.app.domain.model.ApiClient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    // Obtener todas las ApiKeys por id de cliente
    java.util.List<ApiKey> findAllByApiClient_Id(Long clientId);

    Optional<ApiKey> findByKeyAndApiClientAndActiveTrue(String key, ApiClient apiClient);

    Optional<ApiKey> findByKey(String key);
}
