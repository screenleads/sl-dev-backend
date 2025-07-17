package com.screenleads.backend.app.web.dto;

import lombok.Data;

@Data
public class UserDto {
    private String id;
    private String username;
    private String name;
    private String lastName;
    private String email;
}