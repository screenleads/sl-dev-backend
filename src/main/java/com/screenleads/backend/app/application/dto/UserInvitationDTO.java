package com.screenleads.backend.app.application.dto;

import com.screenleads.backend.app.domain.model.InvitationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInvitationDTO {
    private Long id;
    private String email;
    private Long invitedByUserId;
    private String invitedByName;
    private Long companyId;
    private String companyName;
    private Long roleId;
    private String roleName;
    private Integer roleLevel;
    private String token;
    private String customMessage;
    private InvitationStatus status;
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime acceptedAt;
    private boolean expired;
}
