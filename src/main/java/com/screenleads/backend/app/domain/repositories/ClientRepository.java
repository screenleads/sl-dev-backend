package com.screenleads.backend.app.domain.repositories;

import com.screenleads.backend.app.domain.model.ApiClient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<ApiClient, Long> {
    Optional<ApiClient> findByClientIdAndActiveTrue(String clientId);

    Optional<ApiClient> findByClientId(String clientId);
}
