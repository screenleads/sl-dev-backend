package com.screenleads.backend.app.domain.repositories;

import com.screenleads.backend.app.domain.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByClientIdAndActiveTrue(String clientId);
    Optional<Client> findByClientId(String clientId);
}
