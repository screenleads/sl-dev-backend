// com.screenleads.backend.app.web.mapper.RoleMapper
package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.web.dto.RoleDTO;

public class RoleMapper {

    public static RoleDTO toDTO(Role r) {
        if (r == null)
            return null;
        return new RoleDTO(
                r.getId(), r.getRole(), r.getDescription(), r.getLevel());
    }

    public static Role toEntity(RoleDTO d) {
        if (d == null)
            return null;
        return Role.builder()
                .id(d.id())
                .role(d.role())
                .description(d.description())
                .level(d.level())
                .build();
    }
}
