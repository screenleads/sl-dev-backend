package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.Role;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.web.dto.UserDto;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class UserMapper {

    public static UserDto toDto(User u) {
        if (u == null)
            return null;

        Long companyId = (u.getCompany() != null) ? u.getCompany().getId() : null;

        List<String> roleNames = (u.getRoles() != null)
                ? u.getRoles().stream().map(Role::getRole).collect(Collectors.toList())
                : Collections.emptyList();

        return new UserDto(
                u.getId(),
                u.getEmail(),
                u.getUsername(),
                u.getPassword(),
                u.getName(),
                u.getLastName(),
                companyId,
                roleNames);
    }
}
