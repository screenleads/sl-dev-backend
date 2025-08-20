package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.web.dto.RoleDTO;

public final class RoleMapper {
    private RoleMapper() {
    }

    public static RoleDTO toDTO(Role r) {
        if (r == null)
            return null;
        return new RoleDTO(r.getId(), r.getRole(), r.getDescription(), r.getLevel());
    }

    public static Role toEntity(RoleDTO dto) {
        if (dto == null)
            return null;
        Role r = new Role();
        r.setId(dto.id());
        r.setRole(dto.role());
        r.setDescription(dto.description());
        r.setLevel(dto.level());
        return r;
    }
}
