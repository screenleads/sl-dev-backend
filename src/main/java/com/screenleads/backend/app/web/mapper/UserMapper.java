package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.web.dto.CompanyRefDTO;
import com.screenleads.backend.app.web.dto.UserDto;

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
                .company(toCompanyRefDTO(u.getCompany()))
                .profileImage(u.getProfileImage() != null ? new com.screenleads.backend.app.web.dto.MediaSlimDTO(
                        u.getProfileImage().getId(),
                        u.getProfileImage().getSrc(),
                        u.getProfileImage().getType() != null ? new com.screenleads.backend.app.web.dto.MediaTypeDTO(
                                u.getProfileImage().getType().getId(),
                                u.getProfileImage().getType().getExtension(),
                                u.getProfileImage().getType().getType(),
                                u.getProfileImage().getType().getEnabled()) : null,
                        u.getProfileImage().getCreatedAt(),
                        u.getProfileImage().getUpdatedAt()) : null)
                .role(u.getRole() != null ? new com.screenleads.backend.app.web.dto.RoleDTO(
                        u.getRole().getId(),
                        u.getRole().getRole(),
                        u.getRole().getDescription(),
                        u.getRole().getLevel()) : null)
                .build();
    }

    private static CompanyRefDTO toCompanyRefDTO(Company company) {
        if (company == null) {
            return null;
        }
        return new CompanyRefDTO(company.getId(), company.getName());
    }
}
