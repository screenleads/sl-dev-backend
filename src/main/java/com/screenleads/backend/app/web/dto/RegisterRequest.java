// src/main/java/com/screenleads/backend/app/web/dto/RegisterRequest.java
package com.screenleads.backend.app.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    @NotBlank
    private String username;
    @Email
    private String email;
    @NotBlank
    private String password;
    private String name;
    private String lastName;
    private Long companyId; // opcional: si lo usas al registrar
}
