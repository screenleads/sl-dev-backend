package com.screenleads.backend.app.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;


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
    private RoleDTO role; // Ahora es objeto

    // Nuevo: referencia a compañía como objeto
    private CompanyRefDTO company;

    // Nuevo: imagen de perfil como objeto slim
    private MediaSlimDTO profileImage;

    // solo para crear/actualizar; no lo rellenes al responder
    private String password;
}
