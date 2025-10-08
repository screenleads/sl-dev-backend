// com.screenleads.backend.app.web.dto.RoleDTO
package com.screenleads.backend.app.web.dto;

public record RoleDTO(
        Long id,
        String role,
        String description,
        Integer level) {
}
