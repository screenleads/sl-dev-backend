// src/main/java/com/screenleads/backend/app/web/dto/PasswordChangeRequest.java
package com.screenleads.backend.app.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordChangeRequest {
    @NotBlank
    private String currentPassword;
    @NotBlank
    private String newPassword;
}
