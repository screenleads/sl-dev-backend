package com.screenleads.backend.app.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor // ← necesario para deserializar POST/PUT
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String email;
    private String username;
    private String password;
    private String name;
    private String lastName;
    private Long companyId; // sólo id de la empresa
    private List<String> roles; // nombres de rol, p.e. ["ROLE_ADMIN","ROLE_COMPANY_VIEWER"]
}
