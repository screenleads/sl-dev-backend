package com.screenleads.backend.app.web.dto;

public record CompanyRefDTO(Long id, String name) {
    public CompanyRefDTO(Long id) {
        this(id, null);
    }
}
