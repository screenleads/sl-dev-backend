package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.web.dto.UserDto;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public interface UserMapper {
    static UserDto toDto(User user) {
        List<String> roles = user.getRoles() != null
                ? user.getRoles().stream()
                      .map(Role::getRole)
                      .collect(Collectors.toList())
                : Collections.emptyList();

        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getName(),
                user.getLastName(),
                user.getCompany() != null ? user.getCompany().getId() : null,
                roles
        );
    }
}
