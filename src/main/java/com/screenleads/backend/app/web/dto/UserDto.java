package com.screenleads.backend.app.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String name;
    private String lastName;
    private Long companyId;
    private List<String> roles;

    // solo para crear/actualizar; no lo rellenes al responder
    private String password;
}
