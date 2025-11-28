package com.screenleads.backend.app.domain.repositories;

import com.screenleads.backend.app.domain.model.ApiKey;
import com.screenleads.backend.app.domain.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
        // Obtener todas las ApiKeys por id de cliente
        java.util.List<ApiKey> findAllByClient_Id(Long clientId);
    Optional<ApiKey> findByKeyAndClientAndActiveTrue(String key, Client client);
    Optional<ApiKey> findByKey(String key);
}
