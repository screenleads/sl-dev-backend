package com.screenleads.backend.app.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptInvitationRequest {

    @NotBlank(message = "Token es obligatorio")
    private String token;

    @NotBlank(message = "Email es obligatorio")
    @Email(message = "Email debe ser válido")
    private String email;

    @NotBlank(message = "Nombre es obligatorio")
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank(message = "Apellido es obligatorio")
    @Size(min = 2, max = 100)
    private String lastName;

    @NotBlank(message = "Username es obligatorio")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "Contraseña es obligatoria")
    @Size(min = 6, message = "Contraseña debe tener al menos 6 caracteres")
    private String password;
}
