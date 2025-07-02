package com.screenleads.backend.app.domain.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.screenleads.backend.app.domain.model.MediaType;

public interface MediaTypeRepository extends JpaRepository<MediaType, Long> {
    Optional<MediaType> findByExtension(String extension);

    Optional<MediaType> findByType(String type);
}