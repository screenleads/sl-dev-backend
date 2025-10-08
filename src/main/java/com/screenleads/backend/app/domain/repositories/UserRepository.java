package com.screenleads.backend.app.domain.repositories;

import com.screenleads.backend.app.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"company", "profileImage"})
    Optional<User> findWithCompanyAndProfileImageByUsername(String username);

    boolean existsByUsername(String username); // 👈 añadir

    boolean existsByEmail(String email);
}