package com.screenleads.backend.app.web.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String name;
    private String lastName;
    private String username;
    private String email;
    private String password;
}