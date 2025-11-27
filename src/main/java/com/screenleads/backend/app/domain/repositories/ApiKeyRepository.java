package com.screenleads.backend.app.domain.repositories;

import com.screenleads.backend.app.domain.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByKeyAndClientIdAndActiveTrue(String key, String clientId);
    Optional<ApiKey> findByKey(String key);
}
