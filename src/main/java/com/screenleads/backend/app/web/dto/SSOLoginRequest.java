package com.screenleads.backend.app.web.dto;

import lombok.Data;

@Data
public class SSOLoginRequest {
    private String email;
    private String displayName;
    private String photoURL;
    private String provider;
    private String firebaseUid;
    private String firebaseToken;
}