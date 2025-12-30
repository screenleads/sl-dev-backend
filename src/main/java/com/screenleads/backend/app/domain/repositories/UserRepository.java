package com.screenleads.backend.app.domain.repositories;

import com.screenleads.backend.app.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findByEmail(@Param("email") String email);

    @EntityGraph(attributePaths = { "company", "profileImage" })
    Optional<User> findWithCompanyAndProfileImageByUsername(String username);

    boolean existsByUsername(String username); // 👈 añadir

    boolean existsByEmail(String email);
}