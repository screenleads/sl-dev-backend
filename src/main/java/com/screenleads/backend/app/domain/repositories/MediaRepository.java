package com.screenleads.backend.app.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.screenleads.backend.app.domain.model.Media;

public interface MediaRepository extends JpaRepository<Media, Long> {
}