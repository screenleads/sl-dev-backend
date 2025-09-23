package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.web.dto.UserDto;

import java.util.stream.Collectors;

public class UserMapper {
    public static UserDto toDto(User u) {
        if (u == null)
            return null;
        return UserDto.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .name(u.getName())
                .lastName(u.getLastName())
                .companyId(u.getCompany() != null ? u.getCompany().getId() : null)
                .roles(u.getRoles() != null
                        ? u.getRoles().stream().map(Role::getRole).collect(Collectors.toList())
                        : null)
                .build();
    }
}
