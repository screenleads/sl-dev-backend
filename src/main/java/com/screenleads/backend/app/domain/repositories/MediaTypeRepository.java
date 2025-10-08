package com.screenleads.backend.app.domain.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.screenleads.backend.app.domain.model.MediaType;

public interface MediaTypeRepository extends JpaRepository<MediaType, Long> {
    Optional<MediaType> findByExtension(String extension);

    Optional<MediaType> findByType(String type);

    boolean existsByExtension(String extension);

    boolean existsByType(String type);

    @Query("SELECT m FROM media_type m WHERE LOWER(TRIM(m.extension)) = LOWER(TRIM(:extension))")
    Optional<MediaType> findByExtensionIgnoreCase(@Param("extension") String extension);
}