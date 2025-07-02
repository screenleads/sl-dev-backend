package com.screenleads.backend.app.web.dto;

import com.screenleads.backend.app.domain.model.MediaType;

public record MediaDTO(Long id, String src, MediaType type) {
}
