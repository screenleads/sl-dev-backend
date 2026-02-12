package com.screenleads.backend.app.application.service;

import com.screenleads.backend.app.application.dto.AcceptInvitationRequest;
import com.screenleads.backend.app.application.dto.CreateInvitationRequest;
import com.screenleads.backend.app.application.dto.UserInvitationDTO;
import com.screenleads.backend.app.domain.model.User;

import java.util.List;

public interface UserInvitationService {
    
    UserInvitationDTO createInvitation(CreateInvitationRequest request, String inviterUsername);
    
    List<UserInvitationDTO> getCompanyInvitations(Long companyId);
    
    UserInvitationDTO getInvitationById(Long id);
    
    UserInvitationDTO verifyToken(String token);
    
    User acceptInvitation(AcceptInvitationRequest request);
    
    void cancelInvitation(Long id, String username);
    
    UserInvitationDTO resendInvitation(Long id, String username);
    
    void expireOldInvitations();
}
