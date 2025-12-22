package com.screenleads.backend.app.web.mapper;

import com.screenleads.backend.app.domain.model.Company;
import com.screenleads.backend.app.domain.model.User;
import com.screenleads.backend.app.web.dto.CompanyRefDTO;
import com.screenleads.backend.app.web.dto.UserDto;

public class UserMapper {

    private UserMapper() {
    }

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
                .profileImage(buildProfileImageDTO(u.getProfileImage()))
                .role(u.getRole() != null ? new com.screenleads.backend.app.web.dto.RoleDTO(
                        u.getRole().getId(),
                        u.getRole().getRole(),
                        u.getRole().getDescription(),
                        u.getRole().getLevel()) : null)
                .build();
    }

    private static com.screenleads.backend.app.web.dto.MediaSlimDTO buildProfileImageDTO(
            com.screenleads.backend.app.domain.model.Media profileImage) {
        if (profileImage == null) {
            return null;
        }

        com.screenleads.backend.app.web.dto.MediaTypeDTO mediaTypeDTO = null;
        if (profileImage.getType() != null) {
            mediaTypeDTO = new com.screenleads.backend.app.web.dto.MediaTypeDTO(
                    profileImage.getType().getId(),
                    profileImage.getType().getExtension(),
                    profileImage.getType().getType(),
                    profileImage.getType().getEnabled());
        }

        return new com.screenleads.backend.app.web.dto.MediaSlimDTO(
                profileImage.getId(),
                profileImage.getSrc(),
                mediaTypeDTO,
                profileImage.getCreatedAt(),
                profileImage.getUpdatedAt());
    }

    private static CompanyRefDTO toCompanyRefDTO(Company company) {
        if (company == null) {
            return null;
        }
        return new CompanyRefDTO(company.getId(), company.getName());
    }
}
