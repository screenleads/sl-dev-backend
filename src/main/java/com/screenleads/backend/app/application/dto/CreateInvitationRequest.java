package com.screenleads.backend.app.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvitationRequest {

    @NotBlank(message = "Email es obligatorio")
    @Email(message = "Email debe ser válido")
    private String email;

    @NotNull(message = "Role ID es obligatorio")
    private Long roleId;

    private String customMessage;
}
