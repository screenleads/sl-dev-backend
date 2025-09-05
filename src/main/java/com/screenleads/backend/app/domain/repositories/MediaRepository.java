package com.screenleads.backend.app.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import com.screenleads.backend.app.domain.model.Media;

public interface MediaRepository extends JpaRepository<Media, Long> {
    Optional<Media> findBySrc(String src); // útil si src es único
}