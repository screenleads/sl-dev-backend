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
                .company(u.getCompany() != null ? new com.screenleads.backend.app.web.dto.CompanyRefDTO(u.getCompany().getId(), u.getCompany().getName()) : null)
        .profileImage(u.getProfileImage() != null ? new com.screenleads.backend.app.web.dto.MediaSlimDTO(
            u.getProfileImage().getId(),
            u.getProfileImage().getSrc(),
            u.getProfileImage().getType() != null ? new com.screenleads.backend.app.web.dto.MediaTypeDTO(
                u.getProfileImage().getType().getId(),
                u.getProfileImage().getType().getExtension(),
                u.getProfileImage().getType().getType(),
                u.getProfileImage().getType().getEnabled()
            ) : null,
            u.getProfileImage().getCreatedAt(),
            u.getProfileImage().getUpdatedAt()
        ) : null)
                .role(u.getRole())
                .build();
    }
}
