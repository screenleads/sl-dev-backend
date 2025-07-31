package com.screenleads.backend.app.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.util.List;
@Data
@AllArgsConstructor
public class UserDto {
 private Long id;
    private String email;
    private String username;
    private String name;
    private String lastName;
    private Long companyId;
    private List<String> roles;
}