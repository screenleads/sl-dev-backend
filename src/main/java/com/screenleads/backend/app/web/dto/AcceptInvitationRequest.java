// src/main/java/com/screenleads/backend/app/web/dto/AcceptInvitationRequest.java
package com.screenleads.backend.app.web.dto;

import lombok.Data;

@Data
public class AcceptInvitationRequest {
    private String token;
    private String email;
    private String name;
    private String lastName;
    private String username;
    private String password;
}
